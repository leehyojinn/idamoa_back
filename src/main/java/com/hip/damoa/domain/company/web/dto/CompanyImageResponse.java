package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 업체 이미지 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyImageResponse {

    private Long id;
    private Long companyId;
    private String imageUrl;
    private String imageType;
    private Boolean isPrimary;
    private Integer displayOrder;
    private String title;
    private String description;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private LocalDateTime createdAt;

    public static CompanyImageResponse from(CompanyImage image) {
        return CompanyImageResponse.builder()
                .id(image.getId())
                .companyId(image.getCompany().getId())
                .imageUrl(image.getImageUrl())
                .imageType(image.getImageType())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .title(image.getTitle())
                .description(image.getDescription())
                .width(image.getWidth())
                .height(image.getHeight())
                .fileSize(image.getFileSize())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
