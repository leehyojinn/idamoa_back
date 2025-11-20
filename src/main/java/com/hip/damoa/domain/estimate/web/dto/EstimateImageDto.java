package com.hip.damoa.domain.estimate.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 견적 요청 이미지 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateImageDto {

    private Long fileId;
    private UUID fileUuid;  // 파일 UUID (수정 시 사용)
    private String imageUrl;

    /**
     * URL과 파일 정보로 DTO 생성
     */
    public static EstimateImageDto of(String imageUrl, Long fileId, UUID fileUuid) {
        return EstimateImageDto.builder()
                .fileId(fileId)
                .fileUuid(fileUuid)
                .imageUrl(imageUrl)
                .build();
    }
}
