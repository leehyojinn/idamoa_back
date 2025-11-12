package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 첨부파일 중간 테이블
 *
 * company_images, estimate_attachments와 동일한 패턴으로
 * boards와 files를 연결하는 중간 테이블
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_attachments", indexes = {
    @Index(name = "idx_board_attachments_board_id", columnList = "board_id"),
    @Index(name = "idx_board_attachments_file_id", columnList = "file_id"),
    @Index(name = "idx_board_attachments_type", columnList = "attachment_type"),
    @Index(name = "idx_board_attachments_board_type", columnList = "board_id, attachment_type"),
    @Index(name = "idx_board_attachments_order", columnList = "board_id, display_order")
})
public class BoardAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @Column(name = "file_id", nullable = false)
    private Long fileId;  // files 테이블 ID 참조

    @Column(name = "attachment_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AttachmentType attachmentType;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 첨부파일 타입
     */
    public enum AttachmentType {
        IMAGE,      // 이미지 (GALLERY용)
        DOCUMENT,   // 문서 파일 (DOCUMENT용)
        THUMBNAIL,  // 썸네일 (DOCUMENT용)
        OTHER       // 기타
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
