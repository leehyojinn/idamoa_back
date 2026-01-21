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
@Tag(name = "1014. Filter", description = "필터 카테고리 및 옵션 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/filters")
public class FilterController {

    private final FilterService filterService;

    @Operation(
            summary = "활성 필터 카테고리 조회",
            description = "활성화된 필터 카테고리와 옵션을 조회합니다.\n\n" +
                    "**entityType 파라미터:**\n" +
                    "- 생략: 모든 활성 필터 조회\n" +
                    "- COMPANY: 업체용 필터\n" +
                    "- GALLERY: 갤러리용 필터\n" +
                    "- DOCUMENT: 자료실용 필터"
    )
    @GetMapping
    public ApiResponse<List<FilterCategoryResponse>> getActiveCategories(
            @Parameter(description = "엔티티 타입 (예: COMPANY, BOARD, GALLERY, DOCUMENT)")
            @RequestParam(required = false) String entityType) {
        List<FilterCategoryResponse> categories = filterService.getActiveCategories(entityType);
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

    @Operation(
            summary = "필터 계층 트리 조회",
            description = "특정 카테고리의 필터 옵션을 계층 트리 구조로 조회합니다. " +
                    "parent_id 기반으로 루트 옵션부터 모든 하위 옵션을 재귀적으로 반환합니다.\n\n" +
                    "**응답 구조 예시 (business_type 카테고리)**:\n" +
                    "```json\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"id\": 1,\n" +
                    "    \"code\": \"hospital\",\n" +
                    "    \"name\": \"병원 인테리어\",\n" +
                    "    \"depth\": 0,\n" +
                    "    \"children\": [\n" +
                    "      {\n" +
                    "        \"id\": 2,\n" +
                    "        \"code\": \"hospital_clinic\",\n" +
                    "        \"name\": \"의원급\",\n" +
                    "        \"depth\": 1,\n" +
                    "        \"children\": [\n" +
                    "          { \"id\": 3, \"code\": \"clinic_dermatology\", \"name\": \"피부과\", \"depth\": 2, \"children\": [] }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  { \"id\": 10, \"code\": \"cafe\", \"name\": \"카페 인테리어\", \"depth\": 0, \"children\": [...] }\n" +
                    "]\n" +
                    "```"
    )
    @GetMapping("/{categoryCode}/tree")
    public ApiResponse<List<FilterOptionResponse>> getFilterTree(
            @Parameter(description = "카테고리 코드 (예: business_type, region)", required = true)
            @PathVariable String categoryCode) {

        List<FilterOptionResponse> tree = filterService.getFilterTree(categoryCode);
        return ApiResponse.success(tree);
    }

    @Operation(
            summary = "필터 옵션의 자식 옵션 조회",
            description = "특정 필터 옵션의 직계 자식 옵션만 조회합니다. " +
                    "Lazy loading 방식으로 필요할 때만 자식을 조회할 수 있습니다."
    )
    @GetMapping("/options/{optionId}/children")
    public ApiResponse<List<FilterOptionResponse>> getChildOptions(
            @Parameter(description = "부모 필터 옵션 ID", required = true)
            @PathVariable Long optionId) {

        List<FilterOptionResponse> children = filterService.getChildOptions(optionId);
        return ApiResponse.success(children);
    }
}
