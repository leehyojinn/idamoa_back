package com.hip.damoa.domain.company.repository;

import com.hip.damoa.domain.company.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * 동적 정렬을 지원하는 Company Repository 커스텀 인터페이스
 */
public interface CustomCompanyRepository {

    /**
     * 업체 검색 (동적 정렬 지원)
     *
     * @param keyword 키워드 검색어
     * @param minRating 최소 평점
     * @param hasFilters 필터 적용 여부
     * @param filterOptionIds 필터 옵션 ID 배열
     * @param categoryCount 카테고리 개수 (AND 조건용)
     * @param sortBy 정렬 기준 (LATEST, POPULAR, RATING, REVIEW_COUNT, PREMIUM_TIER, AD_PRIORITY)
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    Page<Company> searchWithDynamicSort(
            String keyword,
            BigDecimal minRating,
            boolean hasFilters,
            List<Long> filterOptionIds,
            Integer categoryCount,
            String sortBy,
            Pageable pageable
    );
}
