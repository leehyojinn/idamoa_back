package com.hip.damoa.domain.user.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.user.service.OAuthService;
import com.hip.damoa.domain.user.web.dto.OAuthCallbackResponse;
import com.hip.damoa.domain.user.web.dto.OAuthLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth 소셜 로그인 REST API
 */
@Slf4j
@Tag(name = "OAuth", description = "소셜 로그인 관련 API (카카오, 네이버, 구글)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {

    private final OAuthService oauthService;

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
        log.debug("OAuth - Refresh Token 쿠키 설정 완료");
    }

    /**
     * OAuth 인가 URL 생성 (카카오)
     */
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 OAuth 인가 URL을 생성합니다")
    @GetMapping("/kakao/authorize")
    public ApiResponse<OAuthLoginResponse> getKakaoAuthorizationUrl() {
        OAuthLoginResponse response = oauthService.getAuthorizationUrl("kakao");
        return ApiResponse.success(response);
    }

    /**
     * OAuth Callback 처리 (카카오)
     */
    @Operation(summary = "카카오 로그인 Callback", description = "카카오 OAuth Callback을 처리하고 JWT 토큰을 발급합니다 (Refresh Token은 HttpOnly 쿠키로 전달)")
    @GetMapping("/kakao/callback")
    public String handleKakaoCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse httpResponse) {
        OAuthCallbackResponse response = oauthService.handleCallback("kakao", code, state);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(httpResponse, response.getTokenInfo().getRefreshToken());

        // HTML 페이지로 리다이렉트 (test-oauth.html) - refreshToken 제거
        String redirectUrl = "/test-oauth.html" +
                "?success=true" +
                "&accessToken=" + response.getTokenInfo().getAccessToken() +
                "&requiresProfileSetup=" + !response.getTokenInfo().isProfileCompleted();

        return "redirect:" + redirectUrl;
    }

    /**
     * OAuth 인가 URL 생성 (네이버)
     */
    @Operation(summary = "네이버 로그인 URL 생성", description = "네이버 OAuth 인가 URL을 생성합니다")
    @GetMapping("/naver/authorize")
    public ApiResponse<OAuthLoginResponse> getNaverAuthorizationUrl() {
        OAuthLoginResponse response = oauthService.getAuthorizationUrl("naver");
        return ApiResponse.success(response);
    }

    /**
     * OAuth Callback 처리 (네이버)
     */
    @Operation(summary = "네이버 로그인 Callback", description = "네이버 OAuth Callback을 처리하고 JWT 토큰을 발급합니다 (Refresh Token은 HttpOnly 쿠키로 전달)")
    @GetMapping("/naver/callback")
    public String handleNaverCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse httpResponse) {
        OAuthCallbackResponse response = oauthService.handleCallback("naver", code, state);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(httpResponse, response.getTokenInfo().getRefreshToken());

        // HTML 페이지로 리다이렉트 (test-oauth.html) - refreshToken 제거
        String redirectUrl = "/test-oauth.html" +
                "?success=true" +
                "&accessToken=" + response.getTokenInfo().getAccessToken() +
                "&requiresProfileSetup=" + !response.getTokenInfo().isProfileCompleted();

        return "redirect:" + redirectUrl;
    }

    /**
     * OAuth 인가 URL 생성 (구글)
     */
    @Operation(summary = "구글 로그인 URL 생성", description = "구글 OAuth 인가 URL을 생성합니다")
    @GetMapping("/google/authorize")
    public ApiResponse<OAuthLoginResponse> getGoogleAuthorizationUrl() {
        OAuthLoginResponse response = oauthService.getAuthorizationUrl("google");
        return ApiResponse.success(response);
    }

    /**
     * OAuth Callback 처리 (구글)
     */
    @Operation(summary = "구글 로그인 Callback", description = "구글 OAuth Callback을 처리하고 JWT 토큰을 발급합니다 (Refresh Token은 HttpOnly 쿠키로 전달)")
    @GetMapping("/google/callback")
    public String handleGoogleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse httpResponse) {
        OAuthCallbackResponse response = oauthService.handleCallback("google", code, state);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(httpResponse, response.getTokenInfo().getRefreshToken());

        // HTML 페이지로 리다이렉트 (test-oauth.html) - refreshToken 제거
        String redirectUrl = "/test-oauth.html" +
                "?success=true" +
                "&accessToken=" + response.getTokenInfo().getAccessToken() +
                "&requiresProfileSetup=" + !response.getTokenInfo().isProfileCompleted();

        return "redirect:" + redirectUrl;
    }
}
