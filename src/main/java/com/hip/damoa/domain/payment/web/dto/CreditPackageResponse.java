package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.CreditPackage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 크레딧 충전 패키지 응답 DTO
 *
 * 패키지는 4개 (1만원권, 3만원권, 5만원권, 10만원권)
 * 수량은 사용자가 충전 시 직접 지정
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "크레딧 충전 패키지 응답")
public class CreditPackageResponse {

    @Schema(description = "패키지 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "패키지 코드", example = "KRW_30000")
    private String code;

    @Schema(description = "화면 표시명", example = "3만원권")
    private String displayName;

    @Schema(description = "단위 금액 (원)", example = "30000")
    private Integer unitAmount;

    @Schema(description = "보너스율 (%)", example = "5.0")
    private BigDecimal bonusRate;

    @Schema(description = "최대 보너스 한도 (원, null = 무제한)", example = "40000")
    private Integer maxBonus;

    @Schema(description = "보너스 적용 가능 여부 (3만원 이상)", example = "true")
    private Boolean bonusEligible;

    @Schema(description = "패키지 설명", example = "3만원 이상 충전 시 5% 보너스")
    private String description;

    @Schema(description = "활성화 여부", example = "true")
    private Boolean isActive;

    public static CreditPackageResponse from(CreditPackage pkg) {
        return CreditPackageResponse.builder()
                .uuid(pkg.getUuid())
                .code(pkg.getCode())
                .displayName(pkg.getDisplayName())
                .unitAmount(pkg.getUnitAmount())
                .bonusRate(pkg.getBonusRate())
                .maxBonus(pkg.getMaxBonus())
                .bonusEligible(pkg.isBonusEligible())
                .description(pkg.getDescription())
                .isActive(pkg.getIsActive())
                .build();
    }
}
