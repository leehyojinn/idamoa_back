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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 포트폴리오 상담신청 - 관리자 API
 */
@Tag(name = "9030. Admin - Portfolio Consultation", description = "관리자용 포트폴리오 상담신청 관리 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/portfolio-consultations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPortfolioConsultationController {

    private final PortfolioConsultationService consultationService;

    @Operation(summary = "[관리자] 전체 상담신청 목록 조회",
            description = "전체 상담신청 목록을 조회합니다. 삭제된 항목도 조회 가능합니다.\n\n" +
                    "**검색 필터**\n" +
                    "- status: 상태별 필터 (PENDING, IN_PROGRESS, ANSWERED, COMPLETED, CANCELLED)\n" +
                    "- isDeleted: 삭제 여부 필터 (true/false, 미지정 시 전체)\n" +
                    "- keyword: 이름, 이메일, 제목, 내용에서 검색\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc")
    @GetMapping
    public ApiResponse<Page<PortfolioConsultationResponse>> getConsultations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PortfolioConsultationStatus status,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("[관리자] 상담신청 목록 조회: adminEmail={}, status={}, isDeleted={}, keyword={}",
                userDetails.getUsername(), status, isDeleted, keyword);

        Page<PortfolioConsultation> consultations = consultationService.getAdminConsultations(
                status, isDeleted, keyword, pageable);

        return ApiResponse.success(consultations.map(PortfolioConsultationResponse::forAdmin));
    }

    @Operation(summary = "[관리자] 상담신청 상세 조회",
            description = "상담신청 상세 정보를 조회합니다. 삭제된 항목도 조회 가능합니다.\n\n" +
                    "**포함 정보**\n" +
                    "- 신청자 유저 정보 (UUID, 이메일)\n" +
                    "- 신청자 입력 정보 (이름, 연락처, 이메일)\n" +
                    "- 상담 내용 (제목, 내용, 연락방법, 연락가능시간)\n" +
                    "- 답변 정보 (답변 내용, 답변자, 답변일시)\n" +
                    "- 삭제 여부 및 삭제일시")
    @GetMapping("/{uuid}")
    public ApiResponse<PortfolioConsultationResponse> getConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {

        log.info("[관리자] 상담신청 상세 조회: adminEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        PortfolioConsultation consultation = consultationService.getAdminConsultation(uuid);
        return ApiResponse.success(PortfolioConsultationResponse.forAdmin(consultation));
    }

    @Operation(summary = "[관리자] 상담신청 수정",
            description = "상담신청 정보를 수정합니다.\n\n" +
                    "**수정 가능 필드 (모든 필드 선택사항)**\n" +
                    "- name: 신청자 이름\n" +
                    "- phone: 연락처\n" +
                    "- email: 이메일\n" +
                    "- title: 제목\n" +
                    "- content: 내용\n" +
                    "- contactMethod: 연락방법 (PHONE, EMAIL, KAKAO, ANY)\n" +
                    "- availableTime: 연락가능시간\n\n" +
                    "⚠️ 입력된 필드만 수정되고, null인 필드는 기존 값이 유지됩니다.")
    @PutMapping("/{uuid}")
    public ApiResponse<PortfolioConsultationResponse> updateConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid,
            @Valid @RequestBody PortfolioConsultationUpdateRequest request) {

        log.info("[관리자] 상담신청 수정: adminEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        PortfolioConsultation consultation = consultationService.updateAdminConsultation(uuid, request);
        return ApiResponse.success(PortfolioConsultationResponse.forAdmin(consultation));
    }

    @Operation(summary = "[관리자] 상담신청 상태 변경",
            description = "상담신청 상태를 변경합니다.\n\n" +
                    "**상태 종류**\n" +
                    "- PENDING: 대기중\n" +
                    "- IN_PROGRESS: 처리중\n" +
                    "- ANSWERED: 답변완료\n" +
                    "- COMPLETED: 완료\n" +
                    "- CANCELLED: 취소")
    @PatchMapping("/{uuid}/status")
    public ApiResponse<PortfolioConsultationResponse> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid,
            @Valid @RequestBody PortfolioConsultationStatusUpdateRequest request) {

        log.info("[관리자] 상담신청 상태 변경: adminEmail={}, uuid={}, status={}",
                userDetails.getUsername(), uuid, request.getStatus());

        PortfolioConsultation consultation = consultationService.updateAdminConsultationStatus(uuid, request);
        return ApiResponse.success(PortfolioConsultationResponse.forAdmin(consultation));
    }

    @Operation(summary = "[관리자] 상담신청 삭제 (소프트)",
            description = "상담신청을 소프트 삭제합니다.\n\n" +
                    "- isDeleted = true로 변경\n" +
                    "- 목록에서 기본적으로 숨겨지지만, isDeleted=true 필터로 조회 가능\n" +
                    "- 복구 API로 되돌릴 수 있음")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {

        log.info("[관리자] 상담신청 삭제(소프트): adminEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        consultationService.deleteAdminConsultation(uuid);
        return ApiResponse.success();
    }

    @Operation(summary = "[관리자] 상담신청 복구",
            description = "소프트 삭제된 상담신청을 복구합니다.\n\n" +
                    "- isDeleted = false로 변경\n" +
                    "- 정상 목록에서 다시 조회 가능")
    @PostMapping("/{uuid}/restore")
    public ApiResponse<PortfolioConsultationResponse> restoreConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {

        log.info("[관리자] 상담신청 복구: adminEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        PortfolioConsultation consultation = consultationService.restoreAdminConsultation(uuid);
        return ApiResponse.success(PortfolioConsultationResponse.forAdmin(consultation));
    }

    @Operation(summary = "[관리자] 상담신청 영구 삭제",
            description = "상담신청을 영구적으로 삭제합니다.\n\n" +
                    "⚠️ **주의: 이 작업은 되돌릴 수 없습니다!**\n\n" +
                    "- 데이터베이스에서 완전히 삭제\n" +
                    "- 복구 불가능")
    @DeleteMapping("/{uuid}/permanent")
    public ApiResponse<Void> hardDeleteConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {

        log.info("[관리자] 상담신청 영구 삭제: adminEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        consultationService.hardDeleteAdminConsultation(uuid);
        return ApiResponse.success();
    }
}
