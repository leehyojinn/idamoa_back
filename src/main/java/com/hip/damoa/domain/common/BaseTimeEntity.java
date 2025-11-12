package com.hip.damoa.domain.common;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 시간 정보만 포함하는 베이스 엔티티
 *
 * 사용 대상:
 * - 로그성 데이터 (삭제하면 안되는 감사 로그)
 * - 설정 데이터 (삭제보다 업데이트가 필요한 경우)
 *
 * 포함 필드:
 * - id: BIGSERIAL (내부 참조용)
 * - uuid: UUID (외부 API용)
 * - created_at, updated_at: 생성/수정 시간
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    private UUID uuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
