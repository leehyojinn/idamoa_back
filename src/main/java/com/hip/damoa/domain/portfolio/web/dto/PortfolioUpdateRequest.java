package com.hip.damoa.domain.portfolio.web.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioUpdateRequest {

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

    // ===== 프로모션 관련 =====

    private String promotionType;  // STANDARD, PREMIUM (업그레이드용)

    private Boolean autoRenew;

    private Boolean cancelPromotion;  // true: 자동갱신 해제
}
