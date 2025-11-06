package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyLike;
import com.hip.damoa.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyLikeRepository extends JpaRepository<CompanyLike, Long> {

    // Check if user liked company
    boolean existsByCompanyAndUser(Company company, User user);

    // Find like by company and user
    Optional<CompanyLike> findByCompanyAndUser(Company company, User user);

    // Count likes for company
    long countByCompany(Company company);

    // Delete like
    void deleteByCompanyAndUser(Company company, User user);
}
