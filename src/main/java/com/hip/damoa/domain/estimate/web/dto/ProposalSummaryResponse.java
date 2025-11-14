package com.hip.damoa.domain.estimate.web.dto;

import com.hip.damoa.domain.estimate.model.EstimateProposal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 제안 요약 응답 DTO (기타 사용자용 - 상세 내용 제외)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제안 요약 정보 (제한된 정보)")
public class ProposalSummaryResponse {

    @Schema(description = "제안 UUID")
    private UUID uuid;

    @Schema(description = "업체명")
    private String companyName;

    @Schema(description = "제안 제목")
    private String title;

    @Schema(description = "제안 상태", example = "SUBMITTED")
    private String status;

    @Schema(description = "제안 제출일")
    private LocalDateTime createdAt;

    @Schema(description = "제안 유효 여부")
    private Boolean isValid;

    /**
     * Entity → 요약 DTO 변환
     */
    public static ProposalSummaryResponse from(EstimateProposal proposal) {
        return ProposalSummaryResponse.builder()
                .uuid(proposal.getUuid())
                .companyName(proposal.getCompany().getName())
                .title(proposal.getTitle())
                .status(proposal.getStatus())
                .createdAt(proposal.getCreatedAt())
                .isValid(proposal.isValid())
                .build();
    }
}
