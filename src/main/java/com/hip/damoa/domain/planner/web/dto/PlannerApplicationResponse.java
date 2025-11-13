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
import java.util.stream.Collectors;

/**
 * 플래너 신청서 상세 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerApplicationResponse {

    private UUID uuid;
    private Long userId;

    // 신청 정보
    private String title;
    private String content;
    private ConsultationMethod consultationMethod;
    private List<String> requestTypes;

    // 신청자 정보
    private String applicantName;
    private String applicantPhone;
    private String applicantEmail;

    // 사업장 정보
    private String businessName;
    private String businessAddress;
    private String businessAreaSize;
    private String businessType;

    // 첨부파일
    private List<Long> attachmentFileIds;

    // 희망 일정
    private List<PreferredDateDto> preferredDates;

    // 상태 관리
    private PlannerApplicationStatus status;
    private String adminResponse;
    private String adminMemo;
    private Long assignedAdminId;
    private String assignedAdminName;

    // 타임스탬프
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → DTO 변환
     */
    public static PlannerApplicationResponse from(PlannerApplication entity) {
        List<String> requestTypes = entity.getRequestTypes() != null
                ? Arrays.asList(entity.getRequestTypes())
                : List.of();

        List<Long> attachmentFileIds = entity.getAttachmentFileIds() != null
                ? Arrays.asList(entity.getAttachmentFileIds())
                : List.of();

        List<PreferredDateDto> preferredDates = entity.getPreferredDates().stream()
                .map(PreferredDateDto::from)
                .collect(Collectors.toList());

        return PlannerApplicationResponse.builder()
                .uuid(entity.getUuid())
                .userId(entity.getUser().getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .consultationMethod(entity.getConsultationMethod())
                .requestTypes(requestTypes)
                .applicantName(entity.getApplicantName())
                .applicantPhone(entity.getApplicantPhone())
                .applicantEmail(entity.getApplicantEmail())
                .businessName(entity.getBusinessName())
                .businessAddress(entity.getBusinessAddress())
                .businessAreaSize(entity.getBusinessAreaSize())
                .businessType(entity.getBusinessType())
                .attachmentFileIds(attachmentFileIds)
                .preferredDates(preferredDates)
                .status(entity.getStatus())
                .adminResponse(entity.getAdminResponse())
                .adminMemo(entity.getAdminMemo())
                .assignedAdminId(entity.getAssignedAdmin() != null ? entity.getAssignedAdmin().getId() : null)
                .assignedAdminName(entity.getAssignedAdmin() != null ? entity.getAssignedAdmin().getEmail() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
