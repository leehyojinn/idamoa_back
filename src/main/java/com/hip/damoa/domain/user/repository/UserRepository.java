package com.hip.damoa.domain.user.repository;

import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    // Admin Dashboard Statistics
    Long countByIsDeletedFalse();

    Long countByStatusAndIsDeletedFalse(UserStatus status);

    Long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime dateTime);

    @Query(value = "SELECT COUNT(*) FROM users u WHERE :role = ANY(u.roles) AND u.is_deleted = false", nativeQuery = true)
    Long countByRolesContainingAndIsDeletedFalse(@Param("role") String role);

    Long countByEmailVerifiedTrueAndIsDeletedFalse();

    Long countByPhoneVerifiedTrueAndIsDeletedFalse();

    Long countByIdentityVerifiedTrueAndIsDeletedFalse();

    Long countByMarketingAgreedTrueAndIsDeletedFalse();

    // Admin User Management
    Optional<User> findByUuidAndIsDeletedFalse(UUID uuid);

    Page<User> findByIsDeletedFalse(Pageable pageable);

    Page<User> findByStatusAndIsDeletedFalse(UserStatus status, Pageable pageable);

    /**
     * 관리자용 회원 검색
     * - keyword: 이메일 검색
     * - status: 상태 필터
     * - role: 역할 필터 (배열 내 검색)
     */
    @Query(value = """
        SELECT u.* FROM users u
        WHERE u.is_deleted = false
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:status IS NULL OR :status = '' OR u.status = :status)
        AND (:role IS NULL OR :role = '' OR :role = ANY(u.roles))
        ORDER BY u.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM users u
        WHERE u.is_deleted = false
        AND (:keyword IS NULL OR :keyword = '' OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:status IS NULL OR :status = '' OR u.status = :status)
        AND (:role IS NULL OR :role = '' OR :role = ANY(u.roles))
        """,
        nativeQuery = true)
    Page<User> searchForAdmin(
        @Param("keyword") String keyword,
        @Param("status") String status,
        @Param("role") String role,
        Pageable pageable
    );
}
