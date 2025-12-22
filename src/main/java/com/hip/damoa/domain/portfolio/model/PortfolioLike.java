package com.hip.damoa.domain.portfolio.model;

import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 포트폴리오 좋아요
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"portfolio_id", "user_id"}),
    indexes = {
        @Index(name = "idx_portfolio_likes_portfolio_id", columnList = "portfolio_id"),
        @Index(name = "idx_portfolio_likes_user_id", columnList = "user_id")
    }
)
public class PortfolioLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CompanyPortfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 정적 팩토리 메서드
     */
    public static PortfolioLike of(CompanyPortfolio portfolio, User user) {
        return PortfolioLike.builder()
                .portfolio(portfolio)
                .user(user)
                .build();
    }
}
