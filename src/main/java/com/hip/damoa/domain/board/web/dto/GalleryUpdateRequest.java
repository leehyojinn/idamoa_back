package com.hip.damoa.domain.board.web.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Gallery 게시글 수정 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryUpdateRequest {

    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;

    private Long categoryId;

    @Size(min = 1, message = "이미지는 최소 1개 이상 필요합니다")
    private List<String> imageUuids;

    private String relatedLink;

    private GalleryCreateRequest.CopyrightInfo copyright;

    private List<Long> filterOptionIds;

    private String[] tags;

    /**
     * type_data JSONB 생성
     */
    public Map<String, Object> toTypeData() {
        if (imageUuids == null) {
            return null;
        }

        return Map.of(
                "images", imageUuids,
                "relatedLink", relatedLink != null ? relatedLink : "",
                "copyright", copyright != null ? Map.of(
                        "owner", copyright.getOwner() != null ? copyright.getOwner() : "",
                        "license", copyright.getLicense() != null ? copyright.getLicense() : "All Rights Reserved",
                        "attribution", copyright.getAttribution() != null ? copyright.getAttribution() : "선택"
                ) : Map.of()
        );
    }
}
