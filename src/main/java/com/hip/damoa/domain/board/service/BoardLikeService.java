package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardLike;
import com.hip.damoa.domain.board.repository.BoardLikeRepository;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 게시글 좋아요 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardLikeService {

    private final BoardLikeRepository boardLikeRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 좋아요 토글 (추가/제거)
     *
     * @return true: 좋아요 추가됨, false: 좋아요 제거됨
     */
    @Transactional
    public boolean toggleLike(UUID boardUuid, String userEmail) {
        log.info("좋아요 토글: boardUuid={}, userEmail={}", boardUuid, userEmail);

        // Board 조회
        Board board = boardRepository.findByUuidAndIsDeletedFalse(boardUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        // User 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 기존 좋아요 확인
        boolean exists = boardLikeRepository.existsByBoardIdAndUserId(board.getId(), user.getId());

        if (exists) {
            // 좋아요 제거
            boardLikeRepository.deleteByBoardIdAndUserId(board.getId(), user.getId());
            board.decrementLikeCount();
            boardRepository.save(board);
            log.info("좋아요 제거 완료: boardUuid={}, userEmail={}, likeCount={}", boardUuid, userEmail, board.getLikeCount());
            return false;
        } else {
            // 좋아요 추가
            BoardLike like = BoardLike.builder()
                    .board(board)
                    .user(user)
                    .build();
            boardLikeRepository.save(like);
            board.incrementLikeCount();
            boardRepository.save(board);
            log.info("좋아요 추가 완료: boardUuid={}, userEmail={}, likeCount={}", boardUuid, userEmail, board.getLikeCount());
            return true;
        }
    }

    /**
     * 좋아요 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isLiked(UUID boardUuid, String userEmail) {
        Board board = boardRepository.findByUuidAndIsDeletedFalse(boardUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return boardLikeRepository.existsByBoardIdAndUserId(board.getId(), user.getId());
    }

    /**
     * 게시글의 좋아요 수 조회
     */
    @Transactional(readOnly = true)
    public long getLikeCount(UUID boardUuid) {
        Board board = boardRepository.findByUuidAndIsDeletedFalse(boardUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        return board.getLikeCount();
    }
}
