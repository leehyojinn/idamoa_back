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
 * Document 게시글 생성 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCreateRequest {

    @Schema(description = "제목", example = "병원 인테리어 설계도면 공유")
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Schema(description = "내용", example = "50평 규모 치과 인테리어 설계도면입니다. 대기실, 진료실, 상담실 포함되어 있습니다.")
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "파일 UUID 목록 (파일 업로드 API로 받은 UUID 사용)",
            example = "[\"550e8400-e29b-41d4-a716-446655440000\", \"550e8400-e29b-41d4-a716-446655440001\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "파일은 최소 1개 이상 필요합니다")
    @Size(min = 1, message = "파일은 최소 1개 이상 필요합니다")
    private List<String> fileUuids; // File UUID 목록

    @Schema(description = "썸네일 이미지 UUID (선택)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String thumbnailUuid; // 썸네일 이미지 UUID

    @Schema(description = "유료 파일 여부", example = "false")
    private Boolean isPaid; // 유료 여부

    @Schema(description = "가격 (원) - 유료인 경우만", example = "0")
    private Integer price; // 가격 (KRW)

    @Schema(description = "필터 옵션 ID 목록", example = "[1, 2, 3]")
    private List<Long> filterOptionIds; // 필터 옵션 ID 목록

    @Schema(description = "태그", example = "[\"치과\", \"50평\", \"모던\"]")
    private String[] tags;

    @Schema(description = "즉시 게시 여부", example = "true")
    private Boolean isPublished;

    @Schema(description = "비공개 여부", example = "false")
    private Boolean isPrivate;

    /**
     * type_data JSONB 생성
     */
    public Map<String, Object> toTypeData() {
        return Map.of(
                "files", fileUuids,
                "thumbnail", thumbnailUuid != null ? thumbnailUuid : "",
                "isPaid", isPaid != null ? isPaid : false,
                "price", price != null ? price : 0
        );
    }
}
