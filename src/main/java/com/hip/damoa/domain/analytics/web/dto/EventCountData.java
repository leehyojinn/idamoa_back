package com.hip.damoa.domain.analytics.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이벤트 카운트 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCountData {

    @Schema(
            description = "이벤트 이름\n" +
                    "Google Analytics에서 발생한 이벤트의 이름입니다.\n" +
                    "예: page_view (페이지 조회), click (클릭), form_submit (폼 제출) 등",
            example = "page_view"
    )
    private String eventName;

    @Schema(
            description = "이벤트 카운트\n" +
                    "해당 이벤트가 발생한 총 횟수입니다.\n" +
                    "기간 내 누적된 이벤트 발생 건수를 나타냅니다.",
            example = "1234"
    )
    private Long eventCount;
}
