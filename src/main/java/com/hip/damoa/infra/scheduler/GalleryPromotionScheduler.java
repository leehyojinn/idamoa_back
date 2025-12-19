package com.hip.damoa.infra.scheduler;

import com.hip.damoa.domain.board.model.GalleryPromotion;
import com.hip.damoa.domain.board.service.GalleryPromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 갤러리 우대등록 스케줄러
 *
 * 일일 작업:
 * 1. 자동 갱신 처리 (매일 23:55) - 먼저 갱신 처리
 * 2. 만료된 우대 처리 (매일 자정)
 * 3. 갱신 알림 발송 (매일 09:00) - 3일 전 알림
 *
 * 우대 등록 규칙:
 * - 일반우대 (STANDARD): 50,000원/월, 가중치 1
 * - 강력우대 (PREMIUM): 100,000원/월, 가중치 3 (3배 노출)
 * - 월 단위 자동 갱신 (선택)
 * - 크레딧 부족 시 자동 갱신 실패 → 만료
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GalleryPromotionScheduler {

    private final GalleryPromotionService galleryPromotionService;

    /**
     * 만료된 우대 처리
     * 매일 자정에 실행 (00:00)
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void processExpiredPromotions() {
        try {
            log.info("=== 갤러리 우대 만료 처리 스케줄러 시작 ===");

            List<GalleryPromotion> expiredPromotions = galleryPromotionService.getExpiredPromotions();
            int expiredCount = 0;

            for (GalleryPromotion promotion : expiredPromotions) {
                try {
                    galleryPromotionService.processExpiration(promotion);
                    expiredCount++;
                } catch (Exception e) {
                    log.error("우대 만료 처리 실패: promotionId={}", promotion.getId(), e);
                }
            }

            log.info("만료된 우대 처리 완료: {}개", expiredCount);
            log.info("=== 갤러리 우대 만료 처리 완료 ===");

        } catch (Exception e) {
            log.error("갤러리 우대 만료 처리 중 오류 발생", e);
        }
    }

    /**
     * 자동 갱신 처리
     * 매일 23:55에 실행 (자정 전에 갱신하여 끊김 없이 진행)
     */
    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Seoul")
    public void processAutoRenewals() {
        try {
            log.info("=== 갤러리 우대 자동 갱신 처리 시작 ===");

            List<GalleryPromotion> autoRenewalTargets = galleryPromotionService.getAutoRenewalTargets();
            int renewedCount = 0;
            int failedCount = 0;

            for (GalleryPromotion promotion : autoRenewalTargets) {
                try {
                    boolean success = galleryPromotionService.processAutoRenewal(promotion);
                    if (success) {
                        renewedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (Exception e) {
                    log.error("우대 자동 갱신 처리 실패: promotionId={}", promotion.getId(), e);
                    failedCount++;
                }
            }

            log.info("자동 갱신 처리 완료: 성공 {}개, 실패 {}개", renewedCount, failedCount);
            log.info("=== 갤러리 우대 자동 갱신 처리 완료 ===");

        } catch (Exception e) {
            log.error("갤러리 우대 자동 갱신 처리 중 오류 발생", e);
        }
    }

    /**
     * 갱신 알림 발송
     * 매일 09:00에 실행 (3일 전 알림)
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendRenewalNotifications() {
        try {
            log.info("=== 갤러리 우대 갱신 알림 발송 시작 ===");

            List<GalleryPromotion> notificationTargets = galleryPromotionService.getRenewalNotificationTargets();
            int notifiedCount = 0;

            for (GalleryPromotion promotion : notificationTargets) {
                try {
                    galleryPromotionService.processRenewalNotification(promotion);
                    notifiedCount++;
                } catch (Exception e) {
                    log.error("우대 갱신 알림 발송 실패: promotionId={}", promotion.getId(), e);
                }
            }

            log.info("갱신 알림 발송 완료: {}개", notifiedCount);
            log.info("=== 갤러리 우대 갱신 알림 발송 완료 ===");

        } catch (Exception e) {
            log.error("갤러리 우대 갱신 알림 발송 중 오류 발생", e);
        }
    }

    /**
     * 초기 실행 (애플리케이션 시작 시)
     * 애플리케이션 시작 2분 후에 한번 실행하여 누적 처리
     * (AdCampaignScheduler와 겹치지 않도록 시간 차이를 둠)
     */
    @Scheduled(initialDelay = 120000, fixedDelay = Long.MAX_VALUE)
    public void initialProcessing() {
        try {
            log.info("=== 초기 갤러리 우대 처리 시작 ===");

            // 자동 갱신 처리
            List<GalleryPromotion> autoRenewalTargets = galleryPromotionService.getAutoRenewalTargets();
            int renewedCount = 0;
            for (GalleryPromotion promotion : autoRenewalTargets) {
                if (galleryPromotionService.processAutoRenewal(promotion)) {
                    renewedCount++;
                }
            }

            // 만료 처리
            List<GalleryPromotion> expiredPromotions = galleryPromotionService.getExpiredPromotions();
            int expiredCount = 0;
            for (GalleryPromotion promotion : expiredPromotions) {
                galleryPromotionService.processExpiration(promotion);
                expiredCount++;
            }

            // 갱신 알림 발송
            List<GalleryPromotion> notificationTargets = galleryPromotionService.getRenewalNotificationTargets();
            int notifiedCount = 0;
            for (GalleryPromotion promotion : notificationTargets) {
                galleryPromotionService.processRenewalNotification(promotion);
                notifiedCount++;
            }

            log.info("초기 처리 완료: 갱신 {}개, 만료 {}개, 알림 {}개",
                    renewedCount, expiredCount, notifiedCount);
            log.info("=== 초기 갤러리 우대 처리 완료 ===");

        } catch (Exception e) {
            log.error("초기 갤러리 우대 처리 중 오류 발생", e);
        }
    }

    /**
     * 수동 실행 API용 - 만료 처리
     */
    public int processExpiredPromotionsNow() {
        log.info("=== 수동 갤러리 우대 만료 처리 시작 ===");
        List<GalleryPromotion> expiredPromotions = galleryPromotionService.getExpiredPromotions();
        int count = 0;
        for (GalleryPromotion promotion : expiredPromotions) {
            galleryPromotionService.processExpiration(promotion);
            count++;
        }
        log.info("수동 만료 처리 완료: {}개", count);
        return count;
    }

    /**
     * 수동 실행 API용 - 자동 갱신 처리
     */
    public int processAutoRenewalsNow() {
        log.info("=== 수동 갤러리 우대 자동 갱신 처리 시작 ===");
        List<GalleryPromotion> targets = galleryPromotionService.getAutoRenewalTargets();
        int count = 0;
        for (GalleryPromotion promotion : targets) {
            if (galleryPromotionService.processAutoRenewal(promotion)) {
                count++;
            }
        }
        log.info("수동 자동 갱신 처리 완료: {}개", count);
        return count;
    }

    /**
     * 수동 실행 API용 - 갱신 알림 발송
     */
    public int sendRenewalNotificationsNow() {
        log.info("=== 수동 갤러리 우대 갱신 알림 발송 시작 ===");
        List<GalleryPromotion> targets = galleryPromotionService.getRenewalNotificationTargets();
        int count = 0;
        for (GalleryPromotion promotion : targets) {
            galleryPromotionService.processRenewalNotification(promotion);
            count++;
        }
        log.info("수동 갱신 알림 발송 완료: {}개", count);
        return count;
    }
}
