package com.hip.damoa.domain.analytics.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Analytics 페이지 데이터 Request (limit 포함)
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsPageRequest {

    @Schema(description = "시작 날짜", example = "2025-01-01", required = true)
    @NotBlank(message = "시작 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD 형식이어야 합니다")
    private String startDateStr;

    @Schema(description = "종료 날짜", example = "2025-12-31", required = true)
    @NotBlank(message = "종료 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD 형식이어야 합니다")
    private String endDateStr;

    @Schema(description = "조회 개수 제한", example = "20", defaultValue = "20")
    @Min(value = 1, message = "limit은 1 이상이어야 합니다")
    @Builder.Default
    private Integer limit = 20;

    /**
     * 시작 날짜를 LocalDate로 변환 (내부 사용)
     */
    @JsonIgnore
    public LocalDate getStartDate() {
        return LocalDate.parse(startDateStr);
    }

    /**
     * 종료 날짜를 LocalDate로 변환 (내부 사용)
     */
    @JsonIgnore
    public LocalDate getEndDate() {
        return LocalDate.parse(endDateStr);
    }

    public String getStartDateStr() {
        return startDateStr;
    }

    public String getEndDateStr() {
        return endDateStr;
    }

    public void setStartDateStr(String startDateStr) {
        this.startDateStr = startDateStr;
    }

    public void setEndDateStr(String endDateStr) {
        this.endDateStr = endDateStr;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
