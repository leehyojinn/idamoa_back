package com.hip.damoa.domain.inquiry.repository;

import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import com.hip.damoa.domain.inquiry.model.InquiryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Optional<Inquiry> findByUuidAndIsDeletedFalse(UUID uuid);

    Page<Inquiry> findByIsDeletedFalse(Pageable pageable);

    // ===== 관리자용 쿼리 (삭제된 데이터 포함) =====

    Optional<Inquiry> findByUuid(UUID uuid);

    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Inquiry> findByStatusAndIsDeletedFalse(InquiryStatus status, Pageable pageable);

    // User specific queries for general inquiries
    Page<Inquiry> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Optional<Inquiry> findByUuidAndUserIdAndIsDeletedFalse(UUID uuid, Long userId);

    // Admin Dashboard Statistics
    Long countByIsDeletedFalse();

    Long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime dateTime);

    @Query("SELECT COUNT(i) FROM Inquiry i WHERE CAST(i.status AS string) = :status AND i.isDeleted = false")
    Long countByStatusAndIsDeletedFalse(@Param("status") String status);

    @Query("SELECT CAST(i.inquiryType AS string) AS type, COUNT(i) AS count FROM Inquiry i " +
           "WHERE i.isDeleted = false GROUP BY i.inquiryType")
    Map<String, Long> countGroupByInquiryType();

    // Search functionality - 기본 검색 (hasAnswer 조건 없음)
    @Query("SELECT DISTINCT i FROM Inquiry i " +
           "WHERE i.isDeleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inquiryType IS NULL OR i.inquiryType = :inquiryType) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:userEmail IS NULL OR :userEmail = '' OR i.user.email = :userEmail)")
    Page<Inquiry> searchInquiriesBasic(@Param("keyword") String keyword,
                                        @Param("inquiryType") InquiryType inquiryType,
                                        @Param("status") InquiryStatus status,
                                        @Param("userEmail") String userEmail,
                                        Pageable pageable);

    // Search functionality - 답변 있는 문의만
    @Query("SELECT DISTINCT i FROM Inquiry i " +
           "WHERE i.isDeleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inquiryType IS NULL OR i.inquiryType = :inquiryType) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:userEmail IS NULL OR :userEmail = '' OR i.user.email = :userEmail) " +
           "AND EXISTS (SELECT 1 FROM InquiryAnswer a WHERE a.inquiry.id = i.id AND a.isDeleted = false)")
    Page<Inquiry> searchInquiriesWithAnswer(@Param("keyword") String keyword,
                                             @Param("inquiryType") InquiryType inquiryType,
                                             @Param("status") InquiryStatus status,
                                             @Param("userEmail") String userEmail,
                                             Pageable pageable);

    // Search functionality - 답변 없는 문의만
    @Query("SELECT DISTINCT i FROM Inquiry i " +
           "WHERE i.isDeleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inquiryType IS NULL OR i.inquiryType = :inquiryType) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:userEmail IS NULL OR :userEmail = '' OR i.user.email = :userEmail) " +
           "AND NOT EXISTS (SELECT 1 FROM InquiryAnswer a WHERE a.inquiry.id = i.id AND a.isDeleted = false)")
    Page<Inquiry> searchInquiriesWithoutAnswer(@Param("keyword") String keyword,
                                                @Param("inquiryType") InquiryType inquiryType,
                                                @Param("status") InquiryStatus status,
                                                @Param("userEmail") String userEmail,
                                                Pageable pageable);

    // ===== 관리자용 검색 쿼리 (삭제된 데이터 포함) =====

    // Admin Search - 기본 검색 (삭제된 데이터 포함)
    @Query("SELECT DISTINCT i FROM Inquiry i " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inquiryType IS NULL OR i.inquiryType = :inquiryType) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:userEmail IS NULL OR :userEmail = '' OR i.user.email = :userEmail)")
    Page<Inquiry> adminSearchInquiriesBasic(@Param("keyword") String keyword,
                                             @Param("inquiryType") InquiryType inquiryType,
                                             @Param("status") InquiryStatus status,
                                             @Param("userEmail") String userEmail,
                                             Pageable pageable);

    // Admin Search - 답변 있는 문의만 (삭제된 데이터 포함)
    @Query("SELECT DISTINCT i FROM Inquiry i " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inquiryType IS NULL OR i.inquiryType = :inquiryType) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:userEmail IS NULL OR :userEmail = '' OR i.user.email = :userEmail) " +
           "AND EXISTS (SELECT 1 FROM InquiryAnswer a WHERE a.inquiry.id = i.id)")
    Page<Inquiry> adminSearchInquiriesWithAnswer(@Param("keyword") String keyword,
                                                  @Param("inquiryType") InquiryType inquiryType,
                                                  @Param("status") InquiryStatus status,
                                                  @Param("userEmail") String userEmail,
                                                  Pageable pageable);

    // Admin Search - 답변 없는 문의만 (삭제된 데이터 포함)
    @Query("SELECT DISTINCT i FROM Inquiry i " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inquiryType IS NULL OR i.inquiryType = :inquiryType) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:userEmail IS NULL OR :userEmail = '' OR i.user.email = :userEmail) " +
           "AND NOT EXISTS (SELECT 1 FROM InquiryAnswer a WHERE a.inquiry.id = i.id)")
    Page<Inquiry> adminSearchInquiriesWithoutAnswer(@Param("keyword") String keyword,
                                                     @Param("inquiryType") InquiryType inquiryType,
                                                     @Param("status") InquiryStatus status,
                                                     @Param("userEmail") String userEmail,
                                                     Pageable pageable);

    // User-specific search
    @Query("SELECT DISTINCT i FROM Inquiry i " +
           "WHERE i.isDeleted = false " +
           "AND i.user.id = :userId " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(i.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:inquiryType IS NULL OR i.inquiryType = :inquiryType) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:startDate IS NULL OR i.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR i.createdAt <= :endDate)")
    Page<Inquiry> searchMyInquiries(@Param("userId") Long userId,
                                     @Param("keyword") String keyword,
                                     @Param("inquiryType") InquiryType inquiryType,
                                     @Param("status") InquiryStatus status,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);
}
