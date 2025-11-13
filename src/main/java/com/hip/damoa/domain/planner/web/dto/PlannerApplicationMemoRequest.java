package com.hip.damoa.domain.planner.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 플래너 신청서 메모 등록 요청 DTO (Admin)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerApplicationMemoRequest {

    @NotBlank(message = "메모 내용은 필수입니다")
    private String memo;
}
