package com.hip.damoa.domain.admin.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * 필터 옵션 순서 변경 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionReorderRequest {

    @NotNull(message = "카테고리 ID는 필수입니다")
    private Long categoryId;

    @NotEmpty(message = "옵션 순서 목록은 비어있을 수 없습니다")
    private List<OptionOrder> optionOrders;

    /**
     * 옵션 순서 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionOrder {

        @NotNull(message = "옵션 ID는 필수입니다")
        private Long optionId;

        @NotNull(message = "표시 순서는 필수입니다")
        private Integer displayOrder;
    }
}