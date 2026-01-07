package com.hip.damoa.domain.community.web.dto;

import com.hip.damoa.domain.file.model.File;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityFileResponse {

    private UUID uuid;
    private String originalFilename;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String fileExtension;
    private String attachmentType;
    private Integer displayOrder;

    public static CommunityFileResponse from(File file, String attachmentType, Integer displayOrder) {
        return CommunityFileResponse.builder()
                .uuid(file.getUuid())
                .originalFilename(file.getOriginalFilename())
                .fileUrl(file.getFileUrl())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .fileExtension(file.getFileExtension())
                .attachmentType(attachmentType)
                .displayOrder(displayOrder)
                .build();
    }
}
