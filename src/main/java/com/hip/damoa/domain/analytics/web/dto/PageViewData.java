package com.hip.damoa.domain.analytics.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 페이지별 조회수 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageViewData {

    private String pagePath;          // 페이지 경로
    private Long screenPageViews;     // 페이지 뷰
    private Long activeUsers;         // 고유 사용자
    private Double avgSessionDuration; // 평균 체류 시간
}
