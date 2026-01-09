package com.hip.damoa.domain.popup.web.dto;

import com.hip.damoa.domain.popup.model.Popup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 팝업 Response DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팝업 응답")
public class PopupResponse {

    @Schema(description = "팝업 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "팝업 제목", example = "신년 이벤트 안내")
    private String title;

    @Schema(description = "팝업 내용", example = "<p>2025년 신년 이벤트에 참여하세요!</p>")
    private String content;

    @Schema(description = "팝업 이미지 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID imageUuid;

    @Schema(description = "팝업 이미지 URL (S3)", example = "https://s3.amazonaws.com/bucket/image.jpg")
    private String imageUrl;

    @Schema(description = "클릭 시 이동할 URL", example = "https://example.com/event")
    private String linkUrl;

    @Schema(description = "팝업 너비", example = "600")
    private Integer width;

    @Schema(description = "너비 단위", example = "px")
    private String widthUnit;

    @Schema(description = "팝업 높이", example = "800")
    private Integer height;

    @Schema(description = "높이 단위", example = "px")
    private String heightUnit;

    @Schema(description = "팝업 위치", example = "CENTER")
    private String position;

    @Schema(description = "X축 오프셋", example = "0")
    private Integer offsetX;

    @Schema(description = "X축 오프셋 단위", example = "px")
    private String offsetXUnit;

    @Schema(description = "Y축 오프셋", example = "0")
    private Integer offsetY;

    @Schema(description = "Y축 오프셋 단위", example = "px")
    private String offsetYUnit;

    // 모바일 설정
    @Schema(description = "모바일 설정 활성화 여부", example = "true")
    private Boolean mobileEnabled;

    @Schema(description = "모바일 너비", example = "90")
    private Integer mobileWidth;

    @Schema(description = "모바일 너비 단위", example = "%")
    private String mobileWidthUnit;

    @Schema(description = "모바일 높이", example = "80")
    private Integer mobileHeight;

    @Schema(description = "모바일 높이 단위", example = "%")
    private String mobileHeightUnit;

    @Schema(description = "모바일 팝업 위치", example = "CENTER")
    private String mobilePosition;

    @Schema(description = "모바일 X축 오프셋", example = "0")
    private Integer mobileOffsetX;

    @Schema(description = "모바일 X축 오프셋 단위", example = "px")
    private String mobileOffsetXUnit;

    @Schema(description = "모바일 Y축 오프셋", example = "0")
    private Integer mobileOffsetY;

    @Schema(description = "모바일 Y축 오프셋 단위", example = "px")
    private String mobileOffsetYUnit;

    @Schema(description = "노출 시작일시", example = "2025-01-01T00:00:00")
    private LocalDateTime displayStartDate;

    @Schema(description = "노출 종료일시", example = "2025-12-31T23:59:59")
    private LocalDateTime displayEndDate;

    @Schema(description = "노출 순서", example = "0")
    private Integer displayOrder;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;

    @Schema(description = "조회수", example = "1234")
    private Long viewCount;

    @Schema(description = "클릭수", example = "567")
    private Long clickCount;

    @Schema(description = "생성일시", example = "2025-01-01T00:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-02T12:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "생성자", example = "admin@example.com")
    private String createdBy;

    @Schema(description = "수정자", example = "admin@example.com")
    private String updatedBy;

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    /**
     * Entity to DTO (이미지 URL 없음)
     */
    public static PopupResponse from(Popup popup) {
        return from(popup, null);
    }

    /**
     * Entity to DTO (이미지 URL 포함)
     */
    public static PopupResponse from(Popup popup, String imageUrl) {
        return PopupResponse.builder()
                .uuid(popup.getUuid())
                .title(popup.getTitle())
                .content(popup.getContent())
                .imageUuid(popup.getImageUuid())
                .imageUrl(imageUrl)
                .linkUrl(popup.getLinkUrl())
                .width(popup.getWidth())
                .widthUnit(popup.getWidthUnit())
                .height(popup.getHeight())
                .heightUnit(popup.getHeightUnit())
                .position(popup.getPosition())
                .offsetX(popup.getOffsetX())
                .offsetXUnit(popup.getOffsetXUnit())
                .offsetY(popup.getOffsetY())
                .offsetYUnit(popup.getOffsetYUnit())
                .mobileEnabled(popup.getMobileEnabled())
                .mobileWidth(popup.getMobileWidth())
                .mobileWidthUnit(popup.getMobileWidthUnit())
                .mobileHeight(popup.getMobileHeight())
                .mobileHeightUnit(popup.getMobileHeightUnit())
                .mobilePosition(popup.getMobilePosition())
                .mobileOffsetX(popup.getMobileOffsetX())
                .mobileOffsetXUnit(popup.getMobileOffsetXUnit())
                .mobileOffsetY(popup.getMobileOffsetY())
                .mobileOffsetYUnit(popup.getMobileOffsetYUnit())
                .displayStartDate(popup.getDisplayStartDate())
                .displayEndDate(popup.getDisplayEndDate())
                .displayOrder(popup.getDisplayOrder())
                .isActive(popup.getIsActive())
                .viewCount(popup.getViewCount())
                .clickCount(popup.getClickCount())
                .createdAt(popup.getCreatedAt())
                .updatedAt(popup.getUpdatedAt())
                .createdBy(popup.getCreatedBy())
                .updatedBy(popup.getUpdatedBy())
                .isDeleted(popup.getIsDeleted())
                .build();
    }
}
