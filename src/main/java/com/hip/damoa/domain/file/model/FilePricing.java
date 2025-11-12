package com.hip.damoa.domain.file.model;

import com.hip.damoa.domain.common.BaseTimeEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * 파일 가격 정보 엔티티
 *
 * 파일 다운로드 과금 정보를 관리합니다
 */
@Entity
@Table(name = "file_pricing", indexes = {
    @Index(name = "idx_file_pricing_file_id", columnList = "file_id"),
    @Index(name = "idx_file_pricing_is_paid", columnList = "is_paid"),
    @Index(name = "idx_file_pricing_is_active", columnList = "is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilePricing extends BaseTimeEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private File file;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(name = "price", nullable = false)
    private Integer price = 0;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "KRW";

    @Column(name = "download_limit")
    private Integer downloadLimit;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Builder
    public FilePricing(File file, Boolean isPaid, Integer price, String currency,
                       Integer downloadLimit, Boolean isActive, String description,
                       Map<String, Object> metadata, Long createdBy) {
        this.file = file;
        this.isPaid = isPaid != null ? isPaid : false;
        this.price = price != null ? price : 0;
        this.currency = currency != null ? currency : "KRW";
        this.downloadLimit = downloadLimit;
        this.isActive = isActive != null ? isActive : true;
        this.description = description;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.createdBy = createdBy;
    }

    // 비즈니스 메서드
    public void updatePrice(Integer price) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.price = price;
    }

    public void updateDownloadLimit(Integer downloadLimit) {
        if (downloadLimit != null && downloadLimit <= 0) {
            throw new IllegalArgumentException("Download limit must be positive");
        }
        this.downloadLimit = downloadLimit;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void setAsFree() {
        this.isPaid = false;
        this.price = 0;
    }

    public void setAsPaid(Integer price) {
        if (price <= 0) {
            throw new IllegalArgumentException("Paid file price must be greater than 0");
        }
        this.isPaid = true;
        this.price = price;
    }

    public void setUpdatedBy(Long userId) {
        this.updatedBy = userId;
    }

    public boolean isFree() {
        return !this.isPaid || this.price == 0;
    }

    public boolean hasDownloadLimit() {
        return this.downloadLimit != null && this.downloadLimit > 0;
    }
}
