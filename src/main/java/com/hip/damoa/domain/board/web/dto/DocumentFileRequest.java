package com.hip.damoa.domain.board.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Document 게시글 파일 정보 DTO
 * 파일별 개별 가격 설정 지원
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFileRequest {

    @Schema(description = "파일 UUID", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "파일 UUID는 필수입니다")
    private String uuid;

    @Schema(description = "유료 파일 여부", example = "true")
    private Boolean isPaid;

    @Schema(description = "가격 (원) - 유료인 경우", example = "5000")
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;

    /**
     * 무료 파일인지 확인
     */
    public boolean isFree() {
        return !Boolean.TRUE.equals(isPaid) || price == null || price <= 0;
    }

    /**
     * 유효한 가격인지 확인
     */
    public int getEffectivePrice() {
        if (isFree()) {
            return 0;
        }
        return price;
    }
}
