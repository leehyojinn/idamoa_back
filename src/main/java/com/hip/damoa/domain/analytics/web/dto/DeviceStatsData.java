package com.hip.damoa.domain.analytics.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기기별 통계 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatsData {

    @Schema(
            description = "기기 유형\n" +
                    "사용자가 접속한 기기의 카테고리를 나타냅니다.\n" +
                    "mobile (모바일), desktop (데스크톱), tablet (태블릿) 중 하나입니다.",
            example = "mobile"
    )
    private String deviceCategory;

    @Schema(
            description = "세션 수\n" +
                    "해당 기기에서 발생한 총 세션 수입니다.\n" +
                    "세션은 사용자의 사이트 방문을 의미합니다.",
            example = "1000"
    )
    private Long sessions;

    @Schema(
            description = "사용자 수\n" +
                    "해당 기기를 사용한 고유 사용자 수입니다.\n" +
                    "중복을 제거한 실제 방문자 수를 나타냅니다.",
            example = "800"
    )
    private Long users;

    @Schema(
            description = "이탈률\n" +
                    "사용자가 한 페이지만 보고 떠난 비율입니다. (0.0 ~ 1.0)\n" +
                    "이탈률이 높을수록 사용자가 사이트에 관심이 적다는 의미입니다.",
            example = "0.45"
    )
    private Double bounceRate;
}
