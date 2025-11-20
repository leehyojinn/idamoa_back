package com.hip.damoa.domain.inquiry.repository;

import com.hip.damoa.domain.inquiry.model.Inquiry;
import com.hip.damoa.domain.inquiry.model.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Optional<Inquiry> findByUuidAndIsDeletedFalse(UUID uuid);

    Page<Inquiry> findByIsDeletedFalse(Pageable pageable);

    Page<Inquiry> findByStatusAndIsDeletedFalse(InquiryStatus status, Pageable pageable);
}
