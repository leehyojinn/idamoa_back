package com.hip.damoa.domain.file.web.dto;

import com.hip.damoa.domain.file.model.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 구매한 파일 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchasedFileResponse {

    private UUID fileUuid;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private Integer pricePaid;
    private LocalDateTime purchasedAt;

    public static PurchasedFileResponse from(File file, Integer pricePaid, LocalDateTime purchasedAt) {
        return PurchasedFileResponse.builder()
                .fileUuid(file.getUuid())
                .fileName(file.getOriginalFilename())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .contentType(file.getMimeType())
                .pricePaid(pricePaid)
                .purchasedAt(purchasedAt)
                .build();
    }
}
