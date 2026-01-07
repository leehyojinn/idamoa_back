package com.hip.damoa.domain.community.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.community.service.CommunityCategoryService;
import com.hip.damoa.domain.community.web.dto.CommunityCategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Community Category", description = "커뮤니티 카테고리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/categories")
public class CommunityCategoryController {

    private final CommunityCategoryService categoryService;

    @Operation(summary = "카테고리 목록 조회", description = "활성화된 커뮤니티 카테고리 목록을 조회합니다")
    @GetMapping
    public ApiResponse<List<CommunityCategoryResponse>> getCategories() {
        return ApiResponse.success(categoryService.getActiveCategories());
    }

    @Operation(summary = "카테고리 상세 조회", description = "특정 카테고리 정보를 조회합니다")
    @GetMapping("/{uuid}")
    public ApiResponse<CommunityCategoryResponse> getCategory(@PathVariable UUID uuid) {
        return ApiResponse.success(categoryService.getCategory(uuid));
    }
}
