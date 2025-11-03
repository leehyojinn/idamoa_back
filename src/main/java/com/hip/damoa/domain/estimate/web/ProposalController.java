package com.hip.damoa.domain.estimate.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.service.ProposalService;
import com.hip.damoa.domain.estimate.web.dto.ProposalCreateRequest;
import com.hip.damoa.domain.estimate.web.dto.ProposalResponse;
import com.hip.damoa.domain.estimate.web.dto.ProposalUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 견적 제안 REST API
 */
@Slf4j
@Tag(name = "Proposal", description = "견적 제안 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    /**
     * 제안 제출 (업체)
     */
    @Operation(summary = "제안 제출", description = "견적 요청에 대한 제안을 제출합니다 (COMPANY 역할 필요)")
    @PostMapping("/request/{requestId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProposalResponse> createProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @Valid @RequestBody ProposalCreateRequest request) {

        EstimateProposal proposal = proposalService.createProposal(
                userDetails.getUsername(), requestId, request);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 제안 수정 (업체)
     */
    @Operation(summary = "제안 수정", description = "내 제안을 수정합니다 (SUBMITTED, VIEWED 상태만)")
    @PutMapping("/{proposalId}")
    public ApiResponse<ProposalResponse> updateProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long proposalId,
            @Valid @RequestBody ProposalUpdateRequest request) {

        EstimateProposal proposal = proposalService.updateProposal(
                userDetails.getUsername(), proposalId, request);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 제안 철회 (업체)
     */
    @Operation(summary = "제안 철회", description = "내 제안을 철회합니다")
    @DeleteMapping("/{proposalId}/withdraw")
    public ApiResponse<Void> withdrawProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long proposalId) {

        proposalService.withdrawProposal(userDetails.getUsername(), proposalId);

        return ApiResponse.success();
    }

    /**
     * 업체의 제안 목록 조회
     */
    @Operation(summary = "내 제안 목록", description = "내가 제출한 제안 목록을 조회합니다 (업체)")
    @GetMapping("/my")
    public ApiResponse<Page<ProposalResponse>> getMyProposals(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<EstimateProposal> proposals = proposalService.getCompanyProposals(
                userDetails.getUsername(), pageable);

        Page<ProposalResponse> response = proposals.map(ProposalResponse::from);

        return ApiResponse.success(response);
    }

    /**
     * 견적 요청에 대한 제안 목록 조회 (요청자)
     */
    @Operation(summary = "견적 요청별 제안 목록", description = "내 견적 요청에 대한 제안 목록을 조회합니다 (요청자)")
    @GetMapping("/request/{requestId}")
    public ApiResponse<List<ProposalResponse>> getProposalsByRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId) {

        List<EstimateProposal> proposals = proposalService.getProposalsByRequest(
                userDetails.getUsername(), requestId);

        List<ProposalResponse> response = proposals.stream()
                .map(ProposalResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    /**
     * 제안 조회 (요청자)
     */
    @Operation(summary = "제안 상세 조회", description = "제안 상세를 조회하고 확인 처리합니다 (요청자)")
    @GetMapping("/{proposalId}")
    public ApiResponse<ProposalResponse> viewProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long proposalId) {

        EstimateProposal proposal = proposalService.viewProposal(
                userDetails.getUsername(), proposalId);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 제안 수락 (요청자)
     */
    @Operation(summary = "제안 수락", description = "제안을 수락합니다 (요청자)")
    @PostMapping("/{proposalId}/accept")
    public ApiResponse<ProposalResponse> acceptProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long proposalId) {

        EstimateProposal proposal = proposalService.acceptProposal(
                userDetails.getUsername(), proposalId);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 제안 거절 (요청자)
     */
    @Operation(summary = "제안 거절", description = "제안을 거절합니다 (요청자)")
    @PostMapping("/{proposalId}/reject")
    public ApiResponse<ProposalResponse> rejectProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long proposalId,
            @RequestParam(required = false) String reason) {

        EstimateProposal proposal = proposalService.rejectProposal(
                userDetails.getUsername(), proposalId, reason);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }
}
