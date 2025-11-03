package com.hip.damoa.domain.notification.repository;

import com.hip.damoa.domain.notification.model.NotificationSettings;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    // Find by user
    Optional<NotificationSettings> findByUser(User user);

    // Find users with email enabled
    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.emailEnabled = true")
    List<NotificationSettings> findUsersWithEmailEnabled();

    // Find users with SMS enabled
    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.smsEnabled = true")
    List<NotificationSettings> findUsersWithSmsEnabled();

    // Find users with push enabled
    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.pushEnabled = true")
    List<NotificationSettings> findUsersWithPushEnabled();

    // Find users with marketing email consent
    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.marketingEmail = true")
    List<NotificationSettings> findUsersWithMarketingEmailConsent();

    // Find users with marketing SMS consent
    @Query("SELECT ns FROM NotificationSettings ns WHERE ns.marketingSms = true")
    List<NotificationSettings> findUsersWithMarketingSmsConsent();

    // Find users with specific notification type enabled
    @Query("SELECT ns FROM NotificationSettings ns WHERE " +
           "(:type = 'estimate' AND ns.estimateNotification = true) OR " +
           "(:type = 'match' AND ns.matchNotification = true) OR " +
           "(:type = 'payment' AND ns.paymentNotification = true) OR " +
           "(:type = 'review' AND ns.reviewNotification = true)")
    List<NotificationSettings> findUsersWithNotificationTypeEnabled(@Param("type") String type);

    // Count users with email enabled
    long countByEmailEnabledTrue();

    // Count users with SMS enabled
    long countBySmsEnabledTrue();

    // Count users with push enabled
    long countByPushEnabledTrue();

    // Count users with marketing consent
    long countByMarketingEmailTrueOrMarketingSmsTrue();

    // Check if settings exist for user
    boolean existsByUser(User user);
}
