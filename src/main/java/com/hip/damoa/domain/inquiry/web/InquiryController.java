package com.hip.damoa.domain.inquiry.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.inquiry.service.InquiryService;
import com.hip.damoa.domain.inquiry.web.dto.InquiryCreateRequest;
import com.hip.damoa.domain.inquiry.web.dto.InquiryResponse;
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
 * 제휴/광고 문의 API
 */
@Slf4j
@Tag(name = "17. Inquiry", description = "제휴/광고 문의 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의 작성
     */
    @Operation(summary = "문의 작성",
            description = "제휴/광고 문의를 작성합니다. 회원/비회원 모두 작성 가능합니다.\n\n" +
                    "문의 유형:\n" +
                    "- PARTNERSHIP: 제휴 문의\n" +
                    "- ADVERTISEMENT: 광고 문의\n" +
                    "- OTHER: 기타 문의")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<InquiryResponse> createInquiry(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InquiryCreateRequest request) {

        // 회원이면 userDetails.getUsername(), 비회원이면 null
        String userEmail = (userDetails != null) ? userDetails.getUsername() : null;

        InquiryResponse response = inquiryService.createInquiry(userEmail, request);
        return ApiResponse.success(response);
    }
}
