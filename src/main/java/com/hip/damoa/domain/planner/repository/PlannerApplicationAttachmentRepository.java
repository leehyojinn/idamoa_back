package com.hip.damoa.domain.planner.repository;

import com.hip.damoa.domain.planner.model.PlannerApplication;
import com.hip.damoa.domain.planner.model.PlannerApplicationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlannerApplicationAttachmentRepository extends JpaRepository<PlannerApplicationAttachment, Long> {

    /**
     * 플래너 신청서의 첨부파일 조회 (삭제되지 않은 것만, 순서대로)
     */
    List<PlannerApplicationAttachment> findByPlannerApplicationAndIsDeletedFalseOrderByDisplayOrderAsc(
            PlannerApplication plannerApplication);

    /**
     * 플래너 신청서 ID로 첨부파일 조회 (삭제되지 않은 것만)
     */
    List<PlannerApplicationAttachment> findByPlannerApplicationIdAndIsDeletedFalseOrderByDisplayOrderAsc(
            Long plannerApplicationId);

    /**
     * 특정 파일 ID가 이미 첨부되어 있는지 확인
     */
    boolean existsByPlannerApplicationAndFileIdAndIsDeletedFalse(
            PlannerApplication plannerApplication, Long fileId);
}
