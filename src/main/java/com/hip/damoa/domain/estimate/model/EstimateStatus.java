package com.hip.damoa.domain.estimate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 견적 요청 상태
 */
@Schema(description = "견적 요청 상태", enumAsRef = true)
public enum EstimateStatus {
    @Schema(description = "작성중 - 임시 저장 상태")
    DRAFT,

    @Schema(description = "공개됨 - 업체들이 제안 제출 가능")
    PUBLISHED,

    @Schema(description = "진행중 - 제안 검토 중")
    IN_PROGRESS,

    @Schema(description = "매칭됨 - 업체 선정 완료")
    MATCHED,

    @Schema(description = "완료 - 프로젝트 완료")
    COMPLETED,

    @Schema(description = "취소됨 - 견적 요청 취소")
    CANCELLED
}
