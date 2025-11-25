package com.hip.damoa.domain.analytics.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실시간 활성 사용자 수 Response
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeUsersResponse {

    private Long activeUsers; // 현재 실시간 활성 사용자 수
}
