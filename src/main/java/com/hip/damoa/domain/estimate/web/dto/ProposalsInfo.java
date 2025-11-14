package com.hip.damoa.domain.estimate.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 제안 정보 래퍼
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제안 목록 정보")
public class ProposalsInfo {

    @Schema(description = "전체 제안 개수")
    private Integer totalCount;

    @Schema(description = "확인된 제안 개수")
    private Integer viewedCount;

    @Schema(description = "제안 목록 (권한에 따라 전체 정보 또는 요약 정보)")
    private List<?> items;

    @Schema(description = "권한 레벨 (OWNER: 요청자, PROPOSER: 제안자, PUBLIC: 일반 사용자)")
    private String accessLevel;
}
