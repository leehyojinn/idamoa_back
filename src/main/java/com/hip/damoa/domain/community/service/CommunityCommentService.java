package com.hip.damoa.domain.community.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.community.model.*;
import com.hip.damoa.domain.community.repository.*;
import com.hip.damoa.domain.community.web.dto.*;
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
public class CommunityCommentService {

    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    /**
     * 댓글 목록 조회 (트리 구조)
     */
    @Transactional(readOnly = true)
    public List<CommunityCommentResponse> getComments(UUID postUuid, String userEmail) {
        CommunityPost post = postRepository.findByUuidAndIsDeletedFalse(postUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        // 최상위 댓글만 조회
        List<CommunityComment> topLevelComments = commentRepository.findTopLevelCommentsByPostId(post.getId());

        // 현재 사용자 정보
        Long currentUserId = null;
        if (userEmail != null) {
            User currentUser = userRepository.findByEmailAndIsDeletedFalse(userEmail).orElse(null);
            if (currentUser != null) {
                currentUserId = currentUser.getId();
            }
        }

        // 재귀적으로 트리 구조 생성
        return buildCommentTree(topLevelComments, currentUserId);
    }

    /**
     * 댓글 작성
     */
    @Transactional
    public CommunityCommentResponse createComment(UUID postUuid, String userEmail,
                                                   CommunityCommentCreateRequest request, String ipAddress) {
        log.info("커뮤니티 댓글 작성: postUuid={}, userEmail={}", postUuid, userEmail);

        CommunityPost post = postRepository.findByUuidAndIsDeletedFalse(postUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND));

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 익명 허용 여부 확인
        if (Boolean.TRUE.equals(request.getIsAnonymous()) && !post.getCategory().getAllowAnonymous()) {
            throw new BusinessException(ErrorCode.COMMUNITY_ANONYMOUS_NOT_ALLOWED);
        }

        // 부모 댓글 확인
        CommunityComment parent = null;
        if (request.getParentUuid() != null) {
            parent = commentRepository.findByUuidAndIsDeletedFalse(request.getParentUuid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));
        }

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .parent(parent)
                .user(user)
                .content(request.getContent())
                .isAnonymous(request.getIsAnonymous())
                .ipAddress(ipAddress)
                .build();

        comment = commentRepository.save(comment);

        // 게시글 댓글 수 증가
        post.incrementCommentCount();
        postRepository.save(post);

        log.info("커뮤니티 댓글 작성 완료: commentUuid={}", comment.getUuid());

        String authorName = getAuthorName(user);
        return CommunityCommentResponse.from(comment, authorName, false, false, true);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public CommunityCommentResponse updateComment(UUID uuid, String userEmail, CommunityCommentUpdateRequest request) {
        log.info("커뮤니티 댓글 수정: uuid={}, userEmail={}", uuid, userEmail);

        CommunityComment comment = commentRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        // 작성자 확인
        if (!comment.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.COMMUNITY_COMMENT_ACCESS_DENIED);
        }

        comment.update(request.getContent());
        comment = commentRepository.save(comment);

        log.info("커뮤니티 댓글 수정 완료: uuid={}", uuid);

        String authorName = getAuthorName(comment.getUser());
        return CommunityCommentResponse.from(comment, authorName, false, false, true);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void deleteComment(UUID uuid, String userEmail) {
        log.info("커뮤니티 댓글 삭제: uuid={}, userEmail={}", uuid, userEmail);

        CommunityComment comment = commentRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        // 작성자 확인
        if (!comment.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.COMMUNITY_COMMENT_ACCESS_DENIED);
        }

        comment.softDelete();
        commentRepository.save(comment);

        // 게시글 댓글 수 감소
        CommunityPost post = comment.getPost();
        post.decrementCommentCount();
        postRepository.save(post);

