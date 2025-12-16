package com.hip.damoa.domain.filter.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 게시판 필터 응답 DTO
 * 갤러리/자료실 게시판용 필터 정보를 제공합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardFilterResponse {

    private Long id;               // 필터 카테고리 ID
    private UUID uuid;             // 필터 카테고리 UUID
    private String code;           // 필터 카테고리 코드 (예: board_area, board_style)
    private String name;           // 필터 카테고리 이름 (예: 평수, 스타일)
    private String filterType;     // 필터 타입 (SINGLE_SELECT, MULTI_SELECT)
    private String description;    // 필터 설명
    private Boolean isRequired;    // 필수 여부
    @Builder.Default
    private List<BoardFilterOption> options = List.of();  // 필터 옵션 목록

    /**
     * 필터 옵션 내부 클래스
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoardFilterOption {
        private Long id;           // 옵션 ID
        private UUID uuid;         // 옵션 UUID
        private String code;       // 옵션 코드 (예: area_10_20)
        private String name;       // 옵션 이름 (예: 10평대)
        private String description; // 옵션 설명
        private Integer displayOrder;  // 표시 순서
    }

    /**
     * FilterCategoryResponse를 BoardFilterResponse로 변환
     */
    public static BoardFilterResponse from(FilterCategoryResponse category) {
        List<BoardFilterOption> options = null;

        if (category.getOptions() != null) {
            options = category.getOptions().stream()
                    .map(opt -> BoardFilterOption.builder()
                            .id(opt.getId())
                            .uuid(opt.getUuid())
                            .code(opt.getCode())
                            .name(opt.getName())
                            .description(opt.getDescription())
                            .displayOrder(opt.getDisplayOrder())
                            .build())
                    .toList();
        }

        return BoardFilterResponse.builder()
                .id(category.getId())
                .uuid(category.getUuid())
                .code(category.getCode())
                .name(category.getName())
                .filterType(category.getFilterType())
                .description(category.getDescription())
                .isRequired(category.getIsRequired())
                .options(options)
                .build();
    }
}