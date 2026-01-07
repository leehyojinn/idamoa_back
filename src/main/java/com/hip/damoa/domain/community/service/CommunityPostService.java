package com.hip.damoa.domain.community.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.community.model.*;
import com.hip.damoa.domain.community.repository.*;
import com.hip.damoa.domain.community.web.dto.*;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import com.hip.damoa.domain.user.repository.UserProfileRepository;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityPostService {

    private final CommunityPostRepository postRepository;
    private final CommunityCategoryRepository categoryRepository;
    private final CommunityPostAttachmentRepository attachmentRepository;
    private final CommunityLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final FileRepository fileRepository;

    /**
     * 게시글 목록 조회 (공개)
     */
    @Transactional(readOnly = true)
    public Page<CommunityPostListResponse> getPosts(String categorySlug, String keyword,
                                                     String userEmail, Pageable pageable) {
        Page<CommunityPost> posts;

        if (categorySlug != null && keyword != null) {
            posts = postRepository.searchByCategoryAndKeyword(categorySlug, keyword, pageable);
        } else if (categorySlug != null) {
            posts = postRepository.findByCategorySlug(categorySlug, pageable);
        } else if (keyword != null) {
            posts = postRepository.searchByKeyword(keyword, pageable);
        } else {
            posts = postRepository.findAllPublished(pageable);
        }

        return convertToListResponse(posts, userEmail);
    }

    /**
     * 게시글 상세 조회 (공개)
     */
    @Transactional
    public CommunityPostDetailResponse getPost(UUID uuid, String userEmail) {
        CommunityPost post = postRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        // 조회수 증가
        post.incrementViewCount();
        postRepository.save(post);

        return convertToDetailResponse(post, userEmail);
    }

    /**
     * 게시글 작성 (로그인 필수)
     */
    @Transactional
    public CommunityPostDetailResponse createPost(String userEmail, CommunityPostCreateRequest request, String ipAddress) {
        log.info("커뮤니티 게시글 작성: userEmail={}, categoryUuid={}", userEmail, request.getCategoryUuid());

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        CommunityCategory category = categoryRepository.findByUuidAndIsDeletedFalse(request.getCategoryUuid())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_CATEGORY_NOT_FOUND));

        // 익명 허용 여부 확인
        if (Boolean.TRUE.equals(request.getIsAnonymous()) && !category.getAllowAnonymous()) {
            throw new BusinessException(ErrorCode.COMMUNITY_ANONYMOUS_NOT_ALLOWED);
        }

        // 첨부파일 허용 여부 확인
        if (request.getFileUuids() != null && !request.getFileUuids().isEmpty()) {
            if (!category.getAllowAttachments()) {
                throw new BusinessException(ErrorCode.COMMUNITY_ATTACHMENTS_NOT_ALLOWED);
            }
            if (request.getFileUuids().size() > category.getMaxAttachments()) {
                throw new BusinessException(ErrorCode.COMMUNITY_MAX_ATTACHMENTS_EXCEEDED);
            }
        }

        ContentType contentType = ContentType.TEXT;
        if (request.getContentType() != null) {
            try {
                contentType = ContentType.valueOf(request.getContentType().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        CommunityPost post = CommunityPost.builder()
                .category(category)
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .contentType(contentType)
                .isAnonymous(request.getIsAnonymous())
                .ipAddress(ipAddress)
                .build();

        post = postRepository.save(post);

        // 첨부파일 처리
        processAttachments(post, request.getFileUuids());

        log.info("커뮤니티 게시글 작성 완료: postUuid={}", post.getUuid());
        return convertToDetailResponse(post, userEmail);
    }

    /**
     * 게시글 수정 (작성자만)
     */
    @Transactional
    public CommunityPostDetailResponse updatePost(UUID uuid, String userEmail, CommunityPostUpdateRequest request) {
        log.info("커뮤니티 게시글 수정: uuid={}, userEmail={}", uuid, userEmail);

        CommunityPost post = postRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        // 작성자 확인
        if (!post.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.COMMUNITY_POST_ACCESS_DENIED);
        }

        ContentType contentType = null;
        if (request.getContentType() != null) {
            try {
                contentType = ContentType.valueOf(request.getContentType().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        post.update(request.getTitle(), request.getContent(), contentType, request.getIsAnonymous());
        post = postRepository.save(post);

        // 첨부파일 교체
        if (request.getFileUuids() != null) {
            attachmentRepository.deleteByPostId(post.getId());
            processAttachments(post, request.getFileUuids());
        }

        log.info("커뮤니티 게시글 수정 완료: uuid={}", uuid);
        return convertToDetailResponse(post, userEmail);
    }

    /**
     * 게시글 삭제 (작성자만)
     */
    @Transactional
    public void deletePost(UUID uuid, String userEmail) {
        log.info("커뮤니티 게시글 삭제: uuid={}, userEmail={}", uuid, userEmail);

        CommunityPost post = postRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        // 작성자 확인
        if (!post.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.COMMUNITY_POST_ACCESS_DENIED);
        }

        post.softDelete();
        postRepository.save(post);

        log.info("커뮤니티 게시글 삭제 완료: uuid={}", uuid);
    }

    /**
     * 게시글 좋아요
     */
    @Transactional
    public void likePost(UUID uuid, String userEmail) {
        CommunityPost post = postRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<CommunityLike> existingLike = likeRepository.findByPostIdAndUserId(post.getId(), user.getId());

        if (existingLike.isPresent()) {
            CommunityLike like = existingLike.get();
            if (like.getLikeType() == LikeType.LIKE) {
                // 좋아요 취소
                likeRepository.delete(like);
                post.decrementLikeCount();
            } else {
                // 싫어요 → 좋아요로 변경
                like.changeLikeType(LikeType.LIKE);
                likeRepository.save(like);
                post.decrementDislikeCount();
                post.incrementLikeCount();
            }
        } else {
            // 새로운 좋아요
            CommunityLike like = CommunityLike.forPost(post, user, LikeType.LIKE);
            likeRepository.save(like);
            post.incrementLikeCount();
        }

        postRepository.save(post);
    }

    /**
     * 게시글 싫어요
     */
    @Transactional
    public void dislikePost(UUID uuid, String userEmail) {
        CommunityPost post = postRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<CommunityLike> existingLike = likeRepository.findByPostIdAndUserId(post.getId(), user.getId());

        if (existingLike.isPresent()) {
            CommunityLike like = existingLike.get();
            if (like.getLikeType() == LikeType.DISLIKE) {
                // 싫어요 취소
                likeRepository.delete(like);
                post.decrementDislikeCount();
            } else {
                // 좋아요 → 싫어요로 변경
                like.changeLikeType(LikeType.DISLIKE);
                likeRepository.save(like);
                post.decrementLikeCount();
                post.incrementDislikeCount();
            }
        } else {
            // 새로운 싫어요
            CommunityLike like = CommunityLike.forPost(post, user, LikeType.DISLIKE);
            likeRepository.save(like);
            post.incrementDislikeCount();
        }

        postRepository.save(post);
    }

    // ========== 관리자 전용 ==========

    /**
     * 게시글 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public Page<CommunityPostListResponse> getPostsForAdmin(String categorySlug, String keyword, Pageable pageable) {
        Page<CommunityPost> posts = postRepository.searchForAdmin(categorySlug, keyword, pageable);
        return convertToListResponse(posts, null);
    }

    /**
     * 게시글 상세 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public AdminPostDetailResponse getPostForAdmin(UUID uuid) {
        CommunityPost post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        String authorName = getAuthorName(post.getUser());
        List<CommunityFileResponse> attachments = getAttachments(post.getId());

        return AdminPostDetailResponse.from(post, authorName, attachments);
    }

    /**
     * 게시글 수정 (관리자)
     */
    @Transactional
    public AdminPostDetailResponse updatePostByAdmin(UUID uuid, AdminPostUpdateRequest request) {
        log.info("[관리자] 커뮤니티 게시글 수정: uuid={}", uuid);

        CommunityPost post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        ContentType contentType = null;
        if (request.getContentType() != null) {
            try {
                contentType = ContentType.valueOf(request.getContentType().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        post.updateByAdmin(
                request.getTitle(),
                request.getContent(),
                contentType,
                request.getIsPinned(),
                request.getIsNotice(),
                request.getIsPublished()
        );

        post = postRepository.save(post);

        log.info("[관리자] 커뮤니티 게시글 수정 완료: uuid={}", uuid);

        String authorName = getAuthorName(post.getUser());
        List<CommunityFileResponse> attachments = getAttachments(post.getId());

        return AdminPostDetailResponse.from(post, authorName, attachments);
    }

    /**
     * 게시글 삭제 (관리자)
     */
    @Transactional
    public void deletePostByAdmin(UUID uuid) {
        log.info("[관리자] 커뮤니티 게시글 삭제: uuid={}", uuid);

        CommunityPost post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        post.softDelete();
        postRepository.save(post);

        log.info("[관리자] 커뮤니티 게시글 삭제 완료: uuid={}", uuid);
    }

    /**
     * 게시글 비공개 (관리자)
     */
    @Transactional
    public void hidePost(UUID uuid) {
        log.info("[관리자] 커뮤니티 게시글 비공개: uuid={}", uuid);

        CommunityPost post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        post.unpublish();
        postRepository.save(post);

        log.info("[관리자] 커뮤니티 게시글 비공개 완료: uuid={}", uuid);
    }

    /**
     * 게시글 공개 (관리자)
     */
    @Transactional
    public void showPost(UUID uuid) {
        log.info("[관리자] 커뮤니티 게시글 공개: uuid={}", uuid);

        CommunityPost post = postRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        post.publish();
        postRepository.save(post);

        log.info("[관리자] 커뮤니티 게시글 공개 완료: uuid={}", uuid);
    }

    // ========== 헬퍼 메서드 ==========

    private void processAttachments(CommunityPost post, List<String> fileUuids) {
        if (fileUuids == null || fileUuids.isEmpty()) {
            return;
        }

        int order = 0;
        for (String fileUuidStr : fileUuids) {
            UUID fileUuid = UUID.fromString(fileUuidStr);
            File file = fileRepository.findByUuidAndIsDeletedFalse(fileUuid).orElse(null);
            if (file != null) {
                // entityType 업데이트
                file.updateEntityInfo("COMMUNITY_POST", post.getId());
                fileRepository.save(file);

                String attachmentType = determineAttachmentType(file.getMimeType());
                CommunityPostAttachment attachment = CommunityPostAttachment.builder()
                        .post(post)
                        .fileId(file.getId())
                        .attachmentType(attachmentType)
                        .displayOrder(order++)
                        .build();
                attachmentRepository.save(attachment);
            }
        }
    }

    private String determineAttachmentType(String mimeType) {
        if (mimeType == null) return "OTHER";
        if (mimeType.startsWith("image/")) return "IMAGE";
        return "DOCUMENT";
    }

    private Page<CommunityPostListResponse> convertToListResponse(Page<CommunityPost> posts, String userEmail) {
        if (posts.isEmpty()) {
            return posts.map(p -> null);
        }

        Set<Long> postIds = posts.getContent().stream()
                .map(CommunityPost::getId)
                .collect(Collectors.toSet());

        // 작성자 정보 일괄 조회
        Map<Long, String> authorNameMap = getAuthorNames(posts.getContent());

        // 좋아요 정보 조회 (로그인 시)
        Set<Long> likedPostIds = new HashSet<>();
        Set<Long> dislikedPostIds = new HashSet<>();
        Long currentUserId = null;

        if (userEmail != null) {
            User currentUser = userRepository.findByEmailAndIsDeletedFalse(userEmail).orElse(null);
            if (currentUser != null) {
                currentUserId = currentUser.getId();
                likedPostIds = likeRepository.findLikedPostIdsByUserIdAndPostIdIn(currentUserId, postIds);
                dislikedPostIds = likeRepository.findDislikedPostIdsByUserIdAndPostIdIn(currentUserId, postIds);
            }
        }

        final Long finalCurrentUserId = currentUserId;
        final Set<Long> finalLikedPostIds = likedPostIds;
        final Set<Long> finalDislikedPostIds = dislikedPostIds;

        return posts.map(post -> {
            String authorName = authorNameMap.getOrDefault(post.getUser().getId(), post.getUser().getEmail());
            Boolean isLiked = finalLikedPostIds.contains(post.getId());
            Boolean isDisliked = finalDislikedPostIds.contains(post.getId());
            Boolean isOwner = finalCurrentUserId != null && post.getUser().getId().equals(finalCurrentUserId);
            return CommunityPostListResponse.from(post, authorName, isLiked, isDisliked, isOwner);
        });
    }

    private CommunityPostDetailResponse convertToDetailResponse(CommunityPost post, String userEmail) {
        String authorName = getAuthorName(post.getUser());
        List<CommunityFileResponse> attachments = getAttachments(post.getId());

        Boolean isLiked = false;
        Boolean isDisliked = false;
        Boolean isOwner = false;

        if (userEmail != null) {
            User currentUser = userRepository.findByEmailAndIsDeletedFalse(userEmail).orElse(null);
            if (currentUser != null) {
                isLiked = likeRepository.existsByPostIdAndUserIdAndLikeType(post.getId(), currentUser.getId(), LikeType.LIKE);
                isDisliked = likeRepository.existsByPostIdAndUserIdAndLikeType(post.getId(), currentUser.getId(), LikeType.DISLIKE);
                isOwner = post.getUser().getId().equals(currentUser.getId());
            }
        }

        return CommunityPostDetailResponse.from(post, authorName, attachments, isLiked, isDisliked, isOwner);
    }

    private String getAuthorName(User user) {
        return userProfileRepository.findByUser(user)
                .map(UserProfile::getName)
                .orElse(user.getEmail());
    }

    private Map<Long, String> getAuthorNames(List<CommunityPost> posts) {
        Set<Long> userIds = posts.stream()
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, String> result = new HashMap<>();
        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                result.put(userId, getAuthorName(user));
            }
        }
        return result;
    }

    private List<CommunityFileResponse> getAttachments(Long postId) {
        List<CommunityPostAttachment> attachments = attachmentRepository.findByPostIdOrderByDisplayOrderAsc(postId);

        List<CommunityFileResponse> result = new ArrayList<>();
        for (CommunityPostAttachment att : attachments) {
            File file = fileRepository.findById(att.getFileId()).orElse(null);
            if (file != null && !file.getIsDeleted()) {
                result.add(CommunityFileResponse.from(file, att.getAttachmentType(), att.getDisplayOrder()));
            }
        }
        return result;
    }
}
