package com.hip.damoa.domain.notification.repository;

import com.hip.damoa.domain.notification.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    // Find by email
    List<EmailVerification> findByEmail(String email);

    // Find by verification token
    Optional<EmailVerification> findByVerificationToken(String verificationToken);

    // Find latest by email and purpose
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email " +
           "AND v.purpose = :purpose " +
           "ORDER BY v.createdAt DESC LIMIT 1")
    Optional<EmailVerification> findLatestByEmailAndPurpose(@Param("email") String email,
                                                              @Param("purpose") String purpose);

    // Find by email and status
    List<EmailVerification> findByEmailAndStatus(String email, String status);

    // Find pending by email
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email " +
           "AND v.status = 'PENDING' " +
           "AND v.expiresAt > :now " +
           "ORDER BY v.createdAt DESC")
    List<EmailVerification> findPendingByEmail(@Param("email") String email,
                                                 @Param("now") LocalDateTime now);

    // Find by status
    List<EmailVerification> findByStatus(String status);

    // Find expired verifications
    @Query("SELECT v FROM EmailVerification v WHERE v.status = 'PENDING' " +
           "AND v.expiresAt < :now")
    List<EmailVerification> findExpiredVerifications(@Param("now") LocalDateTime now);

    // Find by purpose
    List<EmailVerification> findByPurpose(String purpose);

    // Find verified by email
    @Query("SELECT v FROM EmailVerification v WHERE v.email = :email " +
           "AND v.status = 'VERIFIED' " +
           "ORDER BY v.verifiedAt DESC")
    List<EmailVerification> findVerifiedByEmail(@Param("email") String email);

    // Count by email and date range
    @Query("SELECT COUNT(v) FROM EmailVerification v WHERE v.email = :email " +
           "AND v.createdAt >= :startDate")
    long countByEmailSince(@Param("email") String email,
                           @Param("startDate") LocalDateTime startDate);

    // Count by status
    long countByStatus(String status);

    // Count verified
    long countByStatusAndPurpose(String status, String purpose);

    // Check if token exists and is valid
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END " +
           "FROM EmailVerification v WHERE v.verificationToken = :token " +
           "AND v.status = 'PENDING' " +
           "AND v.expiresAt > :now")
    boolean isTokenValid(@Param("token") String token,
                         @Param("now") LocalDateTime now);

    // Delete old verifications
    @Query("DELETE FROM EmailVerification v WHERE v.createdAt < :beforeDate")
    void deleteOldVerifications(@Param("beforeDate") LocalDateTime beforeDate);
}
