package com.hip.damoa.domain.inquiry.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryAnswer;
import com.hip.damoa.domain.inquiry.model.InquiryAttachment;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.model.InquiryType;
import com.hip.damoa.domain.inquiry.repository.InquiryAnswerRepository;
import com.hip.damoa.domain.inquiry.repository.InquiryAttachmentRepository;
import com.hip.damoa.domain.inquiry.repository.InquiryRepository;
import com.hip.damoa.domain.inquiry.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 일반 문의 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final InquiryAttachmentRepository inquiryAttachmentRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    /**
     * 문의 작성 (로그인 필수)
     */
    @Transactional
    public InquiryResponse createInquiry(String userEmail, InquiryCreateRequest request) {
        log.info("일반 문의 작성 시작: userEmail={}, type={}", userEmail, request.getInquiryType());

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .inquiryType(request.getInquiryType())
                .title(request.getTitle())
                .content(request.getContent())
                .status(InquiryStatus.PENDING)
                .build();

        inquiry = inquiryRepository.save(inquiry);

        // 첨부파일 처리
        if (request.getFileUuids() != null && !request.getFileUuids().isEmpty()) {
            processAttachments(inquiry, request.getFileUuids());
        }

        log.info("일반 문의 작성 완료: id={}, uuid={}, attachments={}",
                inquiry.getId(), inquiry.getUuid(),
                request.getFileUuids() != null ? request.getFileUuids().size() : 0);

        // 첨부파일 정보 조회
        List<InquiryAttachmentResponse> attachments = getAttachments(inquiry.getId());

        return InquiryResponse.from(inquiry, attachments);
    }

    /**
     * 내 문의 목록 조회 (사용자용)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getMyInquiries(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Page<Inquiry> inquiries = inquiryRepository.findByUserIdAndIsDeletedFalse(user.getId(), pageable);

        return inquiries.map(inquiry -> {
            boolean hasAnswer = inquiryAnswerRepository.existsByInquiryId(inquiry.getId());
            return InquiryListResponse.from(inquiry, hasAnswer);
        });
    }

    /**
     * 내 문의 상세 조회 (사용자용)
     */
    @Transactional(readOnly = true)
    public InquiryResponse getMyInquiry(String userEmail, UUID uuid) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Inquiry inquiry = inquiryRepository.findByUuidAndUserIdAndIsDeletedFalse(uuid, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 첨부파일 조회
        List<InquiryAttachmentResponse> attachments = getAttachments(inquiry.getId());

        // 답변이 있는지 확인
        InquiryAnswerResponse answerResponse = inquiryAnswerRepository.findByInquiryId(inquiry.getId())
                .map(InquiryAnswerResponse::from)
                .orElse(null);

        if (answerResponse != null) {
            return InquiryResponse.from(inquiry, answerResponse, attachments);
        }

        return InquiryResponse.from(inquiry, attachments);
    }

    /**
     * 내 문의 수정 (사용자용, 답변 전 PENDING 상태만 가능)
     */
    @Transactional
    public InquiryResponse updateMyInquiry(String userEmail, UUID uuid, InquiryUpdateRequest request) {
        log.info("일반 문의 수정 시작: userEmail={}, uuid={}", userEmail, uuid);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Inquiry inquiry = inquiryRepository.findByUuidAndUserIdAndIsDeletedFalse(uuid, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // PENDING 상태일 때만 수정 가능
        if (inquiry.getStatus() != InquiryStatus.PENDING) {
            throw new BusinessException(ErrorCode.INQUIRY_NOT_EDITABLE);
        }

        inquiry.updateContent(request.getTitle(), request.getContent());
        inquiryRepository.save(inquiry);

        // 첨부파일 업데이트 (null이면 변경 없음, 빈 배열이면 모든 파일 삭제)
        if (request.getFileUuids() != null) {
            // 기존 첨부파일 Soft delete
            List<InquiryAttachment> existingAttachments = inquiryAttachmentRepository
                    .findByInquiryIdAndIsDeletedFalseOrderByDisplayOrderAsc(inquiry.getId());
            existingAttachments.forEach(attachment -> attachment.softDelete());

            // 새 첨부파일 추가
            if (!request.getFileUuids().isEmpty()) {
                processAttachments(inquiry, request.getFileUuids());
            }
        }

        log.info("일반 문의 수정 완료: uuid={}", uuid);

        // 첨부파일 정보 조회
        List<InquiryAttachmentResponse> attachments = getAttachments(inquiry.getId());

        return InquiryResponse.from(inquiry, attachments);
    }

    /**
     * 내 문의 삭제 (사용자용, 답변 전 PENDING 상태만 가능)
     */
    @Transactional
    public void deleteMyInquiry(String userEmail, UUID uuid) {
        log.info("일반 문의 삭제 시작: userEmail={}, uuid={}", userEmail, uuid);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Inquiry inquiry = inquiryRepository.findByUuidAndUserIdAndIsDeletedFalse(uuid, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // PENDING 상태일 때만 삭제 가능
        if (inquiry.getStatus() != InquiryStatus.PENDING) {
            throw new BusinessException(ErrorCode.INQUIRY_NOT_EDITABLE);
        }

        inquiry.softDelete();
        inquiryRepository.save(inquiry);

        log.info("일반 문의 삭제 완료: uuid={}", uuid);
    }

    // ===== 관리자 기능 =====

    /**
     * 문의 목록 조회 (관리자용 - 삭제된 데이터 포함)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getAllInquiries(Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);

        return inquiries.map(inquiry -> {
            boolean hasAnswer = inquiryAnswerRepository.existsByInquiryId(inquiry.getId());
            return InquiryListResponse.from(inquiry, hasAnswer);
        });
    }

    /**
     * 문의 검색 (관리자용 - 삭제된 데이터 포함) - 개별 파라미터
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> searchInquiries(String keyword, String inquiryType, String status,
                                                      String userEmail, Boolean hasAnswer, Pageable pageable) {
        // String을 Enum으로 변환 (null이면 null 유지)
        InquiryType typeEnum = null;
        if (inquiryType != null && !inquiryType.isEmpty()) {
            try {
                typeEnum = InquiryType.valueOf(inquiryType);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INQUIRY_TYPE);
            }
        }

        InquiryStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = InquiryStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_INQUIRY_STATUS);
            }
        }

        // 관리자용 쿼리 사용 (삭제된 데이터 포함)
        Page<Inquiry> inquiries;
        if (hasAnswer == null) {
            inquiries = inquiryRepository.adminSearchInquiriesBasic(
                    keyword, typeEnum, statusEnum, userEmail, pageable);
        } else if (hasAnswer) {
            inquiries = inquiryRepository.adminSearchInquiriesWithAnswer(
                    keyword, typeEnum, statusEnum, userEmail, pageable);
        } else {
            inquiries = inquiryRepository.adminSearchInquiriesWithoutAnswer(
                    keyword, typeEnum, statusEnum, userEmail, pageable);
        }

        return inquiries.map(inquiry -> {
            boolean hasAnswerResult = inquiryAnswerRepository.existsByInquiryId(inquiry.getId());
            return InquiryListResponse.from(inquiry, hasAnswerResult);
        });
    }

    /**
     * 문의 검색 (관리자용) - DTO 사용 (레거시)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> searchInquiries(InquirySearchRequest request, Pageable pageable) {
        // hasAnswer에 따라 다른 쿼리 호출 (PostgreSQL 타입 추론 문제 회피)
        Page<Inquiry> inquiries;
        if (request.getHasAnswer() == null) {
            inquiries = inquiryRepository.searchInquiriesBasic(
                    request.getKeyword(),
                    request.getInquiryType(),
                    request.getStatus(),
                    request.getUserEmail(),
                    pageable);
        } else if (request.getHasAnswer()) {
            inquiries = inquiryRepository.searchInquiriesWithAnswer(
                    request.getKeyword(),
                    request.getInquiryType(),
                    request.getStatus(),
                    request.getUserEmail(),
                    pageable);
        } else {
            inquiries = inquiryRepository.searchInquiriesWithoutAnswer(
                    request.getKeyword(),
                    request.getInquiryType(),
                    request.getStatus(),
                    request.getUserEmail(),
                    pageable);
        }

        return inquiries.map(inquiry -> {
            boolean hasAnswer = inquiryAnswerRepository.existsByInquiryId(inquiry.getId());
            return InquiryListResponse.from(inquiry, hasAnswer);
        });
    }

    /**
     * 내 문의 검색 (사용자용)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> searchMyInquiries(String userEmail, InquirySearchRequest request, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Page<Inquiry> inquiries = inquiryRepository.searchMyInquiries(
                user.getId(),
                request.getKeyword(),
                request.getInquiryType(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );

        return inquiries.map(inquiry -> {
            boolean hasAnswer = inquiryAnswerRepository.existsByInquiryId(inquiry.getId());
            return InquiryListResponse.from(inquiry, hasAnswer);
        });
    }

    /**
     * 문의 목록 조회 - 상태 필터 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getInquiriesByStatus(InquiryStatus status, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findByStatusAndIsDeletedFalse(status, pageable);

        return inquiries.map(inquiry -> {
            boolean hasAnswer = inquiryAnswerRepository.existsByInquiryId(inquiry.getId());
            return InquiryListResponse.from(inquiry, hasAnswer);
        });
    }

    /**
     * 문의 상세 조회 (관리자용 - 삭제된 데이터 포함)
     */
    @Transactional(readOnly = true)
    public InquiryResponse getInquiry(UUID uuid) {
        Inquiry inquiry = inquiryRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 첨부파일 조회
        List<InquiryAttachmentResponse> attachments = getAttachments(inquiry.getId());

        // 답변이 있는지 확인
        InquiryAnswerResponse answerResponse = inquiryAnswerRepository.findByInquiryId(inquiry.getId())
                .map(InquiryAnswerResponse::from)
                .orElse(null);

        if (answerResponse != null) {
            return InquiryResponse.from(inquiry, answerResponse, attachments);
        }

        return InquiryResponse.from(inquiry, attachments);
    }

    /**
     * 문의 상태 변경 (관리자용)
     */
    @Transactional
    public InquiryResponse changeStatus(UUID uuid, String status) {
        log.info("문의 상태 변경 시작: uuid={}, status={}", uuid, status);

        Inquiry inquiry = inquiryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        switch (status) {
            case "IN_PROGRESS":
                inquiry.startProgress();
                break;
            case "ANSWERED":
                inquiry.markAnswered();
                break;
            case "CLOSED":
                inquiry.close();
                break;
            default:
                throw new BusinessException(ErrorCode.INVALID_INQUIRY_STATUS);
        }

        inquiryRepository.save(inquiry);

        log.info("문의 상태 변경 완료: uuid={}, status={}", uuid, status);

        return InquiryResponse.from(inquiry);
    }

    /**
     * 문의 답변 작성 (관리자용)
     */
    @Transactional
    public InquiryAnswerResponse createAnswer(String adminEmail, UUID inquiryUuid, InquiryAnswerCreateRequest request) {
        log.info("문의 답변 작성 시작: adminEmail={}, inquiryUuid={}", adminEmail, inquiryUuid);

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Inquiry inquiry = inquiryRepository.findByUuidAndIsDeletedFalse(inquiryUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 이미 답변이 있는지 확인
        if (inquiryAnswerRepository.existsByInquiryId(inquiry.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }

        InquiryAnswer answer = InquiryAnswer.builder()
                .inquiry(inquiry)
                .admin(admin)
                .content(request.getContent())
                .build();

        answer = inquiryAnswerRepository.save(answer);

        // 문의 상태를 답변 완료로 변경
        inquiry.markAnswered();
        inquiryRepository.save(inquiry);

        log.info("문의 답변 작성 완료: answerId={}, inquiryUuid={}", answer.getId(), inquiryUuid);

        return InquiryAnswerResponse.from(answer);
    }

    /**
     * 문의 답변 수정 (관리자용)
     */
    @Transactional
    public InquiryAnswerResponse updateAnswer(String adminEmail, UUID inquiryUuid, InquiryAnswerCreateRequest request) {
        log.info("문의 답변 수정 시작: adminEmail={}, inquiryUuid={}", adminEmail, inquiryUuid);

        InquiryAnswer answer = inquiryAnswerRepository.findByInquiryUuid(inquiryUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        answer.updateContent(request.getContent());
        answer = inquiryAnswerRepository.save(answer);

        log.info("문의 답변 수정 완료: answerId={}, inquiryUuid={}", answer.getId(), inquiryUuid);

        return InquiryAnswerResponse.from(answer);
    }

    /**
     * 문의 답변 삭제 (관리자용)
     */
    @Transactional
    public void deleteAnswer(UUID inquiryUuid) {
        log.info("문의 답변 삭제 시작: inquiryUuid={}", inquiryUuid);

        Inquiry inquiry = inquiryRepository.findByUuidAndIsDeletedFalse(inquiryUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        inquiryAnswerRepository.deleteByInquiryId(inquiry.getId());

        // 문의 상태를 다시 처리중으로 변경
        inquiry.startProgress();
        inquiryRepository.save(inquiry);

        log.info("문의 답변 삭제 완료: inquiryUuid={}", inquiryUuid);
    }

    /**
     * 문의 삭제 (관리자용, Soft Delete)
     */
    @Transactional
    public void deleteInquiry(UUID uuid) {
        log.info("문의 삭제 시작: uuid={}", uuid);

        Inquiry inquiry = inquiryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 답변도 함께 삭제
        inquiryAnswerRepository.deleteByInquiryId(inquiry.getId());

        inquiry.softDelete();
        inquiryRepository.save(inquiry);

        log.info("문의 삭제 완료: uuid={}", uuid);
    }

    /**
     * 첨부파일 처리 (Private Method)
     */
    private void processAttachments(Inquiry inquiry, java.util.List<String> fileUuids) {
        log.info("첨부파일 처리 시작: inquiryId={}, fileCount={}", inquiry.getId(), fileUuids.size());

        int displayOrder = 0;
        for (String fileUuidStr : fileUuids) {
            try {
                UUID fileUuid = UUID.fromString(fileUuidStr);
                File file = fileRepository.findByUuidAndIsDeletedFalse(fileUuid)
                        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

                // File의 entityType과 entityId 업데이트
                file.updateEntityInfo("INQUIRY", inquiry.getId());
                fileRepository.save(file);

                // InquiryAttachment 생성
                InquiryAttachment attachment = InquiryAttachment.builder()
                        .inquiry(inquiry)
                        .fileId(file.getId())
                        .fileType("ATTACHMENT")
                        .displayOrder(displayOrder++)
                        .build();

                inquiryAttachmentRepository.save(attachment);

                log.info("파일 연결 완료: fileId={}, inquiryId={}", file.getId(), inquiry.getId());
            } catch (IllegalArgumentException e) {
                log.error("잘못된 UUID 형식: {}", fileUuidStr);
                throw new BusinessException(ErrorCode.INVALID_UUID_FORMAT);
            }
        }

        log.info("첨부파일 처리 완료: inquiryId={}, attachmentCount={}", inquiry.getId(), displayOrder);
    }

    /**
     * 첨부파일 조회 (Private Method)
     */
    private List<InquiryAttachmentResponse> getAttachments(Long inquiryId) {
        List<InquiryAttachment> attachments = inquiryAttachmentRepository
                .findByInquiryIdAndIsDeletedFalseOrderByDisplayOrderAsc(inquiryId);

        return attachments.stream()
                .map(attachment -> {
                    File file = fileRepository.findById(attachment.getFileId())
                            .orElse(null);
                    if (file != null && !file.getIsDeleted()) {
                        return InquiryAttachmentResponse.from(file, attachment);
                    }
                    return null;
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
}