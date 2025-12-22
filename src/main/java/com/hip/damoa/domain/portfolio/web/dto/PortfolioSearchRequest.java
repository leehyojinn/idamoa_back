package com.hip.damoa.domain.portfolio.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * 포트폴리오 검색 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포트폴리오 검색 요청")
public class PortfolioSearchRequest {

    @Schema(description = "통합 키워드 검색 (제목, 내용, 태그, 업체명에서 검색)", example = "인테리어")
    private String keyword;

    @Schema(description = "필터 옵션 ID 목록", example = "[1, 2, 3]")
    private List<Long> filterOptionIds;

    @Schema(description = "특정 업체의 포트폴리오만 조회", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID companyUuid;

    @Schema(description = "북마크한 포트폴리오만 조회 (로그인 필요)", example = "false")
    private Boolean onlyBookmarked;

    @Schema(description = "내가 작성한 포트폴리오만 조회 (로그인 필요)", example = "false")
    private Boolean onlyMyPosts;

    /**
     * 키워드 검색 여부
     */
    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }

    /**
     * 필터 옵션 검색 여부
     */
    public boolean hasFilterOptions() {
        return filterOptionIds != null && !filterOptionIds.isEmpty();
    }

    /**
     * 특정 업체 검색 여부
     */
    public boolean hasCompanyFilter() {
        return companyUuid != null;
    }

    /**
     * 북마크 필터 여부
     */
    public boolean isOnlyBookmarked() {
        return Boolean.TRUE.equals(onlyBookmarked);
    }

    /**
     * 내 글만 필터 여부
     */
    public boolean isOnlyMyPosts() {
        return Boolean.TRUE.equals(onlyMyPosts);
    }
}
