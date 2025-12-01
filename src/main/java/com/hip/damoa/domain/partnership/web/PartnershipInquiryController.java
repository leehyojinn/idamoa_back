package com.hip.damoa.domain.partnership.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.partnership.service.PartnershipInquiryService;
import com.hip.damoa.domain.partnership.web.dto.PartnershipInquiryCreateRequest;
import com.hip.damoa.domain.partnership.web.dto.PartnershipInquiryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 제휴/광고 문의 컨트롤러 (공개 API)
 */
@Slf4j
@Tag(name = "18. Partnership Inquiry", description = "제휴/광고 문의 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/partnership-inquiries")
public class PartnershipInquiryController {

    private final PartnershipInquiryService partnershipInquiryService;

    @Operation(summary = "제휴/광고 문의 생성",
            description = "제휴 및 광고 관련 문의를 생성합니다.\n\n" +
                    "### 문의 유형 (partnershipType)\n" +
                    "- **PARTNERSHIP**: 사업 제휴 문의\n" +
                    "- **ADVERTISEMENT**: 광고 문의\n" +
                    "- **OTHER**: 기타 비즈니스 문의\n\n" +
                    "### 필수 입력 항목\n" +
                    "- **partnershipType**: 문의 유형 (위 3가지 중 택1)\n" +
                    "- **name**: 문의자 이름 (2-50자)\n" +
                    "- **email**: 유효한 이메일 주소\n" +
                    "- **phone**: 연락처 (형식: 010-1234-5678, 02-123-4567)\n" +
                    "- **content**: 문의 내용 (10-5000자)\n\n" +
                    "### 인증\n" +
                    "- 로그인 없이도 문의 가능 (비회원 문의 지원)\n" +
                    "- 로그인한 경우 문의 내역이 계정과 연결됨\n\n" +
                    "### 응답 코드\n" +
                    "- **201**: 문의 생성 성공\n" +
                    "- **400**: 입력값 검증 실패 (상세 오류 메시지 확인)\n" +
                    "- **500**: 서버 오류")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PartnershipInquiryResponse> createInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PartnershipInquiryCreateRequest request) {
        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        log.info("제휴/광고 문의 생성: userEmail={}, type={}", userEmail, request.getPartnershipType());
        PartnershipInquiryResponse response = partnershipInquiryService.createInquiry(userEmail, request);
        return ApiResponse.success(response);
    }
}