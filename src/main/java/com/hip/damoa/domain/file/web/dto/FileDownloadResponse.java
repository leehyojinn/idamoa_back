package com.hip.damoa.domain.file.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 파일 다운로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDownloadResponse {

    private UUID fileUuid;
    private String fileName;
    private String downloadUrl;
    private Integer price;  // 결제된 금액 (무료: 0)
}
