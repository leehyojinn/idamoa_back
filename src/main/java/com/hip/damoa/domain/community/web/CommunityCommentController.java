package com.hip.damoa.domain.community.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.community.service.CommunityCommentService;
import com.hip.damoa.domain.community.web.dto.CommunityCommentCreateRequest;
import com.hip.damoa.domain.community.web.dto.CommunityCommentResponse;
import com.hip.damoa.domain.community.web.dto.CommunityCommentUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "1032. Community Comment", description = "커뮤니티 댓글 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityCommentController {

    private final CommunityCommentService commentService;

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 트리 구조로 조회합니다")
    @GetMapping("/posts/{postUuid}/comments")
    public ApiResponse<List<CommunityCommentResponse>> getComments(
            @PathVariable UUID postUuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        return ApiResponse.success(commentService.getComments(postUuid, userEmail));
    }

    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/posts/{postUuid}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityCommentResponse> createComment(
            @PathVariable UUID postUuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommunityCommentCreateRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        log.info("커뮤니티 댓글 작성: postUuid={}, userEmail={}", postUuid, userDetails.getUsername());

        return ApiResponse.success(commentService.createComment(postUuid, userDetails.getUsername(), request, ipAddress));
    }

    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다 (작성자만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/comments/{uuid}")
    public ApiResponse<CommunityCommentResponse> updateComment(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommunityCommentUpdateRequest request) {

        log.info("커뮤니티 댓글 수정: uuid={}, userEmail={}", uuid, userDetails.getUsername());
        return ApiResponse.success(commentService.updateComment(uuid, userDetails.getUsername(), request));
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다 (작성자만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/comments/{uuid}")
    public ApiResponse<Void> deleteComment(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("커뮤니티 댓글 삭제: uuid={}, userEmail={}", uuid, userDetails.getUsername());
        commentService.deleteComment(uuid, userDetails.getUsername());
        return ApiResponse.success();
    }

    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 추가/취소합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/comments/{uuid}/like")
    public ApiResponse<Void> likeComment(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        commentService.likeComment(uuid, userDetails.getUsername());
        return ApiResponse.success();
    }

    @Operation(summary = "댓글 싫어요", description = "댓글에 싫어요를 추가/취소합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/comments/{uuid}/dislike")
    public ApiResponse<Void> dislikeComment(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        commentService.dislikeComment(uuid, userDetails.getUsername());
        return ApiResponse.success();
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
