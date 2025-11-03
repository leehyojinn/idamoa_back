package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findByUserAndIsDeletedFalse(User user);

    Optional<UserDevice> findByDeviceTokenAndIsDeletedFalse(String deviceToken);

    Optional<UserDevice> findByUserAndDeviceTokenAndIsDeletedFalse(User user, String deviceToken);

    long countByUserAndIsDeletedFalse(User user);

    void deleteByUserAndDeviceToken(User user, String deviceToken);
}
