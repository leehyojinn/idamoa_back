package com.hip.damoa.domain.board.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Gallery 게시글 생성 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryCreateRequest {

    @Schema(description = "제목", example = "모던한 치과 인테리어 - 50평")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Schema(description = "내용", example = "깔끔하고 모던한 분위기의 치과 인테리어입니다. 화이트와 우드 톤을 조화롭게 배치했습니다.")
    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "이미지 UUID 목록 (파일 업로드 API로 받은 UUID 사용)",
            example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "이미지는 최소 1개 이상 필요합니다")
    @Size(min = 1, message = "이미지는 최소 1개 이상 필요합니다")
    private List<String> imageUuids; // File UUID 목록

    @Schema(description = "관련 링크 (포트폴리오 URL 등)", example = "https://example.com/portfolio/123")
    private String relatedLink; // 관련 링크

    @Schema(description = "저작권 정보")
    private CopyrightInfo copyright; // 저작권 정보

    @Schema(description = "필터 옵션 ID 목록", example = "[8, 65, 90]")
    private List<Long> filterOptionIds; // 필터 옵션 ID 목록

    @Schema(description = "태그", example = "[\"치과\", \"50평\", \"모던\", \"화이트\"]")
    private String[] tags;

    @Schema(description = "즉시 게시 여부", example = "true")
    private Boolean isPublished;

    @Schema(description = "비공개 여부", example = "false")
    private Boolean isPrivate;

    /**
     * 저작권 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CopyrightInfo {
        @Schema(description = "저작권자", example = "홍길동 디자인")
        private String owner; // 저작권자

        @Schema(description = "라이선스", example = "All Rights Reserved")
        private String license; // 라이선스 (All Rights Reserved, CC BY, etc.)

        @Schema(description = "출처 표기", example = "선택")
        private String attribution; // 필수/선택
    }

    /**
     * type_data JSONB 생성
     */
    public Map<String, Object> toTypeData() {
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
