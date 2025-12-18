package com.hip.damoa.domain.filter.repository;

import com.hip.damoa.domain.filter.model.FilterCategory;
import com.hip.damoa.domain.filter.model.FilterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FilterOptionRepository extends JpaRepository<FilterOption, Long>, JpaSpecificationExecutor<FilterOption> {
    Optional<FilterOption> findByCode(String code);
    List<FilterOption> findByCategoryAndIsActiveTrueAndIsDeletedFalse(FilterCategory category);
    List<FilterOption> findByCategoryAndParentIsNullAndIsActiveTrueAndIsDeletedFalse(FilterCategory category);
    List<FilterOption> findByIdIn(List<Long> ids);

    // Public API용 (soft delete 제외)
    List<FilterOption> findByCategoryAndIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc(FilterCategory category);

    // Admin API용 (전체 조회, soft delete 포함)
    List<FilterOption> findByCategoryOrderByDisplayOrderAsc(FilterCategory category);

    // 계층 트리 조회용 메서드
    List<FilterOption> findByParentAndIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc(FilterOption parent);

    List<FilterOption> findByCategoryAndParentIsNullAndIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc(FilterCategory category);

    // 마이그레이션용 전체 옵션 조회
    List<FilterOption> findByIsDeletedFalseOrderByCategoryIdAscDisplayOrderAsc();

    // [N+1 최적화] 카테고리별 활성 옵션 수 일괄 조회
    @Query("SELECT fo.category.id, COUNT(fo.id) FROM FilterOption fo " +
           "WHERE fo.category.id IN :categoryIds " +
           "AND fo.isActive = true AND fo.isDeleted = false " +
           "GROUP BY fo.category.id")
    List<Object[]> countByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);

    // [N+1 최적화] 옵션별 자식 수 일괄 조회
    @Query("SELECT fo.parent.id, COUNT(fo.id) FROM FilterOption fo " +
           "WHERE fo.parent.id IN :parentIds " +
           "GROUP BY fo.parent.id")
    List<Object[]> countChildrenByParentIdIn(@Param("parentIds") List<Long> parentIds);
}
