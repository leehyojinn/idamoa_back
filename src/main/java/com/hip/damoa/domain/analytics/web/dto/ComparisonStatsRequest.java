package com.hip.damoa.domain.analytics.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 기간 비교 통계 Request
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonStatsRequest {

    @Schema(
            description = "현재 기간 시작 날짜\n" +
                    "비교하려는 현재 기간의 시작 날짜입니다.\n" +
                    "YYYY-MM-DD 형식으로 입력해야 합니다.",
            example = "2025-01-15",
            required = true
    )
    @NotBlank(message = "현재 기간 시작 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD 형식이어야 합니다")
    private String currentStartStr;

    @Schema(
            description = "현재 기간 종료 날짜\n" +
                    "비교하려는 현재 기간의 종료 날짜입니다.\n" +
                    "YYYY-MM-DD 형식으로 입력해야 합니다.",
            example = "2025-01-21",
            required = true
    )
    @NotBlank(message = "현재 기간 종료 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD 형식이어야 합니다")
    private String currentEndStr;

    @Schema(
            description = "이전 기간 시작 날짜\n" +
                    "비교 대상이 되는 이전 기간의 시작 날짜입니다.\n" +
                    "YYYY-MM-DD 형식으로 입력해야 합니다.\n" +
                    "예: 이번 주와 지난 주를 비교하려면 지난 주 시작일을 입력합니다.",
            example = "2025-01-08",
            required = true
    )
    @NotBlank(message = "이전 기간 시작 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD 형식이어야 합니다")
    private String previousStartStr;

    @Schema(
            description = "이전 기간 종료 날짜\n" +
                    "비교 대상이 되는 이전 기간의 종료 날짜입니다.\n" +
                    "YYYY-MM-DD 형식으로 입력해야 합니다.\n" +
                    "예: 이번 주와 지난 주를 비교하려면 지난 주 종료일을 입력합니다.",
            example = "2025-01-14",
            required = true
    )
    @NotBlank(message = "이전 기간 종료 날짜는 필수입니다")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "날짜 형식은 YYYY-MM-DD 형식이어야 합니다")
    private String previousEndStr;

    @JsonIgnore
    public LocalDate getCurrentStart() {
        return LocalDate.parse(currentStartStr);
    }

    @JsonIgnore
    public LocalDate getCurrentEnd() {
        return LocalDate.parse(currentEndStr);
    }

    @JsonIgnore
    public LocalDate getPreviousStart() {
        return LocalDate.parse(previousStartStr);
    }

    @JsonIgnore
    public LocalDate getPreviousEnd() {
        return LocalDate.parse(previousEndStr);
    }

    public String getCurrentStartStr() {
        return currentStartStr;
    }

    public void setCurrentStartStr(String currentStartStr) {
        this.currentStartStr = currentStartStr;
    }

    public String getCurrentEndStr() {
        return currentEndStr;
    }

    public void setCurrentEndStr(String currentEndStr) {
        this.currentEndStr = currentEndStr;
    }

    public String getPreviousStartStr() {
        return previousStartStr;
    }

    public void setPreviousStartStr(String previousStartStr) {
        this.previousStartStr = previousStartStr;
    }

    public String getPreviousEndStr() {
        return previousEndStr;
    }

    public void setPreviousEndStr(String previousEndStr) {
        this.previousEndStr = previousEndStr;
    }
}
