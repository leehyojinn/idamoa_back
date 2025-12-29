package com.hip.damoa.domain.filter.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 필터 카테고리 엔티티
 *
 * 필터의 카테고리를 정의합니다 (지역, 진료과, 전문영역, 가격대, 평점, 작업 평수 등)
 */
@Entity
@Table(name = "filter_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilterCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, unique = true, length = 100)
    private String code; // region, department, specialty, price_range, rating, project_size_range

    @Column(nullable = false, length = 200)
    private String name; // 지역, 진료과, 전문영역, 가격대, 평점, 작업 평수

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType; // COMPANY, HOSPITAL, SERVICE

    @Column(name = "filter_type", nullable = false, length = 50)
    private String filterType; // SINGLE_SELECT, MULTI_SELECT, HIERARCHICAL

    @Column(name = "supports_hierarchy", nullable = false)
    private Boolean supportsHierarchy = false;

    @Column(name = "max_depth", nullable = false)
    private Integer maxDepth = 1;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(length = 100)
    private String icon;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_expanded", nullable = false)
    private Boolean isExpanded = false;

    // metadata is inherited from BaseEntity as Map<String, Object>

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<FilterOption> options;

    @Builder
    public FilterCategory(String code, String name, String description, String entityType,
                         String filterType, Boolean supportsHierarchy, Integer maxDepth,
                         Integer displayOrder, String icon, Boolean isActive, Boolean isRequired,
                         Boolean isExpanded) {
        this.uuid = UUID.randomUUID();
        this.code = code;
        this.name = name;
        this.description = description;
        this.entityType = entityType;
        this.filterType = filterType;
        this.supportsHierarchy = supportsHierarchy != null ? supportsHierarchy : false;
        this.maxDepth = maxDepth != null ? maxDepth : 1;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.icon = icon;
        this.isActive = isActive != null ? isActive : true;
        this.isRequired = isRequired != null ? isRequired : false;
        this.isExpanded = isExpanded != null ? isExpanded : false;
        // metadata initialized by BaseEntity
        this.isDeleted = false;
    }

    /**
     * Soft Delete
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    // Setter methods for admin operations
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public void setSupportsHierarchy(Boolean supportsHierarchy) {
        this.supportsHierarchy = supportsHierarchy;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }
}
