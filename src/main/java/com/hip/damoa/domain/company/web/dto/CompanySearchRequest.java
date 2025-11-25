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
     * 통합 키워드 검색
     * 검색 대상: 업체명, 설명, 상세내용, 태그, 키워드, URL, 주소, 서비스지역 등
     * 모든 텍스트 필드에서 통합 검색
     */
    private String keyword;

    /**
     * 최소 평점 필터
     */
    private BigDecimal minRating;

    /**
     * 카테고리별 필터 옵션 ID 목록
     * Map<CategoryId, List<OptionIds>>
     *
     * 예시:
     * - categoryId 1 (지역): [서울(1), 경기(2)]
     * - categoryId 2 (업종): [인테리어(10), 마케팅(11)]
     * - categoryId 3 (진료과): [피부과(20), 산부인과(21)]
     *
     * 로직:
     * - 같은 카테고리 내: OR 조건 (서울 OR 경기)
     * - 다른 카테고리 간: AND 조건 ((서울 OR 경기) AND (인테리어 OR 마케팅))
     *
     * 클라이언트에서 보내는 형식:
     * filtersByCategory: {
     *   "1": [1, 2],      // 지역 카테고리
     *   "2": [10, 11],    // 업종 카테고리
     *   "3": [20, 21]     // 진료과 카테고리
     * }
     */
    private java.util.Map<Long, List<Long>> filtersByCategory;

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
