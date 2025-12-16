package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.file.web.dto.FileUploadResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 견적 제안 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "견적 제안 응답")
public class ProposalResponse {

    @Schema(description = "제안 내부 ID", example = "1")
    private Long id;

    @Schema(description = "제안 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    // 핵심 정보
    @Schema(description = "제안 제목", example = "강남 사무실 인테리어 제안")
    private String title;

    @Schema(description = "제안 상세 설명", example = "10년 경력의 전문 시공팀이 최상의 결과물을 약속드립니다.")
    private String description;

    @Schema(description = "제안 가격 (원)", example = "25000000")
    private BigDecimal price;

    // 상태 정보
    @Schema(description = "제안 상태 (SUBMITTED, VIEWED, SELECTED, REJECTED, WITHDRAWN)", example = "SUBMITTED")
    private String status;

    @Schema(description = "선택 여부", example = "false")
    private Boolean isSelected;

    @Schema(description = "선택된 일시", example = "2025-01-15T14:00:00")
    private LocalDateTime selectedAt;

    // 유효기간
    @Schema(description = "제안 유효기간", example = "2025-02-01")
    private LocalDate validUntil;

    // 첨부파일 (V30: 조인 테이블)
    @Schema(description = "첨부파일 목록")
    @Builder.Default
    private List<AttachmentResponse> attachments = List.of();

    // 추가 정보
    @Schema(description = "가격 상세 내역 (JSON)", example = "{\"자재비\": 10000000, \"인건비\": 15000000}")
    private Map<String, Object> pricingDetails;

    @Schema(description = "일정 정보 (JSON)", example = "{\"시작일\": \"2025-02-01\", \"완료예정일\": \"2025-03-15\"}")
    private Map<String, Object> timeline;

    // 업체 정보
    @Schema(description = "업체 ID", example = "1")
    private Long companyId;

    @Schema(description = "업체 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID companyUuid;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String companyName;

    // 견적 요청 정보
    @Schema(description = "견적 요청 ID", example = "1")
    private Long requestId;

    @Schema(description = "견적 요청 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID requestUuid;

    @Schema(description = "견적 요청 제목", example = "강남 사무실 인테리어 견적 요청")
    private String requestTitle;

    // 시간 정보
    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-02T14:00:00")
    private LocalDateTime updatedAt;

    // 삭제 여부 (관리자용)
    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    public static ProposalResponse from(EstimateProposal proposal) {
        return ProposalResponse.builder()
                .id(proposal.getId())
                .uuid(proposal.getUuid())
                .title(proposal.getTitle())
                .description(proposal.getDescription())
                .price(proposal.getPrice())
                .status(proposal.getStatus())
                .isSelected(proposal.getIsSelected())
                .selectedAt(proposal.getSelectedAt())
                .validUntil(proposal.getValidUntil())
                // attachments는 Service에서 별도로 설정
                .pricingDetails(proposal.getPricingDetails())
                .timeline(proposal.getTimeline())
                .companyId(proposal.getCompany().getId())
                .companyUuid(proposal.getCompany().getUuid())
                .companyName(proposal.getCompany().getName())
                .requestId(proposal.getRequest().getId())
                .requestUuid(proposal.getRequest().getUuid())
                .requestTitle(proposal.getRequest().getTitle())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .isDeleted(proposal.getIsDeleted())
                .build();
    }
}
