package com.hip.damoa.domain.community.repository;

import com.hip.damoa.domain.community.model.CommunityLike;
import com.hip.damoa.domain.community.model.LikeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface CommunityLikeRepository extends JpaRepository<CommunityLike, Long> {

    // 게시글 좋아요/싫어요 조회
    Optional<CommunityLike> findByPostIdAndUserId(Long postId, Long userId);

    // 댓글 좋아요/싫어요 조회
    Optional<CommunityLike> findByCommentIdAndUserId(Long commentId, Long userId);

    // 게시글에 대한 사용자의 좋아요 여부
    boolean existsByPostIdAndUserIdAndLikeType(Long postId, Long userId, LikeType likeType);

    // 댓글에 대한 사용자의 좋아요 여부
    boolean existsByCommentIdAndUserIdAndLikeType(Long commentId, Long userId, LikeType likeType);

    // 게시글 좋아요 삭제
    void deleteByPostIdAndUserId(Long postId, Long userId);

    // 댓글 좋아요 삭제
    void deleteByCommentIdAndUserId(Long commentId, Long userId);

    // 사용자가 좋아요한 게시글 ID 목록
    @Query("SELECT l.post.id FROM CommunityLike l " +
           "WHERE l.post.id IN :postIds AND l.user.id = :userId AND l.likeType = 'LIKE'")
    Set<Long> findLikedPostIdsByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") Set<Long> postIds);

    // 사용자가 싫어요한 게시글 ID 목록
    @Query("SELECT l.post.id FROM CommunityLike l " +
           "WHERE l.post.id IN :postIds AND l.user.id = :userId AND l.likeType = 'DISLIKE'")
    Set<Long> findDislikedPostIdsByUserIdAndPostIdIn(@Param("userId") Long userId, @Param("postIds") Set<Long> postIds);
}
