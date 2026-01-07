package com.hip.damoa.domain.community.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCommentCreateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;

    private UUID parentUuid;  // 대댓글인 경우 부모 댓글 UUID

    private Boolean isAnonymous;
}
