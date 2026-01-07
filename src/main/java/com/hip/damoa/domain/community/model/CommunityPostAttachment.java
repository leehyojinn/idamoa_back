package com.hip.damoa.domain.community.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 커뮤니티 게시글 첨부파일 엔티티
 */
@Entity
@Table(name = "community_post_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "attachment_type", length = 20)
    private String attachmentType = "IMAGE";

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public CommunityPostAttachment(CommunityPost post, Long fileId, String attachmentType, Integer displayOrder) {
        this.post = post;
        this.fileId = fileId;
        this.attachmentType = attachmentType != null ? attachmentType : "IMAGE";
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }
}
