package com.hip.damoa.infra.payment;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * INICIS Payment Gateway Implementation
 *
 * INICIS 결제 연동 (샘플 구현)
 * 문서: https://manual.inicis.com/
 *
 * 환경변수 설정 필요:
 * - INICIS_MID: 가맹점 ID (테스트: INIpayTest)
 * - INICIS_SIGN_KEY: Sign Key (실제 발급받은 키)
 *
 * 주의: INICIS는 실제 구현 시 별도 라이브러리가 필요할 수 있습니다.
 * 이 구현은 기본 구조만 제공합니다.
 */
@Slf4j
@Component
public class InicisGateway implements PaymentGateway {

    @Value("${payment.inicis.mid}")
    private String mid;

    @Value("${payment.inicis.sign-key}")
    private String signKey;

    @Value("${payment.inicis.return-url}")
    private String returnUrl;

    @Value("${payment.inicis.close-url}")
    private String closeUrl;

    @Override
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        // INICIS는 프론트엔드에서 INIStdPay 모듈을 로드하여 결제창을 호출합니다
        // 서버는 결제 요청 시 signature(hash) 값을 생성하여 제공합니다

        try {
            String timestamp = String.valueOf(System.currentTimeMillis());
            String price = request.getAmount().toString();

            // Signature 생성 (SHA-256)
            String signData = mid + request.getOrderId() + price + timestamp + signKey;
            String signature = generateSignature(signData);

            log.info("INICIS payment prepared: orderId={}, signature={}", request.getOrderId(), signature);

            // 실제로는 프론트엔드에서 INIStdPay.pay() 호출
            return PaymentPrepareResponse.builder()
                .paymentUrl(null)  // 프론트엔드에서 직접 처리
                .pgTransactionId(signature)  // signature를 임시 TID로 사용
                .orderId(request.getOrderId())
                .build();

        } catch (Exception e) {
            log.error("INICIS prepare payment error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public PaymentApprovalResponse approvePayment(String pgToken, String orderId) {
        // INICIS 결제 승인은 프론트엔드에서 결제 완료 후
        // return_url로 POST 요청이 오면 서버에서 승인 처리합니다
        // 실제 구현 시 INICIS 제공 라이브러리 사용 필요

        log.info("INICIS payment approval (mock): orderId={}", orderId);

        return PaymentApprovalResponse.builder()
            .pgTransactionId(pgToken)
            .orderId(orderId)
            .paymentMethod("INICIS_CARD")
            .amount(BigDecimal.ZERO)  // 실제 값은 INICIS 응답에서 파싱
            .approvedAt(LocalDateTime.now())
            .build();
    }

    @Override
    public PaymentCancelResponse cancelPayment(String pgTransactionId, BigDecimal amount, String reason) {
        // INICIS 결제 취소는 별도 API 호출 또는 관리자 페이지에서 처리
        // 실제 구현 시 INICIS 취소 API 연동 필요

        log.info("INICIS payment cancel (mock): tid={}, amount={}", pgTransactionId, amount);

        return PaymentCancelResponse.builder()
            .pgTransactionId(pgTransactionId)
            .canceledAmount(amount)
            .cancelReason(reason)
            .canceledAt(LocalDateTime.now())
            .build();
    }

    @Override
    public PaymentQueryResponse queryPayment(String pgTransactionId) {
        // INICIS 거래 조회는 별도 API 호출
        // 실제 구현 시 INICIS 조회 API 연동 필요

        log.info("INICIS payment query (mock): tid={}", pgTransactionId);

        return PaymentQueryResponse.builder()
            .pgTransactionId(pgTransactionId)
            .status("APPROVED")
            .amount(BigDecimal.ZERO)
            .canceledAmount(BigDecimal.ZERO)
            .build();
    }

    @Override
    public boolean verifyWebhookSignature(String signature, String payload) {
        // INICIS는 signature 검증 필요
        // 실제 구현 시 INICIS 제공 검증 로직 사용
        return true;
    }

    /**
     * Generate SHA-256 signature
     */
    private String generateSignature(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes("UTF-8"));
        return HexFormat.of().formatHex(hash);
    }
}
