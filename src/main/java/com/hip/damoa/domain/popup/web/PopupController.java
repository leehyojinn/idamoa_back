package com.hip.damoa.domain.popup.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.popup.service.PopupService;
import com.hip.damoa.domain.popup.web.dto.PopupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 팝업 Controller (일반 사용자용)
 *
 * 홈페이지 팝업 조회 및 통계 API
 */
@Tag(name = "1015. Popup", description = "팝업 API - 조회 및 통계")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/popups")
public class PopupController {

    private final PopupService popupService;

    @Operation(summary = "활성 팝업 목록 조회",
            description = "현재 활성화되어 있고 노출 기간 내에 있는 팝업 목록을 조회합니다.\n\n" +
                    "**필터링 조건:**\n" +
                    "- isActive = true (활성화된 팝업만)\n" +
                    "- isDeleted = false (삭제되지 않은 팝업만)\n" +
                    "- displayStartDate가 null이거나 현재 시간 이전\n" +
                    "- displayEndDate가 null이거나 현재 시간 이후\n\n" +
                    "**정렬:**\n" +
                    "- displayOrder 오름차순 (낮을수록 먼저 표시)\n\n" +
                    "**응답 포함 정보:**\n" +
                    "- 팝업 제목, 내용\n" +
                    "- 이미지 UUID 및 URL (S3)\n" +
                    "- 클릭 시 이동할 링크 URL\n" +
                    "- 노출 기간 (시작일, 종료일)\n" +
                    "- 조회수, 클릭수\n\n" +
                    "**권한:**\n" +
                    "- 누구나 조회 가능 (로그인 불필요)\n\n" +
                    "**활용:**\n" +
                    "- 홈페이지 팝업 표시\n" +
                    "- 팝업 레이어 UI 구성\n\n" +
                    "**프론트엔드 구현 가이드:**\n" +
                    "1. 홈페이지 로드 시 이 API 호출\n" +
                    "2. 응답받은 팝업 목록을 displayOrder 순서대로 표시\n" +
                    "3. 각 팝업의 imageUrl을 사용하여 이미지 표시\n" +
                    "4. linkUrl이 있으면 클릭 시 해당 URL로 이동 (incrementClickCount API 호출)\n" +
                    "5. \"오늘하루 열지 않기\" 기능은 localStorage 사용:\n" +
                    "   - 키: `popup_hide_{uuid}`\n" +
                    "   - 값: 오늘 날짜 (YYYY-MM-DD)\n" +
                    "   - 팝업 표시 전 localStorage 확인, 오늘 날짜와 일치하면 숨김")
    @GetMapping("/active")
    public ApiResponse<List<PopupResponse>> getActivePopups() {

        log.info("활성 팝업 목록 조회 요청");

        List<PopupResponse> response = popupService.getActivePopups();

        return ApiResponse.success(response);
    }

    @Operation(summary = "팝업 조회수 증가",
            description = "팝업 조회수를 1 증가시킵니다.\n\n" +
                    "**호출 시점:**\n" +
                    "- 팝업이 사용자에게 표시될 때\n" +
                    "- 한 번만 호출 (중복 호출 방지 권장)\n\n" +
                    "**권한:**\n" +
                    "- 누구나 호출 가능 (로그인 불필요)\n\n" +
                    "**활용:**\n" +
                    "- 팝업 노출 통계 수집\n" +
                    "- 관리자가 팝업 효과 측정")
    @PostMapping("/{uuid}/view")
    public ApiResponse<Void> incrementViewCount(@PathVariable UUID uuid) {

        log.debug("팝업 조회수 증가 요청: uuid={}", uuid);

        popupService.incrementViewCount(uuid);

        return ApiResponse.success();
    }

    @Operation(summary = "팝업 클릭수 증가",
            description = "팝업 클릭수를 1 증가시킵니다.\n\n" +
                    "**호출 시점:**\n" +
                    "- 사용자가 팝업을 클릭하여 링크로 이동할 때\n" +
                    "- linkUrl이 있는 경우에만 호출\n\n" +
                    "**권한:**\n" +
                    "- 누구나 호출 가능 (로그인 불필요)\n\n" +
                    "**활용:**\n" +
                    "- 팝업 클릭 통계 수집\n" +
                    "- CTR(Click Through Rate) 계산 (클릭수/조회수)\n" +
                    "- 관리자가 팝업 효과 측정")
    @PostMapping("/{uuid}/click")
    public ApiResponse<Void> incrementClickCount(@PathVariable UUID uuid) {

        log.debug("팝업 클릭수 증가 요청: uuid={}", uuid);

        popupService.incrementClickCount(uuid);

        return ApiResponse.success();
    }
}
