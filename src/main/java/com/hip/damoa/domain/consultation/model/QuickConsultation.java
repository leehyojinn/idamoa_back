package com.hip.damoa.domain.consultation.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 빠른상담 엔티티
 *
 * 비회원/회원 모두 상담 신청 가능
 * 비회원은 비밀번호(4자리)로 조회 가능
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quick_consultations")
public class QuickConsultation extends BaseEntity {

    // 사용자 정보 (비회원인 경우 null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 신청자 정보 (비회원도 필수)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    // 비밀번호 (비회원 조회용, 4자리 평문)
    @Column(name = "password", length = 4)
    private String password;

    // 상담 내용
    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "preferred_contact_method", length = 20)
    private String preferredContactMethod;

    @Column(name = "preferred_contact_time", length = 100)
    private String preferredContactTime;

    // 4가지 동의
    @Column(name = "personal_info_consent", nullable = false)
    @Builder.Default
    private Boolean personalInfoConsent = false;

    @Column(name = "personal_info_consent_at")
    private LocalDateTime personalInfoConsentAt;

    @Column(name = "third_party_consent", nullable = false)
    @Builder.Default
    private Boolean thirdPartyConsent = false;

    @Column(name = "third_party_consent_at")
    private LocalDateTime thirdPartyConsentAt;

    @Column(name = "terms_of_service_consent", nullable = false)
    @Builder.Default
    private Boolean termsOfServiceConsent = false;

    @Column(name = "terms_of_service_consent_at")
    private LocalDateTime termsOfServiceConsentAt;

    @Column(name = "marketing_consent", nullable = false)
    @Builder.Default
    private Boolean marketingConsent = false;

    @Column(name = "marketing_consent_at")
    private LocalDateTime marketingConsentAt;

    @Column(name = "consent_ip_address", length = 50)
    private String consentIpAddress;

    @Column(name = "consent_version", length = 20)
    private String consentVersion;

    // 관리자 할당/처리
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_company_id")
    private Company assignedCompany;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ConsultationStatus status = ConsultationStatus.SUBMITTED;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by")
    private User respondedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // 메타데이터
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", length = 500)
    private String referrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    // ===== 비즈니스 메서드 =====

    /**
     * 회원인지 확인
     */
    public boolean isMember() {
        return this.user != null;
    }

    /**
     * 본인 확인 (회원)
     */
    public boolean isOwnedBy(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    /**
     * 비밀번호 검증 (비회원)
     */
    public boolean verifyPassword(String password) {
        return this.password != null && this.password.equals(password);
    }

    /**
     * 상태 변경
     */
    public void changeStatus(ConsultationStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 업체 배정
     */
    public void assignToCompany(Company company, User assignedBy) {
        this.assignedCompany = company;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
        this.status = ConsultationStatus.IN_PROGRESS;
    }

    /**
     * 답변 작성
     */
    public void respond(String responseMessage, User respondedBy) {
        this.responseMessage = responseMessage;
        this.respondedBy = respondedBy;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * 상담 완료
     */
    public void complete(String completionNotes) {
        this.completionNotes = completionNotes;
        this.completedAt = LocalDateTime.now();
        this.status = ConsultationStatus.COMPLETED;
    }

    /**
     * 상담 취소
     */
    public void cancel(String cancellationReason) {
        this.cancellationReason = cancellationReason;
        this.status = ConsultationStatus.CANCELLED;
    }

    /**
     * 상담 내용 수정 (SUBMITTED 상태에서만 가능)
     */
    public void updateConsultation(
            String name,
            String phone,
            String email,
            String subject,
            String message,
            String preferredContactMethod,
            String preferredContactTime) {

        this.name = name;
        this.phone = phone;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.preferredContactMethod = preferredContactMethod;
        this.preferredContactTime = preferredContactTime;
    }

    /**
     * 수정 가능 여부 확인 (SUBMITTED 상태만 수정 가능)
     */
    public boolean isEditable() {
        return this.status == ConsultationStatus.SUBMITTED;
    }

    /**
     * Soft Delete with deletedBy
     */
    public void softDelete(User deletedBy) {
        super.softDelete();
        this.deletedBy = deletedBy;
    }
}
