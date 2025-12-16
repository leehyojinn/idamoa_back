package com.hip.damoa.domain.partnership.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * 제휴/광고 문의 엔티티
 * - 비회원도 작성 가능
 * - 답변 시스템 없음 (직접 연락)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "partnership_inquiries")
public class PartnershipInquiry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;  // 문의 작성자 (회원인 경우)

    @Enumerated(EnumType.STRING)
    @Column(name = "partnership_type", nullable = false, length = 20)
    private PartnershipType partnershipType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;  // 작성자명

    @Column(name = "email", nullable = false, length = 100)
    private String email;  // 이메일

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;  // 연락처

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;  // 문의 내용

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PartnershipStatus status = PartnershipStatus.PENDING;

    // ===== Business Methods =====

    /**
     * 상태 변경: 처리 중으로
     */
    public void startProgress() {
        this.status = PartnershipStatus.IN_PROGRESS;
    }

    /**
     * 상태 변경: 완료
     */
    public void complete() {
        this.status = PartnershipStatus.COMPLETED;
    }

    /**
     * 상태 변경: 취소
     */
    public void cancel() {
        this.status = PartnershipStatus.CANCELLED;
    }

    /**
     * 상태 업데이트 (관리자용)
     */
    public void updateStatus(PartnershipStatus status) {
        this.status = status;
    }
}