package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyFilterOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 업체-필터 옵션 Repository
 */
public interface CompanyFilterOptionRepository extends JpaRepository<CompanyFilterOption, Long> {

    /**
     * 특정 업체의 모든 필터 옵션 조회
     */
    List<CompanyFilterOption> findByCompany(Company company);

    /**
     * 특정 업체의 필터 옵션 삭제
     */
    void deleteByCompany(Company company);

    /**
     * 특정 업체의 필터 옵션 개수
     */
    long countByCompany(Company company);
}
