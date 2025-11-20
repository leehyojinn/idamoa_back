package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 업체 이미지 DTO (목록용 간단 정보)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyImageDto {

    private Long id;
    private Long fileId;
    private UUID fileUuid;  // 파일 UUID (수정 시 사용)
    private String imageUrl;
    private String imageType;
    private Boolean isPrimary;
    private Integer displayOrder;
    private String title;

    /**
     * Entity → DTO 변환
     */
    public static CompanyImageDto from(CompanyImage image) {
        return from(image, null, null);
    }

    public static CompanyImageDto from(CompanyImage image, String imageUrl) {
        return from(image, imageUrl, null);
    }

    public static CompanyImageDto from(CompanyImage image, String imageUrl, UUID fileUuid) {
        return CompanyImageDto.builder()
                .id(image.getId())
                .fileId(image.getFileId())
                .fileUuid(fileUuid)  // 파일 UUID
                .imageUrl(imageUrl)  // File ID → URL 변환된 값 사용
                .imageType(image.getImageType())
                .isPrimary(image.getIsPrimary())
                .displayOrder(image.getDisplayOrder())
                .title(image.getTitle())
                .build();
    }
}
