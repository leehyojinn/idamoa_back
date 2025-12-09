package com.hip.damoa.infra.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * KakaoPay Payment Gateway Implementation
 *
 * 카카오페이 결제 연동
 * 문서: https://developers.kakao.com/docs/latest/ko/kakaopay/common
 *
 * 환경변수 설정 필요:
 * - KAKAOPAY_ADMIN_KEY: 카카오페이 Admin Key
 * - KAKAOPAY_CID: 가맹점 코드 (테스트: TC0ONETIME)
 */
@Slf4j
@Component
public class KakaoPayGateway implements PaymentGateway {

    private static final String READY_URL = "https://kapi.kakao.com/v1/payment/ready";
    private static final String APPROVE_URL = "https://kapi.kakao.com/v1/payment/approve";
    private static final String CANCEL_URL = "https://kapi.kakao.com/v1/payment/cancel";
    private static final String ORDER_URL = "https://kapi.kakao.com/v1/payment/order";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.kakaopay.admin-key}")
    private String adminKey;

    @Value("${payment.kakaopay.cid}")
    private String cid;

    @Value("${payment.kakaopay.approval-url}")
    private String approvalUrl;

    @Value("${payment.kakaopay.cancel-url}")
    private String cancelUrl;

    @Value("${payment.kakaopay.fail-url}")
    private String failUrl;

    public KakaoPayGateway() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + adminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 동적 URL 사용 (프론트엔드에서 제공된 경우), 없으면 서버 설정 사용
            String effectiveApprovalUrl = hasValue(request.getSuccessUrl()) ? request.getSuccessUrl() : approvalUrl;
            String effectiveCancelUrl = hasValue(request.getCancelUrl()) ? request.getCancelUrl() : cancelUrl;
            String effectiveFailUrl = hasValue(request.getFailUrl()) ? request.getFailUrl() : failUrl;

            log.debug("KakaoPay URLs - approval: {}, cancel: {}, fail: {}",
                    effectiveApprovalUrl, effectiveCancelUrl, effectiveFailUrl);

            String body = "cid=" + cid +
                "&partner_order_id=" + request.getOrderId() +
                "&partner_user_id=" + request.getCustomerEmail() +
                "&item_name=" + request.getOrderName() +
                "&quantity=1" +
                "&total_amount=" + request.getAmount().intValue() +
                "&tax_free_amount=0" +
                "&approval_url=" + effectiveApprovalUrl +
                "&cancel_url=" + effectiveCancelUrl +
                "&fail_url=" + effectiveFailUrl;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(READY_URL, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            String tid = (String) responseBody.get("tid");
            String nextRedirectPcUrl = (String) responseBody.get("next_redirect_pc_url");

            log.info("KakaoPay payment prepared: orderId={}, tid={}", request.getOrderId(), tid);

            return PaymentPrepareResponse.builder()
                .paymentUrl(nextRedirectPcUrl)
                .pgTransactionId(tid)
                .orderId(request.getOrderId())
                .build();

        } catch (HttpClientErrorException e) {
            log.error("KakaoPay prepare payment failed: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (Exception e) {
            log.error("KakaoPay prepare payment error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public PaymentApprovalResponse approvePayment(String pgToken, String orderId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + adminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "cid=" + cid +
                "&tid=" + pgToken +
                "&partner_order_id=" + orderId +
                "&partner_user_id=user_id" +  // Should be retrieved from order
                "&pg_token=" + pgToken;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(APPROVE_URL, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            String tid = (String) responseBody.get("tid");
            Map<String, Object> amount = (Map<String, Object>) responseBody.get("amount");
            Integer totalAmount = (Integer) amount.get("total");
            String approvedAtStr = (String) responseBody.get("approved_at");

            LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME);

            log.info("KakaoPay payment approved: orderId={}, tid={}, amount={}", orderId, tid, totalAmount);

            return PaymentApprovalResponse.builder()
                .pgTransactionId(tid)
                .orderId(orderId)
                .paymentMethod("KAKAOPAY")
                .amount(BigDecimal.valueOf(totalAmount))
                .approvedAt(approvedAt)
                .build();

        } catch (HttpClientErrorException e) {
            log.error("KakaoPay approve payment failed: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (Exception e) {
            log.error("KakaoPay approve payment error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public PaymentCancelResponse cancelPayment(String pgTransactionId, BigDecimal amount, String reason) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + adminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "cid=" + cid +
                "&tid=" + pgTransactionId +
                "&cancel_amount=" + amount.intValue() +
                "&cancel_tax_free_amount=0";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(CANCEL_URL, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            String tid = (String) responseBody.get("tid");
            String canceledAtStr = (String) responseBody.get("canceled_at");
            LocalDateTime canceledAt = LocalDateTime.parse(canceledAtStr, DateTimeFormatter.ISO_DATE_TIME);

            log.info("KakaoPay payment canceled: tid={}, amount={}", tid, amount);

            return PaymentCancelResponse.builder()
                .pgTransactionId(tid)
                .canceledAmount(amount)
                .cancelReason(reason)
                .canceledAt(canceledAt)
                .build();

        } catch (HttpClientErrorException e) {
            log.error("KakaoPay cancel payment failed: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (Exception e) {
            log.error("KakaoPay cancel payment error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public PaymentQueryResponse queryPayment(String pgTransactionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + adminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String body = "cid=" + cid + "&tid=" + pgTransactionId;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ORDER_URL, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
            }

            String tid = (String) responseBody.get("tid");
            String status = (String) responseBody.get("status");
            Map<String, Object> amount = (Map<String, Object>) responseBody.get("amount");
            Integer totalAmount = (Integer) amount.get("total");
            Integer canceledAmount = (Integer) amount.get("canceled");

            return PaymentQueryResponse.builder()
                .pgTransactionId(tid)
                .status(status)
                .amount(BigDecimal.valueOf(totalAmount))
                .canceledAmount(BigDecimal.valueOf(canceledAmount))
                .build();

        } catch (HttpClientErrorException e) {
            log.error("KakaoPay query payment failed: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        } catch (Exception e) {
            log.error("KakaoPay query payment error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String signature, String payload) {
        // KakaoPay does not provide webhook signature verification
        // Verification should be done by checking transaction status via queryPayment()
        return true;
    }

    /**
     * 문자열이 null이 아니고 비어있지 않은지 확인
     */
    private boolean hasValue(String str) {
        return str != null && !str.isBlank();
    }
}
