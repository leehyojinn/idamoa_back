package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUser(User user);

    Optional<UserProfile> findByUserAndIsDeletedFalse(User user);

    Optional<UserProfile> findByUserId(Long userId);
}
