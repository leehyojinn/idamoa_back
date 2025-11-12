package com.hip.damoa.domain.filter.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.filter.service.FilterService;
import com.hip.damoa.domain.filter.web.dto.FilterCategoryResponse;
import com.hip.damoa.domain.filter.web.dto.FilterOptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 필터 조회 API (공개)
 */
@Tag(name = "13. Filter", description = "필터 카테고리 및 옵션 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/filters")
public class FilterController {

    private final FilterService filterService;

    @Operation(
            summary = "모든 활성 필터 카테고리 조회",
            description = "모든 활성화된 필터 카테고리와 옵션을 조회합니다. " +
                    "업체 등록 시 사용 가능한 모든 필터를 확인할 수 있습니다."
    )
    @GetMapping
    public ApiResponse<List<FilterCategoryResponse>> getAllActiveCategories() {
        List<FilterCategoryResponse> categories = filterService.getAllActiveCategories();
        return ApiResponse.success(categories);
    }

    @Operation(
            summary = "특정 필터 카테고리 조회",
            description = "카테고리 코드로 특정 필터 카테고리와 옵션을 조회합니다. " +
                    "예: region, department, specialty, project_size"
    )
    @GetMapping("/{categoryCode}")
    public ApiResponse<FilterCategoryResponse> getCategoryByCode(
            @Parameter(description = "카테고리 코드 (예: region, department, specialty)", required = true)
            @PathVariable String categoryCode) {

        FilterCategoryResponse category = filterService.getCategoryByCode(categoryCode);
        return ApiResponse.success(category);
    }

    @Operation(
            summary = "필터 카테고리의 옵션만 조회",
            description = "특정 카테고리의 옵션 목록만 조회합니다. " +
                    "카테고리 메타데이터 없이 옵션만 필요한 경우 사용합니다."
    )
    @GetMapping("/{categoryCode}/options")
    public ApiResponse<List<FilterOptionResponse>> getOptionsByCategory(
            @Parameter(description = "카테고리 코드", required = true)
            @PathVariable String categoryCode) {

        List<FilterOptionResponse> options = filterService.getOptionsByCategory(categoryCode);
        return ApiResponse.success(options);
    }

    @Operation(
            summary = "필수 필터 카테고리 조회",
            description = "업체 등록 시 반드시 선택해야 하는 필수 필터 카테고리를 조회합니다."
    )
    @GetMapping("/required")
    public ApiResponse<List<FilterCategoryResponse>> getRequiredCategories() {
        List<FilterCategoryResponse> categories = filterService.getRequiredCategories();
        return ApiResponse.success(categories);
    }

    @Operation(
            summary = "필터 타입별 카테고리 조회",
            description = "필터 타입(SINGLE_SELECT, MULTI_SELECT 등)별로 카테고리를 조회합니다."
    )
    @GetMapping("/by-type/{filterType}")
    public ApiResponse<List<FilterCategoryResponse>> getCategoriesByFilterType(
            @Parameter(description = "필터 타입 (예: SINGLE_SELECT, MULTI_SELECT)", required = true)
            @PathVariable String filterType) {

        List<FilterCategoryResponse> categories = filterService.getCategoriesByFilterType(filterType);
        return ApiResponse.success(categories);
    }
}
