package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.ConsultationMethod;
import com.hip.damoa.domain.planner.model.PlannerApplication;
import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 플래너 신청 목록 응답 DTO (공개용)
 * 민감정보 제외 (전화번호, 이메일 제외)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "플래너 신청 목록 응답 (공개)")
public class PlannerApplicationSummaryResponse {

    @Schema(description = "신청서 UUID")
    private UUID uuid;

    @Schema(description = "신청서 제목")
    private String title;

    @Schema(description = "상담 방법")
    private ConsultationMethod consultationMethod;

    @Schema(description = "요청 내용")
    private List<String> requestTypes;

    @Schema(description = "신청자 이름")
    private String applicantName;

    @Schema(description = "사업장명")
    private String businessName;

    @Schema(description = "사업장 위치 (대략적)")
    private String businessAddress;

    @Schema(description = "업종")
    private String businessType;

    @Schema(description = "상태")
    private PlannerApplicationStatus status;

    @Schema(description = "신청일")
    private LocalDateTime createdAt;

    /**
     * Entity -> DTO 변환
     */
    public static PlannerApplicationSummaryResponse from(PlannerApplication application) {
        return PlannerApplicationSummaryResponse.builder()
                .uuid(application.getUuid())
                .title(application.getTitle())
                .consultationMethod(application.getConsultationMethod())
                .requestTypes(application.getRequestTypes() != null
                        ? List.of(application.getRequestTypes())
                        : List.of())
                .applicantName(application.getApplicantName())
                .businessName(application.getBusinessName())
                .businessAddress(application.getBusinessAddress())
                .businessType(application.getBusinessType())
                .status(application.getStatus())
                .createdAt(application.getCreatedAt())
                .build();
    }
}
