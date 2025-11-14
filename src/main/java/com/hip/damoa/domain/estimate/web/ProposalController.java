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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "제안 제출",
            description = "견적 요청에 대한 제안을 제출합니다.\n\n" +
                    "**권한**\n" +
                    "- COMPANY 역할이 필요합니다\n" +
                    "- 업체 프로필이 등록되어 있어야 합니다\n\n" +
                    "**제안 상태 (status)**\n" +
                    "- SUBMITTED: 제출됨 (초기 상태)\n" +
                    "- VIEWED: 요청자가 확인\n" +
                    "- SELECTED: 수락됨\n" +
                    "- REJECTED: 거절됨\n" +
                    "- WITHDRAWN: 업체가 철회\n\n" +
                    "**필수 항목**\n" +
                    "- title: 제안 제목\n" +
                    "- description: 제안 상세 설명\n" +
                    "- price: 제안 가격\n\n" +
                    "**선택 항목**\n" +
                    "- validUntil: 제안 유효기간\n" +
                    "- attachments: 첨부파일 (포트폴리오, 견적서 등)\n" +
                    "- pricingDetails: 가격 상세 내역 (JSON)\n" +
                    "- timeline: 일정 정보 (JSON)\n" +
                    "- proposedStartDate/proposedEndDate: 제안 시작/완료일")
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
    @Operation(summary = "제안 수정",
            description = "내 제안을 수정합니다.\n\n" +
                    "**수정 가능 상태**\n" +
                    "- SUBMITTED: 제출됨 (수정 가능)\n" +
                    "- VIEWED: 확인됨 (수정 가능)\n\n" +
                    "**수정 불가 상태**\n" +
                    "- SELECTED: 수락된 제안은 수정 불가\n" +
                    "- REJECTED: 거절된 제안은 수정 불가\n" +
                    "- WITHDRAWN: 철회된 제안은 수정 불가\n\n" +
                    "**권한**\n" +
                    "- 본인이 제출한 제안만 수정 가능\n\n" +
                    "**수정 항목**\n" +
                    "- 모든 필드 수정 가능 (null인 필드는 변경되지 않음)")
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
    @Operation(summary = "제안 철회",
            description = "내 제안을 철회합니다.\n\n" +
                    "**철회 가능 상태**\n" +
                    "- SUBMITTED: 제출됨\n" +
                    "- VIEWED: 확인됨\n\n" +
                    "**철회 불가 상태**\n" +
                    "- SELECTED: 이미 수락된 제안은 철회 불가\n" +
                    "- REJECTED: 이미 거절된 제안\n" +
                    "- WITHDRAWN: 이미 철회된 제안\n\n" +
                    "**철회 후**\n" +
                    "- 상태가 WITHDRAWN으로 변경됩니다\n" +
                    "- 요청자의 제안 목록에서 제외됩니다\n" +
                    "- 복구가 불가능합니다")
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
    @Operation(summary = "내 제안 목록",
            description = "내가 제출한 제안 목록을 조회합니다.\n\n" +
                    "**대상**\n" +
                    "- 업체(COMPANY) 역할 사용자\n" +
                    "- 본인이 제출한 모든 제안\n\n" +
                    "**제안 상태**\n" +
                    "- SUBMITTED: 제출됨\n" +
                    "- VIEWED: 요청자가 확인\n" +
                    "- SELECTED: 수락됨\n" +
                    "- REJECTED: 거절됨\n" +
                    "- WITHDRAWN: 철회됨\n\n" +
                    "**정렬**\n" +
                    "- 최신순 정렬 (createdAt DESC)")
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
    @Operation(summary = "견적 요청별 제안 조회",
            description = "권한에 따라 다른 수준의 제안 정보를 조회합니다.\n\n" +
                    "**요청자 (견적 요청 작성자)**\n" +
                    "- 해당 견적에 제출된 모든 제안을 조회할 수 있습니다\n" +
                    "- 각 제안의 상세 정보 포함\n\n" +
                    "**업체 (제안 제출자)**\n" +
                    "- 본인이 제출한 제안만 조회 가능\n" +
                    "- 다른 업체의 제안은 볼 수 없음\n\n" +
                    "**기타 사용자**\n" +
                    "- 제안 개수만 조회 가능\n" +
                    "- 제안 상세는 볼 수 없음\n\n" +
                    "**응답 형식**\n" +
                    "- 요청자/업체: List<ProposalResponse>\n" +
                    "- 기타: { \"proposalCount\": 5 }")
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
    @Operation(summary = "제안 상세 조회",
            description = "제안 상세를 조회하고 확인 처리합니다.\n\n" +
                    "**권한**\n" +
                    "- 견적 요청 작성자만 조회 가능\n" +
                    "- 또는 제안을 제출한 업체\n\n" +
                    "**자동 처리**\n" +
                    "- 요청자가 처음 조회 시 상태가 SUBMITTED → VIEWED로 변경됩니다\n" +
                    "- 업체는 본인 제안만 조회 가능\n\n" +
                    "**응답 내용**\n" +
                    "- 제안 상세 정보\n" +
                    "- 첨부파일 목록\n" +
                    "- 가격 상세 내역\n" +
                    "- 일정 정보")
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
    @Operation(summary = "제안 수락",
            description = "제안을 수락합니다.\n\n" +
                    "**권한**\n" +
                    "- 견적 요청 작성자만 수락 가능\n\n" +
                    "**수락 가능 상태**\n" +
                    "- SUBMITTED: 제출됨\n" +
                    "- VIEWED: 확인됨\n\n" +
                    "**수락 후 처리**\n" +
                    "- 제안 상태가 SELECTED로 변경\n" +
                    "- isSelected = true로 설정\n" +
                    "- selectedAt 시간 기록\n" +
                    "- 견적 요청 상태가 MATCHED로 변경\n\n" +
                    "**주의사항**\n" +
                    "- 하나의 견적에 대해 여러 제안을 수락할 수 있습니다\n" +
                    "- 수락된 제안은 수정/철회가 불가능합니다")
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
    @Operation(summary = "제안 거절",
            description = "제안을 거절합니다.\n\n" +
                    "**권한**\n" +
                    "- 견적 요청 작성자만 거절 가능\n\n" +
                    "**거절 가능 상태**\n" +
                    "- SUBMITTED: 제출됨\n" +
                    "- VIEWED: 확인됨\n\n" +
                    "**거절 후 처리**\n" +
                    "- 제안 상태가 REJECTED로 변경\n" +
                    "- isSelected = false 설정\n" +
                    "- 거절 사유 기록 (선택)\n\n" +
                    "**파라미터**\n" +
                    "- reason (선택): 거절 사유\n\n" +
                    "**주의사항**\n" +
                    "- 거절된 제안은 다시 수락할 수 없습니다\n" +
                    "- 업체에게 알림이 전송됩니다")
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