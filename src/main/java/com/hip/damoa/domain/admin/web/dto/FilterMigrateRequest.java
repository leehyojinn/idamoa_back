package com.hip.damoa.domain.admin.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필터 옵션 마이그레이션 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "필터 옵션 마이그레이션 요청")
public class FilterMigrateRequest {

    @NotNull(message = "소스 필터 옵션 ID는 필수입니다")
    @Schema(description = "기존 필터 옵션 ID (마이그레이션 원본)", example = "26", required = true)
    private Long sourceOptionId;

    @NotNull(message = "타겟 필터 옵션 ID는 필수입니다")
    @Schema(description = "새 필터 옵션 ID (마이그레이션 대상)", example = "384", required = true)
    private Long targetOptionId;

    @Builder.Default
    @Schema(description = "마이그레이션 후 기존 옵션 비활성화 여부", example = "true", defaultValue = "false")
    private Boolean deactivateSource = false;

    @Builder.Default
    @Schema(description = "마이그레이션 후 기존 옵션 삭제 여부 (soft delete)", example = "false", defaultValue = "false")
    private Boolean deleteSource = false;
}
