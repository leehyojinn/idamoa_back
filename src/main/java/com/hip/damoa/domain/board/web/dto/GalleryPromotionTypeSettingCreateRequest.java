package com.hip.damoa.domain.board.web.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 갤러리 우대등록 타입 설정 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryPromotionTypeSettingCreateRequest {

    @NotBlank(message = "우대 타입 코드는 필수입니다")
    @Size(max = 30, message = "우대 타입 코드는 30자 이하로 입력해주세요")
    @Pattern(regexp = "^[A-Z_]+$", message = "우대 타입 코드는 대문자와 언더스코어만 사용할 수 있습니다")
    private String promotionType;

    @NotBlank(message = "표시명은 필수입니다")
    @Size(max = 50, message = "표시명은 50자 이하로 입력해주세요")
    private String displayName;

    @NotNull(message = "가격은 필수입니다")
    @DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다")
    private BigDecimal price;

    @NotNull(message = "가중치는 필수입니다")
    @Min(value = 1, message = "가중치는 1 이상이어야 합니다")
    private Integer weight;

    @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다")
    private Integer displayOrder;

    @Size(max = 200, message = "설명은 200자 이하로 입력해주세요")
    private String description;
}
