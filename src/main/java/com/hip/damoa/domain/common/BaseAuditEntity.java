package com.hip.damoa.domain.common;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * UUID가 필요 없는 엔티티용 베이스 클래스
 *
 * 사용 대상:
 * - 다른 엔티티의 부속 데이터 (1:1 관계)
 * - 독립적인 API 엔드포인트가 없는 엔티티
 * - 부모 엔티티의 UUID로 접근하는 경우
 *
 * 포함 필드:
 * - id: BIGSERIAL (내부 참조용)
 * - created_at, updated_at: 생성/수정 시간
 *
 * 예시:
 * - FilePricing (File UUID로 접근)
 * - BoardAttachment (Board UUID로 접근)
 */
@Getter
@MappedSuperclass
public abstract class BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
