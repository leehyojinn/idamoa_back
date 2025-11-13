package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 플래너 신청서 상태 변경 요청 DTO (Admin)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerApplicationStatusUpdateRequest {

    @NotNull(message = "상태는 필수입니다")
    private PlannerApplicationStatus status;
}
