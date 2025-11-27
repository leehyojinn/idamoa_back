package com.hip.damoa.domain.inquiry.web.dto;

import com.hip.damoa.domain.inquiry.model.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 일반 문의 작성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일반 문의 작성 요청")
public class InquiryCreateRequest {

    @Schema(description = "문의 유형", example = "BUG", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "문의 유형은 필수입니다")
    private InquiryType inquiryType;

    @Schema(description = "문의 제목", example = "결제 오류 관련 문의", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
    private String title;

    @Schema(description = "문의내용", example = "결제 진행 중 오류가 발생했습니다...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "문의내용은 필수입니다")
    private String content;

    @Schema(description = "첨부파일 UUID 목록", example = "[\"550e8400-e29b-41d4-a716-446655440000\"]")
    @Size(max = 10, message = "첨부파일은 최대 10개까지 가능합니다")
    private List<String> fileUuids;
}
