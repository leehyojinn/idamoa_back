package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 제휴업체 엔티티
 */
@Entity
@Table(name = "company_partnerships", indexes = {
    @Index(name = "idx_company_partnerships_status", columnList = "status"),
    @Index(name = "idx_company_partnerships_end_date", columnList = "end_date"),
    @Index(name = "idx_company_partnerships_display_order", columnList = "display_order")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyPartnership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    // ===== Business Methods =====

    /**
     * 제휴 만료 처리
     */
    public void expire() {
        this.status = "EXPIRED";
    }

    /**
     * 제휴 취소 처리
     */
    public void cancel() {
        this.status = "CANCELLED";
    }

    /**
     * 활성 상태 여부 확인
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status) && !this.getIsDeleted();
    }

    /**
     * 현재 유효한 제휴인지 확인 (날짜 기반)
     */
    public boolean isValidNow() {
        LocalDate today = LocalDate.now();
        return isActive()
            && !today.isBefore(startDate)
            && !today.isAfter(endDate);
    }

    /**
     * 만료 여부 확인 (오늘 기준)
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    /**
     * 제휴 정보 업데이트
     */
    public void update(Integer displayOrder, LocalDate startDate,
                       LocalDate endDate, String adminMemo) {
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
        if (adminMemo != null) this.adminMemo = adminMemo;
    }

    /**
     * 기간 연장
     */
    public void extendEndDate(LocalDate newEndDate) {
        if (newEndDate.isAfter(this.endDate)) {
            this.endDate = newEndDate;
        }
    }
}
