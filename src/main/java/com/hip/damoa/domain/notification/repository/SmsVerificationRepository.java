package com.hip.damoa.domain.notification.repository;

import com.hip.damoa.domain.notification.model.SmsVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsVerificationRepository extends JpaRepository<SmsVerification, Long> {

    // Find by phone number
    List<SmsVerification> findByPhoneNumber(String phoneNumber);

    // Find latest by phone number and purpose
    @Query("SELECT v FROM SmsVerification v WHERE v.phoneNumber = :phoneNumber " +
           "AND v.purpose = :purpose " +
           "ORDER BY v.createdAt DESC LIMIT 1")
    Optional<SmsVerification> findLatestByPhoneNumberAndPurpose(@Param("phoneNumber") String phoneNumber,
                                                                  @Param("purpose") String purpose);

    // Find by phone number and status
    List<SmsVerification> findByPhoneNumberAndStatus(String phoneNumber, String status);

    // Find pending by phone number
    @Query("SELECT v FROM SmsVerification v WHERE v.phoneNumber = :phoneNumber " +
           "AND v.status = 'PENDING' " +
           "AND v.expiresAt > :now " +
           "ORDER BY v.createdAt DESC")
    List<SmsVerification> findPendingByPhoneNumber(@Param("phoneNumber") String phoneNumber,
                                                     @Param("now") LocalDateTime now);

    // Find by status
    List<SmsVerification> findByStatus(String status);

    // Find expired verifications
    @Query("SELECT v FROM SmsVerification v WHERE v.status = 'PENDING' " +
           "AND v.expiresAt < :now")
    List<SmsVerification> findExpiredVerifications(@Param("now") LocalDateTime now);

    // Find by purpose
    List<SmsVerification> findByPurpose(String purpose);

    // Find verified by phone number
    @Query("SELECT v FROM SmsVerification v WHERE v.phoneNumber = :phoneNumber " +
           "AND v.status = 'VERIFIED' " +
           "ORDER BY v.verifiedAt DESC")
    List<SmsVerification> findVerifiedByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    // Count by phone number and date range
    @Query("SELECT COUNT(v) FROM SmsVerification v WHERE v.phoneNumber = :phoneNumber " +
           "AND v.createdAt >= :startDate")
    long countByPhoneNumberSince(@Param("phoneNumber") String phoneNumber,
                                  @Param("startDate") LocalDateTime startDate);

    // Count by status
    long countByStatus(String status);

    // Count verified
    long countByStatusAndPurpose(String status, String purpose);

    // Delete old verifications
    @Query("DELETE FROM SmsVerification v WHERE v.createdAt < :beforeDate")
    void deleteOldVerifications(@Param("beforeDate") LocalDateTime beforeDate);
}
