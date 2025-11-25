package com.hip.damoa.domain.planner.repository;

import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.model.PlannerApplication;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlannerApplicationRepository extends JpaRepository<PlannerApplication, Long> {

    // UUID 기반 조회
    Optional<PlannerApplication> findByUuidAndIsDeletedFalse(UUID uuid);

    // 사용자별 조회
    Page<PlannerApplication> findByUserAndIsDeletedFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    // 상태별 조회 (관리자용)
    Page<PlannerApplication> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(PlannerApplicationStatus status, Pageable pageable);

    // 전체 조회 (관리자용)
    Page<PlannerApplication> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // 사용자 + 상태별 조회
    Page<PlannerApplication> findByUserAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            User user, PlannerApplicationStatus status, Pageable pageable);

    // 개수 조회
    long countByIsDeletedFalse();

    long countByStatusAndIsDeletedFalse(PlannerApplicationStatus status);

    long countByUserAndIsDeletedFalse(User user);

    // Admin Dashboard Statistics
    Long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime dateTime);

    @Query("SELECT COUNT(p) FROM PlannerApplication p WHERE CAST(p.status AS string) = :status AND p.isDeleted = false")
    Long countByStatusAndIsDeletedFalse(@Param("status") String status);
}
