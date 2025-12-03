package com.hip.damoa.domain.filter.repository;

import com.hip.damoa.domain.filter.model.FilterCategory;
import com.hip.damoa.domain.filter.model.FilterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface FilterOptionRepository extends JpaRepository<FilterOption, Long>, JpaSpecificationExecutor<FilterOption> {
    Optional<FilterOption> findByCode(String code);
    List<FilterOption> findByCategoryAndIsActiveTrueAndIsDeletedFalse(FilterCategory category);
    List<FilterOption> findByCategoryAndParentIsNullAndIsActiveTrueAndIsDeletedFalse(FilterCategory category);
    List<FilterOption> findByIdIn(List<Long> ids);

    // FilterService용 추가 메서드 (정렬 포함)
    List<FilterOption> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(FilterCategory category);

    // 계층 트리 조회용 메서드
    List<FilterOption> findByParentAndIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc(FilterOption parent);

    List<FilterOption> findByCategoryAndParentIsNullAndIsActiveTrueAndIsDeletedFalseOrderByDisplayOrderAsc(FilterCategory category);

    // 마이그레이션용 전체 옵션 조회
    List<FilterOption> findByIsDeletedFalseOrderByCategoryIdAscDisplayOrderAsc();
}
