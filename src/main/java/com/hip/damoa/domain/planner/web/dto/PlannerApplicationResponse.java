package com.hip.damoa.domain.planner.web.dto;

import com.hip.damoa.domain.planner.model.PlannerApplicationStatus;
import com.hip.damoa.domain.planner.model.ConsultationMethod;
import com.hip.damoa.domain.planner.model.PlannerApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 플래너 신청서 상세 응답 DTO
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlannerApplicationResponse {

    private UUID uuid;
    private UUID userUuid;

    // 신청 정보
    private String title;
    private String content;
    private ConsultationMethod consultationMethod;
    @Builder.Default
    private List<String> requestTypes = List.of();

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
    @Builder.Default
    private List<AttachmentDto> attachments = List.of();

    // 희망 일정
    @Builder.Default
    private List<PreferredDateDto> preferredDates = List.of();

    // 상태 관리
    private PlannerApplicationStatus status;
    private String adminResponse;
    private String adminMemo;
    private UUID assignedAdminUuid;
    private String assignedAdminName;

    // 타임스탬프
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 삭제 상태 (관리자용)
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    /**
     * Entity → DTO 변환 (레거시 호환 - 첨부파일 정보 없음)
     */
    public static PlannerApplicationResponse from(PlannerApplication entity) {
        return from(entity, List.of());
    }

    /**
     * Entity + 첨부파일 정보 → DTO 변환
     * 삭제된 User의 경우 안전하게 처리
     */
    public static PlannerApplicationResponse from(PlannerApplication entity, List<AttachmentDto> attachments) {
        List<String> requestTypes = entity.getRequestTypes() != null
                ? Arrays.asList(entity.getRequestTypes())
                : List.of();

        List<PreferredDateDto> preferredDates = entity.getPreferredDates().stream()
                .map(PreferredDateDto::from)
                .collect(Collectors.toList());

        UUID userUuid = null;
        try {
            if (entity.getUser() != null) {
                userUuid = entity.getUser().getUuid();
            }
        } catch (Exception e) {
            log.warn("User not found for plannerApplication: {}", entity.getId());
        }

        UUID assignedAdminUuid = null;
        String assignedAdminName = null;
        try {
            if (entity.getAssignedAdmin() != null) {
                assignedAdminUuid = entity.getAssignedAdmin().getUuid();
                assignedAdminName = entity.getAssignedAdmin().getEmail();
            }
        } catch (Exception e) {
            log.warn("AssignedAdmin not found for plannerApplication: {}", entity.getId());
        }

        return PlannerApplicationResponse.builder()
                .uuid(entity.getUuid())
                .userUuid(userUuid)
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
                .attachments(attachments != null ? attachments : List.of())
                .preferredDates(preferredDates)
                .status(entity.getStatus())
                .adminResponse(entity.getAdminResponse())
                .adminMemo(entity.getAdminMemo())
                .assignedAdminUuid(assignedAdminUuid)
                .assignedAdminName(assignedAdminName)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .isDeleted(entity.getIsDeleted())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
