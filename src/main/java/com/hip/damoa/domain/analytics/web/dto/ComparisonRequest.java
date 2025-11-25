package com.hip.damoa.domain.analytics.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기간 비교 통계 Request
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonRequest {

    @NotBlank(message = "현재 기간 시작 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String currentStart;

    @NotBlank(message = "현재 기간 종료 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String currentEnd;

    @NotBlank(message = "이전 기간 시작 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String previousStart;

    @NotBlank(message = "이전 기간 종료 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String previousEnd;
}
