package com.hip.damoa.domain.popup.model;

import com.hip.damoa.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 홈페이지 팝업 엔티티
 *
 * 홈페이지 접속 시 노출되는 팝업을 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "popups", indexes = {
    @Index(name = "idx_popups_is_active_is_deleted_display_order",
           columnList = "is_active, is_deleted, display_order"),
    @Index(name = "idx_popups_display_start_date", columnList = "display_start_date"),
    @Index(name = "idx_popups_display_end_date", columnList = "display_end_date"),
    @Index(name = "idx_popups_created_at", columnList = "created_at")
})
public class Popup extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_uuid", columnDefinition = "UUID")
    private UUID imageUuid;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "display_start_date")
    private LocalDateTime displayStartDate;

    @Column(name = "display_end_date")
    private LocalDateTime displayEndDate;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Builder
    public Popup(String title, String content, UUID imageUuid, String linkUrl,
                 LocalDateTime displayStartDate, LocalDateTime displayEndDate,
                 Integer displayOrder, Boolean isActive, String createdBy) {
        this.title = title;
        this.content = content;
        this.imageUuid = imageUuid;
        this.linkUrl = linkUrl;
        this.displayStartDate = displayStartDate;
        this.displayEndDate = displayEndDate;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isActive = isActive != null ? isActive : true;
        this.viewCount = 0L;
        this.clickCount = 0L;
        this.createdBy = createdBy;
    }

    // 비즈니스 메서드

    /**
     * 팝업 기본 정보 수정
     */
    public void update(String title, String content, UUID imageUuid, String linkUrl,
                      LocalDateTime displayStartDate, LocalDateTime displayEndDate,
                      Integer displayOrder, String updatedBy) {
        this.title = title;
        this.content = content;
        this.imageUuid = imageUuid;
        this.linkUrl = linkUrl;
        this.displayStartDate = displayStartDate;
        this.displayEndDate = displayEndDate;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.updatedBy = updatedBy;
    }

    /**
     * 조회수 증가
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 클릭수 증가
     */
    public void incrementClickCount() {
        this.clickCount++;
    }

    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 현재 시간 기준 노출 가능 여부 확인
     */
    public boolean isDisplayable(LocalDateTime now) {
        if (!isActive || getIsDeleted()) {
            return false;
        }

        // 시작일 체크
        if (displayStartDate != null && now.isBefore(displayStartDate)) {
            return false;
        }

        // 종료일 체크
        if (displayEndDate != null && now.isAfter(displayEndDate)) {
            return false;
        }

        return true;
    }
}
