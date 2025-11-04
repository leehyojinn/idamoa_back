package com.hip.damoa.domain.user.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 사용자 기본 정보 및 인증 관리 테이블
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class
User extends BaseEntity implements UserDetails {

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password", length = 255)
    private String password; // nullable for social login only users

    @Type(StringArrayType.class)
    @Column(name = "roles", columnDefinition = "text[]", nullable = false)
    @Builder.Default
    private String[] roles = new String[]{"USER"};

    // 인증 상태
    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private Boolean phoneVerified = false;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "identity_verified", nullable = false)
    @Builder.Default
    private Boolean identityVerified = false;

    @Column(name = "identity_verified_at")
    private LocalDateTime identityVerifiedAt;

    // 로그인 정보
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private Integer loginCount = 0;

    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default
    private Integer failedLoginCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime accountLockedUntil;

    // 약관 동의
    @Column(name = "terms_agreed", nullable = false)
    @Builder.Default
    private Boolean termsAgreed = false;

    @Column(name = "terms_agreed_at")
    private LocalDateTime termsAgreedAt;

    @Column(name = "privacy_agreed", nullable = false)
    @Builder.Default
    private Boolean privacyAgreed = false;

    @Column(name = "privacy_agreed_at")
    private LocalDateTime privacyAgreedAt;

    @Column(name = "marketing_agreed")
    @Builder.Default
    private Boolean marketingAgreed = false;

    @Column(name = "marketing_agreed_at")
    private LocalDateTime marketingAgreedAt;

    // 프로필 완료
    @Column(name = "profile_completed", nullable = false)
    @Builder.Default
    private Boolean profileCompleted = false;

    @Column(name = "profile_completed_at")
    private LocalDateTime profileCompletedAt;

    // ===== UserDetails Implementation =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.length == 0) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountLockedUntil == null || accountLockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !getIsDeleted();
    }

    // ===== Business Methods =====

    /**
     * 로그인 성공 처리
     */
    public void loginSuccess(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.loginCount++;
        this.failedLoginCount = 0;
        this.accountLockedUntil = null;
    }

    /**
     * 로그인 실패 처리
     */
    public void loginFailed() {
        this.failedLoginCount++;
        // 5회 실패 시 계정 잠금 (30분)
        if (this.failedLoginCount >= 5) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    /**
     * 이메일 인증
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
    }

    /**
     * 전화번호 인증
     */
    public void verifyPhone() {
        this.phoneVerified = true;
        this.phoneVerifiedAt = LocalDateTime.now();
    }

    /**
     * 신원 인증
     */
    public void verifyIdentity() {
        this.identityVerified = true;
        this.identityVerifiedAt = LocalDateTime.now();
    }

    /**
     * 역할 추가
     */
    public void addRole(String role) {
        List<String> roleList = roles != null ? new ArrayList<>(List.of(roles)) : new ArrayList<>();
        if (!roleList.contains(role)) {
            roleList.add(role);
            this.roles = roleList.toArray(new String[0]);
        }
    }

    /**
     * 역할 제거
     */
    public void removeRole(String role) {
        if (roles != null) {
            List<String> roleList = new ArrayList<>(List.of(roles));
            roleList.remove(role);
            this.roles = roleList.toArray(new String[0]);
        }
    }

    /**
     * 역할 확인
     */
    public boolean hasRole(String role) {
        if (roles == null) {
            return false;
        }
        for (String r : roles) {
            if (r.equals(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 마케팅 동의
     */
    public void agreeMarketing() {
        this.marketingAgreed = true;
        this.marketingAgreedAt = LocalDateTime.now();
    }

    /**
     * 마케팅 동의 철회
     */
    public void disagreeMarketing() {
        this.marketingAgreed = false;
        this.marketingAgreedAt = null;
    }

    /**
     * 프로필 설정 완료 처리
     */
    public void completeProfile() {
        this.profileCompleted = true;
        this.profileCompletedAt = LocalDateTime.now();
    }

    /**
     * COMPANY role로 업그레이드
     */
    public void upgradeToCompany() {
        this.roles = new String[]{"COMPANY"};
    }

    /**
     * Role 설정
     */
    public void setRoles(String[] roles) {
        this.roles = roles;
    }
}
