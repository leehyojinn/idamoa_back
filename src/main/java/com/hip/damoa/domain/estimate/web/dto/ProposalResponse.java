package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateProposal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 견적 제안 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalResponse {

    private Long id;
    private Long requestId;
    private String requestTitle;
    private Long companyId;
    private String companyName;
    private BigDecimal proposalAmount;
    private String proposalContent;
    private Integer estimatedDurationDays;
    private LocalDate proposedStartDate;
    private LocalDate proposedEndDate;
    private String[] portfolioLinks;
    private String coverLetter;
    private String status;
    private LocalDateTime viewedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProposalResponse from(EstimateProposal proposal) {
        return ProposalResponse.builder()
                .id(proposal.getId())
                .requestId(proposal.getRequest().getId())
                .requestTitle(proposal.getRequest().getTitle())
                .companyId(proposal.getCompany().getId())
                .companyName(proposal.getCompany().getName())
                .proposalAmount(proposal.getProposalAmount())
                .proposalContent(proposal.getProposalContent())
                .estimatedDurationDays(proposal.getEstimatedDurationDays())
                .proposedStartDate(proposal.getProposedStartDate())
                .proposedEndDate(proposal.getProposedEndDate())
                .portfolioLinks(proposal.getPortfolioLinks())
                .coverLetter(proposal.getCoverLetter())
                .status(proposal.getStatus())
                .viewedAt(proposal.getViewedAt())
                .acceptedAt(proposal.getAcceptedAt())
                .rejectedAt(proposal.getRejectedAt())
                .rejectionReason(proposal.getRejectionReason())
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }
}
