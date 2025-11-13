package com.hip.damoa.domain.planner.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 플래너 신청서 담당자 배정 요청 DTO (Admin)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerApplicationAssignRequest {

    @NotNull(message = "담당자 ID는 필수입니다")
    private Long adminId;
}
