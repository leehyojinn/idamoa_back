package com.hip.damoa.domain.inquiry.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 제휴/광고 문의 엔티티
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inquiries")
public class Inquiry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // 회원일 경우만 (nullable)

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 20)
    private InquiryType inquiryType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;  // 작성자명

    @Column(name = "email", nullable = false, length = 100)
    private String email;  // 이메일주소

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;  // 연락처

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;  // 문의내용

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.PENDING;

    // ===== Business Methods =====

    /**
     * 상태 변경: 처리 중으로
     */
    public void startProgress() {
        this.status = InquiryStatus.IN_PROGRESS;
    }

    /**
     * 상태 변경: 완료로
     */
    public void complete() {
        this.status = InquiryStatus.COMPLETED;
    }

    /**
     * 상태 변경: 취소로
     */
    public void cancel() {
        this.status = InquiryStatus.CANCELLED;
    }
}
