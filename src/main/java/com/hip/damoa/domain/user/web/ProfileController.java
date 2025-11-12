package com.hip.damoa.domain.user.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.user.service.ProfileService;
import com.hip.damoa.domain.user.web.dto.*;
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
 * 프로필 관리 API
 */
@Slf4j
@Tag(name = "03. Profile", description = "프로필 관리 API")
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
}
