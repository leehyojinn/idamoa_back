package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyServiceRepository extends JpaRepository<CompanyService, Long> {

    List<CompanyService> findByCompanyAndIsDeletedFalseOrderByDisplayOrder(Company company);

    Page<CompanyService> findByCompanyAndIsActiveAndIsDeletedFalse(Company company, Boolean isActive, Pageable pageable);

    long countByCompanyAndIsActiveAndIsDeletedFalse(Company company, Boolean isActive);
}