        log.info("커뮤니티 댓글 삭제 완료: uuid={}", uuid);
    }

    /**
     * 댓글 좋아요
     */
    @Transactional
    public void likeComment(UUID uuid, String userEmail) {
        CommunityComment comment = commentRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<CommunityLike> existingLike = likeRepository.findByCommentIdAndUserId(comment.getId(), user.getId());

        if (existingLike.isPresent()) {
            CommunityLike like = existingLike.get();
            if (like.getLikeType() == LikeType.LIKE) {
                likeRepository.delete(like);
                comment.decrementLikeCount();
            } else {
                like.changeLikeType(LikeType.LIKE);
                likeRepository.save(like);
                comment.decrementDislikeCount();
                comment.incrementLikeCount();
            }
        } else {
            CommunityLike like = CommunityLike.forComment(comment, user, LikeType.LIKE);
            likeRepository.save(like);
            comment.incrementLikeCount();
        }

        commentRepository.save(comment);
    }

    /**
     * 댓글 싫어요
     */
    @Transactional
    public void dislikeComment(UUID uuid, String userEmail) {
        CommunityComment comment = commentRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<CommunityLike> existingLike = likeRepository.findByCommentIdAndUserId(comment.getId(), user.getId());

        if (existingLike.isPresent()) {
            CommunityLike like = existingLike.get();
            if (like.getLikeType() == LikeType.DISLIKE) {
                likeRepository.delete(like);
                comment.decrementDislikeCount();
            } else {
                like.changeLikeType(LikeType.DISLIKE);
                likeRepository.save(like);
                comment.decrementLikeCount();
                comment.incrementDislikeCount();
            }
        } else {
            CommunityLike like = CommunityLike.forComment(comment, user, LikeType.DISLIKE);
            likeRepository.save(like);
            comment.incrementDislikeCount();
        }

        commentRepository.save(comment);
    }

    // ========== 관리자 전용 ==========

    /**
     * 댓글 목록 조회 (관리자)
     */
    @Transactional(readOnly = true)
    public Page<CommunityCommentResponse> getCommentsForAdmin(String keyword, Pageable pageable) {
        Page<CommunityComment> comments = commentRepository.searchForAdmin(keyword, pageable);
        return comments.map(comment -> {
            String authorName = getAuthorName(comment.getUser());
            return CommunityCommentResponse.from(comment, authorName, false, false, false);
        });
    }

    /**
     * 댓글 삭제 (관리자)
     */
    @Transactional
    public void deleteCommentByAdmin(UUID uuid) {
        log.info("[관리자] 커뮤니티 댓글 삭제: uuid={}", uuid);

        CommunityComment comment = commentRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        comment.softDelete();
        commentRepository.save(comment);

        // 게시글 댓글 수 감소
        if (!comment.getPost().getIsDeleted()) {
            CommunityPost post = comment.getPost();
            post.decrementCommentCount();
            postRepository.save(post);
        }

        log.info("[관리자] 커뮤니티 댓글 삭제 완료: uuid={}", uuid);
    }

    /**
     * 댓글 비공개 (관리자)
     */
    @Transactional
    public void hideComment(UUID uuid) {
        log.info("[관리자] 커뮤니티 댓글 비공개: uuid={}", uuid);

        CommunityComment comment = commentRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        comment.hide();
        commentRepository.save(comment);

        log.info("[관리자] 커뮤니티 댓글 비공개 완료: uuid={}", uuid);
    }

    /**
     * 댓글 공개 (관리자)
     */
    @Transactional
    public void showComment(UUID uuid) {
        log.info("[관리자] 커뮤니티 댓글 공개: uuid={}", uuid);

        CommunityComment comment = commentRepository.findByUuid(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND));

        comment.show();
        commentRepository.save(comment);

        log.info("[관리자] 커뮤니티 댓글 공개 완료: uuid={}", uuid);
    }

    // ========== 헬퍼 메서드 ==========

    private List<CommunityCommentResponse> buildCommentTree(List<CommunityComment> comments, Long currentUserId) {
        return comments.stream()
                .map(comment -> {
                    String authorName = getAuthorName(comment.getUser());
                    Boolean isLiked = false;
                    Boolean isDisliked = false;
                    Boolean isOwner = false;

                    if (currentUserId != null) {
                        isLiked = likeRepository.existsByCommentIdAndUserIdAndLikeType(
                                comment.getId(), currentUserId, LikeType.LIKE);
                        isDisliked = likeRepository.existsByCommentIdAndUserIdAndLikeType(
                                comment.getId(), currentUserId, LikeType.DISLIKE);
                        isOwner = comment.getUser().getId().equals(currentUserId);
                    }

                    CommunityCommentResponse response = CommunityCommentResponse.from(
                            comment, authorName, isLiked, isDisliked, isOwner);

                    // 대댓글 재귀 처리
                    if (comment.getChildren() != null && !comment.getChildren().isEmpty()) {
                        List<CommunityComment> activeChildren = comment.getChildren().stream()
                                .filter(c -> !c.getIsDeleted())
                                .collect(Collectors.toList());
                        response.setChildren(buildCommentTree(activeChildren, currentUserId));
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    private String getAuthorName(User user) {
        return userProfileRepository.findByUser(user)
                .map(UserProfile::getName)
                .orElse(user.getEmail());
    }
}
