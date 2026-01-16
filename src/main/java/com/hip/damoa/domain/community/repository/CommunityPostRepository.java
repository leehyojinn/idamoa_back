package com.hip.damoa.domain.community.repository;

import com.hip.damoa.domain.community.model.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long>,
        JpaSpecificationExecutor<CommunityPost> {

    Optional<CommunityPost> findByUuidAndIsDeletedFalse(UUID uuid);

    Optional<CommunityPost> findByUuid(UUID uuid);

    // 카테고리별 게시글 목록 (공개된 것만) - 부모 카테고리 조회 시 자식 카테고리 게시글 포함
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE (p.category.slug = :categorySlug OR p.category.parent.slug = :categorySlug) " +
           "AND p.isPublished = true AND p.isDeleted = false " +
           "ORDER BY p.isPinned DESC, p.isNotice DESC, p.createdAt DESC")
    Page<CommunityPost> findByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);

    // 전체 게시글 목록 (공개된 것만)
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE p.isPublished = true AND p.isDeleted = false " +
           "ORDER BY p.isPinned DESC, p.isNotice DESC, p.createdAt DESC")
    Page<CommunityPost> findAllPublished(Pageable pageable);

    // 키워드 검색 (제목, 내용)
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE p.isPublished = true AND p.isDeleted = false " +
           "AND (LOWER(CAST(p.title AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "     OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "ORDER BY p.isPinned DESC, p.isNotice DESC, p.createdAt DESC")
    Page<CommunityPost> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 카테고리 + 키워드 검색 - 부모 카테고리 조회 시 자식 카테고리 게시글 포함
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE (p.category.slug = :categorySlug OR p.category.parent.slug = :categorySlug) " +
           "AND p.isPublished = true AND p.isDeleted = false " +
           "AND (LOWER(CAST(p.title AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "     OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "ORDER BY p.isPinned DESC, p.isNotice DESC, p.createdAt DESC")
    Page<CommunityPost> searchByCategoryAndKeyword(
            @Param("categorySlug") String categorySlug,
            @Param("keyword") String keyword,
            Pageable pageable);

    // 사용자별 게시글
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE p.user.id = :userId AND p.isDeleted = false " +
           "ORDER BY p.createdAt DESC")
    Page<CommunityPost> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 공지글만 조회
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE p.isNotice = true AND p.isPublished = true AND p.isDeleted = false " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> findNotices();

    // 카테고리별 공지글
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE p.category.slug = :categorySlug " +
           "AND p.isNotice = true AND p.isPublished = true AND p.isDeleted = false " +
           "ORDER BY p.createdAt DESC")
    List<CommunityPost> findNoticesByCategory(@Param("categorySlug") String categorySlug);

    // 관리자용: 전체 조회
    @Query("SELECT p FROM CommunityPost p " +
           "WHERE (:categorySlug IS NULL OR p.category.slug = :categorySlug) " +
           "AND (:keyword IS NULL OR LOWER(CAST(p.title AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) " +
           "     OR LOWER(CAST(p.content AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<CommunityPost> searchForAdmin(
            @Param("categorySlug") String categorySlug,
            @Param("keyword") String keyword,
            Pageable pageable);
}
