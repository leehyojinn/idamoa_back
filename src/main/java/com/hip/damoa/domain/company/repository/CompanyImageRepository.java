package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyImageRepository extends JpaRepository<CompanyImage, Long> {

    List<CompanyImage> findByCompanyAndIsDeletedFalseOrderByDisplayOrder(Company company);

    List<CompanyImage> findByCompanyAndIsDeletedFalseOrderByDisplayOrder(Company company, Pageable pageable);

    List<CompanyImage> findByCompanyAndImageTypeAndIsDeletedFalse(Company company, String imageType);

    Optional<CompanyImage> findByCompanyAndImageTypeAndIsPrimaryTrue(Company company, String imageType);

    long countByCompanyAndIsDeletedFalse(Company company);

    List<CompanyImage> findByCompany_IdAndIsDeletedFalse(Long companyId);
}
