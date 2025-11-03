package com.hip.damoa.domain.notification.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 알림 설정
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_settings", indexes = {
    @Index(name = "idx_notification_settings_user_id", columnList = "user_id")
})
public class NotificationSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = true;

    @Column(name = "kakao_enabled", nullable = false)
    @Builder.Default
    private Boolean kakaoEnabled = false;

    @Column(name = "marketing_email", nullable = false)
    @Builder.Default
    private Boolean marketingEmail = false;

    @Column(name = "marketing_sms", nullable = false)
    @Builder.Default
    private Boolean marketingSms = false;

    @Column(name = "estimate_notification", nullable = false)
    @Builder.Default
    private Boolean estimateNotification = true;

    @Column(name = "match_notification", nullable = false)
    @Builder.Default
    private Boolean matchNotification = true;

    @Column(name = "payment_notification", nullable = false)
    @Builder.Default
    private Boolean paymentNotification = true;

    @Column(name = "review_notification", nullable = false)
    @Builder.Default
    private Boolean reviewNotification = true;

    public void updateEmailEnabled(Boolean enabled) {
        this.emailEnabled = enabled;
    }

    public void updateSmsEnabled(Boolean enabled) {
        this.smsEnabled = enabled;
    }

    public void updatePushEnabled(Boolean enabled) {
        this.pushEnabled = enabled;
    }

    public void updateKakaoEnabled(Boolean enabled) {
        this.kakaoEnabled = enabled;
    }

    public void updateMarketingConsent(Boolean email, Boolean sms) {
        this.marketingEmail = email;
        this.marketingSms = sms;
    }
}
