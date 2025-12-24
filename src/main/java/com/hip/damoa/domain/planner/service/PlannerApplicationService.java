package com.hip.damoa.domain.planner.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.model.PlannerApplication;
import com.hip.damoa.domain.planner.model.PlannerApplicationAttachment;
import com.hip.damoa.domain.planner.model.PlannerPreferredDate;
import com.hip.damoa.domain.planner.repository.PlannerApplicationAttachmentRepository;
import com.hip.damoa.domain.planner.repository.PlannerApplicationRepository;
import com.hip.damoa.domain.planner.repository.PlannerPreferredDateRepository;
import com.hip.damoa.domain.planner.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 플래너 신청서 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerApplicationService {

    private final PlannerApplicationRepository plannerApplicationRepository;
    private final PlannerApplicationAttachmentRepository attachmentRepository;
    private final PlannerPreferredDateRepository preferredDateRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final EntityManager entityManager;

    private static final long MAX_TOTAL_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final String ENTITY_TYPE_PLANNER_ATTACHMENT = "PLANNER_ATTACHMENT";

    // ===== 공개 API (비회원 접근 가능) =====

    /**
     * 플래너 신청 목록 조회 (공개)
     * 민감정보 제외
     */
    @Transactional(readOnly = true)
    public Page<PlannerApplicationSummaryResponse> getPlannerApplications(
            PlannerApplicationStatus status, Pageable pageable) {

        log.info("플래너 신청 목록 조회 (공개): status={}", status);

        Page<PlannerApplication> applications;
        if (status != null) {
            applications = plannerApplicationRepository
                    .findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(status, pageable);
        } else {
            applications = plannerApplicationRepository
                    .findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        }

        return applications.map(PlannerApplicationSummaryResponse::from);
    }

    /**
     * 플래너 신청 상세 조회 (공개)
     * 민감정보 제외
     */
    @Transactional(readOnly = true)
    public PlannerApplicationDetailResponse getPlannerApplication(UUID applicationUuid) {
        log.info("플래너 신청 상세 조회 (공개): uuid={}", applicationUuid);

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationDetailResponse.from(application, attachments);
    }

    // ===== 사용자 API (인증 필요) =====

    /**
     * 플래너 신청서 생성 (USER만 가능)
     */
    @Transactional
    public PlannerApplicationResponse createApplication(String userEmail, PlannerApplicationCreateRequest request) {
        log.info("플래너 신청서 생성 시작: userEmail={}", userEmail);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 파일 UUID -> ID 변환 및 검증
        List<Long> fileIds = convertUuidsToFileIds(request.getAttachmentFileUuids());

        // 파일 크기 검증
        validateFileSizes(fileIds);

        // Entity 생성
        PlannerApplication application = PlannerApplication.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .consultationMethod(request.getConsultationMethod())
                .requestTypes(request.getRequestTypes().toArray(new String[0]))
                .applicantName(request.getApplicantName())
                .applicantPhone(request.getApplicantPhone())
                .applicantEmail(request.getApplicantEmail())
                .businessName(request.getBusinessName())
                .businessAddress(request.getBusinessAddress())
                .businessAreaSize(request.getBusinessAreaSize())
                .businessType(request.getBusinessType())
                .build();

        // 희망 일정 추가
        if (request.getPreferredDates() != null) {
            for (PreferredDateDto dto : request.getPreferredDates()) {
                PlannerPreferredDate preferredDate = dto.toEntity();
                application.addPreferredDate(preferredDate);
            }
        }

        application = plannerApplicationRepository.save(application);

        // 첨부파일 저장
        saveAttachments(application, fileIds);

        log.info("플래너 신청서 생성 완료: id={}, uuid={}", application.getId(), application.getUuid());

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    /**
     * 플래너 신청서 수정 (본인, PENDING 상태에서만)
     */
    @Transactional
    public PlannerApplicationResponse updateApplication(String userEmail, UUID applicationUuid,
                                                         PlannerApplicationUpdateRequest request) {
        log.info("플래너 신청서 수정 시작: userEmail={}, uuid={}", userEmail, applicationUuid);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        // 본인 확인
        if (!application.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.PLANNER_APPLICATION_ACCESS_DENIED);
        }

        // 수정 가능 상태 확인
        if (!application.canModify()) {
            throw new BusinessException(ErrorCode.PLANNER_APPLICATION_CANNOT_MODIFY);
        }

        // 파일 UUID -> ID 변환 및 검증
        List<Long> newFileIds = convertUuidsToFileIds(request.getAttachmentFileUuids());

        // 파일 크기 검증
        validateFileSizes(newFileIds);

        // 신청서 정보 수정
        application.update(
                request.getTitle(),
                request.getContent(),
                request.getConsultationMethod(),
                request.getRequestTypes().toArray(new String[0]),
                request.getApplicantName(),
                request.getApplicantPhone(),
                request.getApplicantEmail(),
                request.getBusinessName(),
                request.getBusinessAddress(),
                request.getBusinessAreaSize(),
                request.getBusinessType()
        );

        // 희망 일정 수정 (기존 삭제 후 새로 추가)
        // 기존 희망 일정 명시적 삭제 후 flush (unique constraint 회피)
        preferredDateRepository.deleteByPlannerApplicationId(application.getId());
        entityManager.flush();

        // 새 희망 일정 추가
        if (request.getPreferredDates() != null) {
            for (PreferredDateDto dto : request.getPreferredDates()) {
                PlannerPreferredDate preferredDate = dto.toEntity();
                preferredDate.setPlannerApplication(application);
                preferredDateRepository.save(preferredDate);
            }
        }

        // 첨부파일 수정 (비교 로직)
        updateAttachments(application, newFileIds);

        application = plannerApplicationRepository.save(application);

        log.info("플래너 신청서 수정 완료: uuid={}", applicationUuid);

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    /**
     * 플래너 신청서 삭제 (본인, PENDING 상태에서만)
     */
    @Transactional
    public void deleteApplication(String userEmail, UUID applicationUuid) {
        log.info("플래너 신청서 삭제 시작: userEmail={}, uuid={}", userEmail, applicationUuid);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        // 본인 확인
        if (!application.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.PLANNER_APPLICATION_ACCESS_DENIED);
        }

        // 삭제 가능 상태 확인
        if (!application.canModify()) {
            throw new BusinessException(ErrorCode.PLANNER_APPLICATION_CANNOT_DELETE);
        }

        // Soft Delete
        application.softDelete(user);
        plannerApplicationRepository.save(application);

        log.info("플래너 신청서 삭제 완료: uuid={}", applicationUuid);
    }

    /**
     * 내 플래너 신청서 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PlannerApplicationListResponse> getMyApplications(
            String userEmail, PlannerApplicationStatus status, Pageable pageable) {

        log.info("내 플래너 신청서 목록 조회: userEmail={}, status={}", userEmail, status);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Page<PlannerApplication> applications;
        if (status != null) {
            applications = plannerApplicationRepository
                    .findByUserAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(user, status, pageable);
        } else {
            applications = plannerApplicationRepository
                    .findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user, pageable);
        }

        return applications.map(PlannerApplicationListResponse::from);
    }

    /**
     * 플래너 신청서 상세 조회 (본인)
     */
    @Transactional(readOnly = true)
    public PlannerApplicationResponse getApplication(String userEmail, UUID applicationUuid) {
        log.info("플래너 신청서 조회: userEmail={}, uuid={}", userEmail, applicationUuid);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        // 본인 확인
        if (!application.isOwnedBy(user.getId())) {
            throw new BusinessException(ErrorCode.PLANNER_APPLICATION_ACCESS_DENIED);
        }

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    // ===== 관리자 API =====

    /**
     * 전체 플래너 신청서 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<PlannerApplicationListResponse> getAllApplications(
            PlannerApplicationStatus status, Pageable pageable) {

        log.info("전체 플래너 신청서 목록 조회: status={}", status);

        Page<PlannerApplication> applications;
        if (status != null) {
            applications = plannerApplicationRepository
                    .findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(status, pageable);
        } else {
            applications = plannerApplicationRepository
                    .findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        }

        return applications.map(PlannerApplicationListResponse::from);
    }

    /**
     * 플래너 신청서 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public PlannerApplicationResponse getApplicationAdmin(UUID applicationUuid) {
        log.info("플래너 신청서 조회 (관리자): uuid={}", applicationUuid);

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    /**
     * 플래너 신청서 삭제 (관리자, 모든 상태에서 가능)
     */
    @Transactional
    public void deleteApplicationAdmin(String adminEmail, UUID applicationUuid) {
        log.info("플래너 신청서 삭제 (관리자): adminEmail={}, uuid={}", adminEmail, applicationUuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        // Soft Delete
        application.softDelete(admin);
        plannerApplicationRepository.save(application);

        log.info("플래너 신청서 삭제 완료 (관리자): uuid={}", applicationUuid);
    }

    /**
     * 플래너 신청서 상태 변경 (관리자용)
     */
    @Transactional
    public PlannerApplicationResponse updateStatus(
            UUID applicationUuid, PlannerApplicationStatusUpdateRequest request) {

        log.info("플래너 신청서 상태 변경: uuid={}, newStatus={}", applicationUuid, request.getStatus());

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        application.changeStatus(request.getStatus());
        application = plannerApplicationRepository.save(application);

        log.info("플래너 신청서 상태 변경 완료: uuid={}, status={}", applicationUuid, application.getStatus());

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    /**
     * 플래너 신청서 답변 등록 (관리자용)
     */
    @Transactional
    public PlannerApplicationResponse addResponse(
            UUID applicationUuid, PlannerApplicationResponseRequest request) {

        log.info("플래너 신청서 답변 등록: uuid={}", applicationUuid);

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        application.addResponse(request.getResponse());
        application = plannerApplicationRepository.save(application);

        log.info("플래너 신청서 답변 등록 완료: uuid={}", applicationUuid);

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    /**
     * 플래너 신청서 메모 등록 (관리자용)
     */
    @Transactional
    public PlannerApplicationResponse addMemo(
            UUID applicationUuid, PlannerApplicationMemoRequest request) {

        log.info("플래너 신청서 메모 등록: uuid={}", applicationUuid);

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        application.addMemo(request.getMemo());
        application = plannerApplicationRepository.save(application);

        log.info("플래너 신청서 메모 등록 완료: uuid={}", applicationUuid);

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    /**
     * 플래너 신청서 담당자 배정 (관리자용)
     */
    @Transactional
    public PlannerApplicationResponse assignAdmin(
            UUID applicationUuid, PlannerApplicationAssignRequest request) {

        log.info("플래너 신청서 담당자 배정: uuid={}, adminId={}", applicationUuid, request.getAdminId());

        PlannerApplication application = plannerApplicationRepository
                .findByUuidAndIsDeletedFalse(applicationUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLANNER_APPLICATION_NOT_FOUND));

        User admin = userRepository.findById(request.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        application.assignAdmin(admin);
        application = plannerApplicationRepository.save(application);

        log.info("플래너 신청서 담당자 배정 완료: uuid={}, adminId={}", applicationUuid, admin.getId());

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertAttachmentsToDto(application);

        return PlannerApplicationResponse.from(application, attachments);
    }

    // ===== Private Helper Methods =====

    /**
     * UUID 리스트를 File ID 리스트로 변환
     */
    private List<Long> convertUuidsToFileIds(List<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return List.of();
        }

        List<Long> fileIds = new ArrayList<>();
        for (String uuidStr : uuids) {
            UUID uuid = UUID.fromString(uuidStr);
            File file = fileRepository.findByUuid(uuid)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
            fileIds.add(file.getId());
        }
        return fileIds;
    }

    /**
     * 파일 크기 검증 (전체 100MB 제한)
     */
    private void validateFileSizes(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        log.debug("파일 크기 검증 시작: fileIds={}", fileIds);

        List<File> files = fileRepository.findAllById(fileIds);

        if (files.size() != fileIds.size()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        long totalSize = files.stream()
                .mapToLong(File::getFileSize)
                .sum();

        log.debug("전체 파일 크기: {}MB", totalSize / (1024 * 1024));

        if (totalSize > MAX_TOTAL_FILE_SIZE) {
            log.warn("파일 크기 초과: totalSize={}MB, maxSize={}MB",
                    totalSize / (1024 * 1024), MAX_TOTAL_FILE_SIZE / (1024 * 1024));
            throw new BusinessException(ErrorCode.PLANNER_APPLICATION_FILE_SIZE_EXCEEDED);
        }
    }

    /**
     * 첨부파일 저장
     */
    private void saveAttachments(PlannerApplication application, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        // 파일 엔티티 정보 업데이트 (orphan 삭제 방지)
        List<File> files = fileRepository.findAllById(fileIds);
        for (File file : files) {
            file.updateEntityInfo(ENTITY_TYPE_PLANNER_ATTACHMENT, application.getId());
        }
        fileRepository.saveAll(files);
        log.info("플래너 첨부파일 엔티티 연결: applicationId={}, fileCount={}", application.getId(), files.size());

        int order = 0;
        for (Long fileId : fileIds) {
            PlannerApplicationAttachment attachment = PlannerApplicationAttachment.builder()
                    .plannerApplication(application)
                    .fileId(fileId)
                    .displayOrder(order++)
                    .build();
            attachmentRepository.save(attachment);
        }
    }

    /**
     * 첨부파일 수정 (비교 로직: 기존 유지/soft delete/신규 추가)
     */
    private void updateAttachments(PlannerApplication application, List<Long> newFileIds) {
        // 기존 첨부파일 조회
        List<PlannerApplicationAttachment> existingAttachments =
                attachmentRepository.findByPlannerApplicationAndIsDeletedFalseOrderByDisplayOrderAsc(application);

        Set<Long> existingFileIds = existingAttachments.stream()
                .map(PlannerApplicationAttachment::getFileId)
                .collect(Collectors.toSet());

        Set<Long> newFileIdSet = new HashSet<>(newFileIds != null ? newFileIds : List.of());

        // 삭제되는 파일 ID 수집
        List<Long> removedFileIds = new ArrayList<>();

        // 1. 기존에 있었지만 새 목록에 없는 파일 -> soft delete
        for (PlannerApplicationAttachment existing : existingAttachments) {
            if (!newFileIdSet.contains(existing.getFileId())) {
                existing.softDelete();
                attachmentRepository.save(existing);
                removedFileIds.add(existing.getFileId());
            }
        }

        // 삭제된 파일의 엔티티 연결 해제
        if (!removedFileIds.isEmpty()) {
            List<File> removedFiles = fileRepository.findAllById(removedFileIds);
            for (File file : removedFiles) {
                file.updateEntityInfo(null, null);
            }
            fileRepository.saveAll(removedFiles);
            log.info("플래너 첨부파일 엔티티 연결 해제: applicationId={}, fileCount={}", application.getId(), removedFiles.size());
        }

        // 신규 파일 ID 수집
        List<Long> addedFileIds = new ArrayList<>();

        // 2. 새 목록에만 있는 파일 -> 신규 추가
        int maxOrder = existingAttachments.stream()
                .mapToInt(PlannerApplicationAttachment::getDisplayOrder)
                .max()
                .orElse(-1);

        for (Long fileId : newFileIdSet) {
            if (!existingFileIds.contains(fileId)) {
                PlannerApplicationAttachment attachment = PlannerApplicationAttachment.builder()
                        .plannerApplication(application)
                        .fileId(fileId)
                        .displayOrder(++maxOrder)
                        .build();
                attachmentRepository.save(attachment);
                addedFileIds.add(fileId);
            }
        }

        // 신규 파일의 엔티티 연결
        if (!addedFileIds.isEmpty()) {
            List<File> addedFiles = fileRepository.findAllById(addedFileIds);
            for (File file : addedFiles) {
                file.updateEntityInfo(ENTITY_TYPE_PLANNER_ATTACHMENT, application.getId());
            }
            fileRepository.saveAll(addedFiles);
            log.info("플래너 첨부파일 엔티티 연결: applicationId={}, fileCount={}", application.getId(), addedFiles.size());
        }
    }

    /**
     * 첨부파일을 DTO로 변환
     */
    private List<AttachmentDto> convertAttachmentsToDto(PlannerApplication application) {
        List<PlannerApplicationAttachment> attachments =
                attachmentRepository.findByPlannerApplicationAndIsDeletedFalseOrderByDisplayOrderAsc(application);

        if (attachments.isEmpty()) {
            return List.of();
        }

        List<Long> fileIds = attachments.stream()
                .map(PlannerApplicationAttachment::getFileId)
                .collect(Collectors.toList());

        List<File> files = fileRepository.findAllById(fileIds);
        Map<Long, File> fileMap = files.stream()
                .collect(Collectors.toMap(File::getId, f -> f));

        return attachments.stream()
                .map(att -> {
                    File file = fileMap.get(att.getFileId());
                    if (file == null) return null;
                    return AttachmentDto.of(
                            file.getId(),
                            file.getUuid(),
                            file.getFileUrl(),
                            file.getOriginalFilename()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
