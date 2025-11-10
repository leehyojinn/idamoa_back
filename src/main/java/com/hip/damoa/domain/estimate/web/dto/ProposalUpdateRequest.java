package com.hip.damoa.domain.estimate.web.dto;

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
public class ProposalUpdateRequest {

    @Size(max = 200, message = "제안 제목은 200자를 초과할 수 없습니다")
    private String title;

    private String description;

    @DecimalMin(value = "0.0", inclusive = true, message = "제안 가격은 0 이상이어야 합니다")
    private BigDecimal price;

    // 선택 필드
    private LocalDate validUntil;  // 제안 유효기간

    // 첨부파일 (V30: 조인 테이블)
    private List<AttachmentRequest> attachments; // 첨부파일 목록

    // JSONB 필드 (선택)
    private Map<String, Object> pricingDetails;  // 가격 상세 정보
    private Map<String, Object> timeline;  // 일정 정보

    // HTML에서 받는 필드 (간단한 일정용)
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
}