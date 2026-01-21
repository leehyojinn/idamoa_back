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
@Tag(name = "1016. Quick Consultation", description = "빠른상담 관련 API")
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
            description = "모든 상담 목록을 조회합니다.\n\n" +
                    "**검색 조건:**\n" +
                    "- status: 상담 상태 필터 (선택)\n" +
                    "  - `SUBMITTED`: 접수됨 (상담 대기)\n" +
                    "  - `IN_PROGRESS`: 상담 진행 중\n" +
                    "  - `COMPLETED`: 상담 완료\n" +
                    "  - `CANCELLED`: 취소됨\n" +
                    "  - null: 전체 상담\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: status, updatedAt\n\n" +
                    "**응답:**\n" +
                    "- 상담 목록 (제목, 이름, 전화번호 일부, 상태, 작성일)\n" +
                    "- 로그인한 경우 isMyConsultation=true/false 포함\n" +
                    "- 비로그인 사용자도 조회 가능\n\n" +
                    "**권한:**\n" +
                    "- 누구나 조회 가능 (로그인 불필요)\n" +
                    "- 개인정보 보호를 위해 전화번호는 일부만 표시 (예: 010-****-5678)\n\n" +
                    "**활용:**\n" +
                    "- 상담 게시판 목록\n" +
                    "- 상태별 상담 필터링")
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
            description = "로그인한 사용자의 상담 목록을 조회합니다.\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n\n" +
                    "**응답:**\n" +
                    "- 본인이 작성한 상담 목록만 반환\n" +
                    "- 전체 상담 정보 포함 (전화번호 전체 표시)\n" +
                    "- 최신순 정렬\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: status, updatedAt\n\n" +
                    "**활용:**\n" +
                    "- 마이페이지 - 내 상담 목록\n" +
                    "- 내글만보기 필터")
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
                    "**인증 방식:**\n" +
                    "- 로그인 사용자 + 본인 상담: 비밀번호 불필요 (자동 인증)\n" +
                    "- 로그인 사용자 + 타인 상담: 4자리 비밀번호 필요\n" +
                    "- 비회원: 4자리 비밀번호 필요\n\n" +
                    "**필수 정보:**\n" +
                    "- consultationUuid: 상담 UUID\n" +
                    "- password: 4자리 숫자 비밀번호 (본인 상담이 아닌 경우)\n\n" +
                    "**응답:**\n" +
                    "- 상담 전체 정보 (이름, 전화번호, 이메일, 상담 내용 등)\n" +
                    "- 상담 상태 (SUBMITTED, IN_PROGRESS, COMPLETED, CANCELLED)\n" +
                    "- 관리자 답변 (있는 경우)\n" +
                    "- 생성일, 수정일\n\n" +
                    "**권한:**\n" +
                    "- 비밀번호가 일치해야 조회 가능\n" +
                    "- 비밀번호 불일치 시 401 Unauthorized\n\n" +
                    "**활용:**\n" +
                    "- 상담 상세 페이지\n" +
                    "- 비밀번호 확인 후 상담 내용 확인")
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
    @Operation(summary = "빠른상담 신청",
            description = "빠른상담을 신청합니다.\n\n" +
                    "**필수 정보:**\n" +
                    "- name: 이름 (최대 100자)\n" +
                    "- phone: 전화번호 (형식: 010-1234-5678)\n" +
                    "- message: 상담 내용 (10자 이상 5000자 이하)\n" +
                    "- password: 4자리 숫자 비밀번호 (비회원 조회용)\n" +
                    "- personalInfoConsent: 개인정보 수집 및 이용 동의 (true 필수)\n" +
                    "- thirdPartyConsent: 개인정보 제3자 제공 동의 (true 필수)\n" +
                    "- termsOfServiceConsent: 이용약관 동의 (true 필수)\n\n" +
                    "**선택 정보:**\n" +
                    "- email: 이메일\n" +
                    "- subject: 제목 (최대 200자)\n" +
                    "- preferredContactMethod: 선호 연락 방법 (예: 전화, 이메일, 카톡)\n" +
                    "- preferredContactTime: 선호 연락 시간 (예: 오전 10시~12시)\n" +
                    "- marketingConsent: 마케팅 수신 동의 (선택)\n\n" +
                    "**권한:**\n" +
                    "- 비회원/회원 모두 신청 가능\n" +
                    "- 로그인한 경우 자동으로 회원 정보 연결\n\n" +
                    "**신청 후:**\n" +
                    "- 상담 상태: SUBMITTED (접수됨)\n" +
                    "- 관리자가 확인 후 상태 변경\n" +
                    "- 비밀번호로 상담 내용 조회 가능\n\n" +
                    "**활용:**\n" +
                    "- 빠른상담 신청 폼\n" +
                    "- 1:1 문의 접수")
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
    @Operation(summary = "상담 수정 (회원)",
            description = "로그인한 사용자가 자신의 상담을 수정합니다.\n\n" +
                    "**수정 가능 조건:**\n" +
                    "- 본인이 작성한 상담만 수정 가능\n" +
                    "- SUBMITTED 상태인 경우만 수정 가능\n" +
                    "- IN_PROGRESS, COMPLETED, CANCELLED 상태는 수정 불가\n\n" +
                    "**수정 가능 항목:**\n" +
                    "- 이름, 전화번호, 이메일\n" +
                    "- 제목, 상담 내용\n" +
                    "- 선호 연락 방법, 선호 연락 시간\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n" +
                    "- 작성자 본인만 수정 가능\n\n" +
                    "**주의사항:**\n" +
                    "- 수정 시 updatedAt 자동 갱신\n" +
                    "- 상담 진행 중이거나 완료된 경우 수정 불가")
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
    @Operation(summary = "상담 수정 (비회원)",
            description = "비회원이 비밀번호를 입력하여 상담을 수정합니다.\n\n" +
                    "**수정 가능 조건:**\n" +
                    "- 4자리 비밀번호가 일치해야 함\n" +
                    "- SUBMITTED 상태인 경우만 수정 가능\n" +
                    "- IN_PROGRESS, COMPLETED, CANCELLED 상태는 수정 불가\n\n" +
                    "**필수 정보:**\n" +
                    "- password: 4자리 숫자 비밀번호 (상담 신청 시 입력한 비밀번호)\n" +
                    "- 수정할 항목 (이름, 전화번호, 상담 내용 등)\n\n" +
                    "**권한:**\n" +
                    "- 비밀번호 일치 시 수정 가능\n" +
                    "- 비밀번호 불일치 시 401 Unauthorized\n\n" +
                    "**활용:**\n" +
                    "- 비회원 상담 수정\n" +
                    "- 비밀번호 확인 후 정보 변경")
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
    @Operation(summary = "상담 취소",
            description = "로그인한 사용자가 자신의 상담을 취소합니다.\n\n" +
                    "**취소 가능 조건:**\n" +
                    "- 본인이 작성한 상담만 취소 가능\n" +
                    "- SUBMITTED 또는 IN_PROGRESS 상태인 경우 취소 가능\n" +
                    "- COMPLETED 상태는 취소 불가\n\n" +
                    "**선택 정보:**\n" +
                    "- reason: 취소 사유 (선택)\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n" +
                    "- 작성자 본인만 취소 가능\n\n" +
                    "**취소 후:**\n" +
                    "- 상담 상태가 CANCELLED로 변경\n" +
                    "- cancelledAt 자동 설정\n" +
                    "- 취소된 상담은 복구 불가")
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
    @Operation(summary = "상담 취소 (비회원)",
            description = "비회원이 비밀번호를 입력하여 상담을 취소합니다.\n\n" +
                    "**취소 가능 조건:**\n" +
                    "- 4자리 비밀번호가 일치해야 함\n" +
                    "- SUBMITTED 또는 IN_PROGRESS 상태인 경우 취소 가능\n" +
                    "- COMPLETED 상태는 취소 불가\n\n" +
                    "**필수 정보:**\n" +
                    "- password: 4자리 숫자 비밀번호 (상담 신청 시 입력한 비밀번호)\n\n" +
                    "**선택 정보:**\n" +
                    "- reason: 취소 사유 (선택)\n\n" +
                    "**권한:**\n" +
                    "- 비밀번호 일치 시 취소 가능\n" +
                    "- 비밀번호 불일치 시 401 Unauthorized\n\n" +
                    "**취소 후:**\n" +
                    "- 상담 상태가 CANCELLED로 변경\n" +
                    "- cancelledAt 자동 설정\n" +
                    "- 취소된 상담은 복구 불가\n\n" +
                    "**활용:**\n" +
                    "- 비회원 상담 취소\n" +
                    "- 비밀번호 확인 후 취소 처리")
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
