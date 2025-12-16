package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.ConsultationMethod;
import com.hip.damoa.domain.planner.model.PlannerApplication;
import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.model.PlannerPreferredDate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 플래너 신청 상세 응답 DTO (공개용)
 * 민감정보 제외 (전화번호, 이메일 제외)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "플래너 신청 상세 응답 (공개)")
public class PlannerApplicationDetailResponse {

    @Schema(description = "신청서 UUID")
    private UUID uuid;

    @Schema(description = "신청서 제목")
    private String title;

    @Schema(description = "신청 내용")
    private String content;

    @Schema(description = "상담 방법")
    private ConsultationMethod consultationMethod;

    @Schema(description = "요청 내용")
    @Builder.Default
    private List<String> requestTypes = List.of();

    @Schema(description = "신청자 이름")
    private String applicantName;

    @Schema(description = "사업장명")
    private String businessName;

    @Schema(description = "사업장 주소")
    private String businessAddress;

    @Schema(description = "사업장 면적")
    private String businessAreaSize;

    @Schema(description = "업종")
    private String businessType;

    @Schema(description = "첨부파일 목록")
    @Builder.Default
    private List<AttachmentDto> attachments = List.of();

    @Schema(description = "희망 상담 일정")
    @Builder.Default
    private List<PreferredDateDto> preferredDates = List.of();

    @Schema(description = "상태")
    private PlannerApplicationStatus status;

    @Schema(description = "관리자 답변")
    private String adminResponse;

    @Schema(description = "신청일")
    private LocalDateTime createdAt;

    @Schema(description = "수정일")
    private LocalDateTime updatedAt;

    /**
     * Entity -> DTO 변환
     */
    public static PlannerApplicationDetailResponse from(PlannerApplication application,
                                                         List<AttachmentDto> attachmentDtos) {
        // 희망 일정 변환
        List<PreferredDateDto> preferredDateDtos = application.getPreferredDates().stream()
                .map(pd -> PreferredDateDto.builder()
                        .priority(pd.getPriority())
                        .preferredDate(pd.getPreferredDate())
                        .preferredTime(pd.getPreferredTime())
                        .build())
                .collect(Collectors.toList());

        return PlannerApplicationDetailResponse.builder()
                .uuid(application.getUuid())
                .title(application.getTitle())
                .content(application.getContent())
                .consultationMethod(application.getConsultationMethod())
                .requestTypes(application.getRequestTypes() != null
                        ? List.of(application.getRequestTypes())
                        : List.of())
                .applicantName(application.getApplicantName())
                .businessName(application.getBusinessName())
                .businessAddress(application.getBusinessAddress())
                .businessAreaSize(application.getBusinessAreaSize())
                .businessType(application.getBusinessType())
                .attachments(attachmentDtos)
                .preferredDates(preferredDateDtos)
                .status(application.getStatus())
                .adminResponse(application.getAdminResponse())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}
