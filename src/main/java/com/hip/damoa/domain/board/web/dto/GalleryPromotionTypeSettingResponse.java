package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.board.model.GalleryPromotionTypeSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 갤러리 우대등록 타입별 설정 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryPromotionTypeSettingResponse {

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

    public static GalleryPromotionTypeSettingResponse from(GalleryPromotionTypeSetting setting) {
        return GalleryPromotionTypeSettingResponse.builder()
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
