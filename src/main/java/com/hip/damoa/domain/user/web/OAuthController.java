package com.hip.damoa.domain.user.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.user.service.OAuthService;
import com.hip.damoa.domain.user.web.dto.OAuthCallbackResponse;
import com.hip.damoa.domain.user.web.dto.OAuthLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @Operation(summary = "카카오 로그인 Callback", description = "카카오 OAuth Callback을 처리하고 JWT 토큰을 발급합니다")
    @GetMapping("/kakao/callback")
    public ApiResponse<OAuthCallbackResponse> handleKakaoCallback(
            @RequestParam String code,
            @RequestParam String state) {
        OAuthCallbackResponse response = oauthService.handleCallback("kakao", code, state);
        return ApiResponse.success(response);
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
    @Operation(summary = "네이버 로그인 Callback", description = "네이버 OAuth Callback을 처리하고 JWT 토큰을 발급합니다")
    @GetMapping("/naver/callback")
    public ApiResponse<OAuthCallbackResponse> handleNaverCallback(
            @RequestParam String code,
            @RequestParam String state) {
        OAuthCallbackResponse response = oauthService.handleCallback("naver", code, state);
        return ApiResponse.success(response);
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
    @Operation(summary = "구글 로그인 Callback", description = "구글 OAuth Callback을 처리하고 JWT 토큰을 발급합니다")
    @GetMapping("/google/callback")
    public ApiResponse<OAuthCallbackResponse> handleGoogleCallback(
            @RequestParam String code,
            @RequestParam String state) {
        OAuthCallbackResponse response = oauthService.handleCallback("google", code, state);
        return ApiResponse.success(response);
    }
}
