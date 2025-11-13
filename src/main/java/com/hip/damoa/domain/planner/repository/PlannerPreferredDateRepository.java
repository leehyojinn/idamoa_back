package com.hip.damoa.domain.planner.repository;

import com.hip.damoa.domain.planner.model.PlannerPreferredDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlannerPreferredDateRepository extends JpaRepository<PlannerPreferredDate, Long> {

    // 플래너 신청서 ID로 조회
    List<PlannerPreferredDate> findByPlannerApplicationIdOrderByPriorityAsc(Long plannerApplicationId);

    // 플래너 신청서 ID로 삭제 (cascade로 처리되지만 명시적 삭제 필요 시)
    void deleteByPlannerApplicationId(Long plannerApplicationId);
}
