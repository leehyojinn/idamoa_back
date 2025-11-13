package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.service.CompanyService;
import com.hip.damoa.domain.company.web.dto.CompanyCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyListResponse;
import com.hip.damoa.domain.company.web.dto.CompanyResponse;
import com.hip.damoa.domain.company.web.dto.CompanyUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 업체 관리 REST API (관리자용)
 */
@Slf4j
@Tag(name = "1901. Admin - Company", description = "업체 관리 API (관리자용)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/companies")
public class AdminCompanyController {

    private final CompanyService companyService;

    /**
     * 업체 등록 (관리자용)
     */
    @Operation(summary = "업체 등록 (관리자)", description = "관리자가 특정 사용자를 위한 업체를 등록합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyResponse> createCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long ownerId,
            @Valid @RequestBody CompanyCreateRequest request) {

        Company company = companyService.createCompanyByAdmin(
                userDetails.getUsername(), ownerId, request);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 전체 업체 목록 조회 (관리자용)
     */
    @Operation(summary = "전체 업체 목록 조회 (관리자)", description = "삭제된 업체 포함 모든 업체 목록을 조회합니다")
    @GetMapping
    public ApiResponse<Page<CompanyListResponse>> getAllCompanies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<Company> companies = companyService.getAllCompanies(pageable);
        Page<CompanyListResponse> response = companies.map(CompanyListResponse::from);
        return ApiResponse.success(response);
    }

    /**
     * 업체 조회 (관리자용)
     */
    @Operation(summary = "업체 조회 (관리자)", description = "업체 UUID로 업체 정보를 조회합니다")
    @GetMapping("/{companyUuid}")
    public ApiResponse<CompanyResponse> getCompany(@PathVariable UUID companyUuid) {
        Company company = companyService.getCompany(companyUuid);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 업체 수정 (관리자용)
     */
    @Operation(summary = "업체 수정 (관리자)", description = "관리자가 업체 정보를 수정합니다 (status, featured 포함)")
    @PutMapping("/{companyUuid}")
    public ApiResponse<CompanyResponse> updateCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @Valid @RequestBody CompanyUpdateRequest request) {

        Company company = companyService.updateCompanyByAdmin(
                userDetails.getUsername(), companyUuid, request);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 업체 삭제 (관리자용)
     */
    @Operation(summary = "업체 삭제 (관리자)", description = "관리자가 업체를 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{companyUuid}")
    public ApiResponse<Void> deleteCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        companyService.deleteCompanyByAdmin(userDetails.getUsername(), companyUuid);
        return ApiResponse.success();
    }

    /**
     * 업체 상태 변경
     */
    @Operation(summary = "업체 상태 변경 (관리자)", description = "업체 상태를 변경합니다 (ACTIVE, INACTIVE, SUSPENDED)")
    @PatchMapping("/{companyUuid}/status")
    public ApiResponse<CompanyResponse> changeCompanyStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @RequestParam String status) {

        Company company = companyService.changeCompanyStatus(
                userDetails.getUsername(), companyUuid, status);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 업체 인증
     */
    @Operation(summary = "업체 인증 (관리자)", description = "업체를 인증합니다")
    @PostMapping("/{companyUuid}/verify")
    public ApiResponse<CompanyResponse> verifyCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        Company company = companyService.verifyCompany(
                userDetails.getUsername(), companyUuid);
        return ApiResponse.success(CompanyResponse.from(company));
    }
}
