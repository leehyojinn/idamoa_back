package com.hip.damoa.domain.consultation.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

/**
 * 포트폴리오 상담신청 엔티티
 *
 * 포트폴리오를 보고 해당 업체에 상담신청하는 기능
 * 로그인 회원만 신청 가능
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_consultations", indexes = {
    @Index(name = "idx_portfolio_consultations_user_id", columnList = "user_id"),
    @Index(name = "idx_portfolio_consultations_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_portfolio_consultations_company_id", columnList = "company_id"),
    @Index(name = "idx_portfolio_consultations_status", columnList = "status"),
    @Index(name = "idx_portfolio_consultations_created_at", columnList = "created_at")
})
public class PortfolioConsultation extends BaseEntity {

    // 신청자 (로그인 유저)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    // 해당 포트폴리오
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CompanyPortfolio portfolio;

    // 업체 (조회 편의용, 포트폴리오에서 가져옴)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // 신청자 정보
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    // 상담 내용
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    // 연락 방법
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_method", nullable = false, length = 20)
    private ContactMethod contactMethod;

    // 연락 가능 시간
    @Column(name = "available_time", length = 200)
    private String availableTime;

    // 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PortfolioConsultationStatus status = PortfolioConsultationStatus.PENDING;

    // 업체 메모 (업체용)
    @Column(name = "company_memo", columnDefinition = "TEXT")
    private String companyMemo;

    // 답변 내용 (업체용)
    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    // 답변한 사람 (업체 소유자)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private User answeredBy;

    // ===== 비즈니스 메서드 =====

    /**
     * 본인 확인
     */
    public boolean isOwnedBy(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    /**
     * 업체 소유자 확인
     */
    public boolean isCompanyOwnedBy(Long ownerId) {
        return this.company != null && this.company.getOwner() != null
                && this.company.getOwner().getId().equals(ownerId);
    }

    /**
     * 수정 가능 여부 확인 (PENDING 상태만 수정 가능)
     */
    public boolean isEditable() {
        return this.status == PortfolioConsultationStatus.PENDING;
    }

    /**
     * 삭제 가능 여부 확인 (ANSWERED, COMPLETED 상태에서는 삭제 불가)
     */
    public boolean isDeletable() {
        return this.status != PortfolioConsultationStatus.ANSWERED
                && this.status != PortfolioConsultationStatus.COMPLETED;
    }

    /**
     * 상담신청 내용 수정 (null이 아닌 값만 업데이트)
     */
    public void updateConsultation(
            String name,
            String phone,
            String email,
            String title,
            String content,
            ContactMethod contactMethod,
            String availableTime) {

        if (name != null) this.name = name;
        if (phone != null) this.phone = phone;
        if (email != null) this.email = email;
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (contactMethod != null) this.contactMethod = contactMethod;
        if (availableTime != null) this.availableTime = availableTime;
    }

    /**
     * 상태 변경
     */
    public void changeStatus(PortfolioConsultationStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 메모 등록/수정
     */
    public void updateMemo(String memo) {
        this.companyMemo = memo;
    }

    /**
     * 답변 등록
     */
    public void submitAnswer(String answer, User answeredBy) {
        this.answer = answer;
        this.answeredAt = LocalDateTime.now();
        this.answeredBy = answeredBy;
        this.status = PortfolioConsultationStatus.ANSWERED;
    }

    /**
     * 취소
     */
    public void cancel() {
        this.status = PortfolioConsultationStatus.CANCELLED;
    }
}
