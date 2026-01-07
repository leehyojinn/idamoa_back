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
           "ORDER BY c.displayOrder ASC")
    Page<CommunityCategory> searchForAdmin(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM CommunityCategory c WHERE c.isDeleted = false " +
           "ORDER BY c.displayOrder ASC")
    List<CommunityCategory> findAllActiveCategories();
}
