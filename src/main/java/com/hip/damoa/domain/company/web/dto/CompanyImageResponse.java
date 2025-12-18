package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 업체 이미지 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyImageResponse {

    private UUID companyUuid;
    private UUID fileUuid;  // 파일 UUID (수정 시 사용)
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
        return from(image, null, null);
    }

    public static CompanyImageResponse from(CompanyImage image, String imageUrl) {
        return from(image, imageUrl, null);
    }

    public static CompanyImageResponse from(CompanyImage image, String imageUrl, UUID fileUuid) {
        return CompanyImageResponse.builder()
                .companyUuid(image.getCompany().getUuid())
                .fileUuid(fileUuid)  // 파일 UUID
                .imageUrl(imageUrl)  // File ID → URL 변환된 값 사용
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
