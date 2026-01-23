package com.hip.damoa.domain.community.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.community.service.CommunityPostService;
import com.hip.damoa.domain.community.web.dto.CommunityPostCreateRequest;
import com.hip.damoa.domain.community.web.dto.CommunityPostDetailResponse;
import com.hip.damoa.domain.community.web.dto.CommunityPostListResponse;
import com.hip.damoa.domain.community.web.dto.CommunityPostUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.UUID;

@Tag(name = "1032. Community Post", description = "커뮤니티 게시글 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community/posts")
public class CommunityPostController {

    private final CommunityPostService postService;

    @Operation(summary = "게시글 목록 조회", description = "커뮤니티 게시글 목록을 조회합니다")
    @GetMapping
    public ApiResponse<Page<CommunityPostListResponse>> getPosts(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        return ApiResponse.success(postService.getPosts(categorySlug, keyword, userEmail, pageable));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 내용을 조회합니다 (조회수 증가)")
    @GetMapping("/{uuid}")
    public ApiResponse<CommunityPostDetailResponse> getPost(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails != null ? userDetails.getUsername() : null;
        return ApiResponse.success(postService.getPost(uuid, userEmail));
    }

    @Operation(summary = "게시글 작성", description = "새로운 게시글을 작성합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityPostDetailResponse> createPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommunityPostCreateRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        log.info("커뮤니티 게시글 작성: userEmail={}", userDetails.getUsername());

        return ApiResponse.success(postService.createPost(userDetails.getUsername(), request, ipAddress));
    }

    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다 (작성자만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{uuid}")
    public ApiResponse<CommunityPostDetailResponse> updatePost(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommunityPostUpdateRequest request) {

        log.info("커뮤니티 게시글 수정: uuid={}, userEmail={}", uuid, userDetails.getUsername());
        return ApiResponse.success(postService.updatePost(uuid, userDetails.getUsername(), request));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다 (작성자만 가능)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deletePost(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("커뮤니티 게시글 삭제: uuid={}, userEmail={}", uuid, userDetails.getUsername());
        postService.deletePost(uuid, userDetails.getUsername());
        return ApiResponse.success();
    }

    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 추가/취소합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{uuid}/like")
    public ApiResponse<Void> likePost(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        postService.likePost(uuid, userDetails.getUsername());
        return ApiResponse.success();
    }

    @Operation(summary = "게시글 싫어요", description = "게시글에 싫어요를 추가/취소합니다")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{uuid}/dislike")
    public ApiResponse<Void> dislikePost(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        postService.dislikePost(uuid, userDetails.getUsername());
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
