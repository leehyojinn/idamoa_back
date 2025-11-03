package com.hip.damoa.domain.payment.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    private String pgTransactionId;

    @NotBlank(message = "Status is required")
    private String status;

    private String errorMessage;

    private String signature;  // For webhook signature verification
}
