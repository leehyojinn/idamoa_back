package com.hip.damoa.domain.file.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Presigned URL 생성 요청")
public class PresignedUrlRequest {

    @Schema(description = "원본 파일명 (확장자 포함)",
            example = "profile.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "파일명은 필수입니다")
    private String filename;

    @Schema(description = "파일 MIME 타입\n" +
            "허용 타입:\n" +
            "- 이미지: image/jpeg, image/png, image/gif, image/webp\n" +
            "- 문서: application/pdf, application/msword (.doc), application/vnd.openxmlformats-officedocument.wordprocessingml.document (.docx)\n" +
            "- 스프레드시트: application/vnd.ms-excel (.xls), application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (.xlsx)\n" +
            "- 프레젠테이션: application/vnd.ms-powerpoint (.ppt), application/vnd.openxmlformats-officedocument.presentationml.presentation (.pptx)\n" +
            "- HWP: application/x-hwp, application/haansofthwp, application/vnd.hancom.hwp\n" +
            "- CAD: application/acad, application/autocad_dwg (.dwg), application/dxf (.dxf), model/vnd.dwf (.dwf)\n" +
            "- 텍스트: text/plain",
            example = "image/jpeg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MIME 타입은 필수입니다")
    private String mimeType;

    @Schema(description = "파일 크기 (bytes)",
            example = "2048576",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "파일 크기는 필수입니다")
    private Long fileSize;

    @Schema(description = "엔티티 타입 (파일이 연결될 대상 - 선택)",
            example = "COMPANY_IMAGE",
            allowableValues = {"COMPANY_IMAGE", "PORTFOLIO", "ESTIMATE_ATTACHMENT", "BOARD_ATTACHMENT", "REVIEW_IMAGE"})
    private String entityType;

    @Schema(description = "엔티티 ID (연결할 대상의 ID - 선택)",
            example = "123")
    private Long entityId;
}
