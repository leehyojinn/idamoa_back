package com.hip.damoa.infra.scheduler;

import com.hip.damoa.domain.portfolio.model.PortfolioPromotion;
import com.hip.damoa.domain.portfolio.service.PortfolioPromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 포트폴리오 프로모션 스케줄러
 * - 만료 처리
 * - 자동 갱신
 * - 갱신 알림
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioPromotionScheduler {

    private final PortfolioPromotionService promotionService;

    /**
     * 만료된 프로모션 처리 (매일 00:10)
     */
    @Scheduled(cron = "0 10 0 * * *")
    public void processExpiredPromotions() {
        log.info("포트폴리오 프로모션 만료 처리 시작");

        List<PortfolioPromotion> expiredPromotions = promotionService.getExpiredPromotions();

        int processedCount = 0;
        for (PortfolioPromotion promotion : expiredPromotions) {
            try {
                promotionService.processExpiration(promotion);
                processedCount++;
            } catch (Exception e) {
                log.error("프로모션 만료 처리 실패: promotionId={}", promotion.getId(), e);
            }
        }

        log.info("포트폴리오 프로모션 만료 처리 완료: 총 {} 건, 처리 {} 건",
                expiredPromotions.size(), processedCount);
    }

    /**
     * 자동 갱신 처리 (매일 00:30)
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void processAutoRenewals() {
        log.info("포트폴리오 프로모션 자동 갱신 시작");

        List<PortfolioPromotion> renewalTargets = promotionService.getAutoRenewalTargets();

        int successCount = 0;
        int failCount = 0;

        for (PortfolioPromotion promotion : renewalTargets) {
            try {
                boolean success = promotionService.processAutoRenewal(promotion);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("프로모션 자동 갱신 처리 중 예외: promotionId={}", promotion.getId(), e);
                failCount++;
            }
        }

        log.info("포트폴리오 프로모션 자동 갱신 완료: 총 {} 건, 성공 {} 건, 실패 {} 건",
                renewalTargets.size(), successCount, failCount);
    }

    /**
     * 갱신 알림 발송 (매일 09:00)
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendRenewalNotifications() {
        log.info("포트폴리오 프로모션 갱신 알림 시작");

        List<PortfolioPromotion> notificationTargets = promotionService.getRenewalNotificationTargets();

        int sentCount = 0;
        for (PortfolioPromotion promotion : notificationTargets) {
            try {
                promotionService.processRenewalNotification(promotion);
                sentCount++;
            } catch (Exception e) {
                log.error("갱신 알림 발송 실패: promotionId={}", promotion.getId(), e);
            }
        }

        log.info("포트폴리오 프로모션 갱신 알림 완료: 총 {} 건, 발송 {} 건",
                notificationTargets.size(), sentCount);
    }
}
