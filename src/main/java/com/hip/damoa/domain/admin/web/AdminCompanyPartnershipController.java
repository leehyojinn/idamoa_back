package com.hip.damoa.domain.admin.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.CompanyPartnership;
import com.hip.damoa.domain.company.service.CompanyPartnershipService;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipCreateRequest;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipReorderRequest;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipResponse;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 제휴업체 관리 Controller (관리자 전용)
 */
@Tag(name = "1950. Admin - Company Partnership", description = "제휴업체 관리 API (관리자 전용)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/company-partnerships")
public class AdminCompanyPartnershipController {

    private final CompanyPartnershipService partnershipService;

    @Operation(summary = "제휴업체 등록",
            description = "새로운 제휴업체를 등록합니다.\n\n" +
                    "**필수 정보:**\n" +
                    "- companyUuid: 업체 UUID\n" +
                    "- startDate: 제휴 시작일\n" +
                    "- endDate: 제휴 만료일\n\n" +
                    "**선택 정보:**\n" +
                    "- displayOrder: 노출 순서 (낮을수록 먼저 노출, 기본값 0)\n" +
                    "- adminMemo: 관리자 메모\n\n" +
                    "**주의:**\n" +
                    "- 한 업체당 1개의 활성 제휴만 가능\n" +
                    "- 만료일은 시작일보다 이후여야 함")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompanyPartnershipResponse> createPartnership(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyPartnershipCreateRequest request) {

        log.info("제휴업체 등록 요청: userEmail={}, companyUuid={}",
                userDetails.getUsername(), request.getCompanyUuid());

        CompanyPartnership partnership = partnershipService.createPartnership(
                userDetails.getUsername(), request);

        return ApiResponse.success(CompanyPartnershipResponse.from(partnership));
    }

    @Operation(summary = "제휴업체 목록 조회",
            description = "제휴업체 목록을 조회합니다.\n\n" +
                    "**필터:**\n" +
                    "- status: 상태 필터 (ACTIVE, EXPIRED, CANCELLED)\n\n" +
                    "**페이지네이션:**\n" +
                    "- size: 페이지당 항목 수 (기본 20)\n" +
                    "- page: 페이지 번호 (0부터 시작)\n\n" +
                    "**정렬:**\n" +
                    "- 기본값: displayOrder ASC")
    @GetMapping
    public ApiResponse<Page<CompanyPartnershipResponse>> getPartnerships(
            @Parameter(description = "상태 필터 (ACTIVE, EXPIRED, CANCELLED)")
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC)
            Pageable pageable) {

        log.info("제휴업체 목록 조회 요청: status={}", status);

        Page<CompanyPartnership> partnerships = partnershipService.getPartnershipsForAdmin(status, pageable);
        Page<CompanyPartnershipResponse> response = partnerships.map(CompanyPartnershipResponse::from);

        return ApiResponse.success(response);
    }

    @Operation(summary = "제휴업체 상세 조회",
            description = "특정 제휴업체를 조회합니다.")
    @GetMapping("/{uuid}")
    public ApiResponse<CompanyPartnershipResponse> getPartnership(
            @PathVariable UUID uuid) {

        log.info("제휴업체 상세 조회 요청: uuid={}", uuid);

        CompanyPartnership partnership = partnershipService.getPartnership(uuid);

        return ApiResponse.success(CompanyPartnershipResponse.from(partnership));
    }

    @Operation(summary = "제휴업체 수정",
            description = "제휴업체 정보를 수정합니다.\n\n" +
                    "**수정 가능 정보:**\n" +
                    "- displayOrder: 노출 순서\n" +
                    "- startDate: 시작일\n" +
                    "- endDate: 만료일\n" +
                    "- adminMemo: 관리자 메모")
    @PutMapping("/{uuid}")
    public ApiResponse<CompanyPartnershipResponse> updatePartnership(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyPartnershipUpdateRequest request) {

        log.info("제휴업체 수정 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        CompanyPartnership partnership = partnershipService.updatePartnership(
                userDetails.getUsername(), uuid, request);

        return ApiResponse.success(CompanyPartnershipResponse.from(partnership));
    }

    @Operation(summary = "제휴 상태 토글",
            description = "제휴 상태를 토글합니다 (ACTIVE <-> CANCELLED).\n\n" +
                    "**동작:**\n" +
                    "- ACTIVE -> CANCELLED (비활성화)\n" +
                    "- CANCELLED -> ACTIVE (재활성화)\n" +
                    "- EXPIRED 상태는 토글 불가\n\n" +
                    "**응답:**\n" +
                    "변경된 제휴 정보를 반환합니다.")
    @PatchMapping("/{uuid}/toggle-status")
    public ApiResponse<CompanyPartnershipResponse> togglePartnershipStatus(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("제휴 상태 토글 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        CompanyPartnership partnership = partnershipService.togglePartnershipStatus(
                userDetails.getUsername(), uuid);

        return ApiResponse.success(CompanyPartnershipResponse.from(partnership));
    }

    @Operation(summary = "제휴 삭제",
            description = "제휴를 삭제합니다 (Soft Delete).\n" +
                    "삭제된 제휴는 목록에 노출되지 않습니다.")
    @DeleteMapping("/{uuid}")
    public ApiResponse<Void> deletePartnership(
            @PathVariable UUID uuid,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("제휴 삭제 요청: uuid={}, userEmail={}", uuid, userDetails.getUsername());

        partnershipService.deletePartnership(userDetails.getUsername(), uuid);

        return ApiResponse.success();
    }

    @Operation(summary = "제휴 순서 일괄 변경",
            description = "제휴업체들의 노출 순서를 일괄 변경합니다.\n\n" +
                    "**요청 형식:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"orders\": [\n" +
                    "    { \"partnershipUuid\": \"...\", \"displayOrder\": 0 },\n" +
                    "    { \"partnershipUuid\": \"...\", \"displayOrder\": 1 }\n" +
                    "  ]\n" +
                    "}\n" +
                    "```")
    @PostMapping("/reorder")
    public ApiResponse<Void> reorderPartnerships(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompanyPartnershipReorderRequest request) {

        log.info("제휴 순서 변경 요청: userEmail={}, count={}",
                userDetails.getUsername(), request.getOrders().size());

        partnershipService.reorderPartnerships(userDetails.getUsername(), request);

        return ApiResponse.success();
    }

    @Operation(summary = "특정 업체의 제휴 이력 조회",
            description = "특정 업체의 모든 제휴 이력을 조회합니다 (활성/만료/취소 모두 포함).")
    @GetMapping("/company/{companyUuid}/history")
    public ApiResponse<List<CompanyPartnershipResponse>> getPartnershipHistory(
            @PathVariable UUID companyUuid) {

        log.info("제휴 이력 조회 요청: companyUuid={}", companyUuid);

        List<CompanyPartnership> partnerships = partnershipService.getPartnershipHistory(companyUuid);
        List<CompanyPartnershipResponse> response = partnerships.stream()
                .map(CompanyPartnershipResponse::from)
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }
}
