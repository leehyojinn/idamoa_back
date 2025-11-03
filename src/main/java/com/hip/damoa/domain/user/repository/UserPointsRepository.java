package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserPoints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPointsRepository extends JpaRepository<UserPoints, Long> {

    Optional<UserPoints> findByUser(User user);

    Optional<UserPoints> findByUserAndIsDeletedFalse(User user);

    Page<UserPoints> findByTierAndIsDeletedFalse(String tier, Pageable pageable);

    Page<UserPoints> findByIsDeletedFalseOrderByAvailablePointsDesc(Pageable pageable);
}
