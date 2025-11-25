package com.hip.damoa.domain.analytics.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 일별 트래픽 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrafficData {

    private String date;              // 날짜
    private Long activeUsers;         // 활성 사용자
    private Long sessions;            // 세션 수
    private Long screenPageViews;     // 페이지 뷰
    private Double avgSessionDuration; // 평균 세션 시간
}
