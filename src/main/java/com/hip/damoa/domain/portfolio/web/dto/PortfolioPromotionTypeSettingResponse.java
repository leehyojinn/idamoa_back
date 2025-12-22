package com.hip.damoa.domain.portfolio.web.dto;

import com.hip.damoa.domain.portfolio.model.PortfolioPromotionTypeSetting;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPromotionTypeSettingResponse {

    private UUID uuid;
    private String promotionType;
    private String displayName;
    private BigDecimal price;
    private Integer weight;
    private Integer displayOrder;
    private Boolean isActive;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PortfolioPromotionTypeSettingResponse from(PortfolioPromotionTypeSetting setting) {
        return PortfolioPromotionTypeSettingResponse.builder()
                .uuid(setting.getUuid())
                .promotionType(setting.getPromotionType())
                .displayName(setting.getDisplayName())
                .price(setting.getPrice())
                .weight(setting.getWeight())
                .displayOrder(setting.getDisplayOrder())
                .isActive(setting.getIsActive())
                .description(setting.getDescription())
                .createdAt(setting.getCreatedAt())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
