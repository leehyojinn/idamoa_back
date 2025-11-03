package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

    Page<UserActivityLog> findByUser(User user, Pageable pageable);

    Page<UserActivityLog> findByUserAndEventType(User user, String eventType, Pageable pageable);

    List<UserActivityLog> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    long countByUserAndEventType(User user, String eventType);
}
