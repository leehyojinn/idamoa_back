package com.hip.damoa.domain.consultation.web.dto;

import com.hip.damoa.domain.consultation.model.ConsultationStatus;
import com.hip.damoa.domain.consultation.model.QuickConsultation;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "빠른상담 상세 응답")
public class QuickConsultationResponse {

    @Schema(description = "상담 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    // 신청자 정보
    @Schema(description = "신청자 이름", example = "홍길동")
    private String name;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    // 상담 내용
    @Schema(description = "상담 제목", example = "인테리어 상담 문의")
    private String subject;

    @Schema(description = "상담 내용", example = "사무실 인테리어 관련해서 문의드립니다.")
    private String message;

    @Schema(description = "선호 연락 방법 (PHONE, EMAIL, KAKAO)", example = "PHONE")
    private String preferredContactMethod;

    @Schema(description = "선호 연락 시간", example = "평일 오전 10시~12시")
    private String preferredContactTime;

    // 동의 정보
    @Schema(description = "개인정보 수집 동의 여부", example = "true")
    private Boolean personalInfoConsent;

    @Schema(description = "개인정보 수집 동의 일시", example = "2025-01-01T10:00:00")
    private LocalDateTime personalInfoConsentAt;

    @Schema(description = "제3자 정보 제공 동의 여부", example = "true")
    private Boolean thirdPartyConsent;

    @Schema(description = "제3자 정보 제공 동의 일시", example = "2025-01-01T10:00:00")
    private LocalDateTime thirdPartyConsentAt;

    @Schema(description = "이용약관 동의 여부", example = "true")
    private Boolean termsOfServiceConsent;

    @Schema(description = "이용약관 동의 일시", example = "2025-01-01T10:00:00")
    private LocalDateTime termsOfServiceConsentAt;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "false")
    private Boolean marketingConsent;

    @Schema(description = "마케팅 정보 수신 동의 일시", example = "2025-01-01T10:00:00")
    private LocalDateTime marketingConsentAt;

    // 상태 및 처리 정보
    @Schema(description = "상담 상태 (SUBMITTED, IN_PROGRESS, COMPLETED, CANCELLED)", example = "SUBMITTED")
    private ConsultationStatus status;

    @Schema(description = "관리자 답변 메시지", example = "문의 주셔서 감사합니다. 곧 연락드리겠습니다.")
    private String responseMessage;

    @Schema(description = "답변 일시", example = "2025-01-02T10:00:00")
    private LocalDateTime respondedAt;

    @Schema(description = "완료 일시", example = "2025-01-05T14:00:00")
    private LocalDateTime completedAt;

    @Schema(description = "완료 메모", example = "상담 완료, 계약 진행 예정")
    private String completionNotes;

    @Schema(description = "취소 사유", example = "고객 요청으로 취소")
    private String cancellationReason;

    // 배정 정보 (관리자만 조회 가능)
    @Schema(description = "배정된 업체 UUID (관리자용)", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID assignedCompanyUuid;

    @Schema(description = "배정된 업체명 (관리자용)", example = "다모아 인테리어")
    private String assignedCompanyName;

    @Schema(description = "배정 일시 (관리자용)", example = "2025-01-02T09:00:00")
    private LocalDateTime assignedAt;

    // 시간 정보
    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-02T14:00:00")
    private LocalDateTime updatedAt;

    // 회원 여부
    @Schema(description = "회원 여부", example = "true")
    private Boolean isMember;

    // 삭제 상태 (관리자용)
    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    @Schema(description = "삭제 일시 (관리자용)", example = "2025-01-15T10:00:00")
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
