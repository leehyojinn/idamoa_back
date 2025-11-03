package com.hip.damoa.domain.file.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * 파일
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "files", indexes = {
    @Index(name = "idx_files_uploader_id", columnList = "uploader_id"),
    @Index(name = "idx_files_category_id", columnList = "category_id"),
    @Index(name = "idx_files_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_files_created_at", columnList = "created_at")
})
public class File extends BaseEntity {

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_extension", length = 20)
    private String fileExtension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "entity_type", length = 50)
    private String entityType; // USER_PROFILE, COMPANY_IMAGE, PORTFOLIO, ESTIMATE, REVIEW, etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "download_count", nullable = false)
    @Builder.Default
    private Long downloadCount = 0L;

    @Type(JsonBinaryType.class)
    @Column(name = "image_metadata", columnDefinition = "jsonb")
    private Map<String, Object> imageMetadata; // width, height, etc.

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public void updateDisplayOrder(Integer order) {
        this.displayOrder = order;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void makePublic() {
        this.isPublic = true;
    }

    public void makePrivate() {
        this.isPublic = false;
    }
}
