package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 견적 요청 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateRequestResponse {

    private Long id;
    private Long userId;
    private String userEmail;
    private String title;
    private String description;
    private Map<String, Object> requirements;
    private String[] tags;
    private String[] requiredSkills;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String budgetType;
    private LocalDate preferredStartDate;
    private Integer expectedDurationDays;
    private String location;
    private String postalCode;
    private String status;
    private String visibility;
    private Integer proposalCount;
    private Integer viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EstimateRequestResponse from(EstimateRequest request) {
        return EstimateRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUser().getId())
                .userEmail(request.getUser().getEmail())
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .tags(request.getTags())
                .requiredSkills(request.getRequiredSkills())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .budgetType(request.getBudgetType())
                .preferredStartDate(request.getPreferredStartDate())
                .expectedDurationDays(request.getExpectedDurationDays())
                .location(request.getLocation())
                .postalCode(request.getPostalCode())
                .status(request.getStatus())
                .visibility(request.getVisibility())
                .proposalCount(request.getProposalCount())
                .viewCount(request.getViewCount())
                .publishedAt(request.getPublishedAt())
                .deadline(request.getDeadline())
                .completedAt(request.getCompletedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
