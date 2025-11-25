package com.hip.damoa.domain.analytics.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지역별 통계 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationStatsData {

    @Schema(
            description = "국가\n" +
                    "사용자가 접속한 국가명입니다.\n" +
                    "영문으로 표시됩니다. 예: South Korea (대한민국), United States (미국), Japan (일본) 등",
            example = "South Korea"
    )
    private String country;

    @Schema(
            description = "도시\n" +
                    "사용자가 접속한 도시명입니다.\n" +
                    "영문으로 표시됩니다. 예: Seoul (서울), Busan (부산), New York (뉴욕) 등",
            example = "Seoul"
    )
    private String city;

    @Schema(
            description = "세션 수\n" +
                    "해당 지역에서 발생한 총 세션 수입니다.\n" +
                    "세션은 사용자의 사이트 방문을 의미합니다.",
            example = "500"
    )
    private Long sessions;

    @Schema(
            description = "사용자 수\n" +
                    "해당 지역의 고유 사용자 수입니다.\n" +
                    "중복을 제거한 실제 방문자 수를 나타냅니다.",
            example = "400"
    )
    private Long users;
}
