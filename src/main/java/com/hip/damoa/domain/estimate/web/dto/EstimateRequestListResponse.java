package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 견적 요청 목록 응답 DTO (간략)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateRequestListResponse {

    private Long id;
    private String title;
    private String location;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String budgetType;
    private String status;
    private String visibility;
    private Integer proposalCount;
    private Integer viewCount;
    private LocalDateTime deadline;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public static EstimateRequestListResponse from(EstimateRequest request) {
        return EstimateRequestListResponse.builder()
                .id(request.getId())
                .title(request.getTitle())
                .location(request.getLocation())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .budgetType(request.getBudgetType())
                .status(request.getStatus())
                .visibility(request.getVisibility())
                .proposalCount(request.getProposalCount())
                .viewCount(request.getViewCount())
                .deadline(request.getDeadline())
                .publishedAt(request.getPublishedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
