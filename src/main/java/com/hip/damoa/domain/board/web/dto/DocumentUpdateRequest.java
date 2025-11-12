package com.hip.damoa.domain.board.web.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Document 게시글 수정 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateRequest {

    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;

    private Long categoryId;

    @Size(min = 1, message = "파일은 최소 1개 이상 필요합니다")
    private List<String> fileUuids;

    private String thumbnailUuid;

    private Boolean isPaid;

    private Integer price;

    private List<Long> filterOptionIds;

    private String[] tags;

    /**
     * type_data JSONB 생성
     */
    public Map<String, Object> toTypeData() {
        if (fileUuids == null) {
            return null;
        }

        return Map.of(
                "files", fileUuids,
                "thumbnail", thumbnailUuid != null ? thumbnailUuid : "",
                "isPaid", isPaid != null ? isPaid : false,
                "price", price != null ? price : 0
        );
    }
}
