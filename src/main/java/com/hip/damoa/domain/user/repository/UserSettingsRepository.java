package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUser(User user);

    Optional<UserSettings> findByUserAndIsDeletedFalse(User user);
}
