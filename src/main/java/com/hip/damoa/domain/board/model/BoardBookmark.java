package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 게시글 북마크 엔티티
 *
 * 사용자가 북마크한 게시글을 저장합니다
 */
@Entity
@Table(name = "board_bookmarks", indexes = {
    @Index(name = "idx_board_bookmark_board_id", columnList = "board_id"),
    @Index(name = "idx_board_bookmark_user_id", columnList = "user_id"),
    @Index(name = "idx_board_bookmark_user_board", columnList = "user_id, board_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public BoardBookmark(Board board, User user) {
        this.board = board;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
