package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 견적 요청
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_requests", indexes = {
    @Index(name = "idx_estimate_requests_user_id", columnList = "user_id"),
    @Index(name = "idx_estimate_requests_status", columnList = "status"),
    @Index(name = "idx_estimate_requests_created_at", columnList = "created_at")
})
public class EstimateRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Type(JsonBinaryType.class)
    @Column(name = "requirements", columnDefinition = "jsonb")
    private Map<String, Object> requirements;

    @Column(name = "budget_min", precision = 12, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 12, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "desired_start_date")
    private LocalDate desiredStartDate;

    @Column(name = "desired_end_date")
    private LocalDate desiredEndDate;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Type(StringArrayType.class)
    @Column(name = "images", columnDefinition = "text[]")
    private String[] images;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Type(StringArrayType.class)
    @Column(name = "required_skills", columnDefinition = "text[]")
    private String[] requiredSkills;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @org.hibernate.annotations.ColumnDefault("'DRAFT'")
    @Builder.Default
    private EstimateStatus status = EstimateStatus.DRAFT;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "proposal_count", nullable = false)
    @Builder.Default
    private Integer proposalCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // ===== 범용 필드 추가 (V26 Migration) =====

    @Column(name = "client_name", length = 200)
    private String clientName; // 사업장명/고객명

    @Column(name = "business_type", length = 100)
    private String businessType; // 업종 (카페, 병원, 사무실, 매장 등)

    @Column(name = "area_pyeong", precision = 10, scale = 2)
    private BigDecimal areaPyeong; // 평수

    @Column(name = "contact_name", length = 100)
    private String contactName; // 신청자 이름

    @Column(name = "contact_phone", length = 20)
    private String contactPhone; // 연락처

    @Column(name = "submission_deadline")
    private LocalDateTime submissionDeadline; // 제안 마감일

    // ===== 조인 테이블 관계 (V30 Migration) =====

    @OneToMany(mappedBy = "estimateRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EstimateRequestAttachment> attachments = new ArrayList<>();

    // ===== Business Methods =====

    /**
     * 견적 요청 발행
     */
    public void publish() {
        this.status = EstimateStatus.PUBLISHED;
    }

    /**
     * 진행 중으로 변경
     */
    public void startProgress() {
        this.status = EstimateStatus.IN_PROGRESS;
    }

    /**
     * 매칭 완료
     */
    public void match() {
        this.status = EstimateStatus.MATCHED;
    }

    /**
     * 완료 처리
     */
    public void complete() {
        this.status = EstimateStatus.COMPLETED;
    }

    /**
     * 취소 처리
     */
    public void cancel() {
        this.status = EstimateStatus.CANCELLED;
    }

    /**
     * 제안 수 증가
     */
    public void incrementProposalCount() {
        this.proposalCount++;
    }

    /**
     * 제안 수 감소
     */
    public void decrementProposalCount() {
        if (this.proposalCount > 0) {
            this.proposalCount--;
        }
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    // ===== Update Methods =====

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateCategory(String category) {
        this.category = category;
    }

    public void updateRequirements(Map<String, Object> requirements) {
        this.requirements = requirements;
    }

    public void updateBudgetMin(BigDecimal budgetMin) {
        this.budgetMin = budgetMin;
    }

    public void updateBudgetMax(BigDecimal budgetMax) {
        this.budgetMax = budgetMax;
    }

    public void updateDesiredStartDate(LocalDate desiredStartDate) {
        this.desiredStartDate = desiredStartDate;
    }

    public void updateDesiredEndDate(LocalDate desiredEndDate) {
        this.desiredEndDate = desiredEndDate;
    }

    public void updateLocation(String location) {
        this.location = location;
    }

    public void updateAddress(String address) {
        this.address = address;
    }

    public void updateIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // V26 field update methods
    public void updateClientName(String clientName) {
        this.clientName = clientName;
    }

    public void updateBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public void updateAreaPyeong(BigDecimal areaPyeong) {
        this.areaPyeong = areaPyeong;
    }

    public void updateContactName(String contactName) {
        this.contactName = contactName;
    }

    public void updateContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void updateSubmissionDeadline(LocalDateTime submissionDeadline) {
        this.submissionDeadline = submissionDeadline;
    }

    // ===== 첨부파일 관리 메서드 =====

    /**
     * 첨부파일 추가
     */
    public void addAttachment(EstimateRequestAttachment attachment) {
        this.attachments.add(attachment);
    }

    /**
     * 첨부파일 제거
     */
    public void removeAttachment(EstimateRequestAttachment attachment) {
        this.attachments.remove(attachment);
    }

    /**
     * 모든 첨부파일 제거
     */
    public void clearAttachments() {
        this.attachments.clear();
    }


    // ===== Getter 메서드 추가 (Service에서 사용) =====

    public String getStatusString() {
        return this.status.name();
    }

    public String getVisibility() {
        return this.isPublic ? "PUBLIC" : "PRIVATE";
    }
}