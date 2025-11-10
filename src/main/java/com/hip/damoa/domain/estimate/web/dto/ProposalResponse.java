package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.file.web.dto.FileUploadResponse;
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
public class ProposalResponse {

    private Long id;
    private UUID uuid;

    // 핵심 정보
    private String title;
    private String description;
    private BigDecimal price;

    // 상태 정보
    private String status;
    private Boolean isSelected;
    private LocalDateTime selectedAt;

    // 유효기간
    private LocalDate validUntil;

    // 첨부파일 (V30: 조인 테이블)
    private List<AttachmentResponse> attachments; // 첨부파일 목록

    // 추가 정보
    private Map<String, Object> pricingDetails;
    private Map<String, Object> timeline;

    // 업체 정보
    private Long companyId;
    private String companyName;

    // 견적 요청 정보
    private Long requestId;
    private UUID requestUuid;
    private String requestTitle;

    // 시간 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
                .companyName(proposal.getCompany().getName())
                .requestId(proposal.getRequest().getId())
                .requestUuid(proposal.getRequest().getUuid())
                .requestTitle(proposal.getRequest().getTitle())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }
}