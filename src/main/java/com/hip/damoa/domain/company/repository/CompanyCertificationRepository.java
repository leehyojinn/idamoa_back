package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyCertificationRepository extends JpaRepository<CompanyCertification, Long> {

    List<CompanyCertification> findByCompanyAndIsDeletedFalse(Company company);

    List<CompanyCertification> findByCompanyAndStatusAndIsDeletedFalse(Company company, String status);

    long countByCompanyAndStatusAndIsDeletedFalse(Company company, String status);
}
