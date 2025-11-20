package com.hip.damoa.domain.inquiry.web.dto;

import com.hip.damoa.domain.inquiry.model.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 제휴/광고 문의 작성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제휴/광고 문의 작성 요청")
public class InquiryCreateRequest {

    @Schema(description = "문의 유형", example = "PARTNERSHIP", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "문의 유형은 필수입니다")
    private InquiryType inquiryType;

    @Schema(description = "작성자명", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "작성자명은 필수입니다")
    @Size(max = 100, message = "작성자명은 100자를 초과할 수 없습니다")
    private String name;

    @Schema(description = "이메일주소", example = "contact@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일주소는 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일주소는 100자를 초과할 수 없습니다")
    private String email;

    @Schema(description = "연락처", example = "010-1234-5678", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "연락처는 필수입니다")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다 (예: 010-1234-5678)")
    private String phone;

    @Schema(description = "문의내용", example = "제휴 문의드립니다...", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "문의내용은 필수입니다")
    private String content;
}
