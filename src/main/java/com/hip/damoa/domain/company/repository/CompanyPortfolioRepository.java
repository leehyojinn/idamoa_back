package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyPortfolioRepository extends JpaRepository<CompanyPortfolio, Long> {

    Page<CompanyPortfolio> findByCompanyAndIsDeletedFalse(Company company, Pageable pageable);

    List<CompanyPortfolio> findByCompanyAndFeaturedAndIsDeletedFalse(Company company, Boolean featured);

    long countByCompanyAndIsDeletedFalse(Company company);
}
