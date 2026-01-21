package com.hip.damoa.domain.company.web;

import com.hip.damoa.core.response.ApiResponse;
import com.hip.damoa.domain.company.model.CompanyImage;
import com.hip.damoa.domain.company.model.CompanyPartnership;
import com.hip.damoa.domain.company.repository.CompanyImageRepository;
import com.hip.damoa.domain.company.service.CompanyPartnershipService;
import com.hip.damoa.domain.company.web.dto.CompanyImageDto;
import com.hip.damoa.domain.company.web.dto.CompanyPartnershipListResponse;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 제휴업체 조회 Controller (퍼블릭)
 */
@Tag(name = "1030. Partnerships", description = "제휴업체 조회 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/partnerships")
public class CompanyPartnershipController {

    private final CompanyPartnershipService partnershipService;
    private final CompanyImageRepository companyImageRepository;
    private final FileRepository fileRepository;

    @Operation(summary = "활성 제휴업체 목록 조회",
            description = "현재 활성화된 제휴업체 목록을 조회합니다.\n\n" +
                    "**조건:**\n" +
                    "- 상태: ACTIVE\n" +
                    "- 시작일 <= 오늘 <= 만료일\n" +
                    "- 삭제되지 않은 업체\n\n" +
                    "**정렬:**\n" +
                    "- displayOrder ASC (낮을수록 먼저 노출)\n" +
                    "- 동일 순서인 경우 등록일 ASC\n\n" +
                    "**응답 정보:**\n" +
                    "- 업체 기본 정보 (이름, 슬러그, 설명, 주소, 전화번호 등)\n" +
                    "- 업체 이미지 목록\n" +
                    "- 평점, 리뷰 수, 조회수, 좋아요 수 등")
    @GetMapping
    public ApiResponse<List<CompanyPartnershipListResponse>> getActivePartnerships() {

        log.info("활성 제휴업체 목록 조회 요청");

        List<CompanyPartnership> partnerships = partnershipService.getActivePartnerships();

        if (partnerships.isEmpty()) {
            return ApiResponse.success(List.of());
        }

        // 이미지 일괄 조회 (N+1 방지)
        Set<Long> companyIds = partnerships.stream()
                .map(p -> p.getCompany().getId())
                .collect(Collectors.toSet());

        Map<Long, List<CompanyImageDto>> imageMap = getCompanyImages(companyIds);

        // Response 변환
        List<CompanyPartnershipListResponse> response = partnerships.stream()
                .map(partnership -> {
                    Long companyId = partnership.getCompany().getId();
                    List<CompanyImageDto> images = imageMap.getOrDefault(companyId, List.of());
                    return CompanyPartnershipListResponse.from(partnership, images);
                })
                .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    /**
     * 여러 업체의 이미지 일괄 조회 (N+1 방지)
     * @return Map<companyId, List<CompanyImageDto>>
     */
    private Map<Long, List<CompanyImageDto>> getCompanyImages(Set<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Map.of();
        }

        // 모든 회사의 이미지 일괄 조회 (단일 쿼리)
        List<CompanyImage> images = companyImageRepository.findByCompanyIdInAndIsDeletedFalse(companyIds);

        if (images.isEmpty()) {
            return Map.of();
        }

        // File ID 일괄 조회
        Set<Long> fileIds = images.stream()
                .map(CompanyImage::getFileId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, File> fileMap = fileRepository.findByIdIn(fileIds.stream().toList())
                .stream()
                .collect(Collectors.toMap(File::getId, f -> f));

        // 회사별로 그룹화하여 CompanyImageDto 리스트 생성
        return images.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getCompany().getId(),
                        Collectors.mapping(
                                img -> {
                                    File file = img.getFileId() != null ? fileMap.get(img.getFileId()) : null;
                                    String imageUrl = file != null ? file.getFileUrl() : null;
                                    UUID fileUuid = file != null ? file.getUuid() : null;
                                    return CompanyImageDto.from(img, imageUrl, fileUuid);
                                },
                                Collectors.toList()
                        )
                ));
    }
}
