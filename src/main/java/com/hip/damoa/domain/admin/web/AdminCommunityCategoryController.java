package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.community.service.CommunityCategoryService;
import com.hip.damoa.domain.community.web.dto.AdminCategoryCreateRequest;
import com.hip.damoa.domain.community.web.dto.AdminCategoryUpdateRequest;
import com.hip.damoa.domain.community.web.dto.CommunityCategoryResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 커뮤니티 카테고리 관리 Controller (관리자용)
 */
@Tag(name = "1920. Admin - Community Category", description = "커뮤니티 카테고리 관리 API (관리자 전용)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/community/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityCategoryController {

    private final CommunityCategoryService categoryService;

    @Operation(summary = "카테고리 목록 조회", description = "전체 커뮤니티 카테고리 목록을 조회합니다 (비활성 포함)")
    @GetMapping
    public ApiResponse<Page<CommunityCategoryResponse>> getCategories(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.info("[관리자] 커뮤니티 카테고리 목록 조회: keyword={}", keyword);
        return ApiResponse.success(categoryService.getAllCategories(keyword, pageable));
    }

    @Operation(summary = "카테고리 상세 조회", description = "특정 카테고리 정보를 조회합니다")
    @GetMapping("/{uuid}")
    public ApiResponse<CommunityCategoryResponse> getCategory(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 카테고리 상세 조회: uuid={}", uuid);
        return ApiResponse.success(categoryService.getCategory(uuid));
    }

    @Operation(summary = "카테고리 생성", description = "새로운 커뮤니티 카테고리를 생성합니다")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityCategoryResponse> createCategory(
            @Valid @RequestBody AdminCategoryCreateRequest request) {

        log.info("[관리자] 커뮤니티 카테고리 생성: slug={}", request.getSlug());
        return ApiResponse.success(categoryService.createCategory(request));
    }

    @Operation(summary = "카테고리 수정", description = "커뮤니티 카테고리를 수정합니다")
    @PutMapping("/{uuid}")
    public ApiResponse<CommunityCategoryResponse> updateCategory(
            @PathVariable UUID uuid,
            @Valid @RequestBody AdminCategoryUpdateRequest request) {

        log.info("[관리자] 커뮤니티 카테고리 수정: uuid={}", uuid);
        return ApiResponse.success(categoryService.updateCategory(uuid, request));
    }

    @Operation(summary = "카테고리 삭제", description = "커뮤니티 카테고리를 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteCategory(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 카테고리 삭제: uuid={}", uuid);
        categoryService.deleteCategory(uuid);
        return ApiResponse.success();
    }
}
