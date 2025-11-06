package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.filter.model.FilterOption;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 업체-필터 옵션 다대다 조인 엔티티
 *
 * 업체가 선택한 필터 옵션들을 저장합니다
 */
@Entity
@Table(name = "company_filter_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyFilterOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_option_id", nullable = false)
    private FilterOption filterOption;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public CompanyFilterOption(Company company, FilterOption filterOption) {
        this.company = company;
        this.filterOption = filterOption;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
