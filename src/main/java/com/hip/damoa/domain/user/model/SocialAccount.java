package com.hip.damoa.domain.user.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 소셜 로그인 연동
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "social_accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_social_provider_user", columnNames = {"provider", "provider_user_id"})
    },
    indexes = {
        @Index(name = "idx_social_accounts_user_id", columnList = "user_id"),
        @Index(name = "idx_social_accounts_provider", columnList = "provider")
    })
public class SocialAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 200)
    private String providerUserId;

    @Column(name = "provider_email", length = 100)
    private String providerEmail;

    @Column(name = "provider_name", length = 100)
    private String providerName;

    @Type(JsonBinaryType.class)
    @Column(name = "profile_data", columnDefinition = "jsonb")
    private Map<String, Object> profileData;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "linked_at", nullable = false)
    @Builder.Default
    private LocalDateTime linkedAt = LocalDateTime.now();

    // ===== Business Methods =====

    /**
     * 프로필 정보 업데이트
     */
    public void updateProfile(String email, String name, Map<String, Object> profileData) {
        this.providerEmail = email;
        this.providerName = name;
        this.profileData = profileData;
    }

    /**
     * 토큰 업데이트
     */
    public void updateTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = expiresAt;
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired() {
        return tokenExpiresAt != null && tokenExpiresAt.isBefore(LocalDateTime.now());
    }
}
