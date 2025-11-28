package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.service.ProposalService;
import com.hip.damoa.domain.estimate.web.dto.ProposalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 견적 제안 관리 REST API (관리자용)
 */
@Slf4j
@Tag(name = "1903. Admin - Proposal", description = "견적 제안 관리 API (관리자)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/proposals")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProposalController {

    private final ProposalService proposalService;

    /**
     * 관리자 - 모든 제안 조회
     */
    @Operation(summary = "모든 제안 조회 (관리자)", description = "모든 제안을 조회합니다 (삭제된 데이터 포함)")
    @GetMapping
    public ApiResponse<Page<ProposalResponse>> getAllProposals(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("[관리자] 모든 제안 조회: adminEmail={}", userDetails.getUsername());
        Page<EstimateProposal> proposals = proposalService.getAllProposals(userDetails.getUsername(), pageable);
        Page<ProposalResponse> response = proposals.map(ProposalResponse::from);
        return ApiResponse.success(response);
    }

    /**
     * 관리자 - 제안 상세 조회
     */
    @Operation(summary = "제안 상세 조회 (관리자)", description = "제안 상세 정보를 조회합니다 (삭제된 데이터 포함)")
    @GetMapping("/{proposalUuid}")
    public ApiResponse<ProposalResponse> getProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "제안 UUID") @PathVariable UUID proposalUuid) {
        log.info("[관리자] 제안 상세 조회: adminEmail={}, proposalUuid={}", userDetails.getUsername(), proposalUuid);
        EstimateProposal proposal = proposalService.getProposalByUuidForAdmin(userDetails.getUsername(), proposalUuid);
        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 관리자 - 제안 삭제
     */
    @Operation(summary = "제안 삭제 (관리자)", description = "제안을 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{proposalUuid}")
    public ApiResponse<Void> deleteProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "제안 UUID") @PathVariable UUID proposalUuid) {
        log.info("[관리자] 제안 삭제: adminEmail={}, proposalUuid={}", userDetails.getUsername(), proposalUuid);
        proposalService.deleteProposalByUuid(userDetails.getUsername(), proposalUuid);
        return ApiResponse.success();
    }
}
