package com.hip.damoa.domain.estimate.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 견적 제안 상태
 */
@Schema(description = "견적 제안 상태", enumAsRef = true)
public enum ProposalStatus {
    @Schema(description = "임시저장 - 제출 전 임시 저장 상태")
    DRAFT,

    @Schema(description = "제출됨 - 요청자에게 제안 제출 완료")
    SUBMITTED,

    @Schema(description = "확인됨 - 요청자가 제안 확인")
    VIEWED,

    @Schema(description = "선택됨 - 요청자가 제안 수락")
    SELECTED,

    @Schema(description = "거절됨 - 요청자가 제안 거절")
    REJECTED,

    @Schema(description = "철회됨 - 업체가 제안 철회")
    WITHDRAWN
}
