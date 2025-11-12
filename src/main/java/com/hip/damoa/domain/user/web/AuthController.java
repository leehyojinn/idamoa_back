package com.hip.damoa.domain.user.web;

import com.hip.damoa.core.jwt.LoginResponse;
import com.hip.damoa.core.jwt.TokenInfo;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.user.service.AuthService;
import com.hip.damoa.domain.user.service.VerificationService;
import com.hip.damoa.domain.user.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 REST API
 */
@Slf4j
@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final VerificationService verificationService;

    /**
     * 회원가입 시작 (1단계)
     */
    @Operation(summary = "회원가입 시작", description = "이메일 중복 체크 후 회원가입 토큰 발급")
    @PostMapping("/signup/start")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupStartResponse> startSignup(@Valid @RequestBody SignupStartRequest request) {
        SignupStartResponse response = authService.startSignup(request);
        return ApiResponse.success(response);
    }

    /**
     * 회원가입 완료 (2단계)
     */
    @Operation(summary = "회원가입 완료", description = "이메일/SMS 인증 후 회원가입 완료 및 JWT 토큰 발급")
    @PostMapping("/signup/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenInfo> completeSignup(@Valid @RequestBody SignupCompleteRequest request) {
        TokenInfo tokenInfo = authService.completeSignup(request);
        return ApiResponse.success(tokenInfo);
    }

    /**
     * 로그인
     */
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인 후 JWT 토큰 발급")
    @PostMapping("/login")
    public ApiResponse<TokenInfo> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest httpRequest) {
        TokenInfo tokenInfo = authService.login(request, httpRequest);
        return ApiResponse.success(tokenInfo);
    }

    /**
     * 토큰 갱신
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token 발급")
    @PostMapping("/refresh")
    public ApiResponse<TokenInfo> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        TokenInfo tokenInfo = authService.refreshToken(request);
        return ApiResponse.success(tokenInfo);
    }

    /**
     * 로그아웃
     */
    @Operation(summary = "로그아웃", description = "로그아웃 처리 (Refresh Token 삭제, Access Token 블랙리스트 등록)")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @AuthenticationPrincipal UserDetails userDetails) {

        String accessToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }

        authService.logout(accessToken, userDetails.getUsername());
        return ApiResponse.success();
    }

    /**
     * 이메일 인증 코드 발송
     */
    @Operation(summary = "이메일 인증 코드 발송", description = "회원가입 시 이메일 인증 코드를 발송합니다")
    @PostMapping("/verification/email/send")
    public ApiResponse<Void> sendEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        verificationService.sendEmailVerificationCode(request.getSignupToken(), request.getEmail());
        return ApiResponse.success();
    }

    /**
     * 이메일 인증 코드 확인
     */
    @Operation(summary = "이메일 인증 코드 확인", description = "발송된 이메일 인증 코드를 확인합니다")
    @PostMapping("/verification/email/verify")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerificationConfirmRequest request) {
        verificationService.verifyEmailCode(request.getSignupToken(), request.getCode(), request.getEmail());
        return ApiResponse.success();
    }

    /**
     * SMS 인증 코드 발송
     */
    @Operation(summary = "SMS 인증 코드 발송", description = "회원가입 시 SMS 인증 코드를 발송합니다")
    @PostMapping("/verification/sms/send")
    public ApiResponse<Void> sendSmsVerification(@Valid @RequestBody SmsVerificationRequest request) {
        verificationService.sendSmsVerificationCode(request.getSignupToken(), request.getPhoneNumber());
        return ApiResponse.success();
    }

    /**
     * SMS 인증 코드 확인
     */
    @Operation(summary = "SMS 인증 코드 확인", description = "발송된 SMS 인증 코드를 확인합니다")
    @PostMapping("/verification/sms/verify")
    public ApiResponse<Void> verifySms(@Valid @RequestBody VerificationConfirmRequest request) {
        verificationService.verifySmsCode(request.getSignupToken(), request.getCode());
        return ApiResponse.success();
    }

    /**
     * 비밀번호 재설정 시작 (1단계 - 토큰 발급)
     */
    @Operation(summary = "비밀번호 재설정 시작", description = "이메일로 사용자를 확인하고 재설정 토큰을 발급합니다")
    @PostMapping("/password/reset/start")
    public ApiResponse<PasswordResetStartResponse> startPasswordReset(@Valid @RequestBody PasswordResetRequestRequest request) {
        PasswordResetStartResponse response = authService.startPasswordReset(request.getEmail());
        return ApiResponse.success(response);
    }

    /**
     * 비밀번호 재설정 이메일 인증 코드 발송 (2단계)
     */
    @Operation(summary = "비밀번호 재설정 이메일 인증 코드 발송", description = "재설정 토큰으로 이메일 인증 코드를 발송합니다")
    @PostMapping("/password/reset/verification/send")
    public ApiResponse<Void> sendPasswordResetVerification(@Valid @RequestBody PasswordResetEmailVerificationRequest request) {
        verificationService.sendPasswordResetCode(request.getResetToken(), request.getEmail());
        return ApiResponse.success();
    }

    /**
     * 비밀번호 재설정 인증 코드 확인 (3단계)
     */
    @Operation(summary = "비밀번호 재설정 인증 코드 확인", description = "이메일로 받은 인증 코드를 확인합니다")
    @PostMapping("/password/reset/verification/verify")
    public ApiResponse<Void> verifyPasswordResetCode(@Valid @RequestBody PasswordResetVerifyRequest request) {
        authService.verifyPasswordResetCode(request.getResetToken(), request.getCode());
        return ApiResponse.success();
    }

    /**
     * 비밀번호 재설정 완료 (4단계)
     */
    @Operation(summary = "비밀번호 재설정 완료", description = "인증 완료 후 새 비밀번호로 변경합니다")
    @PostMapping("/password/reset/complete")
    public ApiResponse<Void> completePasswordReset(@Valid @RequestBody PasswordResetCompleteRequest request) {
        authService.completePasswordReset(request.getResetToken(), request.getNewPassword());
        return ApiResponse.success();
    }

    /**
     * 비밀번호 변경 (로그인 상태)
     */
    @Operation(summary = "비밀번호 변경", description = "로그인 상태에서 현재 비밀번호를 확인하고 새 비밀번호로 변경합니다")
    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(userDetails.getUsername(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.success();
    }
}
