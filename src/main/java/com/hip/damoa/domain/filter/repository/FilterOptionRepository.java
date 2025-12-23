package com.hip.damoa.domain.filter.repository;

import com.hip.damoa.domain.filter.model.FilterCategory;
import com.hip.damoa.domain.filter.model.FilterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilterOptionRepository extends JpaRepository<FilterOption, Long>, JpaSpecificationExecutor<FilterOption> {
    Optional<FilterOption> findByCode(String code);
    List<FilterOption> findByCategoryAndIsActiveTrueAndIsDeletedFalse(FilterCategory category);
    List<FilterOption> findByCategoryAndParentIsNullAndIsActiveTrueAndIsDeletedFalse(FilterCategory category);
    List<FilterOption> findByIdIn(List<Long> ids);

    // ===== JSONB 배열 기반 필터 조회 =====

    /**
     * ID 목록으로 필터 옵션 조회 (삭제되지 않은 것만)
     * - JSONB 배열에 저장된 filter_option_ids를 조회할 때 사용
     */
    List<FilterOption> findByIdInAndIsDeletedFalse(Collection<Long> ids);

    /**
     * ID 목록으로 필터 옵션과 카테고리 함께 조회
     * - 응답에 카테고리별로 그룹화할 때 사용
     */
    @Query("""
        SELECT fo FROM FilterOption fo
        JOIN FETCH fo.category
        WHERE fo.id IN :ids AND fo.isDeleted = false
        ORDER BY fo.category.displayOrder, fo.displayOrder
        """)
    List<FilterOption> findByIdInWithCategory(@Param("ids") Collection<Long> ids);

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
