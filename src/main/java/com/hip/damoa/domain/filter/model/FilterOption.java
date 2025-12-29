package com.hip.damoa.domain.filter.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 필터 옵션 엔티티
 *
 * 각 카테고리의 구체적인 옵션을 정의합니다
 * (예: 지역 > 서울 > 강남구, 진료과 > 피부과, 전문영역 > 인테리어)
 */
@Entity
@Table(name = "filter_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FilterOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FilterCategory category;

    @Column(nullable = false, length = 100)
    private String code; // seoul, gangnam-gu, dermatology, interior-design

    @Column(nullable = false, length = 200)
    private String name; // 서울, 강남구, 피부과, 인테리어

    @Column(name = "short_name", length = 100)
    private String shortName;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 계층 구조 (Adjacency List Model)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FilterOption parent;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private List<FilterOption> children = new ArrayList<>();

    @Column(nullable = false)
    private Integer depth = 0; // 0=최상위, 1=2단계, 2=3단계

    @Column(length = 500)
    private String path; // /seoul/gangnam-gu/yeoksam-dong

    // metadata is inherited from BaseEntity as Map<String, Object>

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(length = 100)
    private String icon;

    @Column(length = 20)
    private String color;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_expanded", nullable = false)
    private Boolean isExpanded = false;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public FilterOption(FilterCategory category, String code, String name, String shortName,
                       String description, FilterOption parent, Integer depth, String path,
                       Integer displayOrder, String icon, String color,
                       Boolean isActive, Boolean isDefault, Boolean isExpanded) {
        this.uuid = UUID.randomUUID();
        this.category = category;
        this.code = code;
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.parent = parent;
        this.depth = depth != null ? depth : 0;
        this.path = path;
        // metadata initialized by BaseEntity
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.icon = icon;
        this.color = color;
        this.isActive = isActive != null ? isActive : true;
        this.isDefault = isDefault != null ? isDefault : false;
        this.isExpanded = isExpanded != null ? isExpanded : false;
        this.usageCount = 0;
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

    /**
     * 사용 횟수 증가
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }

    /**
     * 기본값 설정
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * 기본값 해제
     */
    public void unsetDefault() {
        this.isDefault = false;
    }

    // Setter methods for admin operations
    public void setName(String name) {
        this.name = name;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
}
