package com.hip.damoa.domain.company.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 제휴업체 순서 일괄 변경 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyPartnershipReorderRequest {

    @NotEmpty(message = "순서 목록은 필수입니다")
    private List<OrderItem> orders;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {

        @NotNull(message = "제휴 UUID는 필수입니다")
        private UUID partnershipUuid;

        @NotNull(message = "순서는 필수입니다")
        private Integer displayOrder;
    }
}
