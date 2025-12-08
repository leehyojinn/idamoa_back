package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "견적 요청 목록 응답")
public class EstimateRequestListResponse {

    @Schema(description = "견적 요청 내부 ID", example = "1")
    private Long id;

    @Schema(description = "견적 요청 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "견적 요청 제목", example = "강남 사무실 인테리어 견적 요청")
    private String title;

    @Schema(description = "시공 위치", example = "서울시 강남구")
    private String location;

    @Schema(description = "예산 최소 금액 (원)", example = "10000000")
    private BigDecimal budgetMin;

    @Schema(description = "예산 최대 금액 (원)", example = "50000000")
    private BigDecimal budgetMax;

    @Schema(description = "견적 상태 (DRAFT, PUBLISHED, IN_PROGRESS, MATCHED, COMPLETED, CANCELLED)", example = "PUBLISHED")
    private String status;

    @Schema(description = "공개 여부 (PUBLIC, PRIVATE)", example = "PUBLIC")
    private String visibility;

    @Schema(description = "제안 수", example = "5")
    private Integer proposalCount;

    @Schema(description = "조회수", example = "150")
    private Integer viewCount;

    @Schema(description = "만료일시", example = "2025-02-01T23:59:59")
    private LocalDateTime expiresAt;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

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
