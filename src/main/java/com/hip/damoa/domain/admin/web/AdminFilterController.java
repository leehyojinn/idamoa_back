package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.admin.service.AdminFilterService;
import com.hip.damoa.domain.admin.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 필터 관리자 컨트롤러
 * 관리자만 접근 가능한 필터 카테고리 및 옵션 관리 API
 */
@Slf4j
@Tag(name = "1911. Admin Filter", description = "관리자 필터 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/filters")
@PreAuthorize("hasRole('ADMIN')")  // 관리자만 접근 가능
public class AdminFilterController {

    private final AdminFilterService adminFilterService;

    // ==================== 필터 카테고리 관리 ====================

    @Operation(summary = "필터 카테고리 목록 조회",
            description = """
            필터 카테고리 목록을 페이징하여 조회합니다.

            **필터 카테고리란?**
            - 필터의 대분류를 의미합니다 (예: 지역, 스타일, 평수, 색상 등)
            - 각 카테고리는 여러 필터 옵션을 포함합니다

            **조회 옵션**
            - entityType: COMPANY(업체), BOARD(게시판) 중 선택하여 해당 타입의 카테고리만 조회
            - keyword: 카테고리 코드, 이름, 설명에서 검색
            - 정렬: 기본적으로 displayOrder 순서로 정렬됨
            """)
    @GetMapping("/categories")
    public ApiResponse<Page<FilterCategoryResponse>> getFilterCategories(
            @Parameter(description = "엔티티 타입 - COMPANY(업체용 필터), BOARD(게시판용 필터)") @RequestParam(required = false) String entityType,
            @Parameter(description = "검색 키워드 - 카테고리 코드, 이름, 설명에서 검색") @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("필터 카테고리 목록 조회: entityType={}, keyword={}", entityType, keyword);
        Page<FilterCategoryResponse> categories = adminFilterService.getFilterCategories(entityType, keyword, pageable);
        return ApiResponse.success(categories);
    }

    @Operation(summary = "필터 카테고리 상세 조회",
            description = """
            특정 필터 카테고리의 상세 정보를 조회합니다.

            **조회 정보**
            - 카테고리 기본 정보 (이름, 코드, 설명, 타입 등)
            - 카테고리 설정 (계층구조 지원 여부, 최대 깊이, 필수 여부 등)
            - 하위 필터 옵션 목록 (카테고리에 속한 모든 옵션)
            """)
    @GetMapping("/categories/{categoryId}")
    public ApiResponse<FilterCategoryDetailResponse> getFilterCategory(
            @Parameter(description = "조회할 카테고리 ID") @PathVariable Long categoryId) {
        log.info("필터 카테고리 상세 조회: categoryId={}", categoryId);
        FilterCategoryDetailResponse category = adminFilterService.getFilterCategory(categoryId);
        return ApiResponse.success(category);
    }

    @Operation(summary = "필터 카테고리 생성",
            description = """
            새로운 필터 카테고리를 생성합니다.

            **필수 정보**
            - code: 시스템에서 사용할 고유 코드 (소문자, 숫자, 언더스코어만 가능)
            - name: 사용자에게 표시될 이름
            - entityType: COMPANY(업체) 또는 BOARD(게시판)
            - filterType: SINGLE_SELECT(단일선택), MULTI_SELECT(다중선택), HIERARCHICAL(계층구조)

            **선택 정보**
            - supportsHierarchy: 계층구조 지원 여부 (기본값: false)
            - maxDepth: 최대 깊이 (계층구조인 경우, 기본값: 1)
            - displayOrder: 표시 순서 (기본값: 0)
            - isRequired: 필수 필터 여부 (기본값: false)
            """)
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FilterCategoryResponse> createFilterCategory(
            @Valid @RequestBody FilterCategoryCreateRequest request) {

        log.info("필터 카테고리 생성: code={}, name={}, entityType={}",
                request.getCode(), request.getName(), request.getEntityType());
        FilterCategoryResponse category = adminFilterService.createFilterCategory(request);
        return ApiResponse.success(category);
    }

