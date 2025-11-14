package com.hip.damoa.domain.estimate.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "견적 제안 생성 요청")
public class ProposalCreateRequest {

    @Schema(description = "제안 제목", example = "강남 치과 인테리어 제안서", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제안 제목은 필수입니다")
    @Size(max = 200, message = "제안 제목은 200자를 초과할 수 없습니다")
    private String title;

    @Schema(description = "제안 상세 설명",
            example = "50평 규모 치과 인테리어를 10년 경력으로 진행해드립니다. 현대적이고 깔끔한 디자인으로...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제안 설명은 필수입니다")
    private String description;

    @Schema(description = "제안 가격 (원)", example = "80000000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "제안 가격은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = true, message = "제안 가격은 0 이상이어야 합니다")
    private BigDecimal price;

    // 선택 필드
    @Schema(description = "제안 유효기간 (선택)", example = "2025-12-31")
    private LocalDate validUntil;

    // 첨부파일 (V30: 조인 테이블 방식)
    @Schema(description = "첨부파일 목록 (포트폴리오, 견적서 등)")
    private List<AttachmentRequest> attachments;

    // JSONB 필드 (선택)
    @Schema(description = "가격 상세 정보 (선택, JSON 형태)",
            example = "{\"재료비\": 30000000, \"인건비\": 40000000, \"부가세\": 10000000}")
    private Map<String, Object> pricingDetails;

    @Schema(description = "일정 정보 (선택, JSON 형태)",
            example = "{\"설계기간\": \"2주\", \"시공기간\": \"8주\", \"마무리\": \"1주\"}")
    private Map<String, Object> timeline;

    // HTML에서 받는 필드 (간단한 일정용)
    @Schema(description = "제안 시작일 (선택)", example = "2025-12-01")
    private LocalDate proposedStartDate;

    @Schema(description = "제안 완료일 (선택)", example = "2026-02-28")
    private LocalDate proposedEndDate;
}