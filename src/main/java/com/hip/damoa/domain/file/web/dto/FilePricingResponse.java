package com.hip.damoa.domain.file.web.dto;

import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.model.FilePricing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 파일 가격 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePricingResponse {

    private UUID fileUuid;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Boolean isPaid;
    private Integer price;
    private Integer downloadLimit;
    private Boolean isActive;
    private String description;
    private Long totalDownloads;
    private Long totalRevenue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FilePricingResponse from(FilePricing pricing, File file) {
        return FilePricingResponse.builder()
                .fileUuid(file.getUuid())
                .fileName(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .contentType(file.getMimeType())
                .isPaid(pricing.getIsPaid())
                .price(pricing.getPrice())
                .downloadLimit(pricing.getDownloadLimit())
                .isActive(pricing.getIsActive())
                .description(pricing.getDescription())
                .createdAt(pricing.getCreatedAt())
                .updatedAt(pricing.getUpdatedAt())
                .build();
    }

    public static FilePricingResponse from(FilePricing pricing, File file, Long totalDownloads, Long totalRevenue) {
        return FilePricingResponse.builder()
                .fileUuid(file.getUuid())
                .fileName(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .contentType(file.getMimeType())
                .isPaid(pricing.getIsPaid())
                .price(pricing.getPrice())
                .downloadLimit(pricing.getDownloadLimit())
                .isActive(pricing.getIsActive())
                .description(pricing.getDescription())
                .totalDownloads(totalDownloads)
                .totalRevenue(totalRevenue)
                .createdAt(pricing.getCreatedAt())
                .updatedAt(pricing.getUpdatedAt())
                .build();
    }
}
