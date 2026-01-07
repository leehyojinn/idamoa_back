package com.hip.damoa.domain.community.repository;

import com.hip.damoa.domain.community.model.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    Optional<CommunityComment> findByUuidAndIsDeletedFalse(UUID uuid);

    Optional<CommunityComment> findByUuid(UUID uuid);

    // 게시글의 최상위 댓글만 조회 (parent가 null인 것)
    @Query("SELECT c FROM CommunityComment c " +
           "WHERE c.post.id = :postId AND c.parent IS NULL AND c.isDeleted = false " +
           "ORDER BY c.createdAt ASC")
    List<CommunityComment> findTopLevelCommentsByPostId(@Param("postId") Long postId);

    // 게시글의 모든 댓글 조회 (삭제되지 않은 것)
    @Query("SELECT c FROM CommunityComment c " +
           "WHERE c.post.id = :postId AND c.isDeleted = false " +
           "ORDER BY c.createdAt ASC")
    List<CommunityComment> findAllByPostId(@Param("postId") Long postId);

    // 게시글의 댓글 수
    @Query("SELECT COUNT(c) FROM CommunityComment c " +
           "WHERE c.post.id = :postId AND c.isDeleted = false")
    long countByPostId(@Param("postId") Long postId);

    // 대댓글 조회
    @Query("SELECT c FROM CommunityComment c " +
           "WHERE c.parent.id = :parentId AND c.isDeleted = false " +
           "ORDER BY c.createdAt ASC")
    List<CommunityComment> findChildrenByParentId(@Param("parentId") Long parentId);

    // 사용자별 댓글 조회
    @Query("SELECT c FROM CommunityComment c " +
           "WHERE c.user.id = :userId AND c.isDeleted = false " +
           "ORDER BY c.createdAt DESC")
    Page<CommunityComment> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 관리자용: 전체 댓글 조회
    @Query("SELECT c FROM CommunityComment c " +
           "WHERE (:keyword IS NULL OR LOWER(CAST(c.content AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<CommunityComment> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
