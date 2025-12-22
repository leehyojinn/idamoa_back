package com.hip.damoa.domain.portfolio.web.dto;

import com.hip.damoa.domain.portfolio.model.PortfolioPromotion;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioPromotionResponse {

    private UUID uuid;
    private UUID portfolioUuid;
    private String portfolioTitle;
    private String promotionType;
    private Integer weight;
    private BigDecimal monthlyPrice;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoRenew;
    private String status;
    private LocalDateTime createdAt;

    public static PortfolioPromotionResponse from(PortfolioPromotion promotion) {
        return PortfolioPromotionResponse.builder()
                .uuid(promotion.getUuid())
                .portfolioUuid(promotion.getPortfolio().getUuid())
                .portfolioTitle(promotion.getPortfolio().getTitle())
                .promotionType(promotion.getPromotionType())
                .weight(promotion.getWeight())
                .monthlyPrice(promotion.getMonthlyPrice())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .autoRenew(promotion.getAutoRenew())
                .status(promotion.getStatus().name())
                .createdAt(promotion.getCreatedAt())
                .build();
    }
}
