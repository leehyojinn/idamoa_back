package com.hip.damoa.domain.file.repository;

import com.hip.damoa.domain.file.model.FilePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilePricingRepository extends JpaRepository<FilePricing, Long> {

    // File ID로 조회
    Optional<FilePricing> findByFileId(Long fileId);

    // 유료 파일 목록
    List<FilePricing> findByIsPaidTrueAndIsActiveTrue();

    // 활성화된 가격 정보
    List<FilePricing> findByIsActiveTrue();

    // 특정 가격대의 파일
    List<FilePricing> findByIsPaidTrueAndPriceBetween(Integer minPrice, Integer maxPrice);

    // 존재 여부
    boolean existsByFileId(Long fileId);

    // 통계
    long countByIsPaidTrue();

    long countByIsPaidTrueAndIsActiveTrue();
}
