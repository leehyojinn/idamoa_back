package com.hip.damoa.domain.planner.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.model.PlannerApplication;
import com.hip.damoa.domain.planner.model.PlannerPreferredDate;
import com.hip.damoa.domain.planner.repository.PlannerApplicationRepository;
import com.hip.damoa.domain.planner.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 플래너 신청서 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerApplicationService {

    private final PlannerApplicationRepository plannerApplicationRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    private static final long MAX_TOTAL_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    /**
     * 첨부파일 ID 배열을 AttachmentDto 리스트로 변환
     */
    private List<AttachmentDto> convertToAttachmentDtos(Long[] fileIds) {
        if (fileIds == null || fileIds.length == 0) {
            return List.of();
        }

        List<File> files = fileRepository.findAllById(Arrays.asList(fileIds));

        return Arrays.stream(fileIds)
                .map(fileId -> files.stream()
                        .filter(f -> f.getId().equals(fileId))
                        .findFirst()
                        .map(file -> AttachmentDto.of(
                                file.getId(),
                                file.getUuid(),
                                file.getFileUrl(),
                                file.getOriginalFilename()
                        ))
                        .orElse(null))
                .filter(dto -> dto != null)
                .collect(Collectors.<AttachmentDto>toList());
    }

    /**
     * 플래너 신청서 생성 (USER만 가능)
     */
    @Transactional
    public PlannerApplicationResponse createApplication(String userEmail, PlannerApplicationCreateRequest request) {
        log.info("플래너 신청서 생성 시작: userEmail={}", userEmail);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 파일 크기 검증
        validateFileSizes(request.getAttachmentFileIds());

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
                .attachmentFileIds(request.getAttachmentFileIds() != null
                        ? request.getAttachmentFileIds().toArray(new Long[0])
                        : null)
                .build();

        // 희망 일정 추가
        if (request.getPreferredDates() != null) {
            for (PreferredDateDto dto : request.getPreferredDates()) {
                PlannerPreferredDate preferredDate = dto.toEntity();
                application.addPreferredDate(preferredDate);
            }
        }

        application = plannerApplicationRepository.save(application);
        log.info("플래너 신청서 생성 완료: id={}, uuid={}", application.getId(), application.getUuid());

        // 첨부파일 정보 변환
        List<AttachmentDto> attachments = convertToAttachmentDtos(application.getAttachmentFileIds());

        return PlannerApplicationResponse.from(application, attachments);
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
     * 플래너 신청서 상세 조회
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
        List<AttachmentDto> attachments = convertToAttachmentDtos(application.getAttachmentFileIds());

        return PlannerApplicationResponse.from(application, attachments);
    }

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
        List<AttachmentDto> attachments = convertToAttachmentDtos(application.getAttachmentFileIds());

        return PlannerApplicationResponse.from(application, attachments);
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
        List<AttachmentDto> attachments = convertToAttachmentDtos(application.getAttachmentFileIds());

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
        List<AttachmentDto> attachments = convertToAttachmentDtos(application.getAttachmentFileIds());

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
        List<AttachmentDto> attachments = convertToAttachmentDtos(application.getAttachmentFileIds());

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
        List<AttachmentDto> attachments = convertToAttachmentDtos(application.getAttachmentFileIds());

        return PlannerApplicationResponse.from(application, attachments);
    }
}
