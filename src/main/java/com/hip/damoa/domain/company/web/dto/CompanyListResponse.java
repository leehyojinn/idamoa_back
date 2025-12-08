package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.company.model.Company;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "업체 목록 응답")
public class CompanyListResponse {

    @Schema(description = "업체 내부 ID", example = "1")
    private Long id;

    @Schema(description = "업체 UUID (외부 노출용)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String name;

    @Schema(description = "업체 슬러그 (URL용)", example = "damoa-interior")
    private String slug;

    @Schema(description = "업체 설명", example = "20년 경력의 인테리어 전문 업체입니다.")
    private String description;

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

    @Schema(description = "업체 상태 (ACTIVE, INACTIVE, PENDING)", example = "ACTIVE")
    private String status;

    @Schema(description = "추천 업체 여부", example = "true")
    private Boolean featured;

    @Schema(description = "인증 업체 여부", example = "true")
    private Boolean verified;

    @Schema(description = "프리미엄 업체 여부", example = "false")
    private Boolean isPremium;

    @Schema(description = "프리미엄 등급 (BASIC, STANDARD, PREMIUM)", example = "STANDARD")
    private String premiumTier;

    @Schema(description = "업체 이미지 목록")
    private List<CompanyImageDto> images;

    @Schema(description = "필터 그룹 목록 (카테고리별)")
    private List<CompanyFilterGroupDto> filterGroups;

    @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
    private Boolean isLiked;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

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
                .isDeleted(company.getIsDeleted())
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
                .isDeleted(company.getIsDeleted())
                .build();
    }
}
