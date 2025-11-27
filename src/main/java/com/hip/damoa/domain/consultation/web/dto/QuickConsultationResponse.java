package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 빠른상담 상세 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickConsultationResponse {

    private UUID uuid;

    // 신청자 정보
    private String name;
    private String phone;
    private String email;

    // 상담 내용
    private String subject;
    private String message;
    private String preferredContactMethod;
    private String preferredContactTime;

    // 동의 정보
    private Boolean personalInfoConsent;
    private LocalDateTime personalInfoConsentAt;
    private Boolean thirdPartyConsent;
    private LocalDateTime thirdPartyConsentAt;
    private Boolean termsOfServiceConsent;
    private LocalDateTime termsOfServiceConsentAt;
    private Boolean marketingConsent;
    private LocalDateTime marketingConsentAt;

    // 상태 및 처리 정보
    private ConsultationStatus status;
    private String responseMessage;
    private LocalDateTime respondedAt;
    private LocalDateTime completedAt;
    private String completionNotes;
    private String cancellationReason;

    // 배정 정보 (관리자만 조회 가능)
    private UUID assignedCompanyUuid;
    private String assignedCompanyName;
    private LocalDateTime assignedAt;

    // 시간 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 회원 여부
    private Boolean isMember;

    // 삭제 상태 (관리자용)
    private Boolean isDeleted;
    private LocalDateTime deletedAt;

    public static QuickConsultationResponse from(QuickConsultation consultation) {
        return QuickConsultationResponse.builder()
                .uuid(consultation.getUuid())
                .name(consultation.getName())
                .phone(consultation.getPhone())
                .email(consultation.getEmail())
                .subject(consultation.getSubject())
                .message(consultation.getMessage())
                .preferredContactMethod(consultation.getPreferredContactMethod())
                .preferredContactTime(consultation.getPreferredContactTime())
                .personalInfoConsent(consultation.getPersonalInfoConsent())
                .personalInfoConsentAt(consultation.getPersonalInfoConsentAt())
                .thirdPartyConsent(consultation.getThirdPartyConsent())
                .thirdPartyConsentAt(consultation.getThirdPartyConsentAt())
                .termsOfServiceConsent(consultation.getTermsOfServiceConsent())
                .termsOfServiceConsentAt(consultation.getTermsOfServiceConsentAt())
                .marketingConsent(consultation.getMarketingConsent())
                .marketingConsentAt(consultation.getMarketingConsentAt())
                .status(consultation.getStatus())
                .responseMessage(consultation.getResponseMessage())
                .respondedAt(consultation.getRespondedAt())
                .completedAt(consultation.getCompletedAt())
                .completionNotes(consultation.getCompletionNotes())
                .cancellationReason(consultation.getCancellationReason())
                .assignedCompanyUuid(consultation.getAssignedCompany() != null ?
                        consultation.getAssignedCompany().getUuid() : null)
                .assignedCompanyName(consultation.getAssignedCompany() != null ?
                        consultation.getAssignedCompany().getName() : null)
                .assignedAt(consultation.getAssignedAt())
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .isMember(consultation.isMember())
                .isDeleted(consultation.getIsDeleted())
                .deletedAt(consultation.getDeletedAt())
                .build();
    }
}
