package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.Company;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 업체 조회 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {

    private Long id;  // 내부 ID (선택적으로 포함)
    private UUID uuid;  // 외부 노출용 UUID
    private Long ownerId;
    private String ownerEmail;
    private String name;
    private String slug;
    private String description;
    private String detailContent;
    private String detailContentFormat;
    private Map<String, Object> businessInfo;
    private Map<String, Object> businessHours;
    private String businessHoursNote;
    private String[] serviceAreas;
    private String[] tags;
    private String[] keywords;
    private String primaryPhone;
    private String secondaryPhone;
    private String emergencyContact;
    private String email;
    private String websiteUrl;
    private String kakaoChatUrl;
    private Map<String, Object> socialLinks;
    private String address;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Integer viewCount;
    private Integer likeCount;
    private Integer portfolioCount;
    private Integer completedProjects;
    private String status;
    private Boolean featured;
    private Boolean verified;
    private LocalDateTime verifiedAt;
    private LocalDateTime premiumUntil;
    private Boolean isPremium;
    private Boolean isLiked; // 현재 사용자의 좋아요 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CompanyImageDto> images;
    private List<FilterOptionDto> filterOptions; // 업체 분류/전문 영역/작업 평수 등

    /**
     * Entity → DTO 변환
     */
    public static CompanyResponse from(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .uuid(company.getUuid())
                .ownerId(company.getOwner().getId())
                .ownerEmail(company.getOwner().getEmail())
                .name(company.getName())
                .slug(company.getSlug())
                .description(company.getDescription())
                .detailContent(company.getDetailContent())
                .detailContentFormat(company.getDetailContentFormat())
                .businessInfo(company.getBusinessInfo())
                .businessHours(company.getBusinessHours())
                .businessHoursNote(company.getBusinessHoursNote())
                .serviceAreas(company.getServiceAreas())
                .tags(company.getTags())
                .keywords(company.getKeywords())
                .primaryPhone(company.getPrimaryPhone())
                .secondaryPhone(company.getSecondaryPhone())
                .emergencyContact(company.getEmergencyContact())
                .email(company.getEmail())
                .websiteUrl(company.getWebsiteUrl())
                .kakaoChatUrl(company.getKakaoChatUrl())
                .socialLinks(company.getSocialLinks())
                .address(company.getAddress())
                .postalCode(company.getPostalCode())
                .latitude(company.getLatitude())
                .longitude(company.getLongitude())
                .avgRating(company.getAvgRating())
                .reviewCount(company.getReviewCount())
                .viewCount(company.getViewCount())
                .likeCount(company.getLikeCount())
                .portfolioCount(company.getPortfolioCount())
                .completedProjects(company.getCompletedProjects())
                .status(company.getStatus())
                .featured(company.getFeatured())
                .verified(company.getVerified())
                .verifiedAt(company.getVerifiedAt())
                .premiumUntil(company.getPremiumUntil())
                .isPremium(company.isPremium())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}
