package com.hip.damoa.domain.planner.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 플래너 요청 내용 타입
 */
@Schema(description = "요청 내용 타입 (다중 선택 가능)", enumAsRef = true)
public enum RequestType {
    @Schema(description = "종합 컨설팅 - 인테리어부터 운영까지 전반적인 컨설팅")
    FULL_CONSULTING,

    @Schema(description = "신규 창업 - 처음 시작하는 병원/사업장 컨설팅")
    NEW_OPENING,

    @Schema(description = "리모델링 - 기존 공간 리모델링 및 개선")
    REMODELING,

    @Schema(description = "운영 컨설팅 - 운영 효율화 및 개선 컨설팅")
    OPERATION_CONSULTING,

    @Schema(description = "법률 자문 - 법률 관련 자문")
    LEGAL_INQUIRY
}
