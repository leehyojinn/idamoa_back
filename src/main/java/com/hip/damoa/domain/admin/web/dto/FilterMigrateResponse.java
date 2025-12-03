package com.hip.damoa.domain.admin.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 필터 옵션 마이그레이션 결과 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "필터 옵션 마이그레이션 결과 응답")
public class FilterMigrateResponse {

    @Schema(description = "소스 옵션 코드")
    private String sourceOptionCode;

    @Schema(description = "소스 옵션 이름")
    private String sourceOptionName;

    @Schema(description = "타겟 옵션 코드")
    private String targetOptionCode;

    @Schema(description = "타겟 옵션 이름")
    private String targetOptionName;

    @Schema(description = "마이그레이션된 업체 수")
    private Integer migratedCompanyCount;

    @Schema(description = "마이그레이션된 게시글 수")
    private Integer migratedBoardCount;

    @Schema(description = "건너뛴 업체 수 (중복으로 인해)")
    private Integer skippedCompanyCount;

    @Schema(description = "건너뛴 게시글 수 (중복으로 인해)")
    private Integer skippedBoardCount;

    @Schema(description = "소스 옵션 비활성화 여부")
    private Boolean sourceDeactivated;

    @Schema(description = "소스 옵션 삭제 여부")
    private Boolean sourceDeleted;

    @Schema(description = "마이그레이션 완료 시간")
    private LocalDateTime completedAt;

    @Schema(description = "총 처리 시간 (밀리초)")
    private Long processingTimeMs;
}
