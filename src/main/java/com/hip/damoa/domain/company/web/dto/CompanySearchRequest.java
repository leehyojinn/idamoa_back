package com.hip.damoa.domain.company.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 업체 검색 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySearchRequest {

    /**
     * 키워드 검색 (업체명, 설명)
     */
    private String keyword;

    /**
     * 서비스 지역 필터 (배열)
     */
    private String[] serviceAreas;

    /**
     * 태그 필터 (배열)
     */
    private String[] tags;

    /**
     * 최소 평점 필터
     */
    private BigDecimal minRating;

    /**
     * 정렬 기준
     * LATEST: 최신순
     * POPULAR: 인기순 (좋아요 + 조회수)
     * RATING: 평점순
     * REVIEW_COUNT: 후기순
     * PREMIUM_TIER: 프리미엄 등급순
     */
    private String sortBy;
}
