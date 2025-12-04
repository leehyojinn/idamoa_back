package com.hip.damoa.domain.payment.web.dto;

import com.hip.damoa.domain.payment.model.CreditPackage;
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
public class CreditPackageResponse {

    private UUID uuid;
    private String code;              // 패키지 코드 (KRW_10000, KRW_30000, KRW_50000, KRW_100000)
    private String displayName;       // 화면 표시명 (1만원권, 3만원권, 5만원권, 10만원권)
    private Integer unitAmount;       // 단위 금액 (10000, 30000, 50000, 100000)
    private BigDecimal bonusRate;     // 보너스율 (%) - 3만원 이상만 적용
    private Integer maxBonus;         // 최대 보너스 한도 (null = 무제한)
    private Boolean bonusEligible;    // 보너스 적용 가능 여부 (3만원 이상)
    private String description;       // 설명
    private Boolean isActive;         // 활성화 여부

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
