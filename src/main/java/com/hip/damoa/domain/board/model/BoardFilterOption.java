package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.filter.model.FilterOption;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Board-FilterOption 다대다 조인 엔티티
 *
 * 게시글에 적용된 필터 옵션들을 저장합니다
 */
@Entity
@Table(name = "board_filter_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardFilterOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filter_option_id", nullable = false)
    private FilterOption filterOption;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public BoardFilterOption(Board board, FilterOption filterOption) {
        this.board = board;
        this.filterOption = filterOption;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
