package com.hip.damoa.domain.portfolio.model;

import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.filter.model.FilterOption;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 포트폴리오-필터 옵션 연결 엔티티
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_filter_options", indexes = {
    @Index(name = "idx_portfolio_filter_options_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_portfolio_filter_options_filter_option_id", columnList = "filter_option_id")
})
public class PortfolioFilterOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CompanyPortfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_option_id", nullable = false)
    private FilterOption filterOption;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 정적 팩토리 메서드
     */
    public static PortfolioFilterOption of(CompanyPortfolio portfolio, FilterOption filterOption) {
        return PortfolioFilterOption.builder()
                .portfolio(portfolio)
                .filterOption(filterOption)
                .build();
    }
}
