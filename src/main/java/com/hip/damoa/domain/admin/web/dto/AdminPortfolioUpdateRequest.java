package com.hip.damoa.domain.admin.web.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 관리자용 포트폴리오 수정 요청 DTO
 * - 일반 사용자 PortfolioUpdateRequest의 모든 필드 포함
 * - 관리자 전용 필드 추가 (isFeatured, 프로모션 직접 관리)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPortfolioUpdateRequest {

    // ===== 기본 포트폴리오 정보 =====

    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    private String description;

    private String content;

    @Size(max = 100, message = "카테고리는 100자 이내여야 합니다")
    private String category;

    @Size(max = 100, message = "프로젝트 유형은 100자 이내여야 합니다")
    private String projectType;

    @Size(max = 50, message = "프로젝트 규모는 50자 이내여야 합니다")
    private String projectScale;

    private Integer projectDuration;

    private LocalDate projectDate;

    @Size(max = 50, message = "예산 범위는 50자 이내여야 합니다")
    private String budgetRange;

    private BigDecimal actualCost;

    private List<String> imageUuids;

    private List<String> videoUuids;

    private String thumbnailUuid;

    private String[] tags;

    @Size(max = 500, message = "관련 링크는 500자 이내여야 합니다")
    private String relatedLink;

    @Size(max = 200, message = "저작권자는 200자 이내여야 합니다")
    private String copyrightOwner;

    @Size(max = 100, message = "저작권 라이선스는 100자 이내여야 합니다")
    private String copyrightLicense;

    @Size(max = 500, message = "저작권 표시는 500자 이내여야 합니다")
    private String copyrightAttribution;

    private Boolean isPublic;

    private Integer displayOrder;

    private List<Long> filterOptionIds;

    // ===== 관리자 전용 =====

    /**
     * 추천 포트폴리오 설정
     */
    private Boolean isFeatured;

    // ===== 프로모션 관리 =====

    /**
     * 프로모션 타입 (STANDARD, PREMIUM)
     * null: 변경 없음
     */
    private String promotionType;

    /**
     * 자동 갱신 설정
     */
    private Boolean autoRenew;

    /**
     * 프로모션 취소
     * true: 프로모션 취소 (status = CANCELLED)
     */
    private Boolean cancelPromotion;

    /**
     * 프로모션 시작일 (관리자 직접 지정)
     */
    private LocalDate promotionStartDate;

    /**
     * 프로모션 종료일 (관리자 직접 지정)
     */
    private LocalDate promotionEndDate;

    /**
     * 프로모션 가중치 (관리자 직접 지정)
     * 높을수록 노출 우선순위 높음
     */
    private Integer promotionWeight;

    /**
     * 프로모션 월 가격 (관리자 직접 지정)
     * 0: 무료
     */
    private BigDecimal promotionMonthlyPrice;
}
