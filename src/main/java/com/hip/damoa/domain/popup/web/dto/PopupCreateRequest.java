package com.hip.damoa.domain.popup.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 팝업 생성 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팝업 생성 요청")
public class PopupCreateRequest {

    @Schema(description = "팝업 제목", example = "신년 이벤트 안내", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @Schema(description = "팝업 내용 (HTML 지원)", example = "<p>2025년 신년 이벤트에 참여하세요!</p>")
    @Size(max = 10000, message = "내용은 10000자 이내여야 합니다")
    private String content;

    @Schema(description = "팝업 이미지 UUID (files 테이블)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID imageUuid;

    @Schema(description = "클릭 시 이동할 URL", example = "https://example.com/event")
    @Size(max = 500, message = "링크 URL은 500자 이내여야 합니다")
    private String linkUrl;

    @Schema(description = "팝업 너비", example = "600")
    private Integer width;

    @Schema(description = "너비 단위", example = "px", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String widthUnit;

    @Schema(description = "팝업 높이", example = "800")
    private Integer height;

    @Schema(description = "높이 단위", example = "px", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String heightUnit;

    @Schema(description = "팝업 위치", example = "CENTER",
            allowableValues = {"CENTER", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "CUSTOM"})
    private String position;

    @Schema(description = "X축 오프셋 (CUSTOM 위치일 때 사용)", example = "0")
    private Integer offsetX;

    @Schema(description = "X축 오프셋 단위", example = "px", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String offsetXUnit;

    @Schema(description = "Y축 오프셋 (CUSTOM 위치일 때 사용)", example = "0")
    private Integer offsetY;

    @Schema(description = "Y축 오프셋 단위", example = "px", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String offsetYUnit;

    // 모바일 설정
    @Schema(description = "모바일 설정 활성화 여부", example = "true")
    private Boolean mobileEnabled;

    @Schema(description = "모바일 너비", example = "90")
    private Integer mobileWidth;

    @Schema(description = "모바일 너비 단위", example = "%", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String mobileWidthUnit;

    @Schema(description = "모바일 높이", example = "80")
    private Integer mobileHeight;

    @Schema(description = "모바일 높이 단위", example = "%", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String mobileHeightUnit;

    @Schema(description = "모바일 팝업 위치", example = "CENTER",
            allowableValues = {"CENTER", "TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT", "CUSTOM"})
    private String mobilePosition;

    @Schema(description = "모바일 X축 오프셋", example = "0")
    private Integer mobileOffsetX;

    @Schema(description = "모바일 X축 오프셋 단위", example = "px", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String mobileOffsetXUnit;

    @Schema(description = "모바일 Y축 오프셋", example = "0")
    private Integer mobileOffsetY;

    @Schema(description = "모바일 Y축 오프셋 단위", example = "px", allowableValues = {"px", "%", "vw", "vh", "em", "rem"})
    private String mobileOffsetYUnit;

    @Schema(description = "노출 시작일시 (null이면 제한 없음)", example = "2025-01-01T00:00:00")
    private LocalDateTime displayStartDate;

    @Schema(description = "노출 종료일시 (null이면 제한 없음)", example = "2025-12-31T23:59:59")
    private LocalDateTime displayEndDate;

    @Schema(description = "노출 순서 (낮을수록 먼저 표시)", example = "0")
    private Integer displayOrder;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;
}
