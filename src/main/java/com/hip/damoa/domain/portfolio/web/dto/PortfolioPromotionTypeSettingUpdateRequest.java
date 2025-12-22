package com.hip.damoa.domain.portfolio.web.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPromotionTypeSettingUpdateRequest {

    @Size(max = 50, message = "표시명은 50자 이내여야 합니다")
    private String displayName;

    @DecimalMin(value = "0", message = "가격은 0 이상이어야 합니다")
    private BigDecimal price;

    @Min(value = 1, message = "가중치는 1 이상이어야 합니다")
    private Integer weight;

    private Integer displayOrder;

    private Boolean isActive;

    @Size(max = 200, message = "설명은 200자 이내여야 합니다")
    private String description;
}
