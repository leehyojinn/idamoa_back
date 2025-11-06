package com.hip.damoa.domain.company.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 업체 좋아요
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_likes", indexes = {
    @Index(name = "idx_company_likes_company_id", columnList = "company_id"),
    @Index(name = "idx_company_likes_user_id", columnList = "user_id")
},
uniqueConstraints = {
    @UniqueConstraint(name = "uk_company_likes_company_user", columnNames = {"company_id", "user_id"})
})
public class CompanyLike extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
