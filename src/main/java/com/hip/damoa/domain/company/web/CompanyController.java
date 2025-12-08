package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.service.CompanyImageService;
import com.hip.damoa.domain.company.service.CompanyService;
import com.hip.damoa.domain.company.web.dto.*;
import com.hip.damoa.domain.company.web.dto.CompanyFilterGroupDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 업체 관리 REST API (업체 소유자용)
 */
@Slf4j
@Tag(name = "05. Company", description = "업체 관리 API (업체 소유자용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyImageService companyImageService;

    /**
     * 업체 등록
     */
    @Operation(summary = "업체 등록",
            description = "새로운 업체를 등록합니다.\n\n" +
                    "**필수 조건:**\n" +
                    "- COMPANY 역할 필요 (일반 사용자는 등록 불가)\n" +
                    "- 한 사용자당 하나의 업체만 등록 가능\n\n" +
                    "**필수 정보:**\n" +
                    "- name: 업체명\n" +
                    "- primaryPhone: 대표 전화번호 (형식: 02-1234-5678)\n\n" +
                    "**선택 정보:**\n" +
                    "- slug: URL 친화적 슬러그 (미입력 시 자동 생성)\n" +
                    "- description: 짧은 소개\n" +
                    "- detailContent: 상세 설명 (HTML 또는 Markdown)\n" +
                    "- serviceAreas: 서비스 지역 배열\n" +
                    "- tags: 태그 배열\n" +
                    "- filterOptionIds: 필터 옵션 ID 목록 (업종, 전문영역 등)\n" +
                    "- businessInfo: 사업자 정보 (JSON)\n" +
                    "- businessHours: 영업 시간 (JSON)\n" +
                    "- address, postalCode, latitude, longitude: 위치 정보\n" +
                    "- logoImageUuid, coverImageUuid, galleryImageUuids: 이미지 파일 UUID\n\n" +
                    "**이미지 업로드 프로세스:**\n" +
                    "1. `/api/files/presigned` 호출하여 Presigned URL 획득\n" +
                    "2. S3로 파일 업로드\n" +
                    "3. `/api/files/complete` 호출하여 파일 UUID 획득\n" +
                    "4. 획득한 파일 UUID를 logoImageUuid 등에 입력\n\n" +
                    "**응답:**\n" +
                    "- 생성된 업체 정보\n" +
                    "- uuid: 업체 고유 식별자\n" +
                    "- isActive: 기본값 true")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyResponse> createCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyCreateRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        Company company = companyService.createCompany(userDetails.getUsername(), request);

        CompanyResponse response = CompanyResponse.from(company);

        // filterOptions 제거 - filterGroups만 사용

        // 카테고리별로 그룹화된 필터 추가
        List<CompanyFilterGroupDto> filterGroups = companyService.getCompanyFilterGroups(company);
        response.setFilterGroups(filterGroups);

        // 새로 생성된 업체이므로 좋아요는 항상 false
        response.setIsLiked(false);

        return ApiResponse.success(response);
    }

    /**
     * 내 업체 조회
     */
    @Operation(summary = "내 업체 조회",
            description = "로그인한 사용자의 업체 정보를 조회합니다.\n\n" +
                    "**권한:**\n" +
                    "- COMPANY 역할 필요\n" +
                    "- 로그인 필수\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 업체 기본 정보 (이름, 설명, 연락처 등)\n" +
                    "- 업체 이미지 목록 (로고, 커버, 갤러리 등)\n" +
                    "- 필터 옵션 (전문영역, 진료과, 작업평수 등)\n" +
                    "- 평점 및 리뷰 통계\n" +
                    "- 사업자 정보, 영업 시간\n" +
                    "- 위치 정보 (주소, 좌표)\n\n" +
                    "**활용:**\n" +
                    "- 업체 관리 페이지\n" +
                    "- 업체 정보 수정 전 현재 정보 조회\n" +
                    "- 마이페이지 - 내 업체 정보")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ApiResponse<CompanyResponse> getMyCompany(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        Company company = companyService.getMyCompany(userDetails.getUsername());
        List<CompanyImage> images = companyImageService.getCompanyImages(company.getUuid());

        CompanyResponse response = CompanyResponse.from(company);
        // CompanyImageService를 사용하여 File ID → URL 변환
        response.setImages(images.stream()
                .map(companyImageService::toDto)
                .collect(Collectors.toList()));

        // filterOptions 제거 - filterGroups만 사용

        // 카테고리별로 그룹화된 필터 추가
        List<CompanyFilterGroupDto> filterGroups = companyService.getCompanyFilterGroups(company);
        response.setFilterGroups(filterGroups);

        // 본인 업체이므로 좋아요는 항상 false
        response.setIsLiked(false);

        return ApiResponse.success(response);
    }

    /**
     * 업체 조회 (UUID)
     */
    @Operation(summary = "업체 조회 (UUID)",
            description = "업체 UUID로 업체 정보를 조회합니다.\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 업체 기본 정보 (이름, 설명, 연락처 등)\n" +
                    "- 업체 이미지 목록 (로고, 커버, 갤러리 등)\n" +
                    "- 필터 옵션 (전문영역, 진료과, 작업평수 등)\n" +
                    "- 평점 및 리뷰 통계 (평균 평점, 리뷰 개수)\n" +
                    "- 사업자 정보, 영업 시간\n" +
                    "- 위치 정보 (주소, 좌표)\n" +
                    "- 로그인한 경우: 좋아요 여부 포함\n\n" +
                    "**권한:**\n" +
                    "- 누구나 조회 가능 (로그인 불필요)\n" +
                    "- 비활성화된 업체는 조회 불가\n\n" +
                    "**활용:**\n" +
                    "- 업체 상세 페이지\n" +
                    "- 업체 정보 표시\n" +
                    "- 리뷰 작성 전 업체 확인")
    @GetMapping("/{companyUuid}")
    public ApiResponse<CompanyResponse> getCompany(
            @PathVariable UUID companyUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        Company company = companyService.getCompany(companyUuid);
        List<CompanyImage> images = companyImageService.getCompanyImages(company.getUuid());

        CompanyResponse response = CompanyResponse.from(company);
        // CompanyImageService를 사용하여 File ID → URL 변환
        response.setImages(images.stream()
                .map(companyImageService::toDto)
                .collect(Collectors.toList()));

        // filterOptions 제거 - filterGroups만 사용

        // 카테고리별로 그룹화된 필터 추가
        List<CompanyFilterGroupDto> filterGroups = companyService.getCompanyFilterGroups(company);
        response.setFilterGroups(filterGroups);

        // 로그인한 사용자의 좋아요 여부 설정
        if (userDetails != null) {
            boolean isLiked = companyService.isLiked(userDetails.getUsername(), companyUuid);
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        return ApiResponse.success(response);
    }

    /**
     * 업체 조회 (Slug)
     */
    @Operation(summary = "업체 조회 (Slug)",
            description = "업체 Slug(URL 친화적 식별자)로 업체 정보를 조회합니다.\n\n" +
                    "**Slug란?**\n" +
                    "- URL 친화적인 업체 식별자 (예: `gangnam-interior`)\n" +
                    "- 영문 소문자, 숫자, 하이픈(-)만 사용\n" +
                    "- SEO 최적화에 유리\n" +
                    "- 사람이 읽기 쉬운 URL 제공\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 업체 기본 정보 (이름, 설명, 연락처 등)\n" +
                    "- 업체 이미지 목록 (로고, 커버, 갤러리 등)\n" +
                    "- 필터 옵션 (전문영역, 진료과, 작업평수 등)\n" +
                    "- 평점 및 리뷰 통계\n" +
                    "- 사업자 정보, 영업 시간\n" +
                    "- 위치 정보\n" +
                    "- 로그인한 경우: 좋아요 여부 포함\n\n" +
                    "**권한:**\n" +
                    "- 누구나 조회 가능 (로그인 불필요)\n\n" +
                    "**활용:**\n" +
                    "- SEO 친화적 업체 상세 페이지 (예: `/company/gangnam-interior`)\n" +
                    "- 공유하기 쉬운 URL 제공")
    @GetMapping("/slug/{slug}")
    public ApiResponse<CompanyResponse> getCompanyBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails userDetails) {

        Company company = companyService.getCompanyBySlug(slug);
        List<CompanyImage> images = companyImageService.getCompanyImages(company.getUuid());

        CompanyResponse response = CompanyResponse.from(company);
        // CompanyImageService를 사용하여 File ID → URL 변환
        response.setImages(images.stream()
                .map(companyImageService::toDto)
                .collect(Collectors.toList()));

        // filterOptions 제거 - filterGroups만 사용

        // 카테고리별로 그룹화된 필터 추가
        List<CompanyFilterGroupDto> filterGroups = companyService.getCompanyFilterGroups(company);
        response.setFilterGroups(filterGroups);

        // 로그인한 사용자의 좋아요 여부 설정
        if (userDetails != null) {
            boolean isLiked = companyService.isLiked(userDetails.getUsername(), company.getUuid());
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        return ApiResponse.success(response);
    }

    /**
     * 활성 업체 목록 조회
     */
    @Operation(summary = "활성 업체 목록 조회",
            description = "활성화된 업체 목록을 조회합니다.\n\n" +
                    "**조회 조건:**\n" +
                    "- isActive=true인 업체만 조회\n" +
                    "- 삭제되지 않은 업체만 포함\n\n" +
                    "## 정렬\n" +
                    "- 기본값: createdAt DESC (최신순)\n" +
                    "- 사용법: sort=createdAt,desc 또는 sort=createdAt,asc\n" +
                    "- 기타 옵션: rating, reviewCount\n\n" +
                    "**응답:**\n" +
                    "- 업체 목록 (이름, 설명, 대표 이미지, 평점 등)\n" +
                    "- 로그인한 경우 각 업체의 좋아요 여부 포함\n" +
                    "- 페이지 정보 (totalElements, totalPages 등)\n\n" +
                    "**권한:**\n" +
                    "- 누구나 조회 가능 (로그인 불필요)\n\n" +
                    "**활용:**\n" +
                    "- 업체 메인 페이지\n" +
                    "- 전체 업체 목록")
    @GetMapping
    public ApiResponse<Page<CompanyListResponse>> getActiveCompanies(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        Page<Company> companies = companyService.getActiveCompanies(pageable);
        Page<CompanyListResponse> response = companies.map(company -> {
            List<CompanyImageDto> images = companyService.getCompanyImages(company);
            CompanyListResponse listResponse = CompanyListResponse.from(company, images);

            // 카테고리별로 그룹화된 필터 추가
            List<CompanyFilterGroupDto> filterGroups = companyService.getCompanyFilterGroups(company);
            listResponse.setFilterGroups(filterGroups);

            // 로그인한 사용자의 좋아요 여부 설정
            if (userDetails != null) {
                boolean isLiked = companyService.isLiked(userDetails.getUsername(), company.getUuid());
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
    @Operation(summary = "업체 검색 (통합 키워드 + 카테고리별 필터)",
            description = "통합 키워드 검색과 카테고리별 필터링으로 업체를 검색합니다.\n\n" +
                    "**통합 키워드 검색 (keyword):**\n" +
                    "- 업체명, 설명, 상세내용\n" +
                    "- 주소, 웹사이트URL, 이메일, 전화번호\n" +
                    "- 태그 배열 내부의 텍스트도 검색됨\n" +
                    "- 예: keyword=상업공간 검색 시 태그에 '상업공간' 포함된 업체도 검색\n\n" +
                    "**카테고리별 필터 (filters):**\n" +
                    "- 형식: 카테고리ID:옵션ID1,옵션ID2\n" +
                    "- 예: 1:1,2,3 (카테고리1의 옵션 1,2,3)\n" +
                    "- 같은 카테고리 내: OR 조건\n" +
                    "- 다른 카테고리 간: AND 조건\n" +
                    "- 여러 카테고리는 & 구분: 1:1,2&2:10,11\n\n" +
                    "**정렬 기준 (sortBy):**\n" +
                    "| sortBy | 설명 |\n" +
                    "|--------|------|\n" +
                    "| AD_PRIORITY | 광고 우선순위 (기본값) |\n" +
                    "| LATEST | 최신순 |\n" +
                    "| LIKE_COUNT | 좋아요순 |\n" +
                    "| REVIEW_COUNT | 리뷰순 |\n" +
                    "| VIEW_COUNT | 조회수순 |\n" +
                    "| RATING | 평점순 |\n" +
                    "| PREMIUM_TIER | 프리미엄순 |\n\n" +
                    "**정렬 우선순위:**\n" +
                    "- **1순위**: sortBy로 선택한 기준\n" +
                    "- **2순위 이후**: 광고 → 우선순위 → 좋아요 → 리뷰수 → 조회수 → 평점\n\n" +
                    "**예시:**\n" +
                    "| sortBy | 정렬 순서 |\n" +
                    "|--------|----------|\n" +
                    "| AD_PRIORITY | 광고 → 우선순위 → 좋아요 → 리뷰수 → 조회수 → 평점 |\n" +
                    "| LATEST | 생성일 → 광고 → 우선순위 → 좋아요 → 리뷰수 → 조회수 → 평점 |\n" +
                    "| LIKE_COUNT | 좋아요 → 광고 → 우선순위 → 리뷰수 → 조회수 → 평점 |\n" +
                    "| RATING | 평점 → 광고 → 우선순위 → 좋아요 → 리뷰수 → 조회수 |\n\n" +
                    "**검색 예시:**\n" +
                    "```\n" +
                    "GET /api/companies/search?keyword=병원&filters=1:1,2&sortBy=RATING\n" +
                    "// '병원' 키워드, 필터 적용, 평점순 정렬 (평점 1순위, 광고 2순위)\n" +
                    "```")
    @GetMapping("/search")
    public ApiResponse<Page<CompanyListResponse>> searchCompanies(
            @Parameter(description = "통합 키워드 검색", example = "인테리어")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "최소 평점 (0.0~5.0)", example = "4.0")
            @RequestParam(required = false) java.math.BigDecimal minRating,

            @Parameter(description = "필터 옵션 (카테고리ID:옵션ID1,옵션ID2 형식)", examples = {
                @ExampleObject(name = "지역필터", value = "1:1,2,3"),
                @ExampleObject(name = "업종필터", value = "2:10,11"),
                @ExampleObject(name = "복합필터", value = "1:1,2&2:10,11")
            })
            @RequestParam(required = false) String filters,

            @Parameter(description = "정렬 기준", schema = @Schema(allowableValues = {"AD_PRIORITY", "LATEST", "LIKE_COUNT", "REVIEW_COUNT", "VIEW_COUNT", "RATING", "PREMIUM_TIER"}))
            @RequestParam(required = false, defaultValue = "AD_PRIORITY") String sortBy,

            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 동적 필터 파싱 (예: "1:1,2,3&2:10,11" -> {1: [1,2,3], 2: [10,11]})
        java.util.Map<Long, java.util.List<Long>> filtersByCategory = new java.util.HashMap<>();
        if (filters != null && !filters.isEmpty()) {
            String[] categoryFilters = filters.split("&");
            for (String categoryFilter : categoryFilters) {
                String[] parts = categoryFilter.split(":");
                if (parts.length == 2) {
                    try {
                        Long categoryId = Long.parseLong(parts[0]);
                        java.util.List<Long> optionIds = java.util.Arrays.stream(parts[1].split(","))
                            .map(Long::parseLong)
                            .collect(java.util.stream.Collectors.toList());
                        filtersByCategory.put(categoryId, optionIds);
                    } catch (NumberFormatException e) {
                        // 잘못된 형식은 무시
                    }
                }
            }
        }

        CompanySearchRequest searchRequest = CompanySearchRequest.builder()
                .keyword(keyword)
                .minRating(minRating)
                .filtersByCategory(filtersByCategory)
                .sortBy(sortBy)
                .build();

        Page<Company> companies = companyService.searchCompanies(searchRequest, pageable);
        Page<CompanyListResponse> response = companies.map(company -> {
            List<CompanyImageDto> images = companyService.getCompanyImages(company);
            CompanyListResponse listResponse = CompanyListResponse.from(company, images);

            // 카테고리별로 그룹화된 필터 추가
            List<CompanyFilterGroupDto> filterGroups = companyService.getCompanyFilterGroups(company);
            listResponse.setFilterGroups(filterGroups);

            // 로그인한 사용자의 좋아요 여부 설정
            if (userDetails != null) {
                boolean isLiked = companyService.isLiked(userDetails.getUsername(), company.getUuid());
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
    @Operation(summary = "업체 수정",
            description = "업체 정보를 수정합니다.\n\n" +
                    "**수정 가능 항목:**\n" +
                    "- 업체명, 설명, 상세 내용\n" +
                    "- 연락처 (대표 전화, 보조 전화, 이메일 등)\n" +
                    "- 사업자 정보, 영업 시간\n" +
                    "- 서비스 지역, 태그\n" +
                    "- 필터 옵션 (전문영역, 진료과 등)\n" +
                    "- 주소 및 위치 정보\n" +
                    "- 소셜 링크, 웹사이트 URL\n\n" +
                    "**권한:**\n" +
                    "- 업체 소유자만 수정 가능\n" +
                    "- 다른 사용자가 수정 시도 시 403 Forbidden\n\n" +
                    "**주의사항:**\n" +
                    "- 수정 시 updatedAt 자동 갱신\n" +
                    "- slug는 변경 시 중복 확인 필요\n" +
                    "- 이미지 변경은 별도 이미지 API 사용\n\n" +
                    "**활용:**\n" +
                    "- 업체 정보 관리\n" +
                    "- 업체 프로필 업데이트")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{companyUuid}")
    public ApiResponse<CompanyResponse> updateCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid,
            @Valid @RequestBody CompanyUpdateRequest request) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        Company company = companyService.updateCompany(
                userDetails.getUsername(), companyUuid, request);

        CompanyResponse response = CompanyResponse.from(company);

        // filterOptions 제거 - filterGroups만 사용

        // 카테고리별로 그룹화된 필터 추가
        List<CompanyFilterGroupDto> filterGroups = companyService.getCompanyFilterGroups(company);
        response.setFilterGroups(filterGroups);

        // 본인 업체이므로 좋아요는 항상 false
        response.setIsLiked(false);

        return ApiResponse.success(response);
    }

    /**
     * 업체 삭제
     */
    @Operation(summary = "업체 삭제",
            description = "업체를 삭제합니다.\n\n" +
                    "**삭제 방식:**\n" +
                    "- Soft Delete (isDeleted=true 설정)\n" +
                    "- 실제 데이터는 DB에 남아있음\n" +
                    "- 목록 조회 시 노출되지 않음\n\n" +
                    "**권한:**\n" +
                    "- 업체 소유자만 삭제 가능\n" +
                    "- 다른 사용자가 삭제 시도 시 403 Forbidden\n\n" +
                    "**삭제 후:**\n" +
                    "- deletedAt 자동 설정\n" +
                    "- 복구 불가 (UI에서 접근 불가)\n" +
                    "- 연결된 리뷰, 이미지 등은 유지됨\n" +
                    "- 새로운 업체 등록 가능\n\n" +
                    "**주의사항:**\n" +
                    "- 삭제 전 사용자 확인 권장\n" +
                    "- 삭제 후 복구 불가")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{companyUuid}")
    public ApiResponse<Void> deleteCompany(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        companyService.deleteCompany(userDetails.getUsername(), companyUuid);
        return ApiResponse.success();
    }

    /**
     * 업체 보유 여부 확인
     */
    @Operation(summary = "업체 보유 여부 확인",
            description = "현재 사용자가 이미 업체를 보유하고 있는지 확인합니다.\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n\n" +
                    "**응답:**\n" +
                    "- hasCompany: true/false\n" +
                    "  - true: 이미 업체 보유 (추가 등록 불가)\n" +
                    "  - false: 업체 미보유 (등록 가능)\n\n" +
                    "**제약사항:**\n" +
                    "- 한 사용자당 하나의 업체만 등록 가능\n" +
                    "- 삭제된 업체는 카운트되지 않음\n\n" +
                    "**활용:**\n" +
                    "- 업체 등록 전 보유 여부 확인\n" +
                    "- 업체 등록 버튼 활성화/비활성화\n" +
                    "- 중복 등록 방지")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/check")
    public ApiResponse<Map<String, Boolean>> checkHasCompany(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        boolean hasCompany = companyService.hasCompany(userDetails.getUsername());
        return ApiResponse.success(Map.of("hasCompany", hasCompany));
    }

    /**
     * 업체 좋아요 토글
     */
    @Operation(summary = "업체 좋아요 토글",
            description = "업체 좋아요를 추가하거나 취소합니다.\n\n" +
                    "**동작:**\n" +
                    "- 좋아요가 없는 경우: 좋아요 추가 → isLiked=true 반환\n" +
                    "- 좋아요가 있는 경우: 좋아요 취소 → isLiked=false 반환\n\n" +
                    "**권한:**\n" +
                    "- 로그인 필수\n\n" +
                    "**응답:**\n" +
                    "- isLiked: true/false\n" +
                    "  - true: 좋아요 추가됨\n" +
                    "  - false: 좋아요 취소됨\n" +
                    "- message: 성공 메시지\n\n" +
                    "**활용:**\n" +
                    "- 관심 업체 저장\n" +
                    "- 나중에 다시 보기 위한 북마크\n" +
                    "- 업체 인기도 지표")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{companyUuid}/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID companyUuid) {

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        boolean isLiked = companyService.toggleLike(userDetails.getUsername(), companyUuid);
        return ApiResponse.success(Map.of(
                "isLiked", isLiked,
                "message", isLiked ? "좋아요를 추가했습니다" : "좋아요를 취소했습니다"
        ));
    }
}
