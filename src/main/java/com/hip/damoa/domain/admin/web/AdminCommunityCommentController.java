package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.community.service.CommunityCommentService;
import com.hip.damoa.domain.community.web.dto.CommunityCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 커뮤니티 댓글 관리 Controller (관리자용)
 */
@Tag(name = "9922. Admin - Community Comment", description = "커뮤니티 댓글 관리 API (관리자 전용)")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/community/comments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityCommentController {

    private final CommunityCommentService commentService;

    @Operation(summary = "댓글 목록 조회", description = "전체 커뮤니티 댓글 목록을 조회합니다")
    @GetMapping
    public ApiResponse<Page<CommunityCommentResponse>> getComments(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("[관리자] 커뮤니티 댓글 목록 조회: keyword={}", keyword);
        return ApiResponse.success(commentService.getCommentsForAdmin(keyword, pageable));
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다 (Soft Delete)")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deleteComment(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 댓글 삭제: uuid={}", uuid);
        commentService.deleteCommentByAdmin(uuid);
        return ApiResponse.success();
    }

    @Operation(summary = "댓글 비공개", description = "댓글을 비공개 처리합니다")
    @PostMapping("/{uuid}/hide")
    public ApiResponse<Void> hideComment(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 댓글 비공개: uuid={}", uuid);
        commentService.hideComment(uuid);
        return ApiResponse.success();
    }

    @Operation(summary = "댓글 공개", description = "비공개된 댓글을 다시 공개합니다")
    @PostMapping("/{uuid}/show")
    public ApiResponse<Void> showComment(@PathVariable UUID uuid) {
        log.info("[관리자] 커뮤니티 댓글 공개: uuid={}", uuid);
        commentService.showComment(uuid);
        return ApiResponse.success();
    }
}
