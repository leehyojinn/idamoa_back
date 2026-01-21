package com.hip.damoa.domain.consultation.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.consultation.model.PortfolioConsultation;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 포트폴리오 상담신청 - 사용자 API
 */
@Tag(name = "20. Portfolio Consultation", description = "사용자용 포트폴리오 상담신청 API")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio-consultations")
public class PortfolioConsultationController {

    private final PortfolioConsultationService consultationService;

    @Operation(summary = "[사용자] 상담신청 생성",
            description = "포트폴리오를 보고 해당 업체에 상담을 신청합니다.\n\n" +
                    "**필수 필드**\n" +
                    "- portfolioUuid: 상담할 포트폴리오 UUID\n" +
                    "- name: 신청자 이름\n" +
                    "- phone: 전화번호 (형식: 010-1234-5678)\n" +
                    "- email: 이메일\n" +
                    "- title: 상담 제목\n" +
                    "- content: 상담 내용\n" +
                    "- contactMethod: 연락 방법 (PHONE, EMAIL, KAKAO, ANY)\n\n" +
                    "**선택 필드**\n" +
                    "- availableTime: 연락 가능 시간 (예: \"평일 오전 10시~12시\")\n\n" +
                    "**응답:** 생성된 상담신청 정보 (상태: PENDING)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PortfolioConsultationResponse> createConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PortfolioConsultationCreateRequest request) {

        log.info("[사용자] 상담신청 생성: userEmail={}, portfolioUuid={}",
                userDetails.getUsername(), request.getPortfolioUuid());

        PortfolioConsultation consultation = consultationService.createConsultation(
                userDetails.getUsername(), request);

        return ApiResponse.success(PortfolioConsultationResponse.forUser(consultation));
    }

    @Operation(summary = "[사용자] 내 상담신청 목록 조회",
            description = "로그인한 사용자가 신청한 상담신청 목록을 조회합니다.\n\n" +
                    "**정렬**\n" +
                    "- 기본값: createdAt DESC (최신순)\n\n" +
                    "**페이징**\n" +
                    "- page: 페이지 번호 (0부터 시작)\n" +
                    "- size: 페이지 크기 (기본값: 20)")
    @GetMapping("/my")
    public ApiResponse<Page<PortfolioConsultationResponse>> getMyConsultations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("[사용자] 내 상담신청 목록 조회: userEmail={}",
                userDetails.getUsername());

        Page<PortfolioConsultation> consultations = consultationService.getMyConsultations(
                userDetails.getUsername(), pageable);

        return ApiResponse.success(consultations.map(PortfolioConsultationResponse::forUser));
    }

    @Operation(summary = "[사용자] 내 상담신청 상세 조회",
            description = "내가 신청한 상담신청의 상세 정보를 조회합니다.\n\n" +
                    "**포함 정보**\n" +
                    "- 포트폴리오 정보 (UUID, 제목)\n" +
                    "- 업체 정보 (UUID, 이름)\n" +
                    "- 상담 내용\n" +
                    "- 답변 내용 (있는 경우)\n\n" +
                    "⚠️ 본인의 상담신청만 조회 가능")
    @GetMapping("/my/{uuid}")
    public ApiResponse<PortfolioConsultationResponse> getMyConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {

        log.info("[사용자] 내 상담신청 상세 조회: userEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        PortfolioConsultation consultation = consultationService.getMyConsultation(
                userDetails.getUsername(), uuid);

        return ApiResponse.success(PortfolioConsultationResponse.forUser(consultation));
    }

    @Operation(summary = "[사용자] 내 상담신청 수정",
            description = "내가 신청한 상담신청을 수정합니다.\n\n" +
                    "**수정 조건**\n" +
                    "- 본인이 신청한 상담신청\n" +
                    "- 상태가 PENDING(대기중)인 경우만 수정 가능\n\n" +
                    "**수정 가능 필드 (모든 필드 선택사항)**\n" +
                    "- name: 이름\n" +
                    "- phone: 연락처\n" +
                    "- email: 이메일\n" +
                    "- title: 제목\n" +
                    "- content: 내용\n" +
                    "- contactMethod: 연락방법\n" +
                    "- availableTime: 연락가능시간\n\n" +
                    "⚠️ 입력된 필드만 수정되고, null인 필드는 기존 값이 유지됩니다.")
    @PutMapping("/my/{uuid}")
    public ApiResponse<PortfolioConsultationResponse> updateMyConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid,
            @Valid @RequestBody PortfolioConsultationUpdateRequest request) {

        log.info("[사용자] 내 상담신청 수정: userEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        PortfolioConsultation consultation = consultationService.updateMyConsultation(
                userDetails.getUsername(), uuid, request);

        return ApiResponse.success(PortfolioConsultationResponse.forUser(consultation));
    }

    @Operation(summary = "[사용자] 내 상담신청 삭제",
            description = "내가 신청한 상담신청을 삭제합니다 (소프트 삭제).\n\n" +
                    "**삭제 조건**\n" +
                    "- 본인이 신청한 상담신청\n" +
                    "- 상태가 PENDING, IN_PROGRESS, CANCELLED인 경우만 삭제 가능\n\n" +
                    "⚠️ ANSWERED(답변완료), COMPLETED(완료) 상태는 삭제 불가")
    @DeleteMapping("/my/{uuid}")
    public ApiResponse<Void> deleteMyConsultation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID uuid) {

        log.info("[사용자] 내 상담신청 삭제: userEmail={}, uuid={}",
                userDetails.getUsername(), uuid);

        consultationService.deleteMyConsultation(userDetails.getUsername(), uuid);

        return ApiResponse.success();
    }
}
