package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.CompanyReviewImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 리뷰 이미지 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImageDto {

    private Long id;
    private Long fileId;
    private UUID fileUuid;  // 파일 UUID (수정 시 사용)
    private String imageUrl;
    private Integer displayOrder;

    /**
     * Entity → DTO 변환
     */
    public static ReviewImageDto from(CompanyReviewImage image) {
        return from(image, null, null);
    }

    public static ReviewImageDto from(CompanyReviewImage image, String imageUrl) {
        return from(image, imageUrl, null);
    }

    public static ReviewImageDto from(CompanyReviewImage image, String imageUrl, UUID fileUuid) {
        return ReviewImageDto.builder()
                .id(image.getId())
                .fileId(image.getFileId())
                .fileUuid(fileUuid)  // 파일 UUID
                .imageUrl(imageUrl)  // File ID → URL 변환된 값 사용
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
