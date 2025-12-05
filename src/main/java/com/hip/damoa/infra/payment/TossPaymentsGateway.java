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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * Toss Payments Gateway Implementation
 *
 * 토스페이먼츠 결제 연동
 * 문서: https://docs.tosspayments.com/
 *
 * 환경변수 설정 필요:
 * - TOSS_SECRET_KEY: 토스페이먼츠 Secret Key
 * - TOSS_CLIENT_KEY: 토스페이먼츠 Client Key (프론트엔드용)
 */
@Slf4j
@Component
public class TossPaymentsGateway implements PaymentGateway {

    private static final String BASE_URL = "https://api.tosspayments.com/v1";
    private static final String PAYMENT_URL = BASE_URL + "/payments";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${payment.toss.secret-key}")
    private String secretKey;

    @Value("${payment.toss.client-key}")
    private String clientKey;

    @Value("${payment.toss.success-url}")
    private String successUrl;

    @Value("${payment.toss.fail-url}")
    private String failUrl;

    @Value("${payment.toss.webhook-key:}")
    private String webhookKey;

    public TossPaymentsGateway() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        // Toss Payments는 프론트엔드에서 직접 결제창을 호출합니다
        // 서버는 orderId와 amount만 검증용으로 저장
        // 실제 결제는 프론트엔드 → Toss → 서버 Webhook 순서로 진행됩니다

        log.info("Toss Payments order prepared: orderId={}, amount={}",
            request.getOrderId(), request.getAmount());

        return PaymentPrepareResponse.builder()
            .paymentUrl(null)  // 프론트엔드에서 직접 처리
            .pgTransactionId(null)  // 결제 승인 시 생성됨
            .orderId(request.getOrderId())
            .build();
    }

    @Override
    public PaymentApprovalResponse approvePayment(String pgToken, String orderId) {
        // 금액이 없는 경우 - 기본값으로 처리 (실제로는 사용되지 않음)
        throw new BusinessException(ErrorCode.PAYMENT_FAILED);
    }

    @Override
    public PaymentApprovalResponse approvePayment(String paymentKey, String orderId, BigDecimal amount) {
        try {
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 토스페이먼츠 결제 승인 API 요청
            // paymentKey: 토스에서 결제 성공 후 반환한 키
            // orderId: 우리 서버에서 생성한 주문 ID
            // amount: 결제 금액
            Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount.intValue()
            );

            log.info("Toss Payments confirm request: orderId={}, paymentKey={}, amount={}",
                orderId, paymentKey, amount);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = PAYMENT_URL + "/confirm";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            String returnedPaymentKey = (String) responseBody.get("paymentKey");
            String method = (String) responseBody.get("method");
            Integer totalAmount = (Integer) responseBody.get("totalAmount");
            String approvedAtStr = (String) responseBody.get("approvedAt");

            // receipt는 Map 형태일 수 있음
            String receiptUrl = null;
            Object receiptObj = responseBody.get("receipt");
            if (receiptObj instanceof Map) {
                receiptUrl = (String) ((Map<?, ?>) receiptObj).get("url");
            } else if (receiptObj instanceof String) {
                receiptUrl = (String) receiptObj;
            }

            LocalDateTime approvedAt = LocalDateTime.parse(approvedAtStr, DateTimeFormatter.ISO_DATE_TIME);

            log.info("Toss Payments approved: orderId={}, paymentKey={}, amount={}",
                orderId, returnedPaymentKey, totalAmount);

            return PaymentApprovalResponse.builder()
                .pgTransactionId(returnedPaymentKey)
                .orderId(orderId)
                .paymentMethod("TOSS_" + (method != null ? method.toUpperCase() : "CARD"))
                .amount(BigDecimal.valueOf(totalAmount))
                .approvedAt(approvedAt)
                .receiptUrl(receiptUrl)
                .build();

        } catch (HttpClientErrorException e) {
            log.error("Toss Payments approval failed: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (Exception e) {
            log.error("Toss Payments approval error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public PaymentCancelResponse cancelPayment(String pgTransactionId, BigDecimal amount, String reason) {
        try {
            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                "cancelReason", reason,
                "cancelAmount", amount.intValue()
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = PAYMENT_URL + "/" + pgTransactionId + "/cancel";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            String paymentKey = (String) responseBody.get("paymentKey");
            String canceledAtStr = (String) responseBody.get("canceledAt");
            LocalDateTime canceledAt = LocalDateTime.parse(canceledAtStr, DateTimeFormatter.ISO_DATE_TIME);

            log.info("Toss Payments canceled: paymentKey={}, amount={}", paymentKey, amount);

            return PaymentCancelResponse.builder()
                .pgTransactionId(paymentKey)
                .canceledAmount(amount)
                .cancelReason(reason)
                .canceledAt(canceledAt)
                .build();

        } catch (HttpClientErrorException e) {
            log.error("Toss Payments cancel failed: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (Exception e) {
            log.error("Toss Payments cancel error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public PaymentQueryResponse queryPayment(String pgTransactionId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = PAYMENT_URL + "/" + pgTransactionId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
            }

            String paymentKey = (String) responseBody.get("paymentKey");
            String orderId = (String) responseBody.get("orderId");
            String status = (String) responseBody.get("status");
            Integer totalAmount = (Integer) responseBody.get("totalAmount");
            Integer canceledAmount = (Integer) responseBody.get("canceledAmount");

            return PaymentQueryResponse.builder()
                .pgTransactionId(paymentKey)
                .orderId(orderId)
                .status(status)
                .amount(BigDecimal.valueOf(totalAmount))
                .canceledAmount(canceledAmount != null ? BigDecimal.valueOf(canceledAmount) : BigDecimal.ZERO)
                .build();

        } catch (HttpClientErrorException e) {
            log.error("Toss Payments query failed: {}", e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
        } catch (Exception e) {
            log.error("Toss Payments query error", e);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String signature, String payload) {
        if (webhookKey == null || webhookKey.isEmpty()) {
            log.warn("Webhook key is not configured, skipping verification");
            return true;
        }

        try {
            // HMAC-SHA256으로 서명 검증
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec =
                    new javax.crypto.spec.SecretKeySpec(webhookKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            boolean isValid = expectedSignature.equals(signature);
            if (!isValid) {
                log.warn("Webhook signature mismatch: expected={}, actual={}", expectedSignature, signature);
            }
            return isValid;
        } catch (Exception e) {
            log.error("Failed to verify webhook signature", e);
            return false;
        }
    }

    /**
     * Create authorization headers with Basic Auth
     */
    private HttpHeaders createAuthHeaders() {
        String auth = secretKey + ":";
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        return headers;
    }
}
