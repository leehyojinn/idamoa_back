package com.hip.damoa.domain.inquiry.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.repository.InquiryRepository;
import com.hip.damoa.domain.inquiry.web.dto.InquiryCreateRequest;
import com.hip.damoa.domain.inquiry.web.dto.InquiryListResponse;
import com.hip.damoa.domain.inquiry.web.dto.InquiryResponse;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 제휴/광고 문의 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /**
     * 문의 작성 (회원/비회원 모두 가능)
     */
    @Transactional
    public InquiryResponse createInquiry(String userEmail, InquiryCreateRequest request) {
        log.info("문의 작성 시작: userEmail={}, type={}", userEmail, request.getInquiryType());

        // 회원인 경우 User 조회, 비회원인 경우 null
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .inquiryType(request.getInquiryType())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .content(request.getContent())
                .status(InquiryStatus.PENDING)
                .build();

        inquiry = inquiryRepository.save(inquiry);

        log.info("문의 작성 완료: id={}, uuid={}", inquiry.getId(), inquiry.getUuid());

        return InquiryResponse.from(inquiry);
    }

    /**
     * 문의 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getInquiries(Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findByIsDeletedFalse(pageable);
        return inquiries.map(InquiryListResponse::from);
    }

    /**
     * 문의 목록 조회 - 상태 필터 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<InquiryListResponse> getInquiriesByStatus(InquiryStatus status, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findByStatusAndIsDeletedFalse(status, pageable);
        return inquiries.map(InquiryListResponse::from);
    }

    /**
     * 문의 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public InquiryResponse getInquiry(UUID uuid) {
        Inquiry inquiry = inquiryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        return InquiryResponse.from(inquiry);
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
            case "COMPLETED":
                inquiry.complete();
                break;
            case "CANCELLED":
                inquiry.cancel();
                break;
            default:
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        inquiryRepository.save(inquiry);

        log.info("문의 상태 변경 완료: uuid={}, status={}", uuid, status);

        return InquiryResponse.from(inquiry);
    }

    /**
     * 문의 삭제 (관리자용, Soft Delete)
     */
    @Transactional
    public void deleteInquiry(UUID uuid) {
        log.info("문의 삭제 시작: uuid={}", uuid);

        Inquiry inquiry = inquiryRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        inquiry.softDelete();
        inquiryRepository.save(inquiry);

        log.info("문의 삭제 완료: uuid={}", uuid);
    }
}
