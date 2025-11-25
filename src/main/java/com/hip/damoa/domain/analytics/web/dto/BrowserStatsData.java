package com.hip.damoa.domain.analytics.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브라우저별 통계 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserStatsData {

    @Schema(
            description = "브라우저\n" +
                    "사용자가 사용한 웹 브라우저 이름입니다.\n" +
                    "예: Chrome (크롬), Safari (사파리), Firefox (파이어폭스), Edge (엣지) 등",
            example = "Chrome"
    )
    private String browser;

    @Schema(
            description = "세션 수\n" +
                    "해당 브라우저에서 발생한 총 세션 수입니다.\n" +
                    "세션은 사용자의 사이트 방문을 의미합니다.",
            example = "800"
    )
    private Long sessions;

    @Schema(
            description = "사용자 수\n" +
                    "해당 브라우저를 사용한 고유 사용자 수입니다.\n" +
                    "중복을 제거한 실제 방문자 수를 나타냅니다.",
            example = "650"
    )
    private Long users;
}
