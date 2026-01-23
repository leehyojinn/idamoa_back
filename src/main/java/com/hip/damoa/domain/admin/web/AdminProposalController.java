package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.service.ProposalService;
import com.hip.damoa.domain.estimate.web.dto.ProposalResponse;
import com.hip.damoa.domain.estimate.web.dto.ProposalUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "9904. Admin - Proposal", description = "견적 제안 관리 API (관리자)")
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
    @Operation(summary = "모든 제안 조회 (관리자)",
            description = "모든 제안을 조회합니다 (삭제된 데이터 포함)\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
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
     * 관리자 - 제안 수정
     */
    @Operation(summary = "제안 수정 (관리자)",
            description = """
                    제안을 수정합니다. 권한 검증 없이 모든 제안을 수정할 수 있습니다.

                    ## 수정 가능 항목
                    - title: 제안 제목
                    - description: 제안 설명
                    - price: 제안 가격
                    - validUntil: 유효 기간
                    - pricingDetails: 가격 상세
                    - timeline: 일정 정보
                    - attachments: 첨부파일
                    """)
    @PutMapping("/{proposalUuid}")
    public ApiResponse<ProposalResponse> updateProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "제안 UUID") @PathVariable UUID proposalUuid,
            @Valid @RequestBody ProposalUpdateRequest request) {
        log.info("[관리자] 제안 수정: adminEmail={}, proposalUuid={}", userDetails.getUsername(), proposalUuid);
        EstimateProposal proposal = proposalService.updateProposalByAdmin(proposalUuid, request);
        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 관리자 - 제안 상태 변경
     */
    @Operation(summary = "제안 상태 변경 (관리자)",
            description = """
                    제안의 상태를 변경합니다.

                    ## 상태 종류
                    - SUBMITTED: 제출됨
                    - VIEWED: 확인됨
                    - SELECTED: 수락됨
                    - REJECTED: 거절됨
                    - WITHDRAWN: 철회됨
                    """)
    @PatchMapping("/{proposalUuid}/status")
    public ApiResponse<ProposalResponse> updateProposalStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "제안 UUID") @PathVariable UUID proposalUuid,
            @Parameter(description = "변경할 상태") @RequestParam String status) {
        log.info("[관리자] 제안 상태 변경: adminEmail={}, proposalUuid={}, status={}",
                userDetails.getUsername(), proposalUuid, status);
        EstimateProposal proposal = proposalService.updateProposalStatusByAdmin(proposalUuid, status);
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
