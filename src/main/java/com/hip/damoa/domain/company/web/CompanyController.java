package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.service.CompanyService;
import com.hip.damoa.domain.company.web.dto.*;
import com.hip.damoa.domain.company.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

/**
 * 업체 관리 REST API (업체 소유자용)
 */
@Slf4j
@Tag(name = "Company", description = "업체 관리 API (업체 소유자용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 업체 등록
     */
    @Operation(summary = "업체 등록", description = "새로운 업체를 등록합니다 (COMPANY 역할 필요)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyResponse> createCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyCreateRequest request) {

        Company company = companyService.createCompany(userDetails.getUsername(), request);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 내 업체 조회
     */
    @Operation(summary = "내 업체 조회", description = "로그인한 사용자의 업체 정보를 조회합니다")
    @GetMapping("/my")
    public ApiResponse<CompanyResponse> getMyCompany(
            @AuthenticationPrincipal UserDetails userDetails) {

        Company company = companyService.getMyCompany(userDetails.getUsername());
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 업체 조회 (ID)
     */
    @Operation(summary = "업체 조회 (ID)", description = "업체 ID로 업체 정보를 조회합니다")
    @GetMapping("/{companyId}")
    public ApiResponse<CompanyResponse> getCompany(@PathVariable Long companyId) {
        Company company = companyService.getCompany(companyId);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 업체 조회 (Slug)
     */
    @Operation(summary = "업체 조회 (Slug)", description = "업체 Slug로 업체 정보를 조회합니다")
    @GetMapping("/slug/{slug}")
    public ApiResponse<CompanyResponse> getCompanyBySlug(@PathVariable String slug) {
        Company company = companyService.getCompanyBySlug(slug);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 활성 업체 목록 조회
     */
    @Operation(summary = "활성 업체 목록 조회", description = "활성화된 업체 목록을 조회합니다 (이미지 포함)")
    @GetMapping
    public ApiResponse<Page<CompanyListResponse>> getActiveCompanies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<Company> companies = companyService.getActiveCompanies(pageable);
        Page<CompanyListResponse> response = companies.map(company -> {
            List<CompanyImageDto> images = companyService.getCompanyImages(company);
            return CompanyListResponse.from(company, images);
        });
        return ApiResponse.success(response);
    }

    /**
     * 업체 검색 (필터링 + 정렬)
     */
    @Operation(summary = "업체 검색", description = "검색 조건과 정렬 기준에 따라 업체를 검색합니다 (공개 API)")
    @GetMapping("/search")
    public ApiResponse<Page<CompanyListResponse>> searchCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String[] serviceAreas,
            @RequestParam(required = false) String[] tags,
            @RequestParam(required = false) java.math.BigDecimal minRating,
            @RequestParam(required = false, defaultValue = "LATEST") String sortBy,
            @PageableDefault(size = 20) Pageable pageable) {

        CompanySearchRequest searchRequest = CompanySearchRequest.builder()
                .keyword(keyword)
                .serviceAreas(serviceAreas)
                .tags(tags)
                .minRating(minRating)
                .sortBy(sortBy)
                .build();

        Page<Company> companies = companyService.searchCompanies(searchRequest, pageable);
        Page<CompanyListResponse> response = companies.map(company -> {
            List<CompanyImageDto> images = companyService.getCompanyImages(company);
            return CompanyListResponse.from(company, images);
        });

        return ApiResponse.success(response);
    }

    /**
     * 업체 수정
     */
    @Operation(summary = "업체 수정", description = "업체 정보를 수정합니다 (소유자만 가능)")
    @PutMapping("/{companyId}")
    public ApiResponse<CompanyResponse> updateCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long companyId,
            @Valid @RequestBody CompanyUpdateRequest request) {

        Company company = companyService.updateCompany(
                userDetails.getUsername(), companyId, request);
        return ApiResponse.success(CompanyResponse.from(company));
    }

    /**
     * 업체 삭제
     */
    @Operation(summary = "업체 삭제", description = "업체를 삭제합니다 (소유자만 가능, Soft Delete)")
    @DeleteMapping("/{companyId}")
    public ApiResponse<Void> deleteCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long companyId) {

        companyService.deleteCompany(userDetails.getUsername(), companyId);
        return ApiResponse.success();
    }
}
