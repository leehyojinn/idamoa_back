package com.hip.damoa.domain.notification.repository;

import com.hip.damoa.domain.notification.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    // Find by provider
    List<NotificationLog> findByProvider(String provider);

    Page<NotificationLog> findByProvider(String provider, Pageable pageable);

    // Find by status
    List<NotificationLog> findByStatus(String status);

    Page<NotificationLog> findByStatus(String status, Pageable pageable);


    // Find by date range
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.createdAt >= :startDate " +
           "AND nl.createdAt <= :endDate " +
           "ORDER BY nl.createdAt DESC")
    List<NotificationLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.createdAt >= :startDate " +
           "AND nl.createdAt <= :endDate " +
           "ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    // Find failed logs
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.status = 'FAILED' " +
           "ORDER BY nl.createdAt DESC")
    List<NotificationLog> findFailedLogs();

    @Query("SELECT nl FROM NotificationLog nl WHERE nl.status = 'FAILED' " +
           "ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findFailedLogs(Pageable pageable);

    // Find by provider and status
    List<NotificationLog> findByProviderAndStatus(String provider, String status);

    // Find by provider and date range
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.provider = :provider " +
           "AND nl.createdAt >= :startDate AND nl.createdAt <= :endDate " +
           "ORDER BY nl.createdAt DESC")
    List<NotificationLog> findByProviderAndDateRange(@Param("provider") String provider,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    // Count by status
    long countByStatus(String status);

    // Count by provider
    long countByProvider(String provider);

    // Count by provider and status
    long countByProviderAndStatus(String provider, String status);

    // Count by date range
    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.createdAt >= :startDate " +
           "AND nl.createdAt <= :endDate")
    long countByDateRange(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);

    // Delete old logs
    @Query("DELETE FROM NotificationLog nl WHERE nl.createdAt < :beforeDate")
    void deleteOldLogs(@Param("beforeDate") LocalDateTime beforeDate);
}
