package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    long countByCreatedAtAfter(LocalDateTime dateTime);
}
