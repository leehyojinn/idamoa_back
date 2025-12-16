package com.hip.damoa.domain.board.model;

import com.hip.damoa.domain.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

/**
 * 게시글 좋아요 엔티티
 *
 * 사용자가 좋아요한 게시글을 저장합니다
 */
@Entity
@Table(name = "board_likes", indexes = {
    @Index(name = "idx_board_like_board_id", columnList = "board_id"),
    @Index(name = "idx_board_like_user_id", columnList = "user_id"),
    @Index(name = "idx_board_like_user_board", columnList = "user_id, board_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_board_like_user_board", columnNames = {"user_id", "board_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotFound(action = NotFoundAction.IGNORE)  // 삭제된 User의 경우 null 반환
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public BoardLike(Board board, User user) {
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
