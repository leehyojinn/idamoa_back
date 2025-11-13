package com.hip.damoa.domain.planner.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 플래너 신청서 상태
 */
@Schema(description = "플래너 신청서 상태", enumAsRef = true)
public enum PlannerApplicationStatus {
    @Schema(description = "대기중 - 신청 접수 대기")
    PENDING,

    @Schema(description = "진행중 - 상담 진행 중")
    IN_PROGRESS,

    @Schema(description = "완료 - 상담 완료")
    COMPLETED,

    @Schema(description = "거절 - 신청 거절")
    REJECTED
}
