package com.hip.damoa.infra.scheduler;

import com.hip.damoa.domain.company.service.CompanyPartnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 제휴업체 스케줄러
 *
 * 일일 작업:
 * - 만료된 제휴 처리 (매일 자정)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyPartnershipScheduler {

    private final CompanyPartnershipService partnershipService;

    /**
     * 만료된 제휴 처리
     * 매일 자정에 실행 (00:00)
     *
     * end_date < today인 ACTIVE 제휴를 EXPIRED로 변경
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void processExpiredPartnerships() {
        try {
            log.info("=== 제휴업체 만료 처리 스케줄러 시작 ===");

            int expiredCount = partnershipService.expirePartnerships();

            log.info("=== 제휴업체 만료 처리 완료: {}개 처리됨 ===", expiredCount);

        } catch (Exception e) {
            log.error("제휴업체 만료 처리 중 오류 발생", e);
        }
    }

    /**
     * 초기 실행 (애플리케이션 시작 시)
     * 애플리케이션 시작 1분 후에 한번 실행하여 누적 처리
     */
    @Scheduled(initialDelay = 60000, fixedDelay = Long.MAX_VALUE)
    public void initialProcessing() {
        try {
            log.info("=== 초기 제휴업체 만료 처리 시작 ===");

            int expiredCount = partnershipService.expirePartnerships();

            log.info("=== 초기 제휴업체 만료 처리 완료: {}개 처리됨 ===", expiredCount);

        } catch (Exception e) {
            log.error("초기 제휴업체 만료 처리 중 오류 발생", e);
        }
    }

    /**
     * 수동 실행 API용
     */
    public int processExpiredPartnershipsNow() {
        log.info("=== 수동 제휴업체 만료 처리 시작 ===");
        int expiredCount = partnershipService.expirePartnerships();
        log.info("수동 만료 처리 완료: {}개 제휴 만료됨", expiredCount);
        return expiredCount;
    }
}
