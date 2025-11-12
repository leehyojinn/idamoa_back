package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardBookmark;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.repository.BoardBookmarkRepository;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 게시글 북마크 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardBookmarkService {

    private final BoardBookmarkRepository boardBookmarkRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    /**
     * 북마크 토글 (추가/제거)
     */
    @Transactional
    public boolean toggleBookmark(UUID boardUuid, String userEmail) {
        log.info("북마크 토글: boardUuid={}, userEmail={}", boardUuid, userEmail);

        // Board 조회
        Board board = boardRepository.findByUuidAndIsDeletedFalse(boardUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        // BoardType 확인
        BoardType boardType = BoardType.fromString(board.getBoardType());
        if (!boardType.supportsBookmarks()) {
            throw new BusinessException(ErrorCode.BOARD_TYPE_NOT_SUPPORT_BOOKMARK);
        }

        // User 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 기존 북마크 확인
        boolean exists = boardBookmarkRepository.existsByBoardIdAndUserId(board.getId(), user.getId());

        if (exists) {
            // 북마크 제거
            boardBookmarkRepository.deleteByBoardIdAndUserId(board.getId(), user.getId());
            log.info("북마크 제거 완료: boardUuid={}, userEmail={}", boardUuid, userEmail);
            return false;
        } else {
            // 북마크 추가
            BoardBookmark bookmark = BoardBookmark.builder()
                    .board(board)
                    .user(user)
                    .build();
            boardBookmarkRepository.save(bookmark);
            log.info("북마크 추가 완료: boardUuid={}, userEmail={}", boardUuid, userEmail);
            return true;
        }
    }

    /**
     * 사용자의 북마크 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardBookmark> getUserBookmarks(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return boardBookmarkRepository.findByUserId(user.getId(), pageable);
    }

    /**
     * 사용자의 특정 타입 북마크 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardBookmark> getUserBookmarksByType(String userEmail, String boardType, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return boardBookmarkRepository.findByUserIdAndBoardType(user.getId(), boardType, pageable);
    }

    /**
     * 북마크 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(UUID boardUuid, String userEmail) {
        Board board = boardRepository.findByUuidAndIsDeletedFalse(boardUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return boardBookmarkRepository.existsByBoardIdAndUserId(board.getId(), user.getId());
    }

    /**
     * 게시글의 북마크 수 조회
     */
    @Transactional(readOnly = true)
    public long getBoardBookmarkCount(UUID boardUuid) {
        Board board = boardRepository.findByUuidAndIsDeletedFalse(boardUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        return boardBookmarkRepository.countByBoardId(board.getId());
    }
}
