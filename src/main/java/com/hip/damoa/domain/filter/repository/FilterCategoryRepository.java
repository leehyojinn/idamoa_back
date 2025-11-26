package com.hip.damoa.domain.filter.repository;

import com.hip.damoa.domain.filter.model.FilterCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FilterCategoryRepository extends JpaRepository<FilterCategory, Long>, JpaSpecificationExecutor<FilterCategory> {
    Optional<FilterCategory> findByCode(String code);
    List<FilterCategory> findByEntityTypeAndIsActiveTrueAndIsDeletedFalse(String entityType);
    List<FilterCategory> findByIsActiveTrueAndIsDeletedFalseOrderByDisplayOrder();

    // FilterService용 추가 메서드
    @Query("SELECT fc FROM FilterCategory fc WHERE fc.isActive = true AND fc.isDeleted = false ORDER BY fc.displayOrder")
    List<FilterCategory> findActiveCategories();

    @Query("SELECT fc FROM FilterCategory fc WHERE fc.isRequired = true AND fc.isActive = true AND fc.isDeleted = false ORDER BY fc.displayOrder")
    List<FilterCategory> findRequiredCategories();

    @Query("SELECT fc FROM FilterCategory fc WHERE fc.filterType = :filterType AND fc.isActive = true AND fc.isDeleted = false ORDER BY fc.displayOrder")
    List<FilterCategory> findActiveByFilterType(String filterType);
}
