package com.hip.damoa.domain.filter.repository;

import com.hip.damoa.domain.filter.model.FilterCategory;
import com.hip.damoa.domain.filter.model.FilterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterOptionRepository extends JpaRepository<FilterOption, Long> {

    List<FilterOption> findByCategoryAndIsActiveTrue(FilterCategory category);

    List<FilterOption> findByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(FilterCategory category);
}
