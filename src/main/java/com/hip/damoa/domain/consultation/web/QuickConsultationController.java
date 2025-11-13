package com.hip.damoa.domain.consultation.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
import com.hip.damoa.domain.consultation.service.QuickConsultationService;
import com.hip.damoa.domain.consultation.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.UUID;

/**
 * 빠른상담 사용자 API 컨트롤러
 */
@Tag(name = "14. Quick Consultation", description = "빠른상담 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
public class QuickConsultationController {

    private final QuickConsultationService consultationService;

    /**
     * 전체/상태별 상담 목록 조회 (Public - 통합)
     */
    @Operation(summary = "전체/상태별 상담 목록 조회",
            description = "모든 상담 목록을 조회합니다. status 파라미터로 상태별 필터링 가능합니다. 비회원도 조회 가능합니다.\n\n" +
                    "로그인한 경우 isMyConsultation 필드로 본인 상담 여부를 확인할 수 있습니다.")
    @GetMapping
    public ApiResponse<Page<QuickConsultationListResponse>> getAllConsultations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ConsultationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("전체/상태별 상담 목록 조회 API 호출: userEmail={}, status={}", userEmail, status);

        Page<QuickConsultationListResponse> response = consultationService.getAllConsultationsPublic(
                userEmail,
                status,
                pageable
        );

        return ApiResponse.success(response);
    }

    /**
     * 내 상담 목록 조회 (회원 전용)
     */
    @Operation(summary = "내 상담 목록 조회 (내글만보기)",
            description = "로그인한 사용자의 상담 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ApiResponse<Page<QuickConsultationListResponse>> getMyConsultations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("내 상담 목록 조회 API 호출: userEmail={}", userDetails.getUsername());

        Page<QuickConsultationListResponse> response = consultationService.getMyConsultations(
                userDetails.getUsername(),
                pageable
        );

        return ApiResponse.success(response);
    }

    /**
     * 상담 상세 조회 (통합 - 회원/비회원 모두)
     */
    @Operation(summary = "상담 상세 조회 (통합)",
            description = "상담 상세 정보를 조회합니다.\n\n" +
                    "- 로그인 사용자 + 본인 상담: 비밀번호 불필요\n" +
                    "- 로그인 사용자 + 타인 상담: 비밀번호 필요\n" +
                    "- 비회원: 비밀번호 필요")
    @PostMapping("/{consultationUuid}/verify")
    public ApiResponse<QuickConsultationResponse> verifyAndGetConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid,
            @RequestBody(required = false) ConsultationPasswordRequest request) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        String password = request != null ? request.getPassword() : null;

        log.info("상담 상세 조회 API 호출 (통합): userEmail={}, consultationUuid={}, hasPassword={}",
                userEmail, consultationUuid, password != null);

        QuickConsultation consultation = consultationService.getConsultationDetail(
                consultationUuid,
                userEmail,
                password
        );

        return ApiResponse.success(QuickConsultationResponse.from(consultation));
    }

    /**
     * 빠른상담 신청 (Public - 비회원/회원 모두 가능)
     */
    @Operation(summary = "빠른상담 신청", description = "빠른상담을 신청합니다. 비회원/회원 모두 가능합니다.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<QuickConsultationResponse> createConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody QuickConsultationCreateRequest request,
            HttpServletRequest httpRequest) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;

        log.info("빠른상담 신청 API 호출: userEmail={}, name={}", userEmail, request.getName());

        QuickConsultation consultation = consultationService.createConsultation(
                userEmail,
                request,
                httpRequest
        );

        return ApiResponse.success(QuickConsultationResponse.from(consultation));
    }

    /**
     * 상담 수정 (회원 전용)
     */
    @Operation(summary = "상담 수정 (회원)", description = "로그인한 사용자가 자신의 상담을 수정합니다. SUBMITTED 상태만 수정 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{consultationUuid}")
    public ApiResponse<QuickConsultationResponse> updateConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid,
            @Valid @RequestBody QuickConsultationUpdateRequest request) {

        log.info("상담 수정 API 호출 (회원): userEmail={}, consultationUuid={}",
                userDetails.getUsername(), consultationUuid);

        QuickConsultation consultation = consultationService.updateConsultation(
                consultationUuid,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success(QuickConsultationResponse.from(consultation));
    }

    /**
     * 상담 수정 (비회원용 - 비밀번호 검증)
     */
    @Operation(summary = "상담 수정 (비회원)", description = "비회원이 비밀번호를 입력하여 상담을 수정합니다. SUBMITTED 상태만 수정 가능합니다.")
    @PutMapping("/{consultationUuid}/with-password")
    public ApiResponse<QuickConsultationResponse> updateConsultationWithPassword(
            @PathVariable UUID consultationUuid,
            @Valid @RequestBody QuickConsultationUpdateRequest request) {

        log.info("상담 수정 API 호출 (비회원): consultationUuid={}", consultationUuid);

        QuickConsultation consultation = consultationService.updateConsultationWithPassword(
                consultationUuid,
                request
        );

        return ApiResponse.success(QuickConsultationResponse.from(consultation));
    }

    /**
     * 상담 취소 (회원 전용)
     */
    @Operation(summary = "상담 취소", description = "로그인한 사용자가 자신의 상담을 취소합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{consultationUuid}")
    public ApiResponse<Void> cancelConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid,
            @RequestParam(required = false) String reason) {

        log.info("상담 취소 API 호출: userEmail={}, consultationUuid={}",
                userDetails.getUsername(), consultationUuid);

        consultationService.cancelConsultation(
                consultationUuid,
                userDetails.getUsername(),
                reason
        );

        return ApiResponse.success();
    }

    /**
     * 상담 취소 (비회원용 - 비밀번호 검증)
     */
    @Operation(summary = "상담 취소 (비회원)", description = "비회원이 비밀번호를 입력하여 상담을 취소합니다.")
    @DeleteMapping("/{consultationUuid}/with-password")
    public ApiResponse<Void> cancelConsultationWithPassword(
            @PathVariable UUID consultationUuid,
            @Valid @RequestBody ConsultationPasswordRequest request,
            @RequestParam(required = false) String reason) {

        log.info("상담 취소 API 호출 (비회원): consultationUuid={}", consultationUuid);

        consultationService.cancelConsultationWithPassword(
                consultationUuid,
                request.getPassword(),
                reason
        );

        return ApiResponse.success();
    }
}
