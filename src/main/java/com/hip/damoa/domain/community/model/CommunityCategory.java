package com.hip.damoa.domain.community.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 커뮤니티 카테고리 엔티티
 */
@Entity
@Table(name = "community_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String icon;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "allow_anonymous", nullable = false)
    private Boolean allowAnonymous = false;

    @Column(name = "require_login", nullable = false)
    private Boolean requireLogin = true;

    @Column(name = "allow_attachments", nullable = false)
    private Boolean allowAttachments = true;

    @Column(name = "max_attachments", nullable = false)
    private Integer maxAttachments = 10;

    @Builder
    public CommunityCategory(String name, String slug, String description, String icon,
                             Integer displayOrder, Boolean isActive, Boolean allowAnonymous,
                             Boolean requireLogin, Boolean allowAttachments, Integer maxAttachments) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.icon = icon;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isActive = isActive != null ? isActive : true;
        this.allowAnonymous = allowAnonymous != null ? allowAnonymous : false;
        this.requireLogin = requireLogin != null ? requireLogin : true;
        this.allowAttachments = allowAttachments != null ? allowAttachments : true;
        this.maxAttachments = maxAttachments != null ? maxAttachments : 10;
    }

    public void update(String name, String description, String icon, Integer displayOrder,
                       Boolean isActive, Boolean allowAnonymous, Boolean requireLogin,
                       Boolean allowAttachments, Integer maxAttachments) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (icon != null) this.icon = icon;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (isActive != null) this.isActive = isActive;
        if (allowAnonymous != null) this.allowAnonymous = allowAnonymous;
        if (requireLogin != null) this.requireLogin = requireLogin;
        if (allowAttachments != null) this.allowAttachments = allowAttachments;
        if (maxAttachments != null) this.maxAttachments = maxAttachments;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
