package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyReviewRepository extends JpaRepository<CompanyReview, Long> {

    Page<CompanyReview> findByCompanyAndIsDeletedFalse(Company company, Pageable pageable);

    Page<CompanyReview> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    Optional<CompanyReview> findByCompanyAndUserAndIsDeletedFalse(Company company, User user);

    long countByCompanyAndIsDeletedFalse(Company company);

    @Query("SELECT AVG(r.rating) FROM CompanyReview r WHERE r.company = :company AND r.isDeleted = false")
    Double getAverageRatingByCompany(@Param("company") Company company);
}
