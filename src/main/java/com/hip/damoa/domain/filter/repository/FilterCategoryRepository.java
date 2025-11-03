package com.hip.damoa.domain.filter.repository;

import com.hip.damoa.domain.filter.model.FilterCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilterCategoryRepository extends JpaRepository<FilterCategory, Long> {

    // Find by category code
    Optional<FilterCategory> findByCategoryCode(String categoryCode);

    // Find by category name
    Optional<FilterCategory> findByCategoryName(String categoryName);

    // Find by filter type
    List<FilterCategory> findByFilterType(String filterType);

    // Find active categories
    @Query("SELECT c FROM FilterCategory c WHERE c.isActive = true " +
           "ORDER BY c.displayOrder ASC")
    List<FilterCategory> findActiveCategories();

    // Find active by filter type
    @Query("SELECT c FROM FilterCategory c WHERE c.filterType = :filterType " +
           "AND c.isActive = true " +
           "ORDER BY c.displayOrder ASC")
    List<FilterCategory> findActiveByFilterType(@Param("filterType") String filterType);

    // Find required categories
    @Query("SELECT c FROM FilterCategory c WHERE c.isRequired = true " +
           "AND c.isActive = true " +
           "ORDER BY c.displayOrder ASC")
    List<FilterCategory> findRequiredCategories();

    // Find by display order range
    @Query("SELECT c FROM FilterCategory c WHERE c.displayOrder >= :minOrder " +
           "AND c.displayOrder <= :maxOrder " +
           "ORDER BY c.displayOrder ASC")
    List<FilterCategory> findByDisplayOrderRange(@Param("minOrder") Integer minOrder,
                                                   @Param("maxOrder") Integer maxOrder);

    // Count by filter type
    long countByFilterType(String filterType);

    // Count active categories
    long countByIsActiveTrue();

    // Count required categories
    long countByIsRequiredTrue();

    // Check if category code exists
    boolean existsByCategoryCode(String categoryCode);
}
