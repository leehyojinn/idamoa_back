package com.hip.damoa.domain.notification.repository;

import com.hip.damoa.domain.notification.model.Notification;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find by recipient
    List<Notification> findByRecipient(User recipient);

    Page<Notification> findByRecipient(User recipient, Pageable pageable);

    // Find by recipient ordered by created date
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientOrderByCreatedAt(@Param("recipient") User recipient,
                                                        Pageable pageable);

    // Find unread by recipient
    @Query("SELECT n FROM Notification n WHERE n.recipient = :recipient " +
           "AND n.isRead = false " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByRecipient(@Param("recipient") User recipient);

    Page<Notification> findUnreadByRecipient(@Param("recipient") User recipient, Pageable pageable);

    // Find by channel
    List<Notification> findByChannel(String channel);

    Page<Notification> findByChannel(String channel, Pageable pageable);

    // Find by recipient and channel
    List<Notification> findByRecipientAndChannel(User recipient, String channel);

    // Find by status
    List<Notification> findByStatus(String status);

    Page<Notification> findByStatus(String status, Pageable pageable);

    // Find pending notifications
    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' " +
           "ORDER BY n.createdAt ASC")
    List<Notification> findPendingNotifications();

    // Find by date range
    @Query("SELECT n FROM Notification n WHERE n.createdAt >= :startDate " +
           "AND n.createdAt <= :endDate " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Count unread by recipient
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient = :recipient " +
           "AND n.isRead = false")
    long countUnreadByRecipient(@Param("recipient") User recipient);

    // Count by status
    long countByStatus(String status);

    // Count by channel
    long countByChannel(String channel);

    // Delete old read notifications
    @Query("DELETE FROM Notification n WHERE n.isRead = true " +
           "AND n.readAt < :beforeDate")
    void deleteOldReadNotifications(@Param("beforeDate") LocalDateTime beforeDate);
}
