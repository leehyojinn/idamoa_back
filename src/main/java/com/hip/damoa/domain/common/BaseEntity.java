package com.hip.damoa.domain.common;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Soft Delete가 필요한 엔티티의 베이스 클래스
 *
 * BaseTimeEntity를 상속하여 다음 필드를 추가:
 * - is_deleted, deleted_at: Soft Delete
 * - metadata: JSONB (확장 데이터)
 *
 * 사용 대상:
 * - 비즈니스 데이터 (삭제가 필요한 경우)
 * - 템플릿, 설정 등 soft delete가 필요한 경우
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
    }

    /**
     * Soft Delete
     */
    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Soft Delete 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    /**
     * 메타데이터 추가
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * 메타데이터 조회
     */
    public Object getMetadata(String key) {
        if (this.metadata == null) {
            return null;
        }
        return this.metadata.get(key);
    }
}
