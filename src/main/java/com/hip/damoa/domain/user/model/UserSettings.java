package com.hip.damoa.domain.user.model;

import com.hip.damoa.domain.common.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 개인 설정
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_settings")
public class UserSettings extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Type(JsonBinaryType.class)
    @Column(name = "notification_settings", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> notificationSettings = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(name = "preferences", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> preferences = new HashMap<>();

    @Type(JsonBinaryType.class)
    @Column(name = "ui_settings", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> uiSettings = new HashMap<>();

    @Column(name = "language", length = 10, nullable = false)
    @Builder.Default
    private String language = "ko";

    @Column(name = "timezone", length = 50, nullable = false)
    @Builder.Default
    private String timezone = "Asia/Seoul";

    // ===== Business Methods =====

    /**
     * 알림 설정 업데이트
     */
    public void updateNotificationSetting(String key, Object value) {
        if (this.notificationSettings == null) {
            this.notificationSettings = new HashMap<>();
        }
        this.notificationSettings.put(key, value);
    }

    /**
     * 사용자 선호도 업데이트
     */
    public void updatePreference(String key, Object value) {
        if (this.preferences == null) {
            this.preferences = new HashMap<>();
        }
        this.preferences.put(key, value);
    }

    /**
     * UI 설정 업데이트
     */
    public void updateUiSetting(String key, Object value) {
        if (this.uiSettings == null) {
            this.uiSettings = new HashMap<>();
        }
        this.uiSettings.put(key, value);
    }

    /**
     * 언어 변경
     */
    public void changeLanguage(String language) {
        this.language = language;
    }

    /**
     * 타임존 변경
     */
    public void changeTimezone(String timezone) {
        this.timezone = timezone;
    }
}
