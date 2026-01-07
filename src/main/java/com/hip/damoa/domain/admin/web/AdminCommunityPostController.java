package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.community.service.CommunityPostService;
import com.hip.damoa.domain.community.web.dto.AdminPostDetailResponse;
import com.hip.damoa.domain.community.web.dto.AdminPostUpdateRequest;
import com.hip.damoa.domain.community.web.dto.CommunityPostListResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 커뮤니티 게시글 관리 Controller (관리자용)
 */
@Tag(name = "1921. Admin - Community Post", description = "커뮤니티 게시글 관리 API (관리자 전용)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/community/posts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityPostController {

    private final CommunityPostService postService;

    @Operation(summary = "게시글 목록 조회", description = "전체 커뮤니티 게시글 목록을 조회합니다 (삭제된 게시글 제외)")
    @GetMapping
    public ApiResponse<Page<CommunityPostListResponse>> getPosts(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("[관리자] 커뮤니티 게시글 목록 조회: categorySlug={}, keyword={}", categorySlug, keyword);
        return ApiResponse.success(postService.getPostsForAdmin(categorySlug, keyword, pageable));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회합니다 (실제 작성자 정보 포함)")
    @GetMapping("/{uuid}")
    public ApiResponse<AdminPostDetailResponse> getPost(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 게시글 상세 조회: uuid={}", uuid);
        return ApiResponse.success(postService.getPostForAdmin(uuid));
    }

    @Operation(summary = "게시글 수정", description = "게시글 정보를 수정합니다 (상단고정, 공지, 발행 여부 등)")
    @PutMapping("/{uuid}")
    public ApiResponse<AdminPostDetailResponse> updatePost(
            @PathVariable UUID uuid,
            @Valid @RequestBody AdminPostUpdateRequest request) {

        log.info("[관리자] 커뮤니티 게시글 수정: uuid={}", uuid);
        return ApiResponse.success(postService.updatePostByAdmin(uuid, request));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deletePost(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 게시글 삭제: uuid={}", uuid);
        postService.deletePostByAdmin(uuid);
        return ApiResponse.success();
    }

    @Operation(summary = "게시글 비공개", description = "게시글을 비공개 처리합니다")
    @PostMapping("/{uuid}/hide")
    public ApiResponse<Void> hidePost(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 게시글 비공개: uuid={}", uuid);
        postService.hidePost(uuid);
        return ApiResponse.success();
    }

    @Operation(summary = "게시글 공개", description = "비공개된 게시글을 다시 공개합니다")
    @PostMapping("/{uuid}/show")
    public ApiResponse<Void> showPost(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 게시글 공개: uuid={}", uuid);
        postService.showPost(uuid);
        return ApiResponse.success();
    }
}
