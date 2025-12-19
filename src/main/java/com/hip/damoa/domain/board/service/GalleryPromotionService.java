package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.*;
import com.hip.damoa.domain.board.repository.BoardFilterOptionRepository;
import com.hip.damoa.domain.board.repository.GalleryPromotionPaymentRepository;
import com.hip.damoa.domain.board.repository.GalleryPromotionRepository;
import com.hip.damoa.domain.payment.model.CreditTransaction;
import com.hip.damoa.domain.payment.service.CreditService;
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
 * 갤러리 우대 등록 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryPromotionService {

    private final GalleryPromotionRepository promotionRepository;
    private final GalleryPromotionPaymentRepository paymentRepository;
    private final BoardFilterOptionRepository boardFilterOptionRepository;
    private final CreditService creditService;
    private final GalleryPromotionSettingsService settingsService;

    // 크레딧 엔티티 타입 상수
    private static final String ENTITY_GALLERY_PROMOTION = "GALLERY_PROMOTION";

    // ===== 우대 등록 =====

    /**
     * 우대 등록 생성 (갤러리 생성 시 호출)
     */
    @Transactional
    public GalleryPromotion createPromotion(User user, Board board, GalleryPromotionType type, Boolean autoRenew) {
        log.info("갤러리 우대 등록 시작: boardId={}, type={}, autoRenew={}", board.getId(), type, autoRenew);

        // 이미 활성 우대가 있는지 확인
        Optional<GalleryPromotion> existingPromotion = promotionRepository
                .findByBoardAndStatusAndIsDeletedFalse(board, "ACTIVE");
        if (existingPromotion.isPresent()) {
            throw new BusinessException(ErrorCode.GALLERY_PROMOTION_ALREADY_EXISTS);
        }

        // 동적 가격/가중치 조회
        BigDecimal price = settingsService.getPriceByType(type);
        Integer weight = settingsService.getWeightByType(type);

        // 크레딧 차감
        CreditTransaction transaction = creditService.spendCredits(
                user,
                price,
                "갤러리 우대 등록 - " + type.name(),
                ENTITY_GALLERY_PROMOTION,
                board.getId()
        );

        // 우대 등록 생성 (동적 가격/가중치 사용)
        GalleryPromotion promotion = GalleryPromotion.create(board, user, type, autoRenew, price, weight);
        promotion = promotionRepository.save(promotion);

        // 결제 내역 생성
        GalleryPromotionPayment payment = GalleryPromotionPayment.createInitial(promotion, transaction.getId());
        paymentRepository.save(payment);

        log.info("갤러리 우대 등록 완료: promotionId={}, amount={}, weight={}", promotion.getId(), price, weight);

        return promotion;
    }

    /**
     * 기존 갤러리에 우대 등록 (수정 API에서 호출)
     */
    @Transactional
    public GalleryPromotion addPromotionToExistingBoard(User user, Board board, GalleryPromotionType type, Boolean autoRenew) {
        // 소유자 확인
        if (!board.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.GALLERY_ACCESS_DENIED);
        }

        return createPromotion(user, board, type, autoRenew);
    }

    // ===== 우대 취소 =====

    /**
     * 우대 취소 (즉시 만료 아님, auto_renew만 false로)
     */
    @Transactional
    public GalleryPromotion cancelPromotion(User user, Board board) {
        log.info("갤러리 우대 취소: boardId={}", board.getId());

        GalleryPromotion promotion = promotionRepository
                .findByBoardAndStatusAndIsDeletedFalse(board, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.GALLERY_PROMOTION_NOT_FOUND));

        // 소유자 확인
        if (!promotion.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.GALLERY_ACCESS_DENIED);
        }

        promotion.cancel();
        promotion = promotionRepository.save(promotion);

        log.info("갤러리 우대 취소 완료: promotionId={}, 만료예정일={}", promotion.getId(), promotion.getEndDate());

        return promotion;
    }

    // ===== 업그레이드 =====

    /**
     * STANDARD → PREMIUM 업그레이드
     */
    @Transactional
    public GalleryPromotion upgradePromotion(User user, Board board) {
        log.info("갤러리 우대 업그레이드: boardId={}", board.getId());

        GalleryPromotion promotion = promotionRepository
                .findByBoardAndStatusAndIsDeletedFalse(board, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.GALLERY_PROMOTION_NOT_FOUND));

        // 소유자 확인
        if (!promotion.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.GALLERY_ACCESS_DENIED);
        }

        // 이미 PREMIUM인지 확인
        if (promotion.getPromotionType() == GalleryPromotionType.PREMIUM) {
            throw new BusinessException(ErrorCode.ALREADY_PREMIUM_PROMOTION);
        }

        // 동적 가격/가중치 조회
        BigDecimal upgradePrice = settingsService.getUpgradePrice();
        BigDecimal premiumPrice = settingsService.getPriceByType(GalleryPromotionType.PREMIUM);
        Integer premiumWeight = settingsService.getWeightByType(GalleryPromotionType.PREMIUM);

        // 차액 결제
        CreditTransaction transaction = creditService.spendCredits(
                user,
                upgradePrice,
                "갤러리 우대 업그레이드 (STANDARD → PREMIUM)",
                ENTITY_GALLERY_PROMOTION,
                promotion.getId()
        );

        // 업그레이드 처리 (동적 가격/가중치 사용)
        promotion.upgradeToPremium(premiumPrice, premiumWeight);
        promotion = promotionRepository.save(promotion);

        // 결제 내역 생성
        GalleryPromotionPayment payment = GalleryPromotionPayment.createUpgrade(promotion, upgradePrice, transaction.getId());
        paymentRepository.save(payment);

        log.info("갤러리 우대 업그레이드 완료: promotionId={}, upgradePrice={}, newWeight={}", promotion.getId(), upgradePrice, premiumWeight);

        return promotion;
    }

    // ===== 자동갱신 설정 =====

    /**
     * 자동갱신 설정 변경
     */
    @Transactional
    public GalleryPromotion updateAutoRenew(User user, Board board, boolean autoRenew) {
        log.info("갤러리 우대 자동갱신 설정: boardId={}, autoRenew={}", board.getId(), autoRenew);

        GalleryPromotion promotion = promotionRepository
                .findByBoardAndStatusAndIsDeletedFalse(board, "ACTIVE")
                .orElseThrow(() -> new BusinessException(ErrorCode.GALLERY_PROMOTION_NOT_FOUND));

        // 소유자 확인
        if (!promotion.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.GALLERY_ACCESS_DENIED);
        }

        promotion.setAutoRenew(autoRenew);
        promotion = promotionRepository.save(promotion);

        log.info("갤러리 우대 자동갱신 설정 완료: promotionId={}, autoRenew={}", promotion.getId(), autoRenew);

        return promotion;
    }

    // ===== 조회 =====

    /**
     * Board의 활성 우대 조회
     */
    @Transactional(readOnly = true)
    public Optional<GalleryPromotion> getActivePromotion(Board board) {
        return promotionRepository.findByBoardAndStatusAndIsDeletedFalse(board, "ACTIVE");
    }

    /**
     * Board UUID로 활성 우대 조회
     */
    @Transactional(readOnly = true)
    public Optional<GalleryPromotion> getActivePromotionByBoardUuid(UUID boardUuid) {
        return promotionRepository.findActiveByBoardUuid(boardUuid);
    }

    /**
     * 내 우대 갤러리 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<GalleryPromotion> getMyPromotions(User user, Pageable pageable) {
        return promotionRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * 활성 우대 전체 조회
     */
    @Transactional(readOnly = true)
    public List<GalleryPromotion> getAllActivePromotions() {
        return promotionRepository.findAllActive();
    }

    /**
     * 필터별 활성 우대 조회
     */
    @Transactional(readOnly = true)
    public List<GalleryPromotion> getActivePromotionsByFilter(List<Long> filterOptionIds) {
        if (filterOptionIds == null || filterOptionIds.isEmpty()) {
            return promotionRepository.findAllActive();
        }
        return promotionRepository.findActiveByFilterOptionIds(filterOptionIds);
    }

    // ===== Featured 갤러리 (가중치 랜덤) =====

    /**
     * 메인 우대 갤러리 조회 (가중치 기반 랜덤)
     */
    @Transactional(readOnly = true)
    public List<GalleryPromotion> getFeaturedPromotions(List<Long> filterOptionIds, int count) {
        log.info("Featured 우대 갤러리 조회: filterOptionIds={}, count={}", filterOptionIds, count);

        // 활성 우대 조회
        List<GalleryPromotion> activePromotions;
        if (filterOptionIds == null || filterOptionIds.isEmpty()) {
            activePromotions = promotionRepository.findAllActive();
        } else {
            activePromotions = promotionRepository.findActiveByFilterOptionIds(filterOptionIds);
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
    private List<GalleryPromotion> weightedRandomSelect(List<GalleryPromotion> promotions, int count) {
        if (promotions.size() <= count) {
            // 전체 개수가 요청 개수보다 적으면 셔플 후 반환
            List<GalleryPromotion> result = new ArrayList<>(promotions);
            Collections.shuffle(result);
            return result;
        }

        // 가중치 기반 선택
        int totalWeight = promotions.stream().mapToInt(GalleryPromotion::getWeight).sum();

        List<GalleryPromotion> result = new ArrayList<>();
        List<GalleryPromotion> pool = new ArrayList<>(promotions);
        Random random = new Random();

        while (result.size() < count && !pool.isEmpty()) {
            int rand = random.nextInt(totalWeight);
            int cumulative = 0;

            for (int i = 0; i < pool.size(); i++) {
                cumulative += pool.get(i).getWeight();
                if (rand < cumulative) {
                    GalleryPromotion selected = pool.remove(i);
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
    public List<GalleryPromotion> getExpiredPromotions() {
        return promotionRepository.findExpiredPromotions(LocalDate.now());
    }

    /**
     * 자동 갱신 대상 조회
     */
    @Transactional(readOnly = true)
    public List<GalleryPromotion> getAutoRenewalTargets() {
        return promotionRepository.findAutoRenewalTargets(LocalDate.now());
    }

    /**
     * 갱신 알림 대상 조회
     */
    @Transactional(readOnly = true)
    public List<GalleryPromotion> getRenewalNotificationTargets() {
        return promotionRepository.findRenewalNotificationTargets(LocalDate.now().plusDays(3));
    }

    /**
     * 만료 처리
     */
    @Transactional
    public void processExpiration(GalleryPromotion promotion) {
        log.info("우대 만료 처리: promotionId={}", promotion.getId());
        promotion.expire();
        promotionRepository.save(promotion);
    }

    /**
     * 자동 갱신 처리
     */
    @Transactional
    public boolean processAutoRenewal(GalleryPromotion promotion) {
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
                    "갤러리 우대 자동 갱신 - " + promotion.getPromotionType().name(),
                    ENTITY_GALLERY_PROMOTION,
                    promotion.getId()
            );

            // 기간 연장
            LocalDate newEndDate = promotion.getEndDate().plusMonths(1);
            promotion.extendPeriod(newEndDate);
            promotionRepository.save(promotion);

            // 결제 내역 생성
            GalleryPromotionPayment payment = GalleryPromotionPayment.createRenewal(promotion, newEndDate, transaction.getId());
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
    public void processRenewalNotification(GalleryPromotion promotion) {
        log.info("우대 갱신 알림 발송: promotionId={}", promotion.getId());
        promotion.markRenewalNotified();
        promotionRepository.save(promotion);
        // TODO: 실제 알림 발송 로직 (이메일, 푸시 등)
    }

    // ===== N+1 최적화용 =====

    /**
     * Board ID 목록으로 활성 우대 일괄 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, GalleryPromotion> getActivePromotionsByBoardIds(List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) {
            return Map.of();
        }
        return promotionRepository.findActiveByBoardIds(boardIds).stream()
                .collect(Collectors.toMap(p -> p.getBoard().getId(), p -> p, (a, b) -> a));
    }
}
