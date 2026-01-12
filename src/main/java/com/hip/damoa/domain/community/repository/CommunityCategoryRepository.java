package com.hip.damoa.domain.community.repository;

import com.hip.damoa.domain.community.model.CommunityCategory;
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
public interface CommunityCategoryRepository extends JpaRepository<CommunityCategory, Long> {

    Optional<CommunityCategory> findByUuidAndIsDeletedFalse(UUID uuid);

    Optional<CommunityCategory> findBySlugAndIsDeletedFalse(String slug);

    List<CommunityCategory> findByIsActiveAndIsDeletedFalseOrderByDisplayOrderAsc(Boolean isActive);

    List<CommunityCategory> findByIsDeletedFalseOrderByDisplayOrderAsc();

    boolean existsBySlugAndIsDeletedFalse(String slug);

    // 관리자용: 삭제된 것 포함
    @Query("SELECT c FROM CommunityCategory c WHERE " +
           "(:keyword IS NULL OR LOWER(CAST(c.name AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
           "ORDER BY c.depth ASC, c.displayOrder ASC")
    Page<CommunityCategory> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM CommunityCategory c WHERE c.isDeleted = false " +
           "ORDER BY c.depth ASC, c.displayOrder ASC")
    List<CommunityCategory> findAllActiveCategories();

    // === 계층 구조 관련 쿼리 ===

    /**
     * 최상위 카테고리 목록 조회 (parent가 null인 것)
     */
    @Query("SELECT c FROM CommunityCategory c WHERE c.parent IS NULL " +
           "AND c.isActive = true AND c.isDeleted = false " +
           "ORDER BY c.displayOrder ASC")
    List<CommunityCategory> findRootCategories();

    /**
     * 최상위 카테고리 목록 조회 - 관리자용 (비활성 포함)
     */
    @Query("SELECT c FROM CommunityCategory c WHERE c.parent IS NULL " +
           "AND c.isDeleted = false " +
           "ORDER BY c.displayOrder ASC")
    List<CommunityCategory> findRootCategoriesForAdmin();

    /**
     * 특정 부모의 자식 카테고리 목록
     */
    @Query("SELECT c FROM CommunityCategory c WHERE c.parent.id = :parentId " +
           "AND c.isActive = true AND c.isDeleted = false " +
           "ORDER BY c.displayOrder ASC")
    List<CommunityCategory> findChildrenByParentId(@Param("parentId") Long parentId);

    /**
     * 특정 부모의 자식 카테고리 목록 - 관리자용
     */
    @Query("SELECT c FROM CommunityCategory c WHERE c.parent.id = :parentId " +
           "AND c.isDeleted = false " +
           "ORDER BY c.displayOrder ASC")
    List<CommunityCategory> findChildrenByParentIdForAdmin(@Param("parentId") Long parentId);

    /**
     * 자식 카테고리 존재 여부 확인
     */
    @Query("SELECT COUNT(c) > 0 FROM CommunityCategory c WHERE c.parent.id = :parentId " +
           "AND c.isDeleted = false")
    boolean hasChildren(@Param("parentId") Long parentId);
}
