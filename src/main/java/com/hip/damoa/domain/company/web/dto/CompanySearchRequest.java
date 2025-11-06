package com.hip.damoa.domain.company.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

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
     * 필터 옵션 ID 목록 (전문영역, 진료과, 작업평수 등)
     * 예: 피부과(1) + 마케팅(5) + 서울(10) 검색 시 [1, 5, 10]
     */
    private List<Long> filterOptionIds;

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
