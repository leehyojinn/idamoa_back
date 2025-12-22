package com.hip.damoa.domain.portfolio.web.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPromotionTypeSettingCreateRequest {

    @NotBlank(message = "프로모션 타입은 필수입니다")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "프로모션 타입은 대문자와 언더스코어만 허용됩니다 (예: SUPER_PREMIUM)")
    private String promotionType;

    @NotBlank(message = "표시명은 필수입니다")
    @Size(max = 50, message = "표시명은 50자 이내여야 합니다")
    private String displayName;

    @NotNull(message = "가격은 필수입니다")
    @DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다")
    private BigDecimal price;

    @NotNull(message = "가중치는 필수입니다")
    @Min(value = 1, message = "가중치는 1 이상이어야 합니다")
    private Integer weight;

    private Integer displayOrder;

    @Size(max = 200, message = "설명은 200자 이내여야 합니다")
    private String description;
}
