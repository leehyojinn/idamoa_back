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

    // Refresh Token 쿠키 설정 상수
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 14 * 24 * 60 * 60; // 14일 (초 단위)

    /**
     * HttpOnly 쿠키에 Refresh Token 설정
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);  // XSS 방지
        cookie.setSecure(false);   // TODO: production에서는 true로 설정 (HTTPS only)
        cookie.setPath("/");       // 모든 경로에서 쿠키 전송
        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);  // 14일
        response.addCookie(cookie);
        log.debug("Refresh Token 쿠키 설정 완료");
    }

    /**
     * Refresh Token 쿠키에서 읽기
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    log.debug("Refresh Token 쿠키에서 읽기 성공");
                    return cookie.getValue();
                }
            }
        }
        log.warn("Refresh Token 쿠키를 찾을 수 없음");
        return null;
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);   // TODO: production에서는 true로 설정
        cookie.setPath("/");
        cookie.setMaxAge(0);  // 즉시 만료
        response.addCookie(cookie);
        log.debug("Refresh Token 쿠키 삭제 완료");
    }

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
    @Operation(summary = "회원가입 완료", description = "이메일/SMS 인증 후 회원가입 완료 및 JWT 토큰 발급 (Refresh Token은 HttpOnly 쿠키로 전달)")
    @PostMapping("/signup/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoginResponse> completeSignup(
            @Valid @RequestBody SignupCompleteRequest request,
            HttpServletResponse response) {
        TokenInfo tokenInfo = authService.completeSignup(request);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(response, tokenInfo.getRefreshToken());

        // Access Token만 포함된 응답 반환
        return ApiResponse.success(LoginResponse.from(tokenInfo));
    }

    /**
     * 로그인
     */
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인 후 JWT 토큰 발급 (Refresh Token은 HttpOnly 쿠키로 전달)")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody UserLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        TokenInfo tokenInfo = authService.login(request, httpRequest);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(httpResponse, tokenInfo.getRefreshToken());

        // Access Token만 포함된 응답 반환
        return ApiResponse.success(LoginResponse.from(tokenInfo));
    }

    /**
     * 토큰 갱신
     */
    @Operation(summary = "토큰 갱신", description = "HttpOnly 쿠키의 Refresh Token으로 새로운 Access Token 발급")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        // 쿠키에서 Refresh Token 읽기
        String refreshToken = getRefreshTokenFromCookie(request);

        // TokenRefreshRequest 생성
        TokenRefreshRequest tokenRefreshRequest = TokenRefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();

        // 토큰 갱신
        TokenInfo tokenInfo = authService.refreshToken(tokenRefreshRequest);

        // 새로운 Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(response, tokenInfo.getRefreshToken());

        // Access Token만 포함된 응답 반환
        return ApiResponse.success(LoginResponse.from(tokenInfo));
    }

    /**
     * 로그아웃
     */
    @Operation(summary = "로그아웃", description = "로그아웃 처리 (Refresh Token 쿠키 삭제, Redis에서 Refresh Token 삭제, Access Token 블랙리스트 등록)")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletResponse response) {

        String accessToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }

        authService.logout(accessToken, userDetails.getUsername());

        // Refresh Token 쿠키 삭제
        deleteRefreshTokenCookie(response);

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
