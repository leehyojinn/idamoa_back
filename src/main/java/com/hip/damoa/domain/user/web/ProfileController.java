package com.hip.damoa.domain.user.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.user.service.ProfileService;
import com.hip.damoa.domain.user.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 프로필 관리 API
 */
@Slf4j
@Tag(name = "03. Profile", description = "프로필 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * 프로필 상태 조회
     */
    @Operation(summary = "프로필 상태 조회", description = "현재 사용자의 프로필 완성 여부 및 타입을 조회합니다")
    @GetMapping("/api/users/profile/status")
    public ApiResponse<ProfileStatusResponse> getProfileStatus(
            @AuthenticationPrincipal UserDetails userDetails) {

        ProfileStatusResponse response = profileService.getProfileStatus(userDetails.getUsername());
        return ApiResponse.success(response);
    }

    /**
     * 프로필 조회 (통합 - USER/COMPANY 자동 판단)
     */
    @Operation(summary = "프로필 조회 (통합)",
            description = "로그인한 사용자의 프로필을 조회합니다.\n\n" +
                    "USER와 COMPANY 프로필을 자동으로 판단하여 반환합니다.\n" +
                    "- profileType으로 USER/COMPANY 구분\n" +
                    "- DB 컬럼명 기준으로 통일 (phone, bio)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/profile")
    public ApiResponse<ProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        ProfileResponse response = profileService.getProfile(userDetails.getUsername());
        return ApiResponse.success(response);
    }

    /**
     * USER 프로필 생성
     */
    @Operation(summary = "USER 프로필 생성", description = "USER 프로필을 생성하고 프로필 완성 처리를 합니다")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/users/profile")
    public ApiResponse<UserProfileResponse> createUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileCreateRequest request) {

        UserProfileResponse response = profileService.createUserProfile(
                userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * COMPANY 프로필 생성
     */
    @Operation(summary = "COMPANY 프로필 생성", description = "COMPANY 프로필을 생성하고 role을 COMPANY로 변경합니다")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/companies/profile")
    public ApiResponse<CompanyProfileResponse> createCompanyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyProfileCreateRequest request) {

        CompanyProfileResponse response = profileService.createCompanyProfile(
                userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * USER 프로필 수정
     */
    @Operation(summary = "USER 프로필 수정", description = "로그인한 사용자의 USER 프로필을 수정합니다")
    @PutMapping("/api/users/profile")
    public ApiResponse<UserProfileResponse> updateUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request) {

        UserProfileResponse response = profileService.updateUserProfile(
                userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }

    /**
     * COMPANY 프로필 수정
     */
    @Operation(summary = "COMPANY 프로필 수정", description = "로그인한 사용자의 COMPANY 프로필을 수정합니다")
    @PutMapping("/api/companies/profile")
    public ApiResponse<CompanyProfileResponse> updateCompanyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyProfileUpdateRequest request) {

        CompanyProfileResponse response = profileService.updateCompanyProfile(
                userDetails.getUsername(), request);
        return ApiResponse.success(response);
    }
}
