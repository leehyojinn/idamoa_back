package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 견적 첨부파일
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_attachments", indexes = {
    @Index(name = "idx_estimate_attachments_request_id", columnList = "request_id"),
    @Index(name = "idx_estimate_attachments_proposal_id", columnList = "proposal_id")
})
public class EstimateAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private EstimateRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private EstimateProposal proposal;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ===== Business Methods =====

    /**
     * 다운로드 횟수 증가
     */
    public void incrementDownloadCount() {
        this.downloadCount++;
    }
}
