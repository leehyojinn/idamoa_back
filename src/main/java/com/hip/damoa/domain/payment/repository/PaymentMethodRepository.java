package com.hip.damoa.domain.payment.repository;

import com.hip.damoa.domain.payment.model.PaymentMethod;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    // Find by user
    List<PaymentMethod> findByUser(User user);

    // Find active by user
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user = :user " +
           "AND pm.isActive = true " +
           "ORDER BY pm.isDefault DESC, pm.createdAt DESC")
    List<PaymentMethod> findActiveByUser(@Param("user") User user);

    // Find default by user
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user = :user " +
           "AND pm.isDefault = true AND pm.isActive = true")
    Optional<PaymentMethod> findDefaultByUser(@Param("user") User user);

    // Find by user and method type
    List<PaymentMethod> findByUserAndMethodType(User user, String methodType);

    // Find by method type
    List<PaymentMethod> findByMethodType(String methodType);

    // Find by user and is default
    List<PaymentMethod> findByUserAndIsDefaultTrue(User user);

    // Count by user
    long countByUser(User user);

    // Count active by user
    @Query("SELECT COUNT(pm) FROM PaymentMethod pm WHERE pm.user = :user " +
           "AND pm.isActive = true")
    long countActiveByUser(@Param("user") User user);

    // Check if default exists for user
    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
           "FROM PaymentMethod pm WHERE pm.user = :user " +
           "AND pm.isDefault = true AND pm.isActive = true")
    boolean hasDefaultPaymentMethod(@Param("user") User user);
}
