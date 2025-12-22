package com.hip.damoa.domain.portfolio.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.CompanyPortfolio;
import com.hip.damoa.domain.file.model.File;
import jakarta.persistence.*;
import lombok.*;

/**
 * 포트폴리오 첨부파일 중간 테이블
 *
 * board_attachments와 동일한 패턴으로
 * company_portfolios와 files를 연결하는 중간 테이블
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "portfolio_attachments", indexes = {
    @Index(name = "idx_portfolio_attachments_portfolio_id", columnList = "portfolio_id"),
    @Index(name = "idx_portfolio_attachments_file_id", columnList = "file_id"),
    @Index(name = "idx_portfolio_attachments_type", columnList = "attachment_type"),
    @Index(name = "idx_portfolio_attachments_order", columnList = "portfolio_id, display_order")
})
public class PortfolioAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private CompanyPortfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(name = "attachment_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AttachmentType attachmentType = AttachmentType.IMAGE;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 첨부파일 타입
     */
    public enum AttachmentType {
        IMAGE,      // 이미지
        VIDEO,      // 동영상
        THUMBNAIL   // 썸네일
    }

    // ===== Business Methods =====

    /**
     * 표시 순서 변경
     */
    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }

    /**
     * 설명 수정
     */
    public void updateDescription(String description) {
        this.description = description;
    }
}
