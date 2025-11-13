package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.model.ConsultationMethod;
import com.hip.damoa.domain.planner.model.PlannerApplication;
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
public class PlannerApplicationListResponse {

    private UUID uuid;
    private String title;
    private ConsultationMethod consultationMethod;
    private List<String> requestTypes;
    private String applicantName;
    private PlannerApplicationStatus status;
    private LocalDateTime createdAt;

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
                .build();
    }
}
