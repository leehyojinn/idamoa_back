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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * OAuth 소셜 로그인 REST API
 */
@Slf4j
@Tag(name = "02. OAuth", description = "소셜 로그인 관련 API (카카오, 네이버, 구글)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
public class OAuthController {

    private final OAuthService oauthService;

    @Value("${oauth.frontend-redirect-url}")
    private String frontendRedirectUrl;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    // Refresh Token 쿠키 설정 상수
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 14 * 24 * 60 * 60; // 14일 (초 단위)

    /**
     * HttpOnly 쿠키에 Refresh Token 설정 (환경별 Secure, SameSite 분리)
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        // dev, prod 환경은 HTTPS (Secure=true, SameSite=None)
        // local 환경은 HTTP (Secure=false, SameSite 미설정)
        boolean isSecureEnvironment = "dev".equals(activeProfile) || "prod".equals(activeProfile);

        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .path("/")
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE)
                .httpOnly(true);

        if (isSecureEnvironment) {
            // HTTPS 환경: Secure + SameSite=None (크로스 도메인 지원)
            cookieBuilder.secure(true).sameSite("None");
            log.debug("OAuth - Refresh Token 쿠키 설정 완료 (HTTPS, Secure=true, SameSite=None)");
        } else {
            // HTTP 환경 (local): Secure=false, 프록시 사용 권장
            log.debug("OAuth - Refresh Token 쿠키 설정 완료 (HTTP, Secure=false)");
        }

        ResponseCookie cookie = cookieBuilder.build();
        response.addHeader("Set-Cookie", cookie.toString());
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
    @Operation(summary = "카카오 로그인 Callback",
            description = "카카오 OAuth Callback을 처리하고 프론트엔드로 리다이렉트합니다.\n\n" +
                    "**보안 플로우:**\n" +
                    "1. Refresh Token은 HttpOnly 쿠키에 저장 (XSS 방지)\n" +
                    "2. 프론트엔드 페이지로 리다이렉트 (Access Token은 URL에 포함하지 않음)\n" +
                    "3. 프론트엔드에서 `/api/auth/refresh`를 호출하여 새 Access Token 발급\n\n" +
                    "**리다이렉트 URL 파라미터:**\n" +
                    "- http://localhost:3000/auth/callback?success=true&requiresProfileSetup=true\n" +
                    "- success: 로그인 성공 여부 (true/false)\n" +
                    "- requiresProfileSetup: 프로필 완성 필요 여부 (true/false)")
    @GetMapping("/kakao/callback")
    public void handleKakaoCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse httpResponse) throws IOException {
        OAuthCallbackResponse response = oauthService.handleCallback("kakao", code, state);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(httpResponse, response.getTokenInfo().getRefreshToken());

        // 프론트엔드로 리다이렉트 (Access Token은 URL에 포함하지 않음)
        String redirectUrl = frontendRedirectUrl +
                "?success=true" +
                "&requiresProfileSetup=" + !response.getTokenInfo().isProfileCompleted();

        log.info("OAuth 카카오 로그인 성공 - 리다이렉트: {}", redirectUrl);
        httpResponse.sendRedirect(redirectUrl);
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
    @Operation(summary = "네이버 로그인 Callback",
            description = "네이버 OAuth Callback을 처리하고 프론트엔드로 리다이렉트합니다.\n\n" +
                    "**보안 플로우:**\n" +
                    "1. Refresh Token은 HttpOnly 쿠키에 저장 (XSS 방지)\n" +
                    "2. 프론트엔드 페이지로 리다이렉트 (Access Token은 URL에 포함하지 않음)\n" +
                    "3. 프론트엔드에서 `/api/auth/refresh`를 호출하여 새 Access Token 발급\n\n" +
                    "**리다이렉트 URL 파라미터:**\n" +
                    "- http://localhost:3000/auth/callback?success=true&requiresProfileSetup=true\n" +
                    "- success: 로그인 성공 여부 (true/false)\n" +
                    "- requiresProfileSetup: 프로필 완성 필요 여부 (true/false)")
    @GetMapping("/naver/callback")
    public void handleNaverCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse httpResponse) throws IOException {
        OAuthCallbackResponse response = oauthService.handleCallback("naver", code, state);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(httpResponse, response.getTokenInfo().getRefreshToken());

        // 프론트엔드로 리다이렉트 (Access Token은 URL에 포함하지 않음)
        String redirectUrl = frontendRedirectUrl +
                "?success=true" +
                "&requiresProfileSetup=" + !response.getTokenInfo().isProfileCompleted();

        log.info("OAuth 네이버 로그인 성공 - 리다이렉트: {}", redirectUrl);
        httpResponse.sendRedirect(redirectUrl);
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
    @Operation(summary = "구글 로그인 Callback",
            description = "구글 OAuth Callback을 처리하고 프론트엔드로 리다이렉트합니다.\n\n" +
                    "**보안 플로우:**\n" +
                    "1. Refresh Token은 HttpOnly 쿠키에 저장 (XSS 방지)\n" +
                    "2. 프론트엔드 페이지로 리다이렉트 (Access Token은 URL에 포함하지 않음)\n" +
                    "3. 프론트엔드에서 `/api/auth/refresh`를 호출하여 새 Access Token 발급\n\n" +
                    "**리다이렉트 URL 파라미터:**\n" +
                    "- http://localhost:3000/auth/callback?success=true&requiresProfileSetup=true\n" +
                    "- success: 로그인 성공 여부 (true/false)\n" +
                    "- requiresProfileSetup: 프로필 완성 필요 여부 (true/false)")
    @GetMapping("/google/callback")
    public void handleGoogleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse httpResponse) throws IOException {
        OAuthCallbackResponse response = oauthService.handleCallback("google", code, state);

        // Refresh Token을 HttpOnly 쿠키에 설정
        setRefreshTokenCookie(httpResponse, response.getTokenInfo().getRefreshToken());

        // 프론트엔드로 리다이렉트 (Access Token은 URL에 포함하지 않음)
        String redirectUrl = frontendRedirectUrl +
                "?success=true" +
                "&requiresProfileSetup=" + !response.getTokenInfo().isProfileCompleted();

        log.info("OAuth 구글 로그인 성공 - 리다이렉트: {}", redirectUrl);
        httpResponse.sendRedirect(redirectUrl);
    }
}
