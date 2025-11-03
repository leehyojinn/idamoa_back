package com.hip.damoa.infra.payment;

import java.math.BigDecimal;

/**
 * Payment Gateway Interface
 *
 * 결제 게이트웨이 공통 인터페이스
 */
public interface PaymentGateway {

    /**
     * 결제 준비 (결제 페이지 URL 생성)
     */
    PaymentPrepareResponse preparePayment(PaymentPrepareRequest request);

    /**
     * 결제 승인
     */
    PaymentApprovalResponse approvePayment(String pgToken, String orderId);

    /**
     * 결제 취소/환불
     */
    PaymentCancelResponse cancelPayment(String pgTransactionId, BigDecimal amount, String reason);

    /**
     * 결제 조회
     */
    PaymentQueryResponse queryPayment(String pgTransactionId);

    /**
     * Webhook 서명 검증 (PG사에서 보낸 요청이 맞는지 확인)
     */
    boolean verifyWebhookSignature(String signature, String payload);
}
