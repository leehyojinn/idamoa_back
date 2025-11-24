package com.hip.damoa.domain.planner.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 플래너 신청서 희망 일정
 *
 * 우선순위별 희망 날짜와 시간
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "planner_preferred_dates")
public class PlannerPreferredDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_application_id", nullable = false)
    private PlannerApplication plannerApplication;

    @Column(name = "priority", nullable = false)
    private Integer priority; // 1, 2, 3

    @Column(name = "preferred_date", nullable = false)
    private LocalDate preferredDate;

    @Column(name = "preferred_time", nullable = false, length = 50)
    private String preferredTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 양방향 관계 설정을 위한 setter
    public void setPlannerApplication(PlannerApplication plannerApplication) {
        this.plannerApplication = plannerApplication;
    }
}
