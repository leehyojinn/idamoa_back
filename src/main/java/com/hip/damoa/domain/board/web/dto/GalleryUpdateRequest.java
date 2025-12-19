package com.hip.damoa.domain.board.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "제목", example = "수정된 포트폴리오 제목")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Schema(description = "내용", example = "수정된 내용입니다.")
    @Size(max = 5000, message = "내용은 5000자 이내여야 합니다")
    private String content;

    @Schema(description = "카테고리 ID")
    private Long categoryId;

    @Schema(description = "이미지 UUID 목록")
    @Size(min = 1, message = "이미지는 최소 1개 이상 필요합니다")
    private List<String> imageUuids;

    @Schema(description = "관련 링크")
    private String relatedLink;

    @Schema(description = "저작권 정보")
    private GalleryCreateRequest.CopyrightInfo copyright;

    @Schema(description = "필터 옵션 ID 목록")
    private List<Long> filterOptionIds;

    @Schema(description = "태그")
    private String[] tags;

    // ===== 우대 등록 관리 옵션 (선택형) =====

    @Schema(description = "우대 타입: 신규 등록 또는 업그레이드 시 설정. STANDARD(일반우대 5만원/월), PREMIUM(강력우대 10만원/월)",
            example = "PREMIUM", allowableValues = {"STANDARD", "PREMIUM"})
    private String promotionType;

    @Schema(description = "자동 갱신 여부 변경", example = "true")
    private Boolean autoRenew;

    @Schema(description = "우대 취소 여부: true 설정 시 현재 기간 유지, 다음 달부터 우대 해제", example = "false")
    private Boolean cancelPromotion;

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
