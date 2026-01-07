package com.hip.damoa.domain.community.web.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPostUpdateRequest {

    @Size(max = 300, message = "제목은 300자를 초과할 수 없습니다")
    private String title;

    private String content;

    private String contentType;  // TEXT, HTML

    private Boolean isPinned;

    private Boolean isNotice;

    private Boolean isPublished;
}
