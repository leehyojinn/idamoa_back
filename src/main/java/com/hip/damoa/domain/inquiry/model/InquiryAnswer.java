package com.hip.damoa.domain.inquiry.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * 일반 문의 답변 엔티티
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inquiry_answers")
public class InquiryAnswer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false, unique = true)
    private Inquiry inquiry;  // 문의

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 Admin의 경우 null 반환
    private User admin;  // 답변 작성 관리자

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;  // 답변 내용

    // ===== Business Methods =====

    /**
     * 답변 내용 수정
     */
    public void updateContent(String content) {
        this.content = content;
    }
}