package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 견적 요청 목록 응답 DTO (간략)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimateRequestListResponse {

    private Long id;  // 클라이언트 편의를 위해 ID도 포함
    private UUID uuid;
    private String title;
    private String location;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private String status;
    private String visibility;
    private Integer proposalCount;
    private Integer viewCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Boolean isDeleted;  // 삭제 여부 (관리자용)

    public static EstimateRequestListResponse from(EstimateRequest request) {
        return EstimateRequestListResponse.builder()
                .id(request.getId())
                .uuid(request.getUuid())
                .title(request.getTitle())
                .location(request.getLocation())
                .budgetMin(request.getBudgetMin())
                .budgetMax(request.getBudgetMax())
                .status(request.getStatusString())
                .visibility(request.getVisibility())
                .proposalCount(request.getProposalCount())
                .viewCount(request.getViewCount())
                .expiresAt(request.getExpiresAt())
                .createdAt(request.getCreatedAt())
                .isDeleted(request.getIsDeleted())
                .build();
    }
}
