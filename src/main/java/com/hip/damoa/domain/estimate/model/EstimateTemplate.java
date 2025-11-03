package com.hip.damoa.domain.estimate.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.company.model.Company;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * 견적 템플릿
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "estimate_templates", indexes = {
    @Index(name = "idx_estimate_templates_company_id", columnList = "company_id"),
    @Index(name = "idx_estimate_templates_category", columnList = "category")
})
public class EstimateTemplate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 100)
    private String category;

    @Type(JsonBinaryType.class)
    @Column(name = "template_fields", columnDefinition = "jsonb")
    private Map<String, Object> templateFields;

    @Type(JsonBinaryType.class)
    @Column(name = "default_items", columnDefinition = "jsonb")
    private Map<String, Object> defaultItems;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ===== Business Methods =====

    /**
     * 사용 횟수 증가
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }

    /**
     * 템플릿 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 템플릿 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 공개 설정
     */
    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
