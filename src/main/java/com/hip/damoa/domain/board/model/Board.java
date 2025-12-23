package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.common.BaseEntity;
import com.hip.damoa.domain.user.model.User;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 게시글 엔티티 (통합 게시판)
 *
 * GALLERY, DOCUMENT, NOTICE, EVENT, FAQ 타입의 게시글을 통합 관리
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "boards", indexes = {
    @Index(name = "idx_boards_type", columnList = "board_type"),
    @Index(name = "idx_boards_user_id", columnList = "user_id"),
    @Index(name = "idx_boards_category_id", columnList = "category_id"),
    @Index(name = "idx_boards_published", columnList = "is_published, published_at"),
    @Index(name = "idx_boards_featured", columnList = "is_featured")
})
public class Board extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;

    @Column(name = "board_type", nullable = false, length = 50)
    private String boardType; // NOTICE, EVENT, FAQ, GALLERY, DOCUMENT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BoardCategory category;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Type(JsonBinaryType.class)
    @Column(name = "type_data", columnDefinition = "jsonb")
    private Map<String, Object> typeData = new HashMap<>();

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount = 0;

    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = true;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Type(StringArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "event_status", length = 20)
    private String eventStatus; // ACTIVE, ENDED (EVENT 타입일 경우만)

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 필터 옵션 ID 목록 (JSONB 배열)
     * - 조인 테이블 대신 JSONB 배열로 관리하여 성능 개선
     * - 검색 시 GIN 인덱스 활용
     */
    @Type(JsonBinaryType.class)
    @Column(name = "filter_option_ids", columnDefinition = "jsonb")
    private List<Long> filterOptionIds = new ArrayList<>();

    // 연관관계 (N+1 방지를 위해 @BatchSize 적용)
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardFilterOption> filterOptions = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardBookmark> bookmarks = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardAttachment> attachments = new ArrayList<>();

    @Builder
    public Board(User user, String boardType, BoardCategory category, String title, String content,
                 Map<String, Object> typeData, Boolean isPublished, LocalDateTime publishedAt,
                 String[] tags, Boolean isPrivate, String password, Long createdBy) {
        this.user = user;
        this.boardType = boardType;
        this.category = category;
        this.title = title;
        this.content = content;
        this.typeData = typeData != null ? typeData : new HashMap<>();
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.isPinned = false;
        this.isFeatured = false;
        this.isPublished = isPublished != null ? isPublished : true;
        this.publishedAt = publishedAt != null ? publishedAt : LocalDateTime.now();
        this.tags = tags;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.password = password;
        this.createdBy = createdBy;
    }

    // 비즈니스 메서드
    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateTypeData(Map<String, Object> typeData) {
        this.typeData = typeData;
    }

    public void updateTags(String[] tags) {
        this.tags = tags;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void pin() {
        this.isPinned = true;
    }

    public void unpin() {
        this.isPinned = false;
    }

    public void feature() {
        this.isFeatured = true;
    }

    public void unfeature() {
        this.isFeatured = false;
    }

    public void activateEvent() {
        if ("EVENT".equals(this.boardType)) {
            this.eventStatus = "ACTIVE";
        }
    }

    public void endEvent() {
        if ("EVENT".equals(this.boardType)) {
            this.eventStatus = "ENDED";
        }
    }

    public void updateEventStatus(String status) {
        if ("EVENT".equals(this.boardType)) {
            this.eventStatus = status;
        }
    }

    public void publish() {
        this.isPublished = true;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    public void unpublish() {
        this.isPublished = false;
    }

    public void addFilterOption(BoardFilterOption filterOption) {
        this.filterOptions.add(filterOption);
    }

    public void removeFilterOption(BoardFilterOption filterOption) {
        this.filterOptions.remove(filterOption);
    }

    public void addAttachment(BoardAttachment attachment) {
        this.attachments.add(attachment);
    }

    public void removeAttachment(BoardAttachment attachment) {
        this.attachments.remove(attachment);
    }

    public void setUpdatedBy(Long userId) {
        this.updatedBy = userId;
    }

    /**
     * 필터 옵션 ID 목록 반환
     */
    public List<Long> getFilterOptionIds() {
        return this.filterOptionIds;
    }

    /**
     * 필터 옵션 ID 목록 업데이트 (JSONB 배열)
     */
    public void updateFilterOptionIds(List<Long> ids) {
        this.filterOptionIds = ids != null ? new ArrayList<>(ids) : new ArrayList<>();
    }
}
