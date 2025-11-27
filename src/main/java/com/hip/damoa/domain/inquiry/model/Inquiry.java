package com.hip.damoa.domain.inquiry.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 일반 문의 엔티티 (버그, 결제 오류, 계정 문제 등)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inquiries")
public class Inquiry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 문의 작성자 (필수)

    @Column(name = "title", nullable = false, length = 200)
    private String title;  // 문의 제목

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 20)
    private InquiryType inquiryType;

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
     * 상태 변경: 답변 완료로
     */
    public void markAnswered() {
        this.status = InquiryStatus.ANSWERED;
    }

    /**
     * 상태 변경: 종료로
     */
    public void close() {
        this.status = InquiryStatus.CLOSED;
    }

    /**
     * 문의 내용 수정 (작성자용)
     */
    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    /**
     * 상태 업데이트 (관리자용)
     */
    public void updateStatus(String status) {
        this.status = InquiryStatus.valueOf(status);
    }
}
