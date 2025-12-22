package com.hip.damoa.domain.company.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPartnership;
import com.hip.damoa.domain.company.repository.CompanyPartnershipRepository;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipReorderRequest;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipUpdateRequest;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 제휴업체 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyPartnershipService {

    private final CompanyPartnershipRepository partnershipRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    /**
     * 제휴업체 등록 (관리자용)
     */
    @Transactional
    public CompanyPartnership createPartnership(String adminEmail, CompanyPartnershipCreateRequest request) {
        log.info("제휴업체 등록 시작: adminEmail={}, companyUuid={}", adminEmail, request.getCompanyUuid());

        // 관리자 조회
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_REQUIRED);
        }

        // 업체 조회
        Company company = companyRepository.findByUuidAndIsDeletedFalse(request.getCompanyUuid())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_PROFILE_NOT_FOUND));

        // 이미 활성 제휴가 있는지 확인
        if (partnershipRepository.existsActiveByCompanyId(company.getId())) {
            throw new BusinessException(ErrorCode.PARTNERSHIP_ALREADY_EXISTS);
        }

        // 날짜 유효성 검증
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(ErrorCode.INVALID_PARTNERSHIP_DATES);
        }

        // 제휴 생성
        CompanyPartnership partnership = CompanyPartnership.builder()
                .company(company)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status("ACTIVE")
                .adminMemo(request.getAdminMemo())
                .registeredBy(admin)
                .build();

        partnership = partnershipRepository.save(partnership);

        log.info("제휴업체 등록 완료: partnershipId={}, companyId={}", partnership.getId(), company.getId());

        return partnership;
    }

    /**
     * 제휴 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public CompanyPartnership getPartnership(UUID partnershipUuid) {
        log.info("제휴 상세 조회: uuid={}", partnershipUuid);

        return partnershipRepository.findByUuidAndIsDeletedFalse(partnershipUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_NOT_FOUND));
    }

    /**
     * 제휴 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<CompanyPartnership> getPartnershipsForAdmin(String status, Pageable pageable) {
        log.info("제휴 목록 조회 (관리자): status={}", status);

        if (status != null && !status.isEmpty()) {
            return partnershipRepository.findByStatusAndIsDeletedFalse(status, pageable);
        }

        return partnershipRepository.findAllForAdmin(pageable);
    }

    /**
     * 제휴 수정 (관리자용)
     */
    @Transactional
    public CompanyPartnership updatePartnership(String adminEmail, UUID partnershipUuid,
                                                 CompanyPartnershipUpdateRequest request) {
        log.info("제휴 수정: adminEmail={}, uuid={}", adminEmail, partnershipUuid);

        // 관리자 확인
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_REQUIRED);
        }

        // 제휴 조회
        CompanyPartnership partnership = partnershipRepository.findByUuidAndIsDeletedFalse(partnershipUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_NOT_FOUND));

        // 날짜 유효성 검증
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : partnership.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : partnership.getEndDate();

        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_PARTNERSHIP_DATES);
        }

        // 업데이트
        partnership.update(
                request.getDisplayOrder(),
                request.getStartDate(),
                request.getEndDate(),
                request.getAdminMemo()
        );

        partnership = partnershipRepository.save(partnership);

        log.info("제휴 수정 완료: partnershipId={}", partnership.getId());

        return partnership;
    }

    /**
     * 제휴 취소 (관리자용)
     */
    @Transactional
    public void cancelPartnership(String adminEmail, UUID partnershipUuid) {
        log.info("제휴 취소: adminEmail={}, uuid={}", adminEmail, partnershipUuid);

        // 관리자 확인
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_REQUIRED);
        }

        // 제휴 조회
        CompanyPartnership partnership = partnershipRepository.findByUuidAndIsDeletedFalse(partnershipUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_NOT_FOUND));

        // 취소 처리
        partnership.cancel();
        partnershipRepository.save(partnership);

        log.info("제휴 취소 완료: partnershipId={}", partnership.getId());
    }

    /**
     * 제휴 삭제 (관리자용) - Soft Delete
     */
    @Transactional
    public void deletePartnership(String adminEmail, UUID partnershipUuid) {
        log.info("제휴 삭제: adminEmail={}, uuid={}", adminEmail, partnershipUuid);

        // 관리자 확인
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_REQUIRED);
        }

        // 제휴 조회
        CompanyPartnership partnership = partnershipRepository.findByUuidAndIsDeletedFalse(partnershipUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_NOT_FOUND));

        // Soft Delete
        partnership.softDelete();
        partnershipRepository.save(partnership);

        log.info("제휴 삭제 완료: partnershipId={}", partnership.getId());
    }

    /**
     * 제휴 순서 일괄 변경 (관리자용)
     */
    @Transactional
    public void reorderPartnerships(String adminEmail, CompanyPartnershipReorderRequest request) {
        log.info("제휴 순서 변경: adminEmail={}, count={}", adminEmail, request.getOrders().size());

        // 관리자 확인
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!admin.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_REQUIRED);
        }

        // 순서 변경 처리
        for (CompanyPartnershipReorderRequest.OrderItem orderItem : request.getOrders()) {
            CompanyPartnership partnership = partnershipRepository
                    .findByUuidAndIsDeletedFalse(orderItem.getPartnershipUuid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_NOT_FOUND));

            partnership.update(orderItem.getDisplayOrder(), null, null, null);
            partnershipRepository.save(partnership);
        }

        log.info("제휴 순서 변경 완료");
    }

    /**
     * 활성 제휴업체 목록 조회 (퍼블릭용)
     */
    @Transactional(readOnly = true)
    public List<CompanyPartnership> getActivePartnerships() {
        log.info("활성 제휴업체 목록 조회 (퍼블릭)");

        LocalDate today = LocalDate.now();
        return partnershipRepository.findActivePartnerships(today);
    }

    /**
     * 특정 업체의 활성 제휴 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasActivePartnership(UUID companyUuid) {
        LocalDate today = LocalDate.now();
        return partnershipRepository.existsActivePartnershipByCompanyUuid(companyUuid, today);
    }

    /**
     * 특정 업체의 제휴 이력 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public List<CompanyPartnership> getPartnershipHistory(UUID companyUuid) {
        log.info("제휴 이력 조회: companyUuid={}", companyUuid);
        return partnershipRepository.findByCompanyUuid(companyUuid);
    }

    /**
     * 만료된 제휴 처리 (스케줄러용)
     */
    @Transactional
    public int expirePartnerships() {
        log.info("만료된 제휴 처리 시작");

        LocalDate today = LocalDate.now();
        List<CompanyPartnership> expiredPartnerships = partnershipRepository.findExpiredPartnerships(today);

        for (CompanyPartnership partnership : expiredPartnerships) {
            partnership.expire();
            partnershipRepository.save(partnership);
            log.info("제휴 만료 처리: partnershipId={}, companyId={}",
                    partnership.getId(), partnership.getCompany().getId());
        }

        log.info("만료된 제휴 처리 완료: 처리 건수={}", expiredPartnerships.size());

        return expiredPartnerships.size();
    }
}
