package com.hip.damoa.domain.board.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 갤러리 우대등록 타입 설정 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryPromotionTypeSettingUpdateRequest {

    @Size(max = 50, message = "표시명은 50자 이하로 입력해주세요")
    private String displayName;

    @DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다")
    private BigDecimal price;

    @Min(value = 1, message = "가중치는 1 이상이어야 합니다")
    private Integer weight;

    @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다")
    private Integer displayOrder;

    private Boolean isActive;

    @Size(max = 200, message = "설명은 200자 이하로 입력해주세요")
    private String description;
}
