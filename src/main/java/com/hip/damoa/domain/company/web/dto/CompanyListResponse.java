package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 업체 목록 조회 응답 DTO (간단한 정보만)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyListResponse {

    private Long id;  // 내부 ID (선택적으로 포함)
    private UUID uuid;  // 외부 노출용 UUID
    private String name;
    private String slug;
    private String description;
    private String primaryPhone;
    private String address;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Integer viewCount;
    private Integer likeCount;
    private Integer completedProjects;
    private String status;
    private Boolean featured;
    private Boolean verified;
    private Boolean isPremium;
    private String premiumTier;
    private List<CompanyImageDto> images;
    private Boolean isLiked; // 현재 사용자의 좋아요 여부
    private LocalDateTime createdAt;

    /**
     * Entity → DTO 변환 (목록용 간단 정보)
     */
    public static CompanyListResponse from(Company company) {
        return CompanyListResponse.builder()
                .id(company.getId())
                .uuid(company.getUuid())
                .name(company.getName())
                .slug(company.getSlug())
                .description(company.getDescription())
                .primaryPhone(company.getPrimaryPhone())
                .address(company.getAddress())
                .avgRating(company.getAvgRating())
                .reviewCount(company.getReviewCount())
                .viewCount(company.getViewCount())
                .likeCount(company.getLikeCount())
                .completedProjects(company.getCompletedProjects())
                .status(company.getStatus())
                .featured(company.getFeatured())
                .verified(company.getVerified())
                .isPremium(company.isPremium())
                .premiumTier(company.getPremiumTier())
                .createdAt(company.getCreatedAt())
                .build();
    }

    /**
     * Entity → DTO 변환 (이미지 포함)
     */
    public static CompanyListResponse from(Company company, List<CompanyImageDto> images) {
        return CompanyListResponse.builder()
                .id(company.getId())
                .uuid(company.getUuid())
                .name(company.getName())
                .slug(company.getSlug())
                .description(company.getDescription())
                .primaryPhone(company.getPrimaryPhone())
                .address(company.getAddress())
                .avgRating(company.getAvgRating())
                .reviewCount(company.getReviewCount())
                .viewCount(company.getViewCount())
                .likeCount(company.getLikeCount())
                .completedProjects(company.getCompletedProjects())
                .status(company.getStatus())
                .featured(company.getFeatured())
                .verified(company.getVerified())
                .isPremium(company.isPremium())
                .premiumTier(company.getPremiumTier())
                .images(images)
                .createdAt(company.getCreatedAt())
                .build();
    }
}
