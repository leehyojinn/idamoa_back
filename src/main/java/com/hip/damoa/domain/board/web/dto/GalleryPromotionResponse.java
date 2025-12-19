package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.board.model.GalleryPromotion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "갤러리 우대등록 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryPromotionResponse {

    @Schema(description = "우대 UUID")
    private UUID promotionUuid;

    @Schema(description = "갤러리 UUID")
    private UUID boardUuid;

    @Schema(description = "갤러리 제목")
    private String boardTitle;

    @Schema(description = "사용자 이메일")
    private String userEmail;

    @Schema(description = "우대 타입", example = "STANDARD")
    private String promotionType;

    @Schema(description = "가중치", example = "1")
    private Integer weight;

    @Schema(description = "월 가격 (원)", example = "50000")
    private BigDecimal monthlyPrice;

    @Schema(description = "시작일")
    private LocalDate startDate;

    @Schema(description = "종료일")
    private LocalDate endDate;

    @Schema(description = "자동갱신 여부")
    private Boolean autoRenew;

    @Schema(description = "상태", example = "ACTIVE")
    private String status;

    @Schema(description = "남은 일수")
    private Integer remainingDays;

    @Schema(description = "생성일")
    private LocalDateTime createdAt;

    public static GalleryPromotionResponse from(GalleryPromotion promotion) {
        return GalleryPromotionResponse.builder()
                .promotionUuid(promotion.getUuid())
                .boardUuid(promotion.getBoard().getUuid())
                .boardTitle(promotion.getBoard().getTitle())
                .userEmail(promotion.getUser().getEmail())
                .promotionType(promotion.getPromotionType().name())
                .weight(promotion.getWeight())
                .monthlyPrice(promotion.getMonthlyPrice())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .autoRenew(promotion.getAutoRenew())
                .status(promotion.getStatus())
                .remainingDays(promotion.getRemainingDays())
                .createdAt(promotion.getCreatedAt())
                .build();
    }
}