    @Operation(summary = "필터 카테고리 수정",
            description = """
            필터 카테고리 정보를 수정합니다.

            **수정 가능 항목**
            - name: 표시 이름
            - description: 설명
            - filterType: 필터 타입
            - supportsHierarchy: 계층구조 지원 여부
            - maxDepth: 최대 깊이
            - displayOrder: 표시 순서
            - icon: 아이콘
            - isActive: 활성 상태
            - isRequired: 필수 여부

            **주의사항**
            - code와 entityType은 수정할 수 없습니다
            - 이미 옵션이 있는 카테고리의 일부 설정은 변경이 제한될 수 있습니다
            """)
    @PutMapping("/categories/{categoryId}")
    public ApiResponse<FilterCategoryResponse> updateFilterCategory(
            @Parameter(description = "수정할 카테고리 ID") @PathVariable Long categoryId,
            @Valid @RequestBody FilterCategoryUpdateRequest request) {

        log.info("필터 카테고리 수정: categoryId={}", categoryId);
        FilterCategoryResponse category = adminFilterService.updateFilterCategory(categoryId, request);
        return ApiResponse.success(category);
    }

    @Operation(summary = "필터 카테고리 삭제",
            description = """
            필터 카테고리를 삭제합니다.

            **삭제 방식**
            - Soft Delete 방식으로 데이터는 보존되며 is_deleted 플래그만 변경됩니다
            - 삭제된 카테고리는 복구 가능합니다

            **제약사항**
            - 활성화된 필터 옵션이 있는 카테고리는 삭제할 수 없습니다
            - 사용 중인 카테고리는 먼저 비활성화 후 삭제해야 합니다
            """)
    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteFilterCategory(
            @Parameter(description = "삭제할 카테고리 ID") @PathVariable Long categoryId) {
        log.info("필터 카테고리 삭제: categoryId={}", categoryId);
        adminFilterService.deleteFilterCategory(categoryId);
        return ApiResponse.success();
    }

    @Operation(summary = "필터 카테고리 활성화/비활성화",
            description = """
            필터 카테고리의 활성 상태를 변경합니다.

            **활성화 (isActive=true)**
            - 사용자에게 노출되며 필터링에 사용 가능합니다

            **비활성화 (isActive=false)**
            - 사용자에게 노출되지 않으며 필터링에 사용되지 않습니다
            - 기존 데이터는 유지되며 언제든 다시 활성화 가능합니다
            """)
    @PatchMapping("/categories/{categoryId}/active")
    public ApiResponse<FilterCategoryResponse> toggleFilterCategoryActive(
            @Parameter(description = "상태 변경할 카테고리 ID") @PathVariable Long categoryId,
            @Parameter(description = "활성화 여부 (true: 활성화, false: 비활성화)") @RequestParam boolean isActive) {

        log.info("필터 카테고리 활성화 상태 변경: categoryId={}, isActive={}", categoryId, isActive);
        FilterCategoryResponse category = adminFilterService.toggleFilterCategoryActive(categoryId, isActive);
        return ApiResponse.success(category);
    }

    // ==================== 필터 옵션 관리 ====================

