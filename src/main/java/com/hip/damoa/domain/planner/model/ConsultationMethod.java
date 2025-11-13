package com.hip.damoa.domain.planner.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 플래너 상담 방법
 */
@Schema(description = "상담 방법", enumAsRef = true)
public enum ConsultationMethod {
    @Schema(description = "방문 상담 - 직접 방문하여 상담")
    VISIT,

    @Schema(description = "전화 상담 - 전화로 상담")
    PHONE,

    @Schema(description = "SNS 상담 - 카카오톡 등 SNS로 상담")
    SNS
}
