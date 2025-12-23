package com.hip.damoa.domain.portfolio.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.payment.model.CreditTransaction;
import com.hip.damoa.domain.payment.service.CreditService;
import com.hip.damoa.domain.portfolio.model.*;
import com.hip.damoa.domain.portfolio.repository.PortfolioFilterOptionRepository;
import com.hip.damoa.domain.portfolio.repository.PortfolioPromotionPaymentRepository;
import com.hip.damoa.domain.portfolio.repository.PortfolioPromotionRepository;
import com.hip.damoa.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 포트폴리오 프로모션 (우대등록) 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioPromotionService {

    private final PortfolioPromotionRepository promotionRepository;
    private final PortfolioPromotionPaymentRepository paymentRepository;
    private final PortfolioFilterOptionRepository filterOptionRepository;
    private final CreditService creditService;
    private final PortfolioPromotionSettingsService settingsService;

    private static final String ENTITY_PORTFOLIO_PROMOTION = "PORTFOLIO_PROMOTION";

    // ===== 우대 등록 =====

    /**
     * 우대 등록 생성
     */
    @Transactional
    public PortfolioPromotion createPromotion(User user, CompanyPortfolio portfolio, PortfolioPromotionType type, Boolean autoRenew) {
        log.info("포트폴리오 우대 등록 시작: portfolioId={}, type={}, autoRenew={}", portfolio.getId(), type, autoRenew);

        // 이미 활성 우대가 있는지 확인
        if (promotionRepository.hasActivePromotion(portfolio.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_PROMOTION_ALREADY_EXISTS);
        }

        // 동적 가격/가중치 조회
        BigDecimal price = settingsService.getPriceByType(type);
        Integer weight = settingsService.getWeightByType(type);

        // 크레딧 차감
        CreditTransaction transaction = creditService.spendCredits(
                user,
                price,
                "포트폴리오 우대 등록 - " + type.name(),
                ENTITY_PORTFOLIO_PROMOTION,
                portfolio.getId()
        );

        // 우대 등록 생성 (30일 기간)
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(30);

        PortfolioPromotion promotion = PortfolioPromotion.builder()
                .portfolio(portfolio)
                .user(user)
                .promotionType(type.name())
                .weight(weight)
                .monthlyPrice(price)
                .startDate(startDate)
                .endDate(endDate)
                .autoRenew(autoRenew != null ? autoRenew : false)
                .status(PortfolioPromotionStatus.ACTIVE)
                .build();

        promotion = promotionRepository.save(promotion);

        // 결제 내역 생성
        PortfolioPromotionPayment payment = PortfolioPromotionPayment.builder()
                .promotion(promotion)
                .paymentAmount(price)
                .paymentDate(LocalDate.now())
                .applyFromDate(startDate)
                .applyToDate(endDate)
                .paymentType(PortfolioPromotionPaymentType.INITIAL)
                .creditTransactionId(transaction.getId())
                .build();
        paymentRepository.save(payment);

        log.info("포트폴리오 우대 등록 완료: promotionId={}, amount={}, weight={}", promotion.getId(), price, weight);

        return promotion;
    }

    // ===== 우대 취소 =====

    /**
     * 우대 취소 (자동갱신 해제)
     */
    @Transactional
    public PortfolioPromotion cancelPromotion(User user, CompanyPortfolio portfolio) {
        log.info("포트폴리오 우대 취소: portfolioId={}", portfolio.getId());

        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_PROMOTION_NOT_FOUND));

        // 소유자 확인
        if (promotion.getUser() == null || !promotion.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_ACCESS_DENIED);
        }

        promotion.setAutoRenew(false);
        promotion = promotionRepository.save(promotion);

        log.info("포트폴리오 우대 취소 완료: promotionId={}, 만료예정일={}", promotion.getId(), promotion.getEndDate());

        return promotion;
    }

    // ===== 업그레이드 =====

    /**
     * STANDARD → PREMIUM 업그레이드
     */
    @Transactional
    public PortfolioPromotion upgradePromotion(User user, CompanyPortfolio portfolio) {
        log.info("포트폴리오 우대 업그레이드: portfolioId={}", portfolio.getId());

        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_PROMOTION_NOT_FOUND));

        // 소유자 확인
        if (promotion.getUser() == null || !promotion.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_ACCESS_DENIED);
        }

        // 이미 PREMIUM인지 확인
        if (PortfolioPromotionType.PREMIUM.name().equals(promotion.getPromotionType())) {
            throw new BusinessException(ErrorCode.ALREADY_PREMIUM_PROMOTION);
        }

        // 동적 가격/가중치 조회
        BigDecimal upgradePrice = settingsService.getUpgradePrice();
        BigDecimal premiumPrice = settingsService.getPriceByType(PortfolioPromotionType.PREMIUM);
        Integer premiumWeight = settingsService.getWeightByType(PortfolioPromotionType.PREMIUM);

        // 차액 결제
        CreditTransaction transaction = creditService.spendCredits(
                user,
                upgradePrice,
                "포트폴리오 우대 업그레이드 (STANDARD → PREMIUM)",
                ENTITY_PORTFOLIO_PROMOTION,
                promotion.getId()
        );

        // 업그레이드 처리
        promotion.upgrade(PortfolioPromotionType.PREMIUM.name(), premiumWeight, promotion.getEndDate(), premiumPrice);
        promotion = promotionRepository.save(promotion);

        // 결제 내역 생성
        PortfolioPromotionPayment payment = PortfolioPromotionPayment.builder()
                .promotion(promotion)
                .paymentAmount(upgradePrice)
                .paymentDate(LocalDate.now())
                .applyFromDate(LocalDate.now())
                .applyToDate(promotion.getEndDate())
                .paymentType(PortfolioPromotionPaymentType.UPGRADE)
                .creditTransactionId(transaction.getId())
                .build();
        paymentRepository.save(payment);

        log.info("포트폴리오 우대 업그레이드 완료: promotionId={}, upgradePrice={}, newWeight={}", promotion.getId(), upgradePrice, premiumWeight);

        return promotion;
    }

    // ===== 자동갱신 설정 =====

    /**
     * 자동갱신 설정 변경
     */
    @Transactional
    public PortfolioPromotion updateAutoRenew(User user, CompanyPortfolio portfolio, boolean autoRenew) {
        log.info("포트폴리오 우대 자동갱신 설정: portfolioId={}, autoRenew={}", portfolio.getId(), autoRenew);

        PortfolioPromotion promotion = promotionRepository.findActiveByPortfolioId(portfolio.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_PROMOTION_NOT_FOUND));

        // 소유자 확인
        if (promotion.getUser() == null || !promotion.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.PORTFOLIO_ACCESS_DENIED);
        }

        promotion.setAutoRenew(autoRenew);
        promotion = promotionRepository.save(promotion);

        log.info("포트폴리오 우대 자동갱신 설정 완료: promotionId={}, autoRenew={}", promotion.getId(), autoRenew);

        return promotion;
    }

    // ===== 조회 =====

    /**
     * 포트폴리오의 활성 우대 조회
     */
    @Transactional(readOnly = true)
    public Optional<PortfolioPromotion> getActivePromotion(CompanyPortfolio portfolio) {
        return promotionRepository.findActiveByPortfolioId(portfolio.getId());
    }

    /**
     * 사용자의 우대 포트폴리오 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PortfolioPromotion> getMyPromotions(User user) {
        return promotionRepository.findActiveByUserId(user.getId());
    }

    /**
     * 활성 우대 전체 조회
     */
    @Transactional(readOnly = true)
    public List<PortfolioPromotion> getAllActivePromotions() {
        return promotionRepository.findAllActive();
    }

    /**
     * 상태별 프로모션 조회 (관리자용)
     * @param status 프로모션 상태 (ACTIVE, EXPIRED, CANCELLED, null=전체)
     */
    @Transactional(readOnly = true)
    public List<PortfolioPromotion> getPromotionsByStatus(String status) {
        if (status == null || status.isBlank()) {
            // 전체 조회
            return promotionRepository.findAllByIsDeletedFalse();
        }
        return promotionRepository.findByStatusAndIsDeletedFalse(
                PortfolioPromotionStatus.valueOf(status.toUpperCase()));
    }

    // ===== Featured 포트폴리오 (가중치 랜덤) =====

    /**
     * 메인 우대 포트폴리오 조회 (가중치 기반 랜덤)
     */
    @Transactional(readOnly = true)
    public List<PortfolioPromotion> getFeaturedPromotions(List<Long> filterOptionIds, int count) {
        log.info("Featured 우대 포트폴리오 조회: filterOptionIds={}, count={}", filterOptionIds, count);

        List<PortfolioPromotion> activePromotions = promotionRepository.findAllActive();

        // 필터 적용
        if (filterOptionIds != null && !filterOptionIds.isEmpty()) {
            List<Long> portfolioIds = filterOptionRepository.findPortfolioIdsByAllFilterOptionIds(filterOptionIds, filterOptionIds.size());
            activePromotions = activePromotions.stream()
                    .filter(p -> portfolioIds.contains(p.getPortfolio().getId()))
                    .toList();
        }

        if (activePromotions.isEmpty()) {
            return List.of();
        }

        // 가중치 기반 랜덤 선택
        return weightedRandomSelect(activePromotions, count);
    }

    /**
     * 가중치 기반 랜덤 선택 알고리즘
     */
    private List<PortfolioPromotion> weightedRandomSelect(List<PortfolioPromotion> promotions, int count) {
        if (promotions.size() <= count) {
            List<PortfolioPromotion> result = new ArrayList<>(promotions);
            Collections.shuffle(result);
            return result;
        }

        int totalWeight = promotions.stream().mapToInt(PortfolioPromotion::getWeight).sum();

        List<PortfolioPromotion> result = new ArrayList<>();
        List<PortfolioPromotion> pool = new ArrayList<>(promotions);
        Random random = new Random();

        while (result.size() < count && !pool.isEmpty()) {
            int rand = random.nextInt(totalWeight);
            int cumulative = 0;

            for (int i = 0; i < pool.size(); i++) {
                cumulative += pool.get(i).getWeight();
                if (rand < cumulative) {
                    PortfolioPromotion selected = pool.remove(i);
                    result.add(selected);
                    totalWeight -= selected.getWeight();
                    break;
                }
            }
        }

        return result;
    }

    // ===== 스케줄러용 메서드 =====

    /**
     * 만료 대상 조회
     */
    @Transactional(readOnly = true)
    public List<PortfolioPromotion> getExpiredPromotions() {
        return promotionRepository.findExpiredPromotions(LocalDate.now());
    }

    /**
     * 자동 갱신 대상 조회
     */
    @Transactional(readOnly = true)
    public List<PortfolioPromotion> getAutoRenewalTargets() {
        return promotionRepository.findAutoRenewTargets(LocalDate.now());
    }

    /**
     * 갱신 알림 대상 조회
     */
    @Transactional(readOnly = true)
    public List<PortfolioPromotion> getRenewalNotificationTargets() {
        return promotionRepository.findUnnotifiedExpiringSoon(LocalDate.now().plusDays(3));
    }

    /**
     * 만료 처리
     */
    @Transactional
    public void processExpiration(PortfolioPromotion promotion) {
        log.info("우대 만료 처리: promotionId={}", promotion.getId());
        promotion.expire();
        promotionRepository.save(promotion);
    }

    /**
     * 자동 갱신 처리
     */
    @Transactional
    public boolean processAutoRenewal(PortfolioPromotion promotion) {
        log.info("우대 자동 갱신 처리: promotionId={}", promotion.getId());

        User user = promotion.getUser();
        BigDecimal price = promotion.getMonthlyPrice();

        // 크레딧 잔액 확인
        if (!creditService.hasEnoughCredits(user.getEmail(), price)) {
            log.warn("크레딧 부족으로 자동 갱신 실패: promotionId={}, userId={}", promotion.getId(), user.getId());
            promotion.setAutoRenew(false);
            promotionRepository.save(promotion);
            return false;
        }

        try {
            // 크레딧 차감
            CreditTransaction transaction = creditService.spendCredits(
                    user,
                    price,
                    "포트폴리오 우대 자동 갱신 - " + promotion.getPromotionType(),
                    ENTITY_PORTFOLIO_PROMOTION,
                    promotion.getId()
            );

            // 기간 연장 (30일)
            LocalDate newEndDate = promotion.getEndDate().plusDays(30);
            promotion.renew(newEndDate, price);
            promotionRepository.save(promotion);

            // 결제 내역 생성
            PortfolioPromotionPayment payment = PortfolioPromotionPayment.builder()
                    .promotion(promotion)
                    .paymentAmount(price)
                    .paymentDate(LocalDate.now())
                    .applyFromDate(promotion.getStartDate())
                    .applyToDate(newEndDate)
                    .paymentType(PortfolioPromotionPaymentType.RENEWAL)
                    .creditTransactionId(transaction.getId())
                    .build();
            paymentRepository.save(payment);

            log.info("우대 자동 갱신 완료: promotionId={}, newEndDate={}", promotion.getId(), newEndDate);
            return true;

        } catch (Exception e) {
            log.error("우대 자동 갱신 실패: promotionId={}", promotion.getId(), e);
            return false;
        }
    }

    /**
     * 갱신 알림 처리
     */
    @Transactional
    public void processRenewalNotification(PortfolioPromotion promotion) {
        log.info("우대 갱신 알림 발송: promotionId={}", promotion.getId());
        promotion.markRenewalNotified();
        promotionRepository.save(promotion);
        // TODO: 실제 알림 발송 로직
    }

    // ===== N+1 최적화용 =====

    /**
     * Portfolio ID 목록으로 활성 우대 일괄 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, PortfolioPromotion> getActivePromotionsByPortfolioIds(List<Long> portfolioIds) {
        if (portfolioIds == null || portfolioIds.isEmpty()) {
            return Map.of();
        }
        return promotionRepository.findAllActive().stream()
                .filter(p -> portfolioIds.contains(p.getPortfolio().getId()))
                .collect(Collectors.toMap(p -> p.getPortfolio().getId(), p -> p, (a, b) -> a));
    }
}
