package com.hip.damoa.domain.payment.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.payment.model.CreditPackage;
import com.hip.damoa.domain.payment.repository.CreditPackageRepository;
import com.hip.damoa.domain.payment.web.dto.CreditPackageCreateRequest;
import com.hip.damoa.domain.payment.web.dto.CreditPackageResponse;
import com.hip.damoa.domain.payment.web.dto.CreditPackageUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 관리자용 크레딧 패키지 관리 Service
 *
 * 패키지는 4개 (1만원권, 3만원권, 5만원권, 10만원권)
 * 수량은 사용자가 충전 시 직접 지정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCreditPackageService {

    private final CreditPackageRepository creditPackageRepository;

    /**
     * 허용된 단위 금액
     */
    private static final List<Integer> ALLOWED_UNIT_AMOUNTS = Arrays.asList(10000, 30000, 50000, 100000);

    /**
     * 패키지 생성
     * 일반적으로 4개의 패키지는 마이그레이션으로 생성되지만,
     * 필요 시 관리자가 직접 생성할 수 있습니다.
     */
    @Transactional
    public CreditPackageResponse createPackage(CreditPackageCreateRequest request) {
        log.info("크레딧 패키지 생성: unitAmount={}, bonusRate={}",
                request.getUnitAmount(), request.getBonusRate());

        // 단위 금액 검증
        if (!ALLOWED_UNIT_AMOUNTS.contains(request.getUnitAmount())) {
            throw new BusinessException(ErrorCode.INVALID_CREDIT_PACKAGE,
                    "허용된 단위 금액: " + ALLOWED_UNIT_AMOUNTS);
        }

        // 중복 검증 (단위 금액당 하나의 패키지만 존재)
        if (creditPackageRepository.existsByUnitAmountAndIsDeletedFalse(request.getUnitAmount())) {
            throw new BusinessException(ErrorCode.CREDIT_PACKAGE_ALREADY_EXISTS);
        }

        // 3만원 미만은 보너스 없음
        BigDecimal bonusRate = request.getBonusRate();
        Integer maxBonus = request.getMaxBonus();
        if (request.getUnitAmount() < 30000) {
            bonusRate = BigDecimal.ZERO;
            maxBonus = null;
        }

        CreditPackage creditPackage = CreditPackage.create(
                request.getUnitAmount(),
                bonusRate,
                maxBonus
        );

        if (request.getDescription() != null) {
            creditPackage.update(null, bonusRate, maxBonus, request.getDescription());
        }

        creditPackage = creditPackageRepository.save(creditPackage);

        log.info("크레딧 패키지 생성 완료: id={}, code={}",
                creditPackage.getId(), creditPackage.getCode());

        return CreditPackageResponse.from(creditPackage);
    }

    /**
     * 패키지 수정
     */
    @Transactional
    public CreditPackageResponse updatePackage(UUID packageUuid, CreditPackageUpdateRequest request) {
        log.info("크레딧 패키지 수정: uuid={}", packageUuid);

        CreditPackage creditPackage = creditPackageRepository.findByUuidAndIsDeletedFalse(packageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_PACKAGE_NOT_FOUND));

        // 3만원 미만은 보너스 없음
        BigDecimal bonusRate = request.getBonusRate();
        Integer maxBonus = request.getMaxBonus();
        if (creditPackage.getUnitAmount() < 30000) {
            bonusRate = BigDecimal.ZERO;
            maxBonus = null;
        }

        creditPackage.update(
                request.getDisplayName(),
                bonusRate,
                maxBonus,
                request.getDescription()
        );

        creditPackage = creditPackageRepository.save(creditPackage);

        log.info("크레딧 패키지 수정 완료: id={}, code={}",
                creditPackage.getId(), creditPackage.getCode());

        return CreditPackageResponse.from(creditPackage);
    }

    /**
     * 패키지 활성화/비활성화
     */
    @Transactional
    public CreditPackageResponse toggleActive(UUID packageUuid) {
        log.info("크레딧 패키지 활성화 토글: uuid={}", packageUuid);

        CreditPackage creditPackage = creditPackageRepository.findByUuidAndIsDeletedFalse(packageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_PACKAGE_NOT_FOUND));

        boolean newState = !creditPackage.getIsActive();
        creditPackage.setActive(newState);

        creditPackage = creditPackageRepository.save(creditPackage);

        log.info("크레딧 패키지 활성화 변경: id={}, isActive={}",
                creditPackage.getId(), creditPackage.getIsActive());

        return CreditPackageResponse.from(creditPackage);
    }

    /**
     * 패키지 삭제 (Soft Delete)
     */
    @Transactional
    public void deletePackage(UUID packageUuid) {
        log.info("크레딧 패키지 삭제: uuid={}", packageUuid);

        CreditPackage creditPackage = creditPackageRepository.findByUuidAndIsDeletedFalse(packageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_PACKAGE_NOT_FOUND));

        creditPackage.softDelete();
        creditPackageRepository.save(creditPackage);

        log.info("크레딧 패키지 삭제 완료: id={}, code={}",
                creditPackage.getId(), creditPackage.getCode());
    }

    /**
     * 패키지 조회
     */
    @Transactional(readOnly = true)
    public CreditPackageResponse getPackage(UUID packageUuid) {
        CreditPackage creditPackage = creditPackageRepository.findByUuidAndIsDeletedFalse(packageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_PACKAGE_NOT_FOUND));

        return CreditPackageResponse.from(creditPackage);
    }

    /**
     * 전체 패키지 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<CreditPackageResponse> getPackages(Pageable pageable) {
        return creditPackageRepository.findByIsDeletedFalse(pageable)
                .map(CreditPackageResponse::from);
    }

    /**
     * 패키지 검색 (단위 금액, 활성 상태 필터)
     */
    @Transactional(readOnly = true)
    public Page<CreditPackageResponse> searchPackages(Integer unitAmount, Boolean isActive, Pageable pageable) {
        return creditPackageRepository.searchForAdmin(unitAmount, isActive, pageable)
                .map(CreditPackageResponse::from);
    }

    /**
     * 활성 패키지 목록 조회 (사용자용)
     */
    @Transactional(readOnly = true)
    public List<CreditPackageResponse> getActivePackages() {
        return creditPackageRepository.findByIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc()
                .stream()
                .map(CreditPackageResponse::from)
                .toList();
    }

    /**
     * 표시 순서 변경
     */
    @Transactional
    public CreditPackageResponse updateDisplayOrder(UUID packageUuid, int displayOrder) {
        log.info("크레딧 패키지 표시 순서 변경: uuid={}, displayOrder={}", packageUuid, displayOrder);

        CreditPackage creditPackage = creditPackageRepository.findByUuidAndIsDeletedFalse(packageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_PACKAGE_NOT_FOUND));

        creditPackage.setDisplayOrder(displayOrder);
        creditPackage = creditPackageRepository.save(creditPackage);

        return CreditPackageResponse.from(creditPackage);
    }

    /**
     * 단위 금액으로 패키지 보너스율 변경
     */
    @Transactional
    public CreditPackageResponse updateBonusRateByUnitAmount(
            Integer unitAmount, BigDecimal bonusRate, Integer maxBonus) {
        log.info("보너스율 변경: unitAmount={}, bonusRate={}, maxBonus={}",
                unitAmount, bonusRate, maxBonus);

        // 3만원 미만은 보너스 적용 불가
        if (unitAmount < 30000) {
            throw new BusinessException(ErrorCode.BONUS_NOT_APPLICABLE);
        }

        CreditPackage creditPackage = creditPackageRepository.findByUnitAmountAndIsDeletedFalse(unitAmount)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_PACKAGE_NOT_FOUND));

        creditPackage.updateBonusRate(bonusRate, maxBonus);
        creditPackage = creditPackageRepository.save(creditPackage);

        log.info("보너스율 변경 완료: code={}, bonusRate={}",
                creditPackage.getCode(), creditPackage.getBonusRate());

        return CreditPackageResponse.from(creditPackage);
    }

    /**
     * 통계: 활성 패키지 수
     */
    @Transactional(readOnly = true)
    public long countActivePackages() {
        return creditPackageRepository.countByIsActiveTrueAndIsDeletedFalse();
    }

    /**
     * 허용된 단위 금액 목록 조회
     */
    public List<Integer> getAllowedUnitAmounts() {
        return ALLOWED_UNIT_AMOUNTS;
    }
}
