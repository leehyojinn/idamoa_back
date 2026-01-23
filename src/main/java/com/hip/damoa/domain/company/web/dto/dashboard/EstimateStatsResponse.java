package com.hip.damoa.domain.company.web.dto.dashboard;

import com.hip.damoa.domain.estimate.model.EstimateProposal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 견적 제안 통계 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateStatsResponse {

    private long totalProposals;
    private long accepted;      // SELECTED 상태
    private long rejected;      // REJECTED 상태
    private long pending;       // SUBMITTED 상태
    private Double acceptanceRate;  // 수락률 (%)

    private List<RecentProposal> recentProposals;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentProposal {
        private UUID uuid;
        private String estimateTitle;
        private BigDecimal proposalAmount;
        private String status;
        private Boolean isSelected;
        private LocalDateTime createdAt;

        public static RecentProposal from(EstimateProposal proposal) {
            return RecentProposal.builder()
                    .uuid(proposal.getUuid())
                    .estimateTitle(proposal.getRequest() != null ? proposal.getRequest().getTitle() : "")
                    .proposalAmount(proposal.getPrice())
                    .status(proposal.getStatus())
                    .isSelected(proposal.getIsSelected())
                    .createdAt(proposal.getCreatedAt())
                    .build();
        }
    }

    /**
     * 요약용 (전체 대시보드 요약에 포함)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalProposals;
        private long accepted;
        private long rejected;
        private long pending;
    }
}
