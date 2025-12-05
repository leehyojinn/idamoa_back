package com.hip.damoa.domain.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.payment.model.*;
import com.hip.damoa.domain.payment.repository.*;
import com.hip.damoa.domain.payment.web.dto.*;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import com.hip.damoa.infra.payment.*;
import com.hip.damoa.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;
    private final CreditTransactionRepository transactionRepository;
    private final CreditPackageRepository creditPackageRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    // PaymentGateway Map (Bean 이름으로 주입)
    private final Map<String, PaymentGateway> paymentGateways;

    // Redis 키 상수
    private static final String PAYMENT_SESSION_PREFIX = "payment:session:";
    private static final Duration PAYMENT_SESSION_TTL = Duration.ofMinutes(30);

    // 환불 수수료율 상수
    private static final BigDecimal REFUND_FEE_RATE = BigDecimal.valueOf(0.10); // 10%
    private static final BigDecimal MIN_REFUND_AMOUNT = BigDecimal.valueOf(1000);

    // 트랜잭션 타입 상수
    public static final String TX_EARN = "EARN";
    public static final String TX_SPEND = "SPEND";
    public static final String TX_EXPIRE = "EXPIRE";
    public static final String TX_REFUND = "REFUND";

    // 엔티티 타입 상수
    public static final String ENTITY_PAYMENT = "PAYMENT";
    public static final String ENTITY_FILE_DOWNLOAD = "FILE_DOWNLOAD";
    public static final String ENTITY_AD_CAMPAIGN = "AD_CAMPAIGN";
    public static final String ENTITY_REFUND = "REFUND";

    // ========== 크레딧 조회 ==========

    @Transactional(readOnly = true)
    public Credit getCreditByEmail(String email) {
        User user = findUserByEmail(email);
        return creditRepository.findByUser(user)
                .orElse(null);
    }

    @Transactional
    public Credit getOrCreateCredit(User user) {
        return creditRepository.findByUser(user)
                .orElseGet(() -> createCreditForUser(user));
    }

    @Transactional(readOnly = true)
    public CreditBalanceResponse getBalance(String email) {
        Credit credit = getCreditByEmail(email);
        BigDecimal balance = credit != null ? credit.getAvailableCredits() : BigDecimal.ZERO;

        return CreditBalanceResponse.builder()
                .balance(balance)
                .currency("KRW")
                .build();
    }

    @Transactional(readOnly = true)
    public boolean hasEnoughCredits(String email, BigDecimal amount) {
        Credit credit = getCreditByEmail(email);
        if (credit == null) {
            return false;
        }
        return credit.getAvailableCredits().compareTo(amount) >= 0;
    }

    // ========== 크레딧 충전 ==========

    @Transactional
    public CreditPurchaseResponse initiateCreditPurchase(String email, CreditPurchaseRequest request) {
        log.info("크레딧 충전 시작: email={}, package={}, quantity={}",
                email, request.getPackageCode(), request.getQuantity());

        User user = findUserByEmail(email);

        // 패키지 검증 (DB에서 조회)
        CreditPackage pkg = creditPackageRepository.findByCodeAndIsDeletedFalse(request.getPackageCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDIT_PACKAGE));

        // 비활성 패키지는 충전 불가
        if (!pkg.getIsActive()) {
            throw new BusinessException(ErrorCode.INVALID_CREDIT_PACKAGE);
        }

        // 수량 검증
        int quantity = request.getQuantity();
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        // 동적으로 금액 계산
        int paymentAmount = pkg.calculatePaymentAmount(quantity);
        int bonusCredits = pkg.calculateBonusCredits(quantity);
        int totalCredits = pkg.calculateTotalCredits(quantity);

        // 주문 ID 생성
        String orderId = generateOrderId(user.getId(), pkg, quantity);

        // 결제 게이트웨이 선택
        PaymentGateway gateway = selectGateway(request.getPaymentMethod());

        // Payment 레코드 생성 (PENDING 상태)
        Map<String, Object> gatewayData = new HashMap<>();
        gatewayData.put("packageCode", pkg.getCode());
        gatewayData.put("packageId", pkg.getId());
        gatewayData.put("quantity", quantity);
        gatewayData.put("totalCredits", String.valueOf(totalCredits));
        gatewayData.put("bonusCredits", String.valueOf(bonusCredits));

        Payment payment = Payment.builder()
                .user(user)
                .paymentAmount(BigDecimal.valueOf(paymentAmount))
                .totalAmount(BigDecimal.valueOf(paymentAmount))
                .paymentMethod(request.getPaymentMethod())
                .entityType("CREDIT_PURCHASE")
                .transactionId(orderId)
                .paymentGatewayData(gatewayData)
                .build();
        payment = paymentRepository.save(payment);

        // 결제 세션 Redis 저장 (결제 완료 시 검증용)
        String sessionKey = PAYMENT_SESSION_PREFIX + orderId;
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("paymentId", payment.getId());
        sessionData.put("userId", user.getId());
        sessionData.put("packageCode", pkg.getCode());
        sessionData.put("packageId", pkg.getId());
        sessionData.put("quantity", quantity);
        sessionData.put("totalCredits", totalCredits);
        sessionData.put("bonusCredits", bonusCredits);
        sessionData.put("amount", String.valueOf(paymentAmount));

        redisService.setData(sessionKey, sessionData, PAYMENT_SESSION_TTL);

        // 표시 이름 생성 (예: "3만원권 x 10")
        String displayName = pkg.getDisplayName() + " x " + quantity;

        // PG 결제 준비
        PaymentPrepareRequest prepareRequest = PaymentPrepareRequest.builder()
                .orderId(orderId)
                .orderName("다모아 크레딧 충전 - " + displayName)
                .amount(BigDecimal.valueOf(paymentAmount))
                .customerEmail(email)
                .customerName(user.getEmail())
                .build();

        PaymentPrepareResponse prepareResponse = gateway.preparePayment(prepareRequest);

        log.info("크레딧 충전 준비 완료: paymentId={}, orderId={}, amount={}, credits={}",
                payment.getId(), orderId, paymentAmount, totalCredits);

        return CreditPurchaseResponse.builder()
                .paymentUuid(payment.getUuid())
                .orderId(orderId)
                .paymentUrl(prepareResponse.getPaymentUrl())
                .paymentAmount(BigDecimal.valueOf(paymentAmount))
                .totalCredits(BigDecimal.valueOf(totalCredits))
                .bonusCredits(BigDecimal.valueOf(bonusCredits))
                .packageDisplayName(displayName)
                .build();
    }

    @Transactional
    public void completeCreditPurchase(String orderId, String pgToken) {
        log.info("크레딧 충전 완료 처리: orderId={}", orderId);

        // Redis에서 세션 조회
        String sessionKey = PAYMENT_SESSION_PREFIX + orderId;
        Object sessionObj = redisService.getData(sessionKey);
        if (sessionObj == null) {
            throw new BusinessException(ErrorCode.PAYMENT_SESSION_EXPIRED);
        }

        Map<String, Object> session = convertToMap(sessionObj);

        Long paymentId = getLongValue(session.get("paymentId"));
        String packageCode = (String) session.get("packageCode");
        int quantity = getIntValue(session.get("quantity"));
        int totalCreditsFromSession = getIntValue(session.get("totalCredits"));
        String expectedAmountStr = (String) session.get("amount");
        BigDecimal expectedAmount = new BigDecimal(expectedAmountStr);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        // 이미 완료된 결제인지 확인
        if ("COMPLETED".equals(payment.getStatus())) {
            log.warn("이미 완료된 결제: paymentId={}", paymentId);
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }

        // 패키지 조회 (DB에서)
        CreditPackage pkg = creditPackageRepository.findByCodeAndIsDeletedFalse(packageCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDIT_PACKAGE));

        User user = payment.getUser();

        // 결제 승인 (실제 PG 연동)
        // pgToken은 토스의 경우 paymentKey, 카카오페이의 경우 pg_token
        PaymentGateway gateway = selectGateway(payment.getPaymentMethod());
        PaymentApprovalResponse approvalResponse;
        try {
            // 새로운 메서드 호출 (금액 포함) - 토스페이먼츠 등 금액 검증 필요한 PG용
            approvalResponse = gateway.approvePayment(pgToken, orderId, expectedAmount);
        } catch (Exception e) {
            log.error("PG 결제 승인 실패: orderId={}", orderId, e);
            payment.fail(e.getMessage());
            paymentRepository.save(payment);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        // ★★★ 핵심 보안 검증: PG에서 실제 결제된 금액과 예상 금액 비교 ★★★
        BigDecimal actualPaidAmount = approvalResponse.getAmount();
        if (actualPaidAmount == null || actualPaidAmount.compareTo(expectedAmount) != 0) {
            log.error("결제 금액 불일치 감지! orderId={}, expected={}, actual={}, userId={}",
                    orderId, expectedAmount, actualPaidAmount, user.getId());

            // 결제 실패 처리
            payment.fail("결제 금액 불일치: 예상=" + expectedAmount + ", 실제=" + actualPaidAmount);
            paymentRepository.save(payment);

            // 이미 결제된 금액이 있으면 자동 환불 시도
            if (actualPaidAmount != null && actualPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    log.warn("금액 불일치로 자동 환불 시도: orderId={}, amount={}", orderId, actualPaidAmount);
                    gateway.cancelPayment(approvalResponse.getPgTransactionId(), actualPaidAmount,
                            "결제 금액 불일치로 인한 자동 환불");
                } catch (Exception e) {
                    log.error("자동 환불 실패. 수동 처리 필요: orderId={}, pgTxId={}",
                            orderId, approvalResponse.getPgTransactionId(), e);
                }
            }

            // Redis 세션 삭제
            redisService.deleteData(sessionKey);

            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // Payment 레코드의 금액과도 일치하는지 추가 검증
        if (payment.getPaymentAmount().compareTo(actualPaidAmount) != 0) {
            log.error("Payment 레코드와 금액 불일치! paymentId={}, record={}, actual={}",
                    paymentId, payment.getPaymentAmount(), actualPaidAmount);
            payment.fail("Payment 레코드 금액 불일치");
            paymentRepository.save(payment);

            // 자동 환불
            try {
                gateway.cancelPayment(approvalResponse.getPgTransactionId(), actualPaidAmount,
                        "결제 금액 불일치로 인한 자동 환불");
            } catch (Exception e) {
                log.error("자동 환불 실패. 수동 처리 필요: orderId={}", orderId, e);
            }

            redisService.deleteData(sessionKey);
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        log.info("결제 금액 검증 완료: orderId={}, amount={}", orderId, actualPaidAmount);

        // 결제 상태 업데이트
        payment.complete();
        payment.setPgTransactionId(approvalResponse.getPgTransactionId());
        paymentRepository.save(payment);

        // 크레딧 충전 (세션에 저장된 값 사용 - 금액 검증 완료 후이므로 안전)
        Credit credit = creditRepository.findWithLockByUser(user)
                .orElseGet(() -> createCreditForUser(user));

        BigDecimal totalCredits = BigDecimal.valueOf(totalCreditsFromSession);
        credit.earn(totalCredits);
        creditRepository.save(credit);

        // 표시 이름 생성
        String displayName = pkg.getDisplayName() + " x " + quantity;

        // 거래 내역 기록
        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .credit(credit)
                .transactionType(TX_EARN)
                .amount(totalCredits)
                .balanceAfter(credit.getAvailableCredits())
                .reason("크레딧 충전 - " + displayName)
                .entityType(ENTITY_PAYMENT)
                .entityId(payment.getId())
                .build();
        transactionRepository.save(transaction);

        // Redis 세션 삭제
        redisService.deleteData(sessionKey);

        log.info("크레딧 충전 완료: userId={}, quantity={}, credits={}, balance={}",
                user.getId(), quantity, totalCredits, credit.getAvailableCredits());
    }

    // ========== 크레딧 사용 ==========

    @Transactional
    public CreditTransaction spendCredits(String email, BigDecimal amount, String reason,
                                           String entityType, Long entityId) {
        log.info("크레딧 사용: email={}, amount={}, reason={}", email, amount, reason);

        User user = findUserByEmail(email);

        Credit credit = creditRepository.findWithLockByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_CREDITS));

        if (credit.getAvailableCredits().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS);
        }

        credit.spend(amount);
        creditRepository.save(credit);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .credit(credit)
                .transactionType(TX_SPEND)
                .amount(amount.negate())
                .balanceAfter(credit.getAvailableCredits())
                .reason(reason)
                .entityType(entityType)
                .entityId(entityId)
                .build();
        transaction = transactionRepository.save(transaction);

        log.info("크레딧 사용 완료: userId={}, spent={}, balance={}",
                user.getId(), amount, credit.getAvailableCredits());

        return transaction;
    }

    @Transactional
    public CreditTransaction spendCredits(User user, BigDecimal amount, String reason,
                                           String entityType, Long entityId) {
        log.info("크레딧 사용: userId={}, amount={}, reason={}", user.getId(), amount, reason);

        Credit credit = creditRepository.findWithLockByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_CREDITS));

        if (credit.getAvailableCredits().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS);
        }

        credit.spend(amount);
        creditRepository.save(credit);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .credit(credit)
                .transactionType(TX_SPEND)
                .amount(amount.negate())
                .balanceAfter(credit.getAvailableCredits())
                .reason(reason)
                .entityType(entityType)
                .entityId(entityId)
                .build();
        transaction = transactionRepository.save(transaction);

        log.info("크레딧 사용 완료: userId={}, spent={}, balance={}",
                user.getId(), amount, credit.getAvailableCredits());

        return transaction;
    }

    // ========== 크레딧 환불 ==========

    @Transactional
    public CreditRefundResponse requestRefund(String email, CreditRefundRequest request) {
        log.info("크레딧 환불 요청: email={}, amount={}", email, request.getRefundAmount());

        User user = findUserByEmail(email);

        Credit credit = creditRepository.findWithLockByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_NOT_FOUND));

        BigDecimal requestedAmount = request.getRefundAmount();

        // 환불 가능 금액 검증
        if (requestedAmount.compareTo(MIN_REFUND_AMOUNT) < 0) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_TOO_SMALL);
        }

        if (credit.getAvailableCredits().compareTo(requestedAmount) < 0) {
            throw new BusinessException(ErrorCode.REFUND_EXCEEDS_BALANCE);
        }

        // 환불 수수료 계산
        BigDecimal feeAmount = calculateRefundFee(requestedAmount);
        BigDecimal actualRefundAmount = requestedAmount.subtract(feeAmount);

        // 크레딧 차감
        credit.spend(requestedAmount);
        creditRepository.save(credit);

        // 환불용 Payment 생성 (환불 처리 추적용)
        Payment refundPayment = Payment.builder()
                .user(user)
                .paymentAmount(actualRefundAmount.negate())
                .feeAmount(feeAmount)
                .totalAmount(requestedAmount.negate())
                .paymentMethod("REFUND")
                .status("PENDING")
                .entityType("CREDIT_REFUND")
                .build();
        refundPayment = paymentRepository.save(refundPayment);

        // Refund 레코드 생성
        Refund refund = Refund.builder()
                .payment(refundPayment)
                .refundAmount(actualRefundAmount)
                .refundReason(request.getRefundReason())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .accountHolder(request.getAccountHolder())
                .build();
        refund = refundRepository.save(refund);

        // 거래 내역 기록
        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .credit(credit)
                .transactionType(TX_REFUND)
                .amount(requestedAmount.negate())
                .balanceAfter(credit.getAvailableCredits())
                .reason("크레딧 환불 (수수료: " + feeAmount.intValue() + "원)")
                .entityType(ENTITY_REFUND)
                .entityId(refund.getId())
                .build();
        transactionRepository.save(transaction);

        log.info("크레딧 환불 요청 완료: userId={}, requested={}, fee={}, actual={}",
                user.getId(), requestedAmount, feeAmount, actualRefundAmount);

        return CreditRefundResponse.from(refund, feeAmount);
    }

    // ========== 거래 내역 조회 ==========

    @Transactional(readOnly = true)
    public Page<CreditTransactionResponse> getTransactions(String email, Pageable pageable) {
        User user = findUserByEmail(email);

        return transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(CreditTransactionResponse::from);
    }

    // ========== 패키지 목록 조회 ==========

    /**
     * 활성 패키지 목록 조회 (사용자용)
     */
    @Transactional(readOnly = true)
    public List<CreditPackageResponse> getAvailablePackages() {
        return creditPackageRepository.findByIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc()
                .stream()
                .map(CreditPackageResponse::from)
                .collect(Collectors.toList());
    }


    // ========== Private 헬퍼 메서드 ==========

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private Credit createCreditForUser(User user) {
        Credit credit = Credit.builder()
                .user(user)
                .build();
        return creditRepository.save(credit);
    }

    private PaymentGateway selectGateway(String paymentMethod) {
        if (paymentMethod == null) {
            paymentMethod = "TOSS";
        }

        String gatewayKey = switch (paymentMethod.toUpperCase()) {
            case "KAKAOPAY", "KAKAO_PAY" -> "kakaoPayGateway";
            case "TOSS", "TOSSPAY", "TOSS_PAYMENTS" -> "tossPaymentsGateway";
            default -> "tossPaymentsGateway";  // 기본값
        };

        PaymentGateway gateway = paymentGateways.get(gatewayKey);
        if (gateway == null) {
            log.warn("결제 게이트웨이를 찾을 수 없음: {}, 기본 게이트웨이 사용", gatewayKey);
            // 기본 게이트웨이 반환
            gateway = paymentGateways.values().stream().findFirst().orElse(null);
            if (gateway == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }
        }
        return gateway;
    }

    private String generateOrderId(Long userId, CreditPackage pkg, int quantity) {
        return String.format("CR_%d_%s_X%d_%d", userId, pkg.getCode(), quantity, System.currentTimeMillis());
    }

    private BigDecimal calculateRefundFee(BigDecimal amount) {
        // 10% 수수료, 100원 단위 절상
        BigDecimal fee = amount.multiply(REFUND_FEE_RATE);
        return fee.setScale(-2, RoundingMode.UP);  // 100원 단위 올림
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    private Long getLongValue(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        if (obj instanceof String) {
            return Long.parseLong((String) obj);
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        throw new IllegalArgumentException("Cannot convert to Long: " + obj);
    }

    private int getIntValue(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Long) {
            return ((Long) obj).intValue();
        }
        if (obj instanceof String) {
            return Integer.parseInt((String) obj);
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        throw new IllegalArgumentException("Cannot convert to int: " + obj);
    }
}
