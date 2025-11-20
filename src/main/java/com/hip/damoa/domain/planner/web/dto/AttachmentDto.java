package com.hip.damoa.domain.planner.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 플래너 신청서 첨부파일 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDto {

    private Long fileId;
    private UUID fileUuid;  // 파일 UUID (수정 시 사용)
    private String fileUrl;
    private String fileName;

    /**
     * 파일 정보로 DTO 생성
     */
    public static AttachmentDto of(Long fileId, UUID fileUuid, String fileUrl, String fileName) {
        return AttachmentDto.builder()
                .fileId(fileId)
                .fileUuid(fileUuid)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .build();
    }
}
