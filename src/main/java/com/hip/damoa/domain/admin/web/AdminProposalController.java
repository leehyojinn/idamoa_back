package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.service.ProposalService;
import com.hip.damoa.domain.estimate.web.dto.ProposalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 견적 제안 관리 REST API (관리자용)
 */
@Slf4j
@Tag(name = "16. Admin - Proposal", description = "견적 제안 관리 API (관리자)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/proposals")
public class AdminProposalController {

    private final ProposalService proposalService;

    /**
     * 관리자 - 모든 제안 조회
     */
    @Operation(summary = "모든 제안 조회 (관리자)", description = "모든 제안을 조회합니다")
    @GetMapping
    public ApiResponse<Page<ProposalResponse>> getAllProposals(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<EstimateProposal> proposals = proposalService.getAllProposals(
                userDetails.getUsername(), pageable);

        Page<ProposalResponse> response = proposals.map(ProposalResponse::from);

        return ApiResponse.success(response);
    }

    /**
     * 관리자 - 제안 삭제
     */
    @Operation(summary = "제안 삭제 (관리자)", description = "제안을 삭제합니다")
    @DeleteMapping("/{proposalId}")
    public ApiResponse<Void> deleteProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long proposalId) {

        proposalService.deleteProposal(userDetails.getUsername(), proposalId);

        return ApiResponse.success();
    }
}
