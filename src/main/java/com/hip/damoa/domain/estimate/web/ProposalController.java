package com.hip.damoa.domain.estimate.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.estimate.model.EstimateProposal;
import com.hip.damoa.domain.estimate.service.ProposalService;
import com.hip.damoa.domain.estimate.web.dto.AttachmentResponse;
import com.hip.damoa.domain.estimate.web.dto.ProposalCreateRequest;
import com.hip.damoa.domain.estimate.web.dto.ProposalResponse;
import com.hip.damoa.domain.estimate.web.dto.ProposalUpdateRequest;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.file.web.dto.FileUploadResponse;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 견적 제안 REST API
 */
@Slf4j
@Tag(name = "09. Proposal", description = "견적 제안 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/proposals")
public class ProposalController {

    private final ProposalService proposalService;
    private final FileRepository fileRepository;

    /**
     * 제안서에 첨부파일 정보 추가 (V30: 조인 테이블 방식)
     */
    private ProposalResponse enrichWithFiles(EstimateProposal proposal) {
        ProposalResponse response = ProposalResponse.from(proposal);
        // Service의 getAttachmentResponses 호출하여 첨부파일 목록 설정
        List<AttachmentResponse> attachments = proposalService.getAttachmentResponses(proposal);
        response.setAttachments(attachments);
        return response;
    }

    /**
     * 제안 제출 (업체)
     */
    @Operation(summary = "제안 제출", description = "견적 요청에 대한 제안을 제출합니다 (COMPANY 역할 필요)")
    @PostMapping("/request/{requestUuid}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProposalResponse> createProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestUuid,
            @Valid @RequestBody ProposalCreateRequest request) {

        EstimateProposal proposal = proposalService.createProposalByUuid(
                userDetails.getUsername(), requestUuid, request);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 제안 수정 (업체)
     */
    @Operation(summary = "제안 수정", description = "내 제안을 수정합니다 (SUBMITTED, VIEWED 상태만)")
    @PutMapping("/{proposalUuid}")
    public ApiResponse<ProposalResponse> updateProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID proposalUuid,
            @Valid @RequestBody ProposalUpdateRequest request) {

        EstimateProposal proposal = proposalService.updateProposalByUuid(
                userDetails.getUsername(), proposalUuid, request);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 제안 철회 (업체)
     */
    @Operation(summary = "제안 철회", description = "내 제안을 철회합니다")
    @DeleteMapping("/{proposalUuid}/withdraw")
    public ApiResponse<Void> withdrawProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID proposalUuid) {

        proposalService.withdrawProposalByUuid(userDetails.getUsername(), proposalUuid);

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

        Page<ProposalResponse> response = proposals.map(this::enrichWithFiles);

        return ApiResponse.success(response);
    }

    /**
     * 견적 요청에 대한 제안 목록 조회 (권한별 필터링)
     * - 요청자: 모든 제안 조회
     * - 업체: 자신의 제안만 조회
     * - 기타: 제안 개수만 조회
     */
    @Operation(summary = "견적 요청별 제안 조회", description = "권한에 따라 다른 수준의 제안 정보를 조회합니다")
    @GetMapping("/request/{requestUuid}")
    public ApiResponse<?> getProposalsByRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID requestUuid) {

        Object result = proposalService.getProposalsByRequestUuid(
                userDetails.getUsername(), requestUuid);

        // 권한에 따라 다른 타입이 반환됨
        if (result instanceof List) {
            @SuppressWarnings("unchecked")
            List<EstimateProposal> proposals = (List<EstimateProposal>) result;
            List<ProposalResponse> response = proposals.stream()
                    .map(this::enrichWithFiles)
                    .collect(Collectors.toList());
            return ApiResponse.success(response);
        } else {
            // 제안 개수만 반환되는 경우 (Map)
            return ApiResponse.success(result);
        }
    }

    /**
     * 제안 조회 (요청자)
     */
    @Operation(summary = "제안 상세 조회", description = "제안 상세를 조회하고 확인 처리합니다 (요청자)")
    @GetMapping("/{proposalUuid}")
    public ApiResponse<ProposalResponse> viewProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID proposalUuid) {

        EstimateProposal proposal = proposalService.viewProposalByUuid(
                userDetails.getUsername(), proposalUuid);

        return ApiResponse.success(enrichWithFiles(proposal));
    }

    /**
     * 제안 수락 (요청자)
     */
    @Operation(summary = "제안 수락", description = "제안을 수락합니다 (요청자)")
    @PostMapping("/{proposalUuid}/accept")
    public ApiResponse<ProposalResponse> acceptProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID proposalUuid) {

        EstimateProposal proposal = proposalService.acceptProposalByUuid(
                userDetails.getUsername(), proposalUuid);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }

    /**
     * 제안 거절 (요청자)
     */
    @Operation(summary = "제안 거절", description = "제안을 거절합니다 (요청자)")
    @PostMapping("/{proposalUuid}/reject")
    public ApiResponse<ProposalResponse> rejectProposal(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID proposalUuid,
            @RequestParam(required = false) String reason) {

        EstimateProposal proposal = proposalService.rejectProposalByUuid(
                userDetails.getUsername(), proposalUuid, reason);

        return ApiResponse.success(ProposalResponse.from(proposal));
    }
}