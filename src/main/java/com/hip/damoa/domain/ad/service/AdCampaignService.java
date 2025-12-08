package com.hip.damoa.domain.ad.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.ad.model.AdCampaign;
import com.hip.damoa.domain.ad.model.AdPayment;
import com.hip.damoa.domain.ad.model.AdType;
import com.hip.damoa.domain.ad.repository.AdCampaignRepository;
import com.hip.damoa.domain.ad.repository.AdPaymentRepository;
import com.hip.damoa.domain.ad.web.dto.*;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.payment.model.CreditTransaction;
import com.hip.damoa.domain.payment.service.CreditService;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdCampaignService {

    private final AdCampaignRepository campaignRepository;
    private final AdPaymentRepository paymentRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;

    // 최소 일당 금액
    private static final BigDecimal MIN_DAILY_AMOUNT = BigDecimal.valueOf(500);

    // 허용된 광고 기간 (일)
    private static final List<Integer> ALLOWED_DURATIONS = List.of(7, 14, 30);

    // ========== 캠페인 생성 ==========

    @Transactional
    public AdCampaignResponse createCampaign(String email, AdCampaignCreateRequest request) {
        log.info("광고 캠페인 생성: email={}, company={}", email, request.getCompanyUuid());

        User user = findUserByEmail(email);
        Company company = findCompanyByUuid(request.getCompanyUuid());

        // 회사 소유자 확인
        if (!company.getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        // 기존 활성 캠페인 확인
        if (campaignRepository.existsActiveListingCampaign(company, LocalDate.now())) {
            throw new BusinessException(ErrorCode.AD_CAMPAIGN_ALREADY_EXISTS);
        }

        // 광고 기간 검증
        int durationDays = request.getDurationDays();
        if (!ALLOWED_DURATIONS.contains(durationDays)) {
            throw new BusinessException(ErrorCode.AD_INVALID_DURATION);
        }

        // 결제 금액 검증
        BigDecimal paymentAmount = request.getPaymentAmount();
        BigDecimal minAmount = MIN_DAILY_AMOUNT.multiply(BigDecimal.valueOf(durationDays));
        if (paymentAmount.compareTo(minAmount) < 0) {
            throw new BusinessException(ErrorCode.AD_MIN_DAILY_AMOUNT_NOT_MET);
        }

        // 크레딧 잔액 확인 및 차감
        CreditTransaction creditTx = creditService.spendCredits(
                user,
                paymentAmount,
                String.format("광고 캠페인 결제 (%d일)", durationDays),
                CreditService.ENTITY_AD_CAMPAIGN,
                null  // 캠페인 ID는 아직 없음, 이후 업데이트
        );

        // 시작일과 종료일 계산
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(durationDays - 1);

        // 캠페인 생성
        AdCampaign campaign = AdCampaign.builder()
                .company(company)
                .name(request.getName() != null ? request.getName() : company.getName() + " 광고")
                .description(request.getDescription())
                .adType(AdType.LISTING)
                .startDate(startDate)
                .endDate(endDate)
                .durationDays(durationDays)
                .minDailyAmount(MIN_DAILY_AMOUNT)
                .autoRenew(request.getAutoRenew() != null ? request.getAutoRenew() : false)
                .status("ACTIVE")
                .build();

        campaign = campaignRepository.save(campaign);

        // 사이클 정보 초기화
        campaign.initializeCycle(startDate, paymentAmount);

        // 결제 내역 생성
        AdPayment payment = AdPayment.createInitial(
                campaign,
                paymentAmount,
                startDate,
                endDate,
                creditTx
        );
        payment = paymentRepository.save(payment);

        // 우선순위 점수 계산 및 업데이트
        updateCampaignPriority(campaign);

        log.info("광고 캠페인 생성 완료: campaignId={}, amount={}", campaign.getId(), paymentAmount);

        return AdCampaignResponse.from(campaign, List.of(payment));
    }

    // ========== 추가 결제 ==========

    @Transactional
    public AdCampaignResponse addPayment(String email, UUID campaignUuid, AdPaymentRequest request) {
        log.info("광고 추가 결제: email={}, campaign={}", email, campaignUuid);

        User user = findUserByEmail(email);
        AdCampaign campaign = campaignRepository.findWithLockByUuid(campaignUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.AD_CAMPAIGN_NOT_FOUND));

        // 회사 소유자 확인
        if (!campaign.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        // 활성 캠페인인지 확인
        if (!campaign.isActive()) {
            throw new BusinessException(ErrorCode.AD_CAMPAIGN_NOT_ACTIVE);
        }

        // 결제 금액 검증 (최소 일당 금액 * 남은 일수)
        BigDecimal paymentAmount = request.getPaymentAmount();
        int remainingDays = campaign.getRemainingDays();
        BigDecimal minAmount = MIN_DAILY_AMOUNT.multiply(BigDecimal.valueOf(Math.min(remainingDays, 1)));
        if (paymentAmount.compareTo(minAmount) < 0) {
            throw new BusinessException(ErrorCode.AD_MIN_DAILY_AMOUNT_NOT_MET);
        }

        // 크레딧 차감
        CreditTransaction creditTx = creditService.spendCredits(
                user,
                paymentAmount,
                "광고 캠페인 추가 결제",
                CreditService.ENTITY_AD_CAMPAIGN,
                campaign.getId()
        );

        // 적용 기간 결정 (오늘부터 캠페인 종료일까지)
        LocalDate applyFromDate = LocalDate.now();
        LocalDate applyToDate = campaign.getEndDate();

        // 추가 결제 생성
        AdPayment payment = AdPayment.createAdditional(
                campaign,
                paymentAmount,
                applyFromDate,
                applyToDate,
                creditTx
        );
        payment = paymentRepository.save(payment);

        // 총 지출 및 누적 결제 업데이트
        campaign.addSpent(paymentAmount);
        campaign.addAccumulatedPayment(paymentAmount);

        // 우선순위 재계산
        updateCampaignPriority(campaign);

        log.info("광고 추가 결제 완료: campaignId={}, amount={}, accumulated={}",
                campaign.getId(), paymentAmount, campaign.getAccumulatedPayment());

        List<AdPayment> payments = paymentRepository.findActiveByCampaignId(campaign.getId(), LocalDate.now());
        return AdCampaignResponse.from(campaign, payments);
    }

    // ========== 캠페인 취소 (자동 갱신 중단) ==========

    /**
     * 캠페인 자동 갱신 취소
     * - 현재 사이클은 끝까지 진행 (환불 없음)
     * - 다음 자동 갱신만 중단
     */
    @Transactional
    public AdCampaignResponse cancelCampaign(String email, UUID campaignUuid) {
        log.info("광고 캠페인 자동 갱신 취소: email={}, campaign={}", email, campaignUuid);

        User user = findUserByEmail(email);
        AdCampaign campaign = campaignRepository.findWithLockByUuid(campaignUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.AD_CAMPAIGN_NOT_FOUND));

        // 회사 소유자 확인
        if (!campaign.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        // 이미 취소/완료된 캠페인인지 확인
        if ("CANCELLED".equals(campaign.getStatus()) || "COMPLETED".equals(campaign.getStatus())) {
            throw new BusinessException(ErrorCode.AD_CAMPAIGN_ALREADY_ENDED);
        }

        // 자동 갱신 중단 (캠페인은 종료일까지 계속 진행)
        campaign.setAutoRenew(false);
        campaignRepository.save(campaign);

        log.info("광고 캠페인 자동 갱신 취소 완료: campaignId={}, endDate={}",
                campaign.getId(), campaign.getEndDate());

        List<AdPayment> payments = paymentRepository.findActiveByCampaignId(campaign.getId(), LocalDate.now());
        return AdCampaignResponse.from(campaign, payments);
    }

    // ========== 자동 갱신 토글 ==========

    /**
     * 자동 갱신 설정 토글
     */
    @Transactional
    public AdCampaignResponse toggleAutoRenew(String email, UUID campaignUuid, boolean autoRenew) {
        log.info("광고 캠페인 자동 갱신 설정: email={}, campaign={}, autoRenew={}", email, campaignUuid, autoRenew);

        User user = findUserByEmail(email);
        AdCampaign campaign = campaignRepository.findWithLockByUuid(campaignUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.AD_CAMPAIGN_NOT_FOUND));

        // 회사 소유자 확인
        if (!campaign.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        // 활성 캠페인인지 확인
        if (!campaign.isActive()) {
            throw new BusinessException(ErrorCode.AD_CAMPAIGN_NOT_ACTIVE);
        }

        campaign.setAutoRenew(autoRenew);

        // 자동 갱신 켜면 알림 상태 초기화 (다시 3일 전 알림 발송)
        if (autoRenew && Boolean.TRUE.equals(campaign.getRenewalNotified())) {
            // 이미 알림을 보냈다면 유지 (중복 알림 방지)
        }

        campaignRepository.save(campaign);

        log.info("광고 캠페인 자동 갱신 설정 완료: campaignId={}, autoRenew={}", campaign.getId(), autoRenew);

        List<AdPayment> payments = paymentRepository.findActiveByCampaignId(campaign.getId(), LocalDate.now());
        return AdCampaignResponse.from(campaign, payments);
    }

    // ========== 조회 API ==========

    @Transactional(readOnly = true)
    public AdCampaignResponse getCampaign(String email, UUID campaignUuid) {
        User user = findUserByEmail(email);
        AdCampaign campaign = campaignRepository.findByUuidAndIsDeletedFalse(campaignUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.AD_CAMPAIGN_NOT_FOUND));

        // 회사 소유자 확인
        if (!campaign.getCompany().getOwner().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMPANY_ACCESS_DENIED);
        }

        List<AdPayment> payments = paymentRepository.findActiveByCampaignId(campaign.getId(), LocalDate.now());
        return AdCampaignResponse.from(campaign, payments);
    }

    /**
     * 관리자용 캠페인 조회 (소유자 확인 없음)
     */
    @Transactional(readOnly = true)
    public AdCampaignResponse getCampaignForAdmin(UUID campaignUuid) {
        AdCampaign campaign = campaignRepository.findByUuidAndIsDeletedFalse(campaignUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.AD_CAMPAIGN_NOT_FOUND));

        List<AdPayment> payments = paymentRepository.findActiveByCampaignId(campaign.getId(), LocalDate.now());
        return AdCampaignResponse.from(campaign, payments);
    }

    @Transactional(readOnly = true)
    public AdCampaignResponse getMyCampaign(String email) {
        User user = findUserByEmail(email);
        Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        AdCampaign campaign = campaignRepository.findByCompanyAndStatusAndIsDeletedFalse(company, "ACTIVE")
                .orElse(null);

        if (campaign == null) {
            return null;
        }

        List<AdPayment> payments = paymentRepository.findActiveByCampaignId(campaign.getId(), LocalDate.now());
        return AdCampaignResponse.from(campaign, payments);
    }

    @Transactional(readOnly = true)
    public Page<AdCampaignResponse> getMyCampaignHistory(String email, Pageable pageable) {
        User user = findUserByEmail(email);
        Company company = companyRepository.findByOwnerAndIsDeletedFalse(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        return campaignRepository.findByCompanyAndIsDeletedFalse(company, pageable)
                .map(campaign -> {
                    List<AdPayment> payments = paymentRepository.findByCampaignAndIsDeletedFalse(campaign);
                    return AdCampaignResponse.from(campaign, payments);
                });
    }

    @Transactional(readOnly = true)
    public List<AdCampaignRankingResponse> getActiveCampaignRanking(int limit) {
        LocalDate today = LocalDate.now();
        List<AdCampaign> campaigns = campaignRepository.findActiveCampaignsByPriority(today);

        return campaigns.stream()
                .limit(limit)
                .map((campaign) -> {
                    int rank = campaigns.indexOf(campaign) + 1;
                    return AdCampaignRankingResponse.from(campaign, rank);
                })
                .collect(Collectors.toList());
    }

    // ========== 우선순위 계산 ==========

    /**
     * 캠페인 우선순위 업데이트
     *
     * 1일 가치(dailyValue) 계산:
     * - 7일에 700원 → dailyValue = 100원
     * - 추가 충전 600원 (남은 6일) → 추가 dailyValue = 100원
     * - 총 dailyValue = 200원
     *
     * 우선순위 점수(priorityScore) = 모든 활성 결제의 dailyValue 합
     */
    @Transactional
    public void updateCampaignPriority(AdCampaign campaign) {
        LocalDate today = LocalDate.now();

        // 만료된 캠페인은 우선순위 0
        if (campaign.isExpired()) {
            campaign.updateDailyValue(BigDecimal.ZERO);
            campaign.updateLastCalculatedAt();
            return;
        }

        // 현재 유효한 결제들의 1일 가치(dailyValue) 합산
        // 각 결제의 dailyValue = 결제금액 / 적용일수
        BigDecimal totalDailyValue = paymentRepository.sumDailyValueForCampaign(campaign, today);

        if (totalDailyValue == null) {
            totalDailyValue = BigDecimal.ZERO;
        }

        campaign.updateDailyValue(totalDailyValue.setScale(2, RoundingMode.HALF_UP));

        // 2차 점수 계산 (회사 평점, 리뷰수, 포트폴리오수 + 선등록 보너스)
        BigDecimal secondaryScore = calculateSecondaryScore(campaign);
        campaign.updateSecondaryScore(secondaryScore);

        campaign.updateLastCalculatedAt();
        campaignRepository.save(campaign);

        log.debug("캠페인 우선순위 업데이트: campaignId={}, dailyValue={}, secondary={}",
                campaign.getId(), totalDailyValue, secondaryScore);
    }

    /**
     * 모든 활성 캠페인의 우선순위 재계산
     */
    @Transactional
    public int recalculateAllPriorities() {
        LocalDate today = LocalDate.now();
        List<AdCampaign> campaigns = campaignRepository.findCampaignsForPriorityUpdate(today);

        int count = 0;
        for (AdCampaign campaign : campaigns) {
            updateCampaignPriority(campaign);
            count++;
        }

        log.info("전체 캠페인 우선순위 재계산 완료: count={}", count);
        return count;
    }

    /**
     * 만료된 캠페인 완료 처리
     */
    @Transactional
    public int completeExpiredCampaigns() {
        LocalDate today = LocalDate.now();
        List<AdCampaign> expiredCampaigns = campaignRepository.findExpiredCampaigns(today);

        int count = 0;
        for (AdCampaign campaign : expiredCampaigns) {
            campaign.complete();
            campaignRepository.save(campaign);

            // 활성 결제 소진 처리
            List<AdPayment> activePayments = paymentRepository.findByCampaignAndStatusAndIsDeletedFalse(campaign, "ACTIVE");
            for (AdPayment payment : activePayments) {
                payment.consume();
                paymentRepository.save(payment);
            }

            count++;
        }

        log.info("만료된 캠페인 완료 처리: count={}", count);
        return count;
    }

    /**
     * 만료된 결제 소진 처리
     */
    @Transactional
    public int consumeExpiredPayments() {
        LocalDate today = LocalDate.now();
        List<AdPayment> expiredPayments = paymentRepository.findExpiredPayments(today);

        int count = 0;
        for (AdPayment payment : expiredPayments) {
            payment.consume();
            paymentRepository.save(payment);
            count++;
        }

        log.info("만료된 결제 소진 처리: count={}", count);
        return count;
    }

    // ========== 광고 대상 회사 조회 (Company 정렬용) ==========

    /**
     * 광고 중인 회사 ID 목록과 우선순위 점수 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getAdvertisingCompanyIds() {
        return campaignRepository.findCompanyIdsWithActiveListingCampaign(LocalDate.now());
    }

    // ========== 자동 갱신 처리 ==========

    /**
     * 자동 갱신 대상 캠페인 처리
     * - 오늘 종료되는 캠페인 중 autoRenew=true인 캠페인 처리
     * - 누적 결제 금액으로 새 사이클 시작
     * - 크레딧 부족 시 자동 갱신 중단 및 캠페인 완료 처리
     *
     * @return 갱신된 캠페인 수
     */
    @Transactional
    public int processAutoRenewals() {
        LocalDate today = LocalDate.now();
        List<AdCampaign> eligibleCampaigns = campaignRepository.findCampaignsForAutoRenewal(today);

        int renewedCount = 0;
        int failedCount = 0;

        for (AdCampaign campaign : eligibleCampaigns) {
            try {
                boolean renewed = processAutoRenewal(campaign);
                if (renewed) {
                    renewedCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("자동 갱신 처리 실패: campaignId={}", campaign.getId(), e);
                failedCount++;
            }
        }

        log.info("자동 갱신 처리 완료: 성공={}, 실패={}", renewedCount, failedCount);
        return renewedCount;
    }

    /**
     * 단일 캠페인 자동 갱신 처리
     */
    private boolean processAutoRenewal(AdCampaign campaign) {
        User user = campaign.getCompany().getOwner();
        BigDecimal renewalAmount = campaign.getRenewalAmount();

        // 최소 금액 검증
        BigDecimal minAmount = MIN_DAILY_AMOUNT.multiply(BigDecimal.valueOf(campaign.getDurationDays()));
        if (renewalAmount.compareTo(minAmount) < 0) {
            log.warn("자동 갱신 금액 부족: campaignId={}, renewalAmount={}, minRequired={}",
                    campaign.getId(), renewalAmount, minAmount);
            campaign.setAutoRenew(false);
            campaign.complete();
            campaignRepository.save(campaign);
            return false;
        }

        // 크레딧 잔액 확인 및 차감 시도
        try {
            CreditTransaction creditTx = creditService.spendCredits(
                    user,
                    renewalAmount,
                    String.format("광고 캠페인 자동 갱신 (%d일)", campaign.getDurationDays()),
                    CreditService.ENTITY_AD_CAMPAIGN,
                    campaign.getId()
            );

            // 새 사이클 시작
            LocalDate newStartDate = campaign.getEndDate().plusDays(1);
            LocalDate newEndDate = newStartDate.plusDays(campaign.getDurationDays() - 1);

            // 기존 활성 결제 소진 처리
            List<AdPayment> activePayments = paymentRepository.findByCampaignAndStatusAndIsDeletedFalse(campaign, "ACTIVE");
            for (AdPayment payment : activePayments) {
                payment.consume();
                paymentRepository.save(payment);
            }

            // 새 결제 생성
            AdPayment newPayment = AdPayment.createInitial(
                    campaign,
                    renewalAmount,
                    newStartDate,
                    newEndDate,
                    creditTx
            );
            paymentRepository.save(newPayment);

            // 캠페인 새 사이클 시작
            campaign.startNewCycle(newStartDate, newEndDate, renewalAmount);
            campaign.addSpent(renewalAmount);

            // 우선순위 재계산
            updateCampaignPriority(campaign);

            log.info("자동 갱신 성공: campaignId={}, amount={}, newEndDate={}",
                    campaign.getId(), renewalAmount, newEndDate);

            return true;

        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.INSUFFICIENT_CREDITS) {
                log.warn("크레딧 부족으로 자동 갱신 실패: campaignId={}", campaign.getId());
                campaign.setAutoRenew(false);
                campaign.complete();
                campaignRepository.save(campaign);
                // TODO: 사용자에게 크레딧 부족 알림 발송
                return false;
            }
            throw e;
        }
    }

    /**
     * 갱신 알림 발송 대상 조회 및 알림 발송
     * - 종료 3일 전 알림
     * - 아직 알림 발송 안한 캠페인
     *
     * @return 알림 발송 캠페인 수
     */
    @Transactional
    public int sendRenewalNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate targetEndDate = today.plusDays(3);

        List<AdCampaign> campaigns = campaignRepository.findCampaignsForRenewalNotification(targetEndDate);

        int notifiedCount = 0;
        for (AdCampaign campaign : campaigns) {
            try {
                // 알림 발송 로직 (실제 구현은 NotificationService 연동 필요)
                sendRenewalNotification(campaign);

                // 알림 발송 완료 처리
                campaign.markRenewalNotified();
                campaignRepository.save(campaign);

                notifiedCount++;
                log.info("갱신 알림 발송: campaignId={}, endDate={}, renewalAmount={}",
                        campaign.getId(), campaign.getEndDate(), campaign.getRenewalAmount());

            } catch (Exception e) {
                log.error("갱신 알림 발송 실패: campaignId={}", campaign.getId(), e);
            }
        }

        log.info("갱신 알림 발송 완료: count={}", notifiedCount);
        return notifiedCount;
    }

    /**
     * 갱신 알림 발송 (실제 구현)
     */
    private void sendRenewalNotification(AdCampaign campaign) {
        // TODO: 실제 알림 서비스 연동
        // - 이메일, 푸시, SMS 등
        // - 갱신 예정 금액, 일시, 크레딧 잔액 정보 포함
        log.info("[알림] 광고 캠페인 자동 갱신 예정: companyId={}, campaignId={}, endDate={}, renewalAmount={}",
                campaign.getCompany().getId(), campaign.getId(),
                campaign.getEndDate(), campaign.getRenewalAmount());
    }

    // ========== Private 헬퍼 메서드 ==========

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Company findCompanyByUuid(UUID uuid) {
        return companyRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));
    }

    private BigDecimal calculateSecondaryScore(AdCampaign campaign) {
        Company company = campaign.getCompany();

        // 점수 계산: 평점 * 1000 + 리뷰수 * 10 + 포트폴리오수 * 5
        BigDecimal score = BigDecimal.ZERO;

        if (company.getAvgRating() != null) {
            score = score.add(company.getAvgRating().multiply(BigDecimal.valueOf(1000)));
        }
        if (company.getReviewCount() != null) {
            score = score.add(BigDecimal.valueOf(company.getReviewCount() * 10));
        }
        if (company.getPortfolioCount() != null) {
            score = score.add(BigDecimal.valueOf(company.getPortfolioCount() * 5));
        }

        // 선등록 보너스: 캠페인 생성 시간이 오래될수록 높은 점수
        LocalDateTime createdAt = campaign.getCreatedAt();
        if (createdAt != null) {
            // 2100년까지의 차이를 초 단위로 계산하여 정규화
            long epochDiff = LocalDateTime.of(2100, 1, 1, 0, 0)
                    .toEpochSecond(java.time.ZoneOffset.UTC) - createdAt.toEpochSecond(java.time.ZoneOffset.UTC);
            score = score.add(BigDecimal.valueOf(epochDiff / 1000000.0));
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }
}
