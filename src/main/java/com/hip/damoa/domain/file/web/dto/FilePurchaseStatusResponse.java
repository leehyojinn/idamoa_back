package com.hip.damoa.domain.file.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 파일 구매 상태 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePurchaseStatusResponse {

    private UUID fileUuid;
    private boolean isPaid;        // 유료 파일 여부
    private Integer price;         // 가격 (무료: 0)
    private boolean hasPurchased;  // 이미 구매했는지 여부
    private boolean canDownload;   // 다운로드 가능 여부 (구매함 또는 크레딧 충분)
}
