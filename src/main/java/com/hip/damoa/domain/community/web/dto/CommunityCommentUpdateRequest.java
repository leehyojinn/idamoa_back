package com.hip.damoa.domain.community.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityCommentUpdateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    private String content;
}
