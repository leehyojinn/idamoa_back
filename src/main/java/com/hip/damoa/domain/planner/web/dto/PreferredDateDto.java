package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.PlannerPreferredDate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 희망 일정 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "희망 상담 일정")
public class PreferredDateDto {

    @Schema(description = "우선순위 (1순위, 2순위, 3순위)", example = "1", allowableValues = {"1", "2", "3"})
    @NotNull(message = "우선순위는 필수입니다")
    @Min(value = 1, message = "우선순위는 1~3 사이여야 합니다")
    @Max(value = 3, message = "우선순위는 1~3 사이여야 합니다")
    private Integer priority;

    @Schema(description = "희망 날짜", example = "2025-11-20")
    @NotNull(message = "희망 날짜는 필수입니다")
    private LocalDate preferredDate;

    @Schema(description = "희망 시간대", example = "오전 10시")
    @NotBlank(message = "희망 시간은 필수입니다")
    private String preferredTime;

    /**
     * DTO → Entity 변환
     */
    public PlannerPreferredDate toEntity() {
        return PlannerPreferredDate.builder()
                .priority(this.priority)
                .preferredDate(this.preferredDate)
                .preferredTime(this.preferredTime)
                .build();
    }

    /**
     * Entity → DTO 변환
     */
    public static PreferredDateDto from(PlannerPreferredDate entity) {
        return PreferredDateDto.builder()
                .priority(entity.getPriority())
                .preferredDate(entity.getPreferredDate())
                .preferredTime(entity.getPreferredTime())
                .build();
    }
}
