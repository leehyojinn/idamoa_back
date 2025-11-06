package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.service.CompanyImageService;
import com.hip.damoa.domain.company.service.CompanyService;
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
import java.util.Map;
import java.util.stream.Collectors;

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
    private final CompanyImageService companyImageService;

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
    public ApiResponse<CompanyResponse> getCompany(
            @PathVariable Long companyId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Company company = companyService.getCompany(companyId);
        List<CompanyImage> images = companyImageService.getCompanyImages(companyId);

        CompanyResponse response = CompanyResponse.from(company);
        response.setImages(images.stream()
                .map(CompanyImageDto::from)
                .collect(Collectors.toList()));

        // 로그인한 사용자의 좋아요 여부 설정
        if (userDetails != null) {
            boolean isLiked = companyService.isLiked(userDetails.getUsername(), companyId);
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        return ApiResponse.success(response);
    }

    /**
     * 업체 조회 (Slug)
     */
    @Operation(summary = "업체 조회 (Slug)", description = "업체 Slug로 업체 정보를 조회합니다")
    @GetMapping("/slug/{slug}")
    public ApiResponse<CompanyResponse> getCompanyBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails userDetails) {

        Company company = companyService.getCompanyBySlug(slug);
        List<CompanyImage> images = companyImageService.getCompanyImages(company.getId());

        CompanyResponse response = CompanyResponse.from(company);
        response.setImages(images.stream()
                .map(CompanyImageDto::from)
                .collect(Collectors.toList()));

        // 로그인한 사용자의 좋아요 여부 설정
        if (userDetails != null) {
            boolean isLiked = companyService.isLiked(userDetails.getUsername(), company.getId());
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        return ApiResponse.success(response);
    }

    /**
     * 활성 업체 목록 조회
     */
    @Operation(summary = "활성 업체 목록 조회", description = "활성화된 업체 목록을 조회합니다 (이미지 포함)")
    @GetMapping
    public ApiResponse<Page<CompanyListResponse>> getActiveCompanies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        Page<Company> companies = companyService.getActiveCompanies(pageable);
        Page<CompanyListResponse> response = companies.map(company -> {
            List<CompanyImageDto> images = companyService.getCompanyImages(company);
            CompanyListResponse listResponse = CompanyListResponse.from(company, images);

            // 로그인한 사용자의 좋아요 여부 설정
            if (userDetails != null) {
                boolean isLiked = companyService.isLiked(userDetails.getUsername(), company.getId());
                listResponse.setIsLiked(isLiked);
            } else {
                listResponse.setIsLiked(false);
            }

            return listResponse;
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
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

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
            CompanyListResponse listResponse = CompanyListResponse.from(company, images);

            // 로그인한 사용자의 좋아요 여부 설정
            if (userDetails != null) {
                boolean isLiked = companyService.isLiked(userDetails.getUsername(), company.getId());
                listResponse.setIsLiked(isLiked);
            } else {
                listResponse.setIsLiked(false);
            }

            return listResponse;
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

    /**
     * 업체 보유 여부 확인
     */
    @Operation(summary = "업체 보유 여부 확인", description = "현재 사용자가 이미 업체를 보유하고 있는지 확인합니다")
    @GetMapping("/check")
    public ApiResponse<Map<String, Boolean>> checkHasCompany(
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean hasCompany = companyService.hasCompany(userDetails.getUsername());
        return ApiResponse.success(Map.of("hasCompany", hasCompany));
    }

    /**
     * 업체 좋아요 토글
     */
    @Operation(summary = "업체 좋아요 토글", description = "업체 좋아요를 추가하거나 취소합니다")
    @PostMapping("/{companyId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long companyId) {

        boolean isLiked = companyService.toggleLike(userDetails.getUsername(), companyId);
        return ApiResponse.success(Map.of(
                "isLiked", isLiked,
                "message", isLiked ? "좋아요를 추가했습니다" : "좋아요를 취소했습니다"
        ));
    }
}
