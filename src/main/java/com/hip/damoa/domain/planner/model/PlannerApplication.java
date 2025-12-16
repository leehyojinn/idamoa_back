package com.hip.damoa.domain.planner.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * 플래너 신청서 엔티티
 *
 * USER가 플래너 상담을 신청
 * PENDING 상태에서만 수정/삭제 가능
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "planner_applications")
public class PlannerApplication extends BaseEntity {

    // 사용자 정보 (USER만 신청 가능)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;

    // 신청 정보
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_method", nullable = false, length = 20)
    private ConsultationMethod consultationMethod;

    @Type(StringArrayType.class)
    @Column(name = "request_types", nullable = false, columnDefinition = "text[]")
    private String[] requestTypes;

    // 신청자 정보
    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;

    @Column(name = "applicant_phone", nullable = false, length = 20)
    private String applicantPhone;

    @Column(name = "applicant_email", nullable = false, length = 100)
    private String applicantEmail;

    // 사업장 정보
    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;

    @Column(name = "business_area_size", length = 50)
    private String businessAreaSize;

    @Column(name = "business_type", length = 100)
    private String businessType;

    // 첨부파일 (OneToMany)
    @OneToMany(mappedBy = "plannerApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlannerApplicationAttachment> attachments = new ArrayList<>();

    // 희망 일정 (OneToMany)
    @OneToMany(mappedBy = "plannerApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlannerPreferredDate> preferredDates = new ArrayList<>();

    // 상태 관리
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PlannerApplicationStatus status = PlannerApplicationStatus.PENDING;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 Admin의 경우 null 반환
    private User assignedAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User deletedBy;

    // ===== 비즈니스 메서드 =====

    /**
     * 본인 확인
     */
    public boolean isOwnedBy(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }

    /**
     * 상태 변경
     */
    public void changeStatus(PlannerApplicationStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 관리자 배정
     */
    public void assignAdmin(User admin) {
        this.assignedAdmin = admin;
    }

    /**
     * 답변 작성
     */
    public void addResponse(String response) {
        this.adminResponse = response;
    }

    /**
     * 메모 작성
     */
    public void addMemo(String memo) {
        this.adminMemo = memo;
    }

    /**
     * 희망 일정 추가
     */
    public void addPreferredDate(PlannerPreferredDate preferredDate) {
        this.preferredDates.add(preferredDate);
        preferredDate.setPlannerApplication(this);
    }

    /**
     * Soft Delete with deletedBy
     */
    public void softDelete(User deletedBy) {
        super.softDelete();
        this.deletedBy = deletedBy;
    }

    /**
     * 첨부파일 추가
     */
    public void addAttachment(PlannerApplicationAttachment attachment) {
        this.attachments.add(attachment);
        attachment.setPlannerApplication(this);
    }

    /**
     * 희망 일정 초기화 (수정 시 사용)
     */
    public void clearPreferredDates() {
        this.preferredDates.clear();
    }

    /**
     * 수정 가능 여부 확인
     */
    public boolean canModify() {
        return this.status == PlannerApplicationStatus.PENDING;
    }

    /**
     * 신청서 정보 수정 (PENDING 상태에서만)
     */
    public void update(String title, String content, ConsultationMethod consultationMethod,
                       String[] requestTypes, String applicantName, String applicantPhone,
                       String applicantEmail, String businessName, String businessAddress,
                       String businessAreaSize, String businessType) {
        this.title = title;
        this.content = content;
        this.consultationMethod = consultationMethod;
        this.requestTypes = requestTypes;
        this.applicantName = applicantName;
        this.applicantPhone = applicantPhone;
        this.applicantEmail = applicantEmail;
        this.businessName = businessName;
        this.businessAddress = businessAddress;
        this.businessAreaSize = businessAreaSize;
        this.businessType = businessType;
    }
}
