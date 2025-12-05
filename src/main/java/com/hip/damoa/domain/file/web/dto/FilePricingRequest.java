package com.hip.damoa.domain.file.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일 가격 설정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePricingRequest {

    @NotNull(message = "유료 여부는 필수입니다")
    private Boolean isPaid;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private Integer price;  // 유료인 경우 필수

    @Min(value = 1, message = "다운로드 제한은 1 이상이어야 합니다")
    private Integer downloadLimit;  // null이면 무제한

    private String description;
}
