package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
import com.hip.damoa.domain.consultation.service.QuickConsultationService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 빠른상담 관리자 API 컨트롤러
 */
@Tag(name = "9909. Admin - Quick Consultation", description = "빠른상담 관리자 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/consultations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuickConsultationController {

    private final QuickConsultationService consultationService;

    /**
     * 전체 상담 목록 조회 (관리자 전용)
     */
    @Operation(summary = "[관리자] 전체 상담 목록 조회",
            description = "모든 상담 내역을 조회합니다.\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping
    public ApiResponse<Page<QuickConsultationListResponse>> getAllConsultations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("[관리자] 전체 상담 목록 조회 API 호출: adminEmail={}", userDetails.getUsername());

        Page<QuickConsultation> consultations = consultationService.getAllConsultations(
                userDetails.getUsername(),
                pageable
        );

        Page<QuickConsultationListResponse> response = consultations.map(
                QuickConsultationListResponse::from
        );

        return ApiResponse.success(response);
    }

    /**
     * 상태별 상담 목록 조회 (관리자 전용)
     */
    @Operation(summary = "[관리자] 상태별 상담 목록 조회",
            description = "특정 상태의 상담 내역을 조회합니다.\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping("/status/{status}")
    public ApiResponse<Page<QuickConsultationListResponse>> getConsultationsByStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable ConsultationStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("[관리자] 상태별 상담 목록 조회 API 호출: adminEmail={}, status={}",
                userDetails.getUsername(), status);

        Page<QuickConsultation> consultations = consultationService.getConsultationsByStatus(
                userDetails.getUsername(),
                status,
                pageable
        );

        Page<QuickConsultationListResponse> response = consultations.map(
                QuickConsultationListResponse::from
        );

        return ApiResponse.success(response);
    }

    /**
     * 상담 상세 조회 (관리자 전용)
     */
    @Operation(summary = "[관리자] 상담 상세 조회", description = "상담 상세 정보를 조회합니다.")
    @GetMapping("/{consultationUuid}")
    public ApiResponse<QuickConsultationResponse> getConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid) {

        log.info("[관리자] 상담 상세 조회 API 호출: adminEmail={}, consultationUuid={}",
                userDetails.getUsername(), consultationUuid);

        QuickConsultation consultation = consultationService.getConsultationForAdmin(
                consultationUuid,
                userDetails.getUsername()
        );

        return ApiResponse.success(QuickConsultationResponse.from(consultation));
    }

    /**
     * 상담 상태 변경 (관리자 전용)
     */
    @Operation(summary = "[관리자] 상담 상태 변경", description = "상담 상태를 변경합니다.")
    @PutMapping("/{consultationUuid}/status")
    public ApiResponse<Void> updateConsultationStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid,
            @Valid @RequestBody ConsultationStatusUpdateRequest request) {

        log.info("[관리자] 상담 상태 변경 API 호출: adminEmail={}, consultationUuid={}, status={}",
                userDetails.getUsername(), consultationUuid, request.getStatus());

        consultationService.updateConsultationStatus(
                consultationUuid,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success();
    }

    /**
     * 상담 답변 작성 (관리자 전용)
     */
    @Operation(summary = "[관리자] 상담 답변 작성", description = "상담에 대한 답변을 작성합니다.")
    @PostMapping("/{consultationUuid}/response")
    public ApiResponse<Void> respondToConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid,
            @Valid @RequestBody ConsultationResponseRequest request) {

        log.info("[관리자] 상담 답변 작성 API 호출: adminEmail={}, consultationUuid={}",
                userDetails.getUsername(), consultationUuid);

        consultationService.respondToConsultation(
                consultationUuid,
                userDetails.getUsername(),
                request
        );

        return ApiResponse.success();
    }

    /**
     * 업체 배정 (관리자 전용)
     */
    @Operation(summary = "[관리자] 업체 배정", description = "상담을 특정 업체에 배정합니다.")
    @PutMapping("/{consultationUuid}/assign/{companyUuid}")
    public ApiResponse<Void> assignToCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid,
            @PathVariable UUID companyUuid) {

        log.info("[관리자] 업체 배정 API 호출: adminEmail={}, consultationUuid={}, companyUuid={}",
                userDetails.getUsername(), consultationUuid, companyUuid);

        consultationService.assignConsultationToCompany(
                consultationUuid,
                companyUuid,
                userDetails.getUsername()
        );

        return ApiResponse.success();
    }

    /**
     * 상담 삭제 (관리자 전용)
     */
    @Operation(summary = "[관리자] 상담 삭제", description = "상담을 삭제합니다 (Soft Delete).")
    @DeleteMapping("/{consultationUuid}")
    public ApiResponse<Void> deleteConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID consultationUuid) {

        log.info("[관리자] 상담 삭제 API 호출: adminEmail={}, consultationUuid={}",
                userDetails.getUsername(), consultationUuid);

        consultationService.deleteConsultation(
                consultationUuid,
                userDetails.getUsername()
        );

        return ApiResponse.success();
    }
}
