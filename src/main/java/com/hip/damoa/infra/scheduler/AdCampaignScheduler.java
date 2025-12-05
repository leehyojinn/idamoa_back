package com.hip.damoa.infra.scheduler;

import com.hip.damoa.domain.ad.service.AdCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 광고 캠페인 스케줄러
 *
 * 일일 작업:
 * 1. 자동 갱신 처리 (매일 23:55) - 먼저 갱신 처리
 * 2. 만료된 캠페인 완료 처리 (매일 자정)
 * 3. 만료된 결제 소진 처리 (매일 자정)
 * 4. 모든 활성 캠페인 우선순위 재계산 (매일 00:05)
 * 5. 갱신 알림 발송 (매일 09:00) - 3일 전 알림
 *
 * 우선순위 계산:
 * - 1일 가치 = Σ(결제금액 / 결제시점 남은일수)
 * - 동점 시 secondary_score (평점, 리뷰수, 선등록 보너스)로 정렬
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdCampaignScheduler {

    private final AdCampaignService adCampaignService;

    /**
     * 만료된 캠페인/결제 처리
     * 매일 자정에 실행 (00:00)
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void processExpiredCampaigns() {
        try {
            log.info("=== 광고 캠페인 만료 처리 스케줄러 시작 ===");

            // 1. 만료된 캠페인 완료 처리
            int completedCampaigns = adCampaignService.completeExpiredCampaigns();
            log.info("만료된 캠페인 완료 처리: {}개", completedCampaigns);

            // 2. 만료된 결제 소진 처리
            int consumedPayments = adCampaignService.consumeExpiredPayments();
            log.info("만료된 결제 소진 처리: {}개", consumedPayments);

            log.info("=== 광고 캠페인 만료 처리 완료 ===");

        } catch (Exception e) {
            log.error("광고 캠페인 만료 처리 중 오류 발생", e);
        }
    }

    /**
     * 캠페인 우선순위 재계산
     * 매일 00:05에 실행
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void recalculatePriorities() {
        try {
            log.info("=== 광고 캠페인 우선순위 재계산 시작 ===");

            int recalculated = adCampaignService.recalculateAllPriorities();
            log.info("우선순위 재계산 완료: {}개 캠페인", recalculated);

            log.info("=== 광고 캠페인 우선순위 재계산 완료 ===");

        } catch (Exception e) {
            log.error("광고 캠페인 우선순위 재계산 중 오류 발생", e);
        }
    }

    /**
     * 자동 갱신 처리
     * 매일 23:55에 실행 (자정 전에 갱신하여 끊김 없이 진행)
     */
    @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Seoul")
    public void processAutoRenewals() {
        try {
            log.info("=== 광고 캠페인 자동 갱신 처리 시작 ===");

            int renewedCount = adCampaignService.processAutoRenewals();
            log.info("자동 갱신 처리 완료: {}개 캠페인 갱신됨", renewedCount);

            log.info("=== 광고 캠페인 자동 갱신 처리 완료 ===");

        } catch (Exception e) {
            log.error("광고 캠페인 자동 갱신 처리 중 오류 발생", e);
        }
    }

    /**
     * 갱신 알림 발송
     * 매일 09:00에 실행 (3일 전 알림)
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendRenewalNotifications() {
        try {
            log.info("=== 광고 캠페인 갱신 알림 발송 시작 ===");

            int notifiedCount = adCampaignService.sendRenewalNotifications();
            log.info("갱신 알림 발송 완료: {}개 캠페인", notifiedCount);

            log.info("=== 광고 캠페인 갱신 알림 발송 완료 ===");

        } catch (Exception e) {
            log.error("광고 캠페인 갱신 알림 발송 중 오류 발생", e);
        }
    }

    /**
     * 초기 실행 (애플리케이션 시작 시)
     * 애플리케이션 시작 1분 후에 한번 실행하여 누적 처리
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void initialProcessing() {
        try {
            log.info("=== 초기 광고 캠페인 처리 시작 ===");

            // 자동 갱신 처리
            int renewedCount = adCampaignService.processAutoRenewals();

            // 만료 처리
            int completedCampaigns = adCampaignService.completeExpiredCampaigns();
            int consumedPayments = adCampaignService.consumeExpiredPayments();

            // 우선순위 재계산
            int recalculated = adCampaignService.recalculateAllPriorities();

            // 갱신 알림 발송
            int notifiedCount = adCampaignService.sendRenewalNotifications();

            log.info("초기 처리 완료: 갱신 {}개, 캠페인 {}개 완료, 결제 {}개 소진, 우선순위 {}개 재계산, 알림 {}개",
                    renewedCount, completedCampaigns, consumedPayments, recalculated, notifiedCount);

        } catch (Exception e) {
            log.error("초기 광고 캠페인 처리 중 오류 발생", e);
        }
    }

    /**
     * 수동 실행 API용 - 만료 처리
     */
    public void processExpiredCampaignsNow() {
        log.info("=== 수동 광고 캠페인 만료 처리 시작 ===");
        int completedCampaigns = adCampaignService.completeExpiredCampaigns();
        int consumedPayments = adCampaignService.consumeExpiredPayments();
        log.info("수동 만료 처리 완료: 캠페인 {}개 완료, 결제 {}개 소진",
                completedCampaigns, consumedPayments);
    }

    /**
     * 수동 실행 API용 - 우선순위 재계산
     */
    public int recalculatePrioritiesNow() {
        log.info("=== 수동 광고 캠페인 우선순위 재계산 시작 ===");
        int recalculated = adCampaignService.recalculateAllPriorities();
        log.info("수동 우선순위 재계산 완료: {}개 캠페인", recalculated);
        return recalculated;
    }

    /**
     * 수동 실행 API용 - 자동 갱신 처리
     */
    public int processAutoRenewalsNow() {
        log.info("=== 수동 광고 캠페인 자동 갱신 처리 시작 ===");
        int renewedCount = adCampaignService.processAutoRenewals();
        log.info("수동 자동 갱신 처리 완료: {}개 캠페인 갱신됨", renewedCount);
        return renewedCount;
    }

    /**
     * 수동 실행 API용 - 갱신 알림 발송
     */
    public int sendRenewalNotificationsNow() {
        log.info("=== 수동 광고 캠페인 갱신 알림 발송 시작 ===");
        int notifiedCount = adCampaignService.sendRenewalNotifications();
        log.info("수동 갱신 알림 발송 완료: {}개 캠페인", notifiedCount);
        return notifiedCount;
    }
}
