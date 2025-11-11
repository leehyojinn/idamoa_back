package com.hip.damoa.domain.notification.repository;

import com.hip.damoa.domain.notification.model.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    // Find by code
    Optional<NotificationTemplate> findByCode(String code);

    // Find by code and channel
    Optional<NotificationTemplate> findByCodeAndChannel(String code, String channel);

    // Find by name
    Optional<NotificationTemplate> findByName(String name);

    // Find by channel
    List<NotificationTemplate> findByChannel(String channel);

    Page<NotificationTemplate> findByChannel(String channel, Pageable pageable);

    // Find active templates
    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
           "ORDER BY t.name ASC")
    List<NotificationTemplate> findActiveTemplates();

    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true " +
           "ORDER BY t.name ASC")
    Page<NotificationTemplate> findActiveTemplates(Pageable pageable);

    // Find by channel and active status
    @Query("SELECT t FROM NotificationTemplate t WHERE t.channel = :channel " +
           "AND t.isActive = true")
    List<NotificationTemplate> findActiveByChannel(@Param("channel") String channel);

    // Count by channel
    long countByChannel(String channel);

    // Count active templates
    long countByIsActiveTrue();

}
