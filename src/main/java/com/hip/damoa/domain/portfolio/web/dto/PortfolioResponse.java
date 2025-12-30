package com.hip.damoa.domain.portfolio.web.dto;

import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.filter.model.FilterOption;
import com.hip.damoa.domain.portfolio.model.PortfolioPromotion;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {

    private UUID uuid;
    private String title;
    private String description;
    private String content;
    private String category;
    private String projectType;
    private String projectScale;
    private Integer projectDuration;
    private LocalDate projectDate;
    private String budgetRange;
    private BigDecimal actualCost;

    private List<FileInfo> images;
    private List<FileInfo> videos;
    private String thumbnailUrl;
    private String[] tags;

    private String relatedLink;
    private String copyrightOwner;
    private String copyrightLicense;
    private String copyrightAttribution;

    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer bookmarkCount;

    private Boolean isFeatured;
    private Boolean isPublic;
    private Integer displayOrder;

    private Boolean isBookmarked;
    private Boolean isLiked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 필터 옵션
    private List<FilterOptionInfo> filterOptions;

    // 업체 정보
    private CompanySummary company;

    // 프로모션 정보
    private PromotionInfo promotion;

    // 리뷰 목록 (상세 조회시만)
    private List<ReviewSummary> reviews;

    // ===== Inner Classes =====

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private UUID uuid;
        private String originalFilename;
        private String fileUrl;
        private Long fileSize;
        private String mimeType;
        private String fileExtension;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterOptionInfo {
        private Long id;
        private String filterKey;
        private String value;
        private String displayName;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanySummary {
        private UUID companyUuid;
        private UUID ownerUuid;  // 1:1 채팅용
        private String companyName;
        private String phone;
        private Double averageRating;
        private Integer reviewCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionInfo {
        private UUID promotionUuid;
        private String promotionType;
        private BigDecimal monthlyPrice;
        private Integer weight;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer remainingDays;
        private Boolean autoRenew;
        private String status;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewSummary {
        private UUID reviewUuid;
        private String userName;
        private Integer rating;
        private String content;
        private LocalDateTime createdAt;
        private String reply;
        private LocalDateTime repliedAt;
        private List<FileInfo> images;
    }

    /**
     * Entity → Response 변환
     */
    public static PortfolioResponse from(CompanyPortfolio portfolio,
                                          List<FilterOption> filterOptions,
                                          List<FileInfo> images,
                                          List<FileInfo> videos,
                                          boolean isBookmarked,
                                          boolean isLiked,
                                          CompanySummary company,
                                          List<ReviewSummary> reviews,
                                          PortfolioPromotion promotion) {

        List<FilterOptionInfo> filterOptionInfos = null;
        if (filterOptions != null && !filterOptions.isEmpty()) {
            filterOptionInfos = filterOptions.stream()
                    .map(fo -> FilterOptionInfo.builder()
                            .id(fo.getId())
                            .filterKey(fo.getCode())
                            .value(fo.getCode())
                            .displayName(fo.getName())
                            .build())
                    .toList();
        }

        PromotionInfo promotionInfo = null;
        if (promotion != null) {
            // remainingDays 계산 (endDate - today)
            Integer remainingDays = null;
            if (promotion.getEndDate() != null) {
                remainingDays = (int) java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.now(), promotion.getEndDate());
                if (remainingDays < 0) remainingDays = 0;
            }

            promotionInfo = PromotionInfo.builder()
                    .promotionUuid(promotion.getUuid())
                    .promotionType(promotion.getPromotionType())
                    .monthlyPrice(promotion.getMonthlyPrice())
                    .weight(promotion.getWeight())
                    .startDate(promotion.getStartDate())
                    .endDate(promotion.getEndDate())
                    .remainingDays(remainingDays)
                    .autoRenew(promotion.getAutoRenew())
                    .status(promotion.getStatus().name())
                    .build();
        }

        return PortfolioResponse.builder()
                .uuid(portfolio.getUuid())
                .title(portfolio.getTitle())
                .description(portfolio.getDescription())
                .content(portfolio.getContent())
                .category(portfolio.getCategory())
                .projectType(portfolio.getProjectType())
                .projectScale(portfolio.getProjectScale())
                .projectDuration(portfolio.getProjectDuration())
                .projectDate(portfolio.getProjectDate())
                .budgetRange(portfolio.getBudgetRange())
                .actualCost(portfolio.getActualCost())
                .images(images)
                .videos(videos)
                .thumbnailUrl(portfolio.getThumbnailUrl())
                .tags(portfolio.getTags())
                .relatedLink(portfolio.getRelatedLink())
                .copyrightOwner(portfolio.getCopyrightOwner())
                .copyrightLicense(portfolio.getCopyrightLicense())
                .copyrightAttribution(portfolio.getCopyrightAttribution())
                .viewCount(portfolio.getViewCount())
                .likeCount(portfolio.getLikeCount())
                .commentCount(portfolio.getCommentCount())
                .bookmarkCount(portfolio.getBookmarkCount())
                .isFeatured(portfolio.getIsFeatured())
                .isPublic(portfolio.getIsPublic())
                .displayOrder(portfolio.getDisplayOrder())
                .isBookmarked(isBookmarked)
                .isLiked(isLiked)
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .filterOptions(filterOptionInfos)
                .company(company)
                .promotion(promotionInfo)
                .reviews(reviews)
                .build();
    }

    /**
     * 간단 버전 (리스트용)
     */
    public static PortfolioResponse simpleFrom(CompanyPortfolio portfolio,
                                                 List<FileInfo> images,
                                                 boolean isBookmarked,
                                                 boolean isLiked,
                                                 CompanySummary company,
                                                 PortfolioPromotion promotion) {
        return from(portfolio, null, images, null, isBookmarked, isLiked, company, null, promotion);
    }
}
