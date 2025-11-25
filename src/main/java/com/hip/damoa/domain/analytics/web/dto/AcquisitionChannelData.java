package com.hip.damoa.domain.analytics.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 획득 채널 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquisitionChannelData {

    @Schema(
            description = "소스\n" +
                    "사용자가 유입된 소스를 나타냅니다.\n" +
                    "예: google (구글 검색), facebook (페이스북), direct (직접 방문), naver (네이버) 등",
            example = "google"
    )
    private String source;

    @Schema(
            description = "매체\n" +
                    "트래픽 매체 유형을 나타냅니다.\n" +
                    "예: organic (자연 검색), cpc (클릭당 과금 광고), referral (추천), social (소셜 미디어) 등",
            example = "organic"
    )
    private String medium;

    @Schema(
            description = "세션 수\n" +
                    "해당 소스/매체를 통해 발생한 총 세션 수입니다.\n" +
                    "세션은 사용자의 사이트 방문을 의미합니다.",
            example = "500"
    )
    private Long sessions;

    @Schema(
            description = "신규 사용자 수\n" +
                    "해당 소스/매체를 통해 처음 방문한 사용자 수입니다.\n" +
                    "신규 방문자를 추적하여 마케팅 효과를 측정할 수 있습니다.",
            example = "350"
    )
    private Long newUsers;

    @Schema(
            description = "참여율\n" +
                    "사용자가 사이트에 참여한 비율입니다. (0.0 ~ 1.0)\n" +
                    "10초 이상 머물거나, 2개 이상의 페이지를 조회하거나, 전환 이벤트가 발생한 세션의 비율을 나타냅니다.",
            example = "0.75"
    )
    private Double engagementRate;
}
