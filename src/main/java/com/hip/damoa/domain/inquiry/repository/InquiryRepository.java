package com.hip.damoa.domain.inquiry.repository;

import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
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

    Page<Inquiry> findByStatusAndIsDeletedFalse(InquiryStatus status, Pageable pageable);

    // Admin Dashboard Statistics
    Long countByIsDeletedFalse();

    Long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime dateTime);

    @Query("SELECT COUNT(i) FROM Inquiry i WHERE CAST(i.status AS string) = :status AND i.isDeleted = false")
    Long countByStatusAndIsDeletedFalse(@Param("status") String status);

    @Query("SELECT CAST(i.inquiryType AS string) AS type, COUNT(i) AS count FROM Inquiry i " +
           "WHERE i.isDeleted = false GROUP BY i.inquiryType")
    Map<String, Long> countGroupByInquiryType();
}
