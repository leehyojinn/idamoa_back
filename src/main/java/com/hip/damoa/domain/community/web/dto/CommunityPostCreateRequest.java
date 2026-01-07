package com.hip.damoa.domain.community.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostCreateRequest {

    @NotNull(message = "카테고리는 필수입니다")
    private UUID categoryUuid;

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 300, message = "제목은 300자를 초과할 수 없습니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    private String contentType;  // TEXT, HTML (기본값: TEXT)

    private Boolean isAnonymous;

    private List<String> fileUuids;  // 첨부파일 UUID 목록
}
