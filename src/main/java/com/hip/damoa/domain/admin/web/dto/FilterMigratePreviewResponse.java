package com.hip.damoa.domain.admin.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 필터 옵션 마이그레이션 미리보기 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "필터 옵션 마이그레이션 미리보기 응답")
public class FilterMigratePreviewResponse {

    @Schema(description = "소스 필터 옵션 정보")
    private OptionInfo sourceOption;

    @Schema(description = "타겟 필터 옵션 정보")
    private OptionInfo targetOption;

    @Schema(description = "영향받는 업체 수")
    private Integer affectedCompanyCount;

    @Schema(description = "영향받는 게시글 수")
    private Integer affectedBoardCount;

    @Schema(description = "이미 타겟 옵션을 가진 업체 수 (중복)")
    private Integer duplicateCompanyCount;

    @Schema(description = "이미 타겟 옵션을 가진 게시글 수 (중복)")
    private Integer duplicateBoardCount;

    @Schema(description = "영향받는 업체 샘플 목록 (최대 10개)")
    private List<AffectedCompanyInfo> affectedCompanySamples;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionInfo {
        @Schema(description = "옵션 ID")
        private Long id;

        @Schema(description = "옵션 코드")
        private String code;

        @Schema(description = "옵션 이름")
        private String name;

        @Schema(description = "카테고리 코드")
        private String categoryCode;

        @Schema(description = "카테고리 이름")
        private String categoryName;

        @Schema(description = "사용 횟수")
        private Integer usageCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AffectedCompanyInfo {
        @Schema(description = "업체 ID")
        private Long id;

        @Schema(description = "업체 이름")
        private String name;

        @Schema(description = "이미 타겟 옵션을 가지고 있는지 여부")
        private Boolean hasDuplicate;
    }
}
