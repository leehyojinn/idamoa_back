package com.hip.damoa.domain.company.web.dto;

import com.hip.damoa.domain.filter.model.FilterOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필터 옵션 DTO (API 응답용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterOptionDto {

    private Long id;
    private String code;
    private String name;
    private String categoryCode;
    private String categoryName;

    /**
     * Entity → DTO 변환
     */
    public static FilterOptionDto from(FilterOption filterOption) {
        return FilterOptionDto.builder()
                .id(filterOption.getId())
                .code(filterOption.getCode())
                .name(filterOption.getName())
                .categoryCode(filterOption.getCategory().getCode())
                .categoryName(filterOption.getCategory().getName())
                .build();
    }
}
