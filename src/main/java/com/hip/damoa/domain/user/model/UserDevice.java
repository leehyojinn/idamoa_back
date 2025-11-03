package com.hip.damoa.domain.user.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 디바이스 관리
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_devices", indexes = {
    @Index(name = "idx_user_devices_user_id", columnList = "user_id"),
    @Index(name = "idx_user_devices_device_token", columnList = "device_token")
})
public class UserDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_token", length = 500)
    private String deviceToken;

    @Column(name = "device_type", length = 20)
    private String deviceType; // IOS, ANDROID, WEB

    @Column(name = "device_name", length = 200)
    private String deviceName;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "browser_name", length = 100)
    private String browserName;

    @Column(name = "browser_version", length = 50)
    private String browserVersion;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // ===== Business Methods =====

    /**
     * 디바이스 사용 업데이트
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 디바이스 토큰 업데이트
     */
    public void updateToken(String deviceToken) {
        this.deviceToken = deviceToken;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 디바이스 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 디바이스 활성화
     */
    public void activate() {
        this.isActive = true;
    }
}
