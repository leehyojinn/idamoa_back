package com.hip.damoa.domain.community.web.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostUpdateRequest {

    @Size(max = 300, message = "제목은 300자를 초과할 수 없습니다")
    private String title;

    private String content;

    private String contentType;  // TEXT, HTML

    private Boolean isAnonymous;

    private List<String> fileUuids;  // 첨부파일 UUID 목록 (전체 교체)
}
