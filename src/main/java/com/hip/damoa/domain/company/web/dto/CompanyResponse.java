package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.core.util.ResponseUtils;
import com.hip.damoa.domain.company.model.Company;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "업체 상세 응답")
public class CompanyResponse {

    @Schema(description = "업체 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "소유자 UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID ownerUuid;

    @Schema(description = "소유자 이메일", example = "owner@example.com")
    private String ownerEmail;

    @Schema(description = "업체명", example = "다모아 인테리어")
    private String name;

    @Schema(description = "업체 슬러그 (URL용)", example = "damoa-interior")
    private String slug;

    @Schema(description = "업체 설명", example = "20년 경력의 인테리어 전문 업체입니다.")
    private String description;

    @Schema(description = "상세 소개 내용 (HTML 지원)")
    private String detailContent;

    @Schema(description = "상세 소개 내용 포맷 (HTML, MARKDOWN)", example = "HTML")
    private String detailContentFormat;

    @Schema(description = "사업자 정보")
    private Map<String, Object> businessInfo;

    @Schema(description = "영업시간 정보")
    private Map<String, Object> businessHours;

    @Schema(description = "영업시간 참고사항", example = "공휴일은 휴무입니다.")
    private String businessHoursNote;

    @Schema(description = "태그 배열", example = "[\"인테리어\", \"리모델링\", \"사무실\"]")
    @Builder.Default
    private String[] tags = new String[0];

    @Schema(description = "대표 전화번호", example = "02-1234-5678")
    private String primaryPhone;

    @Schema(description = "보조 전화번호", example = "010-1234-5678")
    private String secondaryPhone;

    @Schema(description = "긴급 연락처", example = "010-9999-9999")
    private String emergencyContact;

    @Schema(description = "업체 이메일", example = "contact@damoa.com")
    private String email;

    @Schema(description = "웹사이트 URL", example = "https://damoa-interior.com")
    private String websiteUrl;

    @Schema(description = "카카오톡 채팅 URL", example = "https://pf.kakao.com/_xxxxx")
    private String kakaoChatUrl;

    @Schema(description = "소셜 링크")
    private Map<String, Object> socialLinks;

    @Schema(description = "주소", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "우편번호", example = "06123")
    private String postalCode;

    @Schema(description = "위도", example = "37.5665")
    private BigDecimal latitude;

    @Schema(description = "경도", example = "126.9780")
    private BigDecimal longitude;

    @Schema(description = "평균 평점 (1.0~5.0)", example = "4.5")
    private BigDecimal avgRating;

    @Schema(description = "리뷰 수", example = "150")
    private Integer reviewCount;

    @Schema(description = "조회수", example = "5000")
    private Integer viewCount;

    @Schema(description = "좋아요 수", example = "320")
    private Integer likeCount;

    @Schema(description = "포트폴리오 수", example = "45")
    private Integer portfolioCount;

    @Schema(description = "완료된 프로젝트 수", example = "85")
    private Integer completedProjects;

    @Schema(description = "업체 상태 (ACTIVE, INACTIVE, PENDING)", example = "ACTIVE")
    private String status;

    @Schema(description = "추천 업체 여부", example = "true")
    private Boolean featured;

    @Schema(description = "인증 업체 여부", example = "true")
    private Boolean verified;

    @Schema(description = "인증 일시", example = "2025-01-01T10:00:00")
    private LocalDateTime verifiedAt;

    @Schema(description = "프리미엄 만료일시", example = "2025-12-31T23:59:59")
    private LocalDateTime premiumUntil;

    @Schema(description = "프리미엄 업체 여부", example = "true")
    private Boolean isPremium;

    @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
    private Boolean isLiked;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-02T14:00:00")
    private LocalDateTime updatedAt;

    @Schema(description = "업체 이미지 목록")
    @Builder.Default
    private List<CompanyImageDto> images = List.of();

    @Schema(description = "필터 그룹 목록 (카테고리별)")
    @Builder.Default
    private List<CompanyFilterGroupDto> filterGroups = List.of();

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    @Schema(description = "삭제 일시 (관리자용)", example = "2025-01-15T10:00:00")
    private LocalDateTime deletedAt;

    /**
     * Entity → DTO 변환
     */
    public static CompanyResponse from(Company company) {
        return CompanyResponse.builder()
                .uuid(company.getUuid())
                .ownerUuid(company.getOwner() != null ? company.getOwner().getUuid() : null)
                .ownerEmail(company.getOwner() != null ? ResponseUtils.safe(company.getOwner().getEmail()) : "")
                .name(ResponseUtils.safe(company.getName()))
                .slug(ResponseUtils.safe(company.getSlug()))
                .description(ResponseUtils.safe(company.getDescription()))
                .detailContent(ResponseUtils.safe(company.getDetailContent()))
                .detailContentFormat(ResponseUtils.safe(company.getDetailContentFormat(), "HTML"))
                .businessInfo(ResponseUtils.safeMap(company.getBusinessInfo()))
                .businessHours(ResponseUtils.safeMap(company.getBusinessHours()))
                .businessHoursNote(ResponseUtils.safe(company.getBusinessHoursNote()))
                .tags(ResponseUtils.safeArray(company.getTags()))
                .primaryPhone(ResponseUtils.safe(company.getPrimaryPhone()))
                .secondaryPhone(ResponseUtils.safe(company.getSecondaryPhone()))
                .emergencyContact(ResponseUtils.safe(company.getEmergencyContact()))
                .email(ResponseUtils.safe(company.getEmail()))
                .websiteUrl(ResponseUtils.safe(company.getWebsiteUrl()))
                .kakaoChatUrl(ResponseUtils.safe(company.getKakaoChatUrl()))
                .socialLinks(ResponseUtils.safeMap(company.getSocialLinks()))
                .address(ResponseUtils.safe(company.getAddress()))
                .postalCode(ResponseUtils.safe(company.getPostalCode()))
                .latitude(ResponseUtils.safe(company.getLatitude()))
                .longitude(ResponseUtils.safe(company.getLongitude()))
                .avgRating(ResponseUtils.safe(company.getAvgRating()))
                .reviewCount(ResponseUtils.safe(company.getReviewCount()))
                .viewCount(ResponseUtils.safe(company.getViewCount()))
                .likeCount(ResponseUtils.safe(company.getLikeCount()))
                .portfolioCount(ResponseUtils.safe(company.getPortfolioCount()))
                .completedProjects(ResponseUtils.safe(company.getCompletedProjects()))
                .status(ResponseUtils.safe(company.getStatus(), "ACTIVE"))
                .featured(ResponseUtils.safe(company.getFeatured()))
                .verified(ResponseUtils.safe(company.getVerified()))
                .verifiedAt(company.getVerifiedAt())
                .premiumUntil(company.getPremiumUntil())
                .isPremium(ResponseUtils.safe(company.isPremium()))
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .isDeleted(ResponseUtils.safe(company.getIsDeleted()))
                .deletedAt(company.getDeletedAt())
                .build();
    }
}
