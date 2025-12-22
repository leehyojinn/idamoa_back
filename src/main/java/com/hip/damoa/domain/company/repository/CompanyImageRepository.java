package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyImage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyImageRepository extends JpaRepository<CompanyImage, Long> {

    Optional<CompanyImage> findByUuidAndIsDeletedFalse(UUID uuid);

    List<CompanyImage> findByCompanyAndIsDeletedFalseOrderByDisplayOrder(Company company);

    List<CompanyImage> findByCompanyAndIsDeletedFalseOrderByDisplayOrder(Company company, Pageable pageable);

    List<CompanyImage> findByCompanyAndImageTypeAndIsDeletedFalse(Company company, String imageType);

    Optional<CompanyImage> findByCompanyAndImageTypeAndIsPrimaryTrue(Company company, String imageType);

    long countByCompanyAndIsDeletedFalse(Company company);

    List<CompanyImage> findByCompany_IdAndIsDeletedFalse(Long companyId);

    /**
     * 여러 업체의 특정 타입 이미지 일괄 조회 (N+1 방지용)
     */
    @Query("SELECT ci FROM CompanyImage ci " +
           "WHERE ci.company.id IN :companyIds " +
           "AND ci.imageType IN :imageTypes " +
           "AND ci.isDeleted = false " +
           "ORDER BY ci.displayOrder ASC")
    List<CompanyImage> findByCompanyIdInAndImageTypeInAndIsDeletedFalse(
            @Param("companyIds") Collection<Long> companyIds,
            @Param("imageTypes") Collection<String> imageTypes);

    /**
     * 여러 업체의 모든 이미지 일괄 조회 (N+1 방지용)
     */
    @Query("SELECT ci FROM CompanyImage ci " +
           "WHERE ci.company.id IN :companyIds " +
           "AND ci.isDeleted = false " +
           "ORDER BY ci.company.id, ci.displayOrder ASC")
    List<CompanyImage> findByCompanyIdInAndIsDeletedFalse(
            @Param("companyIds") Collection<Long> companyIds);
}
