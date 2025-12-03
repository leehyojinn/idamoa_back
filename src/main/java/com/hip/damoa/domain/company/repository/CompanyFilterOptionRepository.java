package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyFilterOption;
import com.hip.damoa.domain.filter.model.FilterOption;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ==================== 마이그레이션 관련 메서드 ====================

    /**
     * 특정 필터 옵션을 사용하는 모든 레코드 조회
     */
    List<CompanyFilterOption> findByFilterOption(FilterOption filterOption);

    /**
     * 특정 필터 옵션을 사용하는 업체 수 조회
     */
    @Query("SELECT COUNT(DISTINCT cfo.company.id) FROM CompanyFilterOption cfo WHERE cfo.filterOption = :filterOption")
    int countDistinctCompanyByFilterOption(@Param("filterOption") FilterOption filterOption);

    /**
     * 특정 필터 옵션을 사용하는 업체 ID 목록 조회
     */
    @Query("SELECT DISTINCT cfo.company.id FROM CompanyFilterOption cfo WHERE cfo.filterOption = :filterOption")
    List<Long> findDistinctCompanyIdsByFilterOption(@Param("filterOption") FilterOption filterOption);

    /**
     * 특정 업체가 특정 필터 옵션을 가지고 있는지 확인
     */
    boolean existsByCompanyAndFilterOption(Company company, FilterOption filterOption);

    /**
     * 특정 업체의 특정 필터 옵션 삭제
     */
    void deleteByCompanyAndFilterOption(Company company, FilterOption filterOption);

    /**
     * 소스 옵션을 사용하면서 타겟 옵션도 이미 사용하는 업체 ID 목록 조회 (중복)
     */
    @Query("""
        SELECT DISTINCT cfo1.company.id
        FROM CompanyFilterOption cfo1
        WHERE cfo1.filterOption = :sourceOption
        AND EXISTS (
            SELECT 1 FROM CompanyFilterOption cfo2
            WHERE cfo2.company = cfo1.company
            AND cfo2.filterOption = :targetOption
        )
    """)
    List<Long> findCompanyIdsWithBothOptions(
            @Param("sourceOption") FilterOption sourceOption,
            @Param("targetOption") FilterOption targetOption
    );

    /**
     * 특정 필터 옵션을 사용하는 업체 샘플 조회 (최대 N개)
     */
    @Query("""
        SELECT cfo FROM CompanyFilterOption cfo
        JOIN FETCH cfo.company
        WHERE cfo.filterOption = :filterOption
    """)
    List<CompanyFilterOption> findByFilterOptionWithCompany(
            @Param("filterOption") FilterOption filterOption,
            Pageable pageable
    );

    /**
     * 벌크 업데이트: 소스 옵션을 타겟 옵션으로 변경 (중복 제외)
     */
    @Modifying
    @Query("""
        UPDATE CompanyFilterOption cfo
        SET cfo.filterOption = :targetOption
        WHERE cfo.filterOption = :sourceOption
        AND cfo.company.id NOT IN :excludeCompanyIds
    """)
    int bulkUpdateFilterOption(
            @Param("sourceOption") FilterOption sourceOption,
            @Param("targetOption") FilterOption targetOption,
            @Param("excludeCompanyIds") List<Long> excludeCompanyIds
    );

    /**
     * 중복인 경우 소스 옵션 레코드 삭제
     */
    @Modifying
    @Query("""
        DELETE FROM CompanyFilterOption cfo
        WHERE cfo.filterOption = :sourceOption
        AND cfo.company.id IN :companyIds
    """)
    int bulkDeleteByFilterOptionAndCompanyIds(
            @Param("sourceOption") FilterOption sourceOption,
            @Param("companyIds") List<Long> companyIds
    );
}
