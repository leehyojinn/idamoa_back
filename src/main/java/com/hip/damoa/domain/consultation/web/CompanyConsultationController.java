package com.hip.damoa.domain.consultation.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.consultation.model.PortfolioConsultation;
import com.hip.damoa.domain.consultation.model.PortfolioConsultationStatus;
import com.hip.damoa.domain.consultation.service.PortfolioConsultationService;
import com.hip.damoa.domain.consultation.web.dto.*;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 포트폴리오 상담신청 - 업체 API
 */
@Tag(name = "19. Company Consultation", description = "업체용 포트폴리오 상담신청 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/{companyUuid}/consultations")
public class CompanyConsultationController {

    private final PortfolioConsultationService consultationService;

    @Operation(summary = "[업체] 상담신청 목록 조회",
            description = "업체에 들어온 상담신청 목록을 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**검색 필터**\n" +
                    "- status: 상태별 필터 (PENDING, IN_PROGRESS, ANSWERED, COMPLETED, CANCELLED)\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)")
    @GetMapping
    public ApiResponse<Page<PortfolioConsultationResponse>> getCompanyConsultations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @RequestParam(required = false) PortfolioConsultationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("[업체] 상담신청 목록 조회: userEmail={}, companyUuid={}, status={}",
                userDetails.getUsername(), companyUuid, status);

        Page<PortfolioConsultation> consultations = consultationService.getCompanyConsultations(
                userDetails.getUsername(), companyUuid, status, pageable);

        return ApiResponse.success(consultations.map(PortfolioConsultationResponse::from));
    }

    @Operation(summary = "[업체] 상담신청 상세 조회",
            description = "상담신청 상세 정보를 조회합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 접근 가능\n\n" +
                    "**포함 정보**\n" +
                    "- 신청자 정보 (이름, 연락처, 이메일)\n" +
                    "- 상담 내용 (제목, 내용, 연락방법, 연락가능시간)\n" +
                    "- 업체 메모\n" +
                    "- 답변 정보")
    @GetMapping("/{uuid}")
    public ApiResponse<PortfolioConsultationResponse> getCompanyConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID uuid) {

        log.info("[업체] 상담신청 상세 조회: userEmail={}, companyUuid={}, uuid={}",
                userDetails.getUsername(), companyUuid, uuid);

        PortfolioConsultation consultation = consultationService.getCompanyConsultation(
                userDetails.getUsername(), companyUuid, uuid);

        return ApiResponse.success(PortfolioConsultationResponse.from(consultation));
    }

    @Operation(summary = "[업체] 상담신청 상태 변경",
            description = "상담신청 상태를 변경합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 변경 가능\n\n" +
                    "**상태 종류**\n" +
                    "- PENDING: 대기중\n" +
                    "- IN_PROGRESS: 처리중\n" +
                    "- ANSWERED: 답변완료 (답변 등록 시 자동 변경)\n" +
                    "- COMPLETED: 완료\n" +
                    "- CANCELLED: 취소\n\n" +
                    "**상태 흐름**\n" +
                    "- PENDING → IN_PROGRESS → ANSWERED → COMPLETED\n" +
                    "- PENDING/IN_PROGRESS → CANCELLED")
    @PatchMapping("/{uuid}/status")
    public ApiResponse<PortfolioConsultationResponse> updateConsultationStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody PortfolioConsultationStatusUpdateRequest request) {

        log.info("[업체] 상담신청 상태 변경: userEmail={}, companyUuid={}, uuid={}, status={}",
                userDetails.getUsername(), companyUuid, uuid, request.getStatus());

        PortfolioConsultation consultation = consultationService.updateConsultationStatus(
                userDetails.getUsername(), companyUuid, uuid, request);

        return ApiResponse.success(PortfolioConsultationResponse.from(consultation));
    }

    @Operation(summary = "[업체] 상담신청 답변 등록",
            description = "상담신청에 답변을 등록합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 답변 가능\n\n" +
                    "**동작**\n" +
                    "- 답변 등록 시 상태가 자동으로 ANSWERED로 변경됩니다\n" +
                    "- answeredAt 타임스탬프가 기록됩니다\n" +
                    "- answeredBy에 답변자 정보가 기록됩니다\n\n" +
                    "**필수 필드**\n" +
                    "- answer: 답변 내용")
    @PatchMapping("/{uuid}/answer")
    public ApiResponse<PortfolioConsultationResponse> submitAnswer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody PortfolioConsultationAnswerRequest request) {

        log.info("[업체] 상담신청 답변 등록: userEmail={}, companyUuid={}, uuid={}",
                userDetails.getUsername(), companyUuid, uuid);

        PortfolioConsultation consultation = consultationService.submitAnswer(
                userDetails.getUsername(), companyUuid, uuid, request);

        return ApiResponse.success(PortfolioConsultationResponse.from(consultation));
    }

    @Operation(summary = "[업체] 상담신청 메모 등록",
            description = "상담신청에 업체용 내부 메모를 등록/수정합니다.\n\n" +
                    "**권한:** 업체 소유자(owner)만 등록 가능\n\n" +
                    "**특징**\n" +
                    "- 메모는 업체 관리자만 볼 수 있습니다\n" +
                    "- 신청자(사용자)에게는 노출되지 않습니다\n" +
                    "- 빈 문자열로 메모 삭제 가능")
    @PatchMapping("/{uuid}/memo")
    public ApiResponse<PortfolioConsultationResponse> updateMemo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @PathVariable UUID uuid,
            @Valid @RequestBody PortfolioConsultationMemoRequest request) {

        log.info("[업체] 상담신청 메모 등록: userEmail={}, companyUuid={}, uuid={}",
                userDetails.getUsername(), companyUuid, uuid);

        PortfolioConsultation consultation = consultationService.updateMemo(
                userDetails.getUsername(), companyUuid, uuid, request);

        return ApiResponse.success(PortfolioConsultationResponse.from(consultation));
    }
}