    @Operation(summary = "필터 옵션 목록 조회",
            description = """
            필터 옵션 목록을 페이징하여 조회합니다.

            **필터 옵션이란?**
            - 각 카테고리에 속한 실제 선택 가능한 값들입니다
            - 예: 지역 카테고리의 '서울', '경기', '부산' 등
            - 예: 스타일 카테고리의 '모던', '클래식', '미니멀' 등

            **조회 필터**
            - categoryId: 특정 카테고리의 옵션만 조회
            - keyword: 옵션 코드, 이름, 설명에서 검색
            - isActive: 활성/비활성 상태로 필터링
            - 정렬: 기본적으로 displayOrder 순서로 정렬됨
            """)
    @GetMapping("/options")
    public ApiResponse<Page<FilterOptionResponse>> getFilterOptions(
            @Parameter(description = "카테고리 ID - 특정 카테고리의 옵션만 조회") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "검색 키워드 - 옵션 코드, 이름, 설명에서 검색") @RequestParam(required = false) String keyword,
            @Parameter(description = "활성 상태 필터 (true: 활성, false: 비활성, null: 전체)") @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("필터 옵션 목록 조회: categoryId={}, keyword={}, isActive={}", categoryId, keyword, isActive);
        Page<FilterOptionResponse> options = adminFilterService.getFilterOptions(categoryId, keyword, isActive, pageable);
        return ApiResponse.success(options);
    }

    @Operation(summary = "필터 옵션 상세 조회",
            description = """
            특정 필터 옵션의 상세 정보를 조회합니다.

            **조회 정보**
            - 옵션 기본 정보 (이름, 코드, 설명 등)
            - 소속 카테고리 정보
            - 계층구조인 경우 부모/자식 옵션 정보
            - 사용 횟수 및 설정 정보
            """)
    @GetMapping("/options/{optionId}")
    public ApiResponse<FilterOptionDetailResponse> getFilterOption(
            @Parameter(description = "조회할 옵션 ID") @PathVariable Long optionId) {
        log.info("필터 옵션 상세 조회: optionId={}", optionId);
        FilterOptionDetailResponse option = adminFilterService.getFilterOption(optionId);
        return ApiResponse.success(option);
    }

    @Operation(summary = "필터 옵션 생성",
            description = """
            새로운 필터 옵션을 생성합니다.

            **필수 정보**
            - categoryId: 소속될 카테고리 ID
            - code: 시스템에서 사용할 고유 코드 (소문자, 숫자, 언더스코어, 하이픈만 가능)
            - name: 사용자에게 표시될 이름

            **선택 정보**
            - shortName: 짧은 이름 (공간이 부족한 곳에서 사용)
            - description: 옵션 설명
            - parentId: 부모 옵션 ID (계층구조인 경우)
            - displayOrder: 표시 순서 (기본값: 0)
            - icon: 아이콘
            - color: 색상 (HEX 형식: #FFFFFF)
            - isDefault: 기본 선택 여부 (기본값: false)

            **계층구조 옵션**
            - 카테고리가 계층구조를 지원하는 경우 부모-자식 관계 설정 가능
            - depth와 path는 자동으로 계산됩니다
            """)
    @PostMapping("/options")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FilterOptionResponse> createFilterOption(
            @Valid @RequestBody FilterOptionCreateRequest request) {

        log.info("필터 옵션 생성: categoryId={}, code={}, name={}",
                request.getCategoryId(), request.getCode(), request.getName());
        FilterOptionResponse option = adminFilterService.createFilterOption(request);
        return ApiResponse.success(option);
    }

    @Operation(summary = "필터 옵션 수정",
            description = """
            필터 옵션 정보를 수정합니다.

            **수정 가능 항목**
            - name: 표시 이름
            - shortName: 짧은 이름
            - description: 설명
            - displayOrder: 표시 순서
            - icon: 아이콘
            - color: 색상
            - isActive: 활성 상태
            - isDefault: 기본값 여부

            **주의사항**
            - code와 categoryId는 수정할 수 없습니다
            - 계층구조의 parent 관계는 수정할 수 없습니다
            """)
    @PutMapping("/options/{optionId}")
    public ApiResponse<FilterOptionResponse> updateFilterOption(
            @Parameter(description = "수정할 옵션 ID") @PathVariable Long optionId,
            @Valid @RequestBody FilterOptionUpdateRequest request) {

        log.info("필터 옵션 수정: optionId={}", optionId);
        FilterOptionResponse option = adminFilterService.updateFilterOption(optionId, request);
        return ApiResponse.success(option);
    }

    @Operation(summary = "필터 옵션 삭제",
            description = """
            필터 옵션을 삭제합니다.

            **삭제 방식**
            - Soft Delete 방식으로 데이터는 보존되며 is_deleted 플래그만 변경됩니다
            - 삭제된 옵션은 복구 가능합니다

            **제약사항**
            - 자식 옵션이 있는 부모 옵션은 삭제할 수 없습니다
            - 사용 중인 옵션 삭제 시 경고가 표시됩니다
            """)
    @DeleteMapping("/options/{optionId}")
    public ApiResponse<Void> deleteFilterOption(
            @Parameter(description = "삭제할 옵션 ID") @PathVariable Long optionId) {
        log.info("필터 옵션 삭제: optionId={}", optionId);
        adminFilterService.deleteFilterOption(optionId);
        return ApiResponse.success();
    }

    @Operation(summary = "필터 옵션 활성화/비활성화",
            description = """
            필터 옵션의 활성 상태를 변경합니다.

            **활성화 (isActive=true)**
            - 사용자에게 노출되며 선택 가능합니다
            - 필터링에 사용됩니다

            **비활성화 (isActive=false)**
            - 사용자에게 노출되지 않으며 선택 불가능합니다
            - 기존에 선택된 데이터는 유지됩니다
            - 언제든 다시 활성화 가능합니다
            """)
    @PatchMapping("/options/{optionId}/active")
    public ApiResponse<FilterOptionResponse> toggleFilterOptionActive(
            @Parameter(description = "상태 변경할 옵션 ID") @PathVariable Long optionId,
            @Parameter(description = "활성화 여부 (true: 활성화, false: 비활성화)") @RequestParam boolean isActive) {

        log.info("필터 옵션 활성화 상태 변경: optionId={}, isActive={}", optionId, isActive);
        FilterOptionResponse option = adminFilterService.toggleFilterOptionActive(optionId, isActive);
        return ApiResponse.success(option);
    }

    @Operation(summary = "필터 옵션 순서 변경",
            description = """
            필터 옵션들의 표시 순서를 일괄 변경합니다.

            **사용 방법**
            - 카테고리 내의 모든 옵션의 순서를 한 번에 재정렬할 때 사용
            - 드래그 앤 드롭 등의 UI에서 순서 변경 후 저장할 때 유용

            **요청 형식**
            - categoryId: 순서를 변경할 카테고리 ID
            - optionOrders: 각 옵션의 ID와 새로운 displayOrder 값의 배열

            **주의사항**
            - 동일 카테고리 내의 옵션만 순서 변경 가능
            - 누락된 옵션은 기존 순서 유지
            """)
    @PatchMapping("/options/reorder")
    public ApiResponse<List<FilterOptionResponse>> reorderFilterOptions(
            @Valid @RequestBody FilterOptionReorderRequest request) {

        log.info("필터 옵션 순서 변경: categoryId={}, optionCount={}",
                request.getCategoryId(), request.getOptionOrders().size());
        List<FilterOptionResponse> options = adminFilterService.reorderFilterOptions(request);
        return ApiResponse.success(options);
    }

    // ==================== 필터 옵션 마이그레이션 ====================

    @Operation(summary = "마이그레이션용 전체 필터 옵션 목록 조회",
            description = """
            필터 옵션 마이그레이션을 위한 전체 옵션 목록을 조회합니다.

            **용도**
            - 마이그레이션 드롭다운 메뉴용으로 모든 필터 옵션을 조회합니다
            - 카테고리별로 정렬되어 반환됩니다
            """)
    @GetMapping("/options/all")
    public ApiResponse<List<FilterOptionResponse>> getAllFilterOptionsForMigration() {
        log.info("마이그레이션용 전체 필터 옵션 목록 조회");
        List<FilterOptionResponse> options = adminFilterService.getAllFilterOptionsForMigration();
        return ApiResponse.success(options);
    }

    @Operation(summary = "필터 옵션 마이그레이션 미리보기",
            description = """
            필터 옵션 마이그레이션 실행 전에 영향받는 데이터를 미리 확인합니다.

            **미리보기 정보**
            - 소스/타겟 옵션의 상세 정보
            - 영향받는 업체/게시글 수
            - 중복으로 인해 건너뛸 업체/게시글 수
            - 영향받는 업체 샘플 목록 (최대 10개)

            **마이그레이션이란?**
            - 기존 필터 옵션(소스)을 사용하는 모든 데이터를 새 필터 옵션(타겟)으로 변경
            - 소스 옵션과 타겟 옵션을 모두 가진 데이터는 중복으로 처리되어 건너뜀
            """)
    @PostMapping("/options/migrate/preview")
    public ApiResponse<FilterMigratePreviewResponse> previewFilterMigration(
            @Valid @RequestBody FilterMigrateRequest request) {
        log.info("필터 옵션 마이그레이션 미리보기: sourceOptionId={}, targetOptionId={}",
                request.getSourceOptionId(), request.getTargetOptionId());
        FilterMigratePreviewResponse preview = adminFilterService.previewFilterMigration(request);
        return ApiResponse.success(preview);
    }

    @Operation(summary = "필터 옵션 마이그레이션 실행",
            description = """
            필터 옵션 마이그레이션을 실행합니다.

            **마이그레이션 과정**
            1. 소스 옵션을 사용하는 모든 업체/게시글을 타겟 옵션으로 변경
            2. 이미 타겟 옵션을 가진 경우 중복 처리 (소스 레코드 삭제)
            3. 선택적으로 소스 옵션 비활성화 또는 삭제

            **옵션 설정**
            - deactivateSource: 마이그레이션 후 소스 옵션 비활성화 (기본값: false)
            - deleteSource: 마이그레이션 후 소스 옵션 삭제 (기본값: false)

            **주의사항**
            - 이 작업은 되돌릴 수 없습니다
            - 먼저 미리보기 API로 영향 범위를 확인하세요
            """)
    @PostMapping("/options/migrate")
    public ApiResponse<FilterMigrateResponse> migrateFilterOption(
            @Valid @RequestBody FilterMigrateRequest request) {
        log.info("필터 옵션 마이그레이션 실행: sourceOptionId={}, targetOptionId={}, deactivateSource={}, deleteSource={}",
                request.getSourceOptionId(), request.getTargetOptionId(),
                request.getDeactivateSource(), request.getDeleteSource());
        FilterMigrateResponse result = adminFilterService.migrateFilterOption(request);
        return ApiResponse.success(result);
    }
}