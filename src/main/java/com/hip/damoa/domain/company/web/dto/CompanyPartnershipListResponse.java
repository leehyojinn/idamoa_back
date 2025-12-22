package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPartnership;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 제휴업체 목록 응답 DTO (퍼블릭용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제휴업체 목록 응답")
public class CompanyPartnershipListResponse {

    @Schema(description = "업체 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID companyUuid;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String companyName;

    @Schema(description = "업체 슬러그 (URL용)", example = "damoa-interior")
    private String companySlug;

    @Schema(description = "업체 설명", example = "20년 경력의 인테리어 전문 업체입니다.")
    private String companyDescription;

    @Schema(description = "대표 전화번호", example = "02-1234-5678")
    private String primaryPhone;

    @Schema(description = "업체 주소", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "평균 평점 (1.0~5.0)", example = "4.5")
    private BigDecimal avgRating;

    @Schema(description = "리뷰 수", example = "150")
    private Integer reviewCount;

    @Schema(description = "조회수", example = "5000")
    private Integer viewCount;

    @Schema(description = "좋아요 수", example = "320")
    private Integer likeCount;

    @Schema(description = "완료된 프로젝트 수", example = "85")
    private Integer completedProjects;

    @Schema(description = "인증 업체 여부", example = "true")
    private Boolean verified;

    @Schema(description = "프리미엄 업체 여부", example = "false")
    private Boolean isPremium;

    @Schema(description = "프리미엄 등급 (BASIC, STANDARD, PREMIUM)", example = "STANDARD")
    private String premiumTier;

    @Schema(description = "업체 이미지 목록")
    @Builder.Default
    private List<CompanyImageDto> images = List.of();

    @Schema(description = "제휴 노출 순서", example = "1")
    private Integer displayOrder;

    /**
     * Entity → DTO 변환
     */
    public static CompanyPartnershipListResponse from(CompanyPartnership partnership,
                                                       List<CompanyImageDto> images) {
        Company company = partnership.getCompany();

        return CompanyPartnershipListResponse.builder()
                .companyUuid(company.getUuid())
                .companyName(company.getName())
                .companySlug(company.getSlug())
                .companyDescription(company.getDescription())
                .primaryPhone(company.getPrimaryPhone())
                .address(company.getAddress())
                .avgRating(company.getAvgRating())
                .reviewCount(company.getReviewCount())
                .viewCount(company.getViewCount())
                .likeCount(company.getLikeCount())
                .completedProjects(company.getCompletedProjects())
                .verified(company.getVerified())
                .isPremium(company.isPremium())
                .premiumTier(company.getPremiumTier())
                .images(images != null ? images : List.of())
                .displayOrder(partnership.getDisplayOrder())
                .build();
    }

    /**
     * Entity → DTO 변환 (이미지 없이)
     */
    public static CompanyPartnershipListResponse from(CompanyPartnership partnership) {
        return from(partnership, List.of());
    }
}
