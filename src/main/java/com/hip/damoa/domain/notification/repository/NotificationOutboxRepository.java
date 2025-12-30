package com.hip.damoa.domain.notification.repository;

import com.hip.damoa.domain.notification.model.NotificationOutbox;
import com.hip.damoa.domain.notification.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 알림 Outbox Repository
 */
@Repository
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    /**
     * UUID로 조회
     */
    Optional<NotificationOutbox> findByUuid(UUID uuid);

    /**
     * 발송 대기 중인 알림 조회 (스케줄러용)
     */
    @Query("SELECT o FROM NotificationOutbox o " +
           "WHERE o.status = :status " +
           "AND o.scheduledAt <= :now " +
           "ORDER BY o.scheduledAt ASC")
    List<NotificationOutbox> findPendingNotifications(@Param("status") OutboxStatus status,
                                                       @Param("now") LocalDateTime now);

    /**
     * 발송 대기 중인 알림 조회 및 잠금 (다중 인스턴스 동시성 제어)
     * FOR UPDATE SKIP LOCKED: 이미 잠긴 행은 건너뛰고 조회
     */
    @Query(value = """
        SELECT * FROM notification_outbox
        WHERE status = 'PENDING'
        AND scheduled_at <= :now
        ORDER BY scheduled_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<NotificationOutbox> findAndLockPendingNotifications(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit);

    /**
     * 디바운스 키로 대기 중인 알림 조회
     */
    @Query("SELECT o FROM NotificationOutbox o " +
           "WHERE o.debounceKey = :debounceKey " +
           "AND o.status = 'PENDING'")
    Optional<NotificationOutbox> findPendingByDebounceKey(@Param("debounceKey") String debounceKey);

    /**
     * 디바운스 키로 대기 중인 알림 취소
     */
    @Modifying
    @Query("UPDATE NotificationOutbox o " +
           "SET o.status = 'CANCELLED', o.processedAt = :now " +
           "WHERE o.debounceKey = :debounceKey " +
           "AND o.status = 'PENDING'")
    int cancelPendingByDebounceKey(@Param("debounceKey") String debounceKey,
                                   @Param("now") LocalDateTime now);

    /**
     * 특정 수신자의 특정 엔티티 관련 대기 알림 취소
     */
    @Modifying
    @Query("UPDATE NotificationOutbox o " +
           "SET o.status = 'CANCELLED', o.processedAt = :now " +
           "WHERE o.recipient.id = :recipientId " +
           "AND o.entityType = :entityType " +
           "AND o.entityId = :entityId " +
           "AND o.status = 'PENDING'")
    int cancelPendingByEntity(@Param("recipientId") Long recipientId,
                              @Param("entityType") String entityType,
                              @Param("entityId") Long entityId,
                              @Param("now") LocalDateTime now);

    /**
     * 재시도 가능한 실패 알림 조회
     */
    @Query("SELECT o FROM NotificationOutbox o " +
           "WHERE o.status = 'FAILED' " +
           "AND o.retryCount < o.maxRetries")
    List<NotificationOutbox> findRetryableFailedNotifications();

    /**
     * 특정 기간 이전의 완료된 알림 삭제 (정리용)
     */
    @Modifying
    @Query("DELETE FROM NotificationOutbox o " +
           "WHERE o.status IN ('SENT', 'CANCELLED', 'FAILED') " +
           "AND o.processedAt < :before")
    int deleteOldProcessedNotifications(@Param("before") LocalDateTime before);

    /**
     * 수신자별 대기 알림 수 조회
     */
    @Query("SELECT COUNT(o) FROM NotificationOutbox o " +
           "WHERE o.recipient.id = :recipientId " +
           "AND o.status = 'PENDING'")
    long countPendingByRecipientId(@Param("recipientId") Long recipientId);
}
