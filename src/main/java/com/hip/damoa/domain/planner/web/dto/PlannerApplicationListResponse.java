package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.model.ConsultationMethod;
import com.hip.damoa.domain.planner.model.PlannerApplication;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 플래너 신청서 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "플래너 신청서 목록 응답")
public class PlannerApplicationListResponse {

    @Schema(description = "신청서 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "신청서 제목", example = "카페 인테리어 컨설팅 신청")
    private String title;

    @Schema(description = "상담 방법 (VISIT, PHONE, SNS)", example = "VISIT")
    private ConsultationMethod consultationMethod;

    @Schema(description = "요청 유형 목록", example = "[\"FULL_CONSULTING\", \"NEW_OPENING\"]")
    private List<String> requestTypes;

    @Schema(description = "신청자 이름", example = "홍길동")
    private String applicantName;

    @Schema(description = "신청 상태 (PENDING, IN_PROGRESS, COMPLETED, REJECTED)", example = "PENDING")
    private PlannerApplicationStatus status;

    @Schema(description = "신청일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    /**
     * Entity → DTO 변환
     */
    public static PlannerApplicationListResponse from(PlannerApplication entity) {
        List<String> requestTypes = entity.getRequestTypes() != null
                ? Arrays.asList(entity.getRequestTypes())
                : List.of();

        return PlannerApplicationListResponse.builder()
                .uuid(entity.getUuid())
                .title(entity.getTitle())
                .consultationMethod(entity.getConsultationMethod())
                .requestTypes(requestTypes)
                .applicantName(entity.getApplicantName())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .isDeleted(entity.getIsDeleted())
                .build();
    }
}
