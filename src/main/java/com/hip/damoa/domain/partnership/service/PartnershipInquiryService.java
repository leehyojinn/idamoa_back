package com.hip.damoa.domain.partnership.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.partnership.model.PartnershipInquiry;
import com.hip.damoa.domain.partnership.model.PartnershipStatus;
import com.hip.damoa.domain.partnership.model.PartnershipType;
import com.hip.damoa.domain.partnership.repository.PartnershipInquiryRepository;
import com.hip.damoa.domain.partnership.web.dto.PartnershipInquiryCreateRequest;
import com.hip.damoa.domain.partnership.web.dto.PartnershipInquiryListResponse;
import com.hip.damoa.domain.partnership.web.dto.PartnershipInquiryResponse;
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
public class PartnershipInquiryService {

    private final PartnershipInquiryRepository partnershipInquiryRepository;
    private final UserRepository userRepository;

    /**
     * 제휴/광고 문의 생성 (비회원 가능)
     */
    @Transactional
    public PartnershipInquiryResponse createInquiry(String userEmail, PartnershipInquiryCreateRequest request) {
        log.info("제휴/광고 문의 생성: email={}", request.getEmail());

        // 문의 유형 검증
        PartnershipType type = parsePartnershipType(request.getPartnershipType());

        // 회원인 경우 User 조회
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        // 문의 생성
        PartnershipInquiry inquiry = PartnershipInquiry.builder()
                .user(user)
                .partnershipType(type)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .content(request.getContent())
                .build();

        inquiry = partnershipInquiryRepository.save(inquiry);

        log.info("제휴/광고 문의 생성 완료: uuid={}", inquiry.getUuid());

        return PartnershipInquiryResponse.from(inquiry);
    }

    /**
     * 제휴/광고 문의 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<PartnershipInquiryListResponse> getInquiries(String status, String type, Pageable pageable) {
        log.info("제휴/광고 문의 목록 조회: status={}, type={}", status, type);

        Page<PartnershipInquiry> inquiries;

        PartnershipStatus statusEnum = status != null ? parsePartnershipStatus(status) : null;
        PartnershipType typeEnum = type != null ? parsePartnershipType(type) : null;

        if (statusEnum != null && typeEnum != null) {
            inquiries = partnershipInquiryRepository
                    .findByStatusAndPartnershipTypeAndIsDeletedFalseOrderByCreatedAtDesc(
                            statusEnum, typeEnum, pageable);
        } else if (statusEnum != null) {
            inquiries = partnershipInquiryRepository
                    .findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(statusEnum, pageable);
        } else if (typeEnum != null) {
            inquiries = partnershipInquiryRepository
                    .findByPartnershipTypeAndIsDeletedFalseOrderByCreatedAtDesc(typeEnum, pageable);
        } else {
            inquiries = partnershipInquiryRepository
                    .findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        }

        return inquiries.map(PartnershipInquiryListResponse::from);
    }

    /**
     * 제휴/광고 문의 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public PartnershipInquiryResponse getInquiry(UUID uuid) {
        log.info("제휴/광고 문의 상세 조회: uuid={}", uuid);

        PartnershipInquiry inquiry = partnershipInquiryRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_INQUIRY_NOT_FOUND));

        return PartnershipInquiryResponse.from(inquiry);
    }

    /**
     * 제휴/광고 문의 상태 변경 (관리자용)
     */
    @Transactional
    public void changeStatus(UUID uuid, String status) {
        log.info("제휴/광고 문의 상태 변경: uuid={}, status={}", uuid, status);

        PartnershipStatus statusEnum = parsePartnershipStatus(status);

        PartnershipInquiry inquiry = partnershipInquiryRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_INQUIRY_NOT_FOUND));

        inquiry.updateStatus(statusEnum);
        partnershipInquiryRepository.save(inquiry);

        log.info("제휴/광고 문의 상태 변경 완료: id={}", inquiry.getId());
    }

    /**
     * 제휴/광고 문의 삭제 (관리자용)
     */
    @Transactional
    public void deleteInquiry(UUID uuid) {
        log.info("제휴/광고 문의 삭제: uuid={}", uuid);

        PartnershipInquiry inquiry = partnershipInquiryRepository
                .findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_INQUIRY_NOT_FOUND));

        inquiry.softDelete();
        partnershipInquiryRepository.save(inquiry);

        log.info("제휴/광고 문의 삭제 완료: id={}", inquiry.getId());
    }

    // ===== Private Helper Methods =====

    /**
     * 문의 유형 문자열을 Enum으로 변환
     */
    private PartnershipType parsePartnershipType(String type) {
        try {
            return PartnershipType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_PARTNERSHIP_TYPE);
        }
    }

    /**
     * 문의 상태 문자열을 Enum으로 변환
     */
    private PartnershipStatus parsePartnershipStatus(String status) {
        try {
            return PartnershipStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_PARTNERSHIP_STATUS);
        }
    }
}