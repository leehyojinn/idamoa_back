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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyReviewRepository extends JpaRepository<CompanyReview, Long> {

    Optional<CompanyReview> findByUuidAndIsDeletedFalse(UUID uuid);

    Page<CompanyReview> findByCompanyAndIsDeletedFalse(Company company, Pageable pageable);

    Page<CompanyReview> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    Optional<CompanyReview> findByCompanyAndUserAndIsDeletedFalse(Company company, User user);

    long countByCompanyAndIsDeletedFalse(Company company);

    @Query("SELECT AVG(r.rating) FROM CompanyReview r WHERE r.company = :company AND r.isDeleted = false")
    Double getAverageRatingByCompany(@Param("company") Company company);

    // 리뷰 조회 (status 필터링)
    Page<CompanyReview> findByCompanyAndStatusOrderByCreatedAtDesc(Company company, String status, Pageable pageable);

    List<CompanyReview> findByCompanyAndStatus(Company company, String status);

    // [N+1 최적화] 여러 Company의 평균 평점 일괄 조회
    @Query("SELECT r.company.id, COALESCE(AVG(r.rating), 0.0) " +
           "FROM CompanyReview r " +
           "WHERE r.company.id IN :companyIds " +
           "AND r.isDeleted = false AND r.status = 'PUBLISHED' " +
           "GROUP BY r.company.id")
    List<Object[]> getAverageRatingsRaw(@Param("companyIds") List<Long> companyIds);

    // [N+1 최적화] 여러 Company의 리뷰 수 일괄 조회
    @Query("SELECT r.company.id, COUNT(r.id) " +
           "FROM CompanyReview r " +
           "WHERE r.company.id IN :companyIds " +
           "AND r.isDeleted = false AND r.status = 'PUBLISHED' " +
           "GROUP BY r.company.id")
    List<Object[]> getReviewCountsRaw(@Param("companyIds") List<Long> companyIds);
}
