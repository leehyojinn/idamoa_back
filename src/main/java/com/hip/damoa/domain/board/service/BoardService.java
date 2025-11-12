package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardCategory;
import com.hip.damoa.domain.board.model.BoardFilterOption;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.repository.BoardCategoryRepository;
import com.hip.damoa.domain.board.repository.BoardFilterOptionRepository;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.filter.model.FilterOption;
import com.hip.damoa.domain.filter.repository.FilterOptionRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 게시판 공통 Service
 *
 * 모든 게시판 타입에서 공통으로 사용하는 기능을 제공합니다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardFilterOptionRepository boardFilterOptionRepository;
    private final FilterOptionRepository filterOptionRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 조회 (UUID)
     */
    @Transactional(readOnly = true)
    public Board getBoard(UUID uuid) {
        return boardRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
    }

    /**
     * 게시글 조회 (ID)
     */
    @Transactional(readOnly = true)
    public Board getBoardById(Long id) {
        return boardRepository.findById(id)
                .filter(board -> !board.getIsDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));
    }

    /**
     * 게시글 목록 조회 (타입별)
     */
    @Transactional(readOnly = true)
    public Page<Board> getBoardsByType(String boardType, Pageable pageable) {
        return boardRepository.findByBoardTypeAndIsDeletedFalseAndIsPublishedTrue(
                boardType, pageable);
    }

    /**
     * 게시글 목록 조회 (카테고리별)
     */
    @Transactional(readOnly = true)
    public Page<Board> getBoardsByCategory(Long categoryId, Pageable pageable) {
        return boardRepository.findByCategoryIdAndIsDeletedFalseAndIsPublishedTrue(
                categoryId, pageable);
    }

    /**
     * 게시글 검색
     */
    @Transactional(readOnly = true)
    public Page<Board> searchBoards(String boardType, String keyword, Pageable pageable) {
        return boardRepository.searchByKeyword(boardType, keyword, pageable);
    }

    /**
     * 북마크된 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Board> getBookmarkedBoards(String boardType, String userEmail, Pageable pageable) {
        return boardRepository.findBookmarkedBoards(boardType, userEmail, pageable);
    }

    /**
     * 필터 옵션으로 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<Board> getBoardsByFilterOptions(String boardType, List<Long> filterOptionIds, Pageable pageable) {
        return boardRepository.findByFilterOptions(boardType, filterOptionIds, pageable);
    }

    /**
     * Featured 게시글 목록
     */
    @Transactional(readOnly = true)
    public List<Board> getFeaturedBoards() {
        return boardRepository.findByIsFeaturedTrueAndIsDeletedFalseAndIsPublishedTrueOrderByPublishedAtDesc();
    }

    /**
     * Pinned 게시글 목록
     */
    @Transactional(readOnly = true)
    public List<Board> getPinnedBoards(String boardType) {
        return boardRepository.findByBoardTypeAndIsPinnedTrueAndIsDeletedFalseAndIsPublishedTrueOrderByPublishedAtDesc(boardType);
    }

    /**
     * 게시글 생성
     */
    @Transactional
    public Board createBoard(String userEmail, String boardType, Long categoryId, String title,
                             String content, Map<String, Object> typeData, String[] tags,
                             Boolean isPublished, Boolean isPrivate, String password) {
        log.info("게시글 생성 시작: userEmail={}, boardType={}, title={}", userEmail, boardType, title);

        // 사용자 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 카테고리 조회 (optional)
        BoardCategory category = null;
        if (categoryId != null) {
            category = boardCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        // 게시글 생성
        Board board = Board.builder()
                .user(user)
                .boardType(boardType)
                .category(category)
                .title(title)
                .content(content)
                .typeData(typeData)
                .tags(tags)
                .isPublished(isPublished != null ? isPublished : true)
                .publishedAt(isPublished != null && isPublished ? LocalDateTime.now() : null)
                .isPrivate(isPrivate != null ? isPrivate : false)
                .password(password)
                .createdBy(user.getId())
                .build();

        board = boardRepository.save(board);
        log.info("게시글 생성 완료: id={}, uuid={}", board.getId(), board.getUuid());

        return board;
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public Board updateBoard(UUID uuid, String userEmail, String title, String content,
                             Map<String, Object> typeData, String[] tags) {
        log.info("게시글 수정 시작: uuid={}, userEmail={}", uuid, userEmail);

        // 게시글 조회
        Board board = getBoard(uuid);

        // 권한 확인
        validateOwnership(board, userEmail);

        // 수정
        if (title != null) {
            board.updateTitle(title);
        }
        if (content != null) {
            board.updateContent(content);
        }
        if (typeData != null) {
            board.updateTypeData(typeData);
        }
        if (tags != null) {
            board.updateTags(tags);
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        board.setUpdatedBy(user.getId());

        log.info("게시글 수정 완료: uuid={}", uuid);
        return board;
    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
    @Transactional
    public void deleteBoard(UUID uuid, String userEmail) {
        log.info("게시글 삭제 시작: uuid={}, userEmail={}", uuid, userEmail);

        Board board = getBoard(uuid);
        validateOwnership(board, userEmail);

        board.softDelete();
        log.info("게시글 삭제 완료: uuid={}", uuid);
    }

    /**
     * 조회수 증가
     */
    @Transactional
    public void incrementViewCount(UUID uuid) {
        Board board = getBoard(uuid);
        board.incrementViewCount();
    }

    /**
     * 필터 옵션 추가
     */
    @Transactional
    public void addFilterOptions(UUID boardUuid, List<Long> filterOptionIds) {
        log.info("필터 옵션 추가: boardUuid={}, filterOptionIds={}", boardUuid, filterOptionIds);

        Board board = getBoard(boardUuid);

        // BoardType 확인
        BoardType boardType = BoardType.fromString(board.getBoardType());
        if (!boardType.supportsFilters()) {
            throw new BusinessException(ErrorCode.BOARD_TYPE_NOT_SUPPORT_FILTER);
        }

        for (Long filterOptionId : filterOptionIds) {
            // 이미 존재하는지 확인
            if (boardFilterOptionRepository.existsByBoardIdAndFilterOptionId(board.getId(), filterOptionId)) {
                continue;
            }

            FilterOption filterOption = filterOptionRepository.findById(filterOptionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILTER_OPTION_NOT_FOUND));

            BoardFilterOption boardFilterOption = BoardFilterOption.builder()
                    .board(board)
                    .filterOption(filterOption)
                    .build();

            boardFilterOptionRepository.save(boardFilterOption);
            board.addFilterOption(boardFilterOption);
        }

        log.info("필터 옵션 추가 완료: boardUuid={}, 추가된 개수={}", boardUuid, filterOptionIds.size());
    }

    /**
     * 필터 옵션 제거
     */
    @Transactional
    public void removeFilterOption(UUID boardUuid, Long filterOptionId) {
        log.info("필터 옵션 제거: boardUuid={}, filterOptionId={}", boardUuid, filterOptionId);

        Board board = getBoard(boardUuid);
        boardFilterOptionRepository.deleteByBoardIdAndFilterOptionId(board.getId(), filterOptionId);

        log.info("필터 옵션 제거 완료");
    }

    /**
     * 게시글의 모든 필터 옵션 조회
     */
    @Transactional(readOnly = true)
    public List<BoardFilterOption> getBoardFilterOptions(UUID boardUuid) {
        Board board = getBoard(boardUuid);
        return boardFilterOptionRepository.findByBoardId(board.getId());
    }

    /**
     * 권한 확인 (작성자 본인인지)
     */
    private void validateOwnership(Board board, String userEmail) {
        if (!board.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 게시글 게시/게시 취소
     */
    @Transactional
    public void togglePublish(UUID uuid, String userEmail) {
        Board board = getBoard(uuid);
        validateOwnership(board, userEmail);

        if (board.getIsPublished()) {
            board.unpublish();
        } else {
            board.publish();
        }
    }

    /**
     * 게시글 고정/고정 해제
     */
    @Transactional
    public void togglePin(UUID uuid, String userEmail) {
        Board board = getBoard(uuid);
        validateOwnership(board, userEmail);

        if (board.getIsPinned()) {
            board.unpin();
        } else {
            board.pin();
        }
    }

    /**
     * 게시글 추천/추천 해제
     */
    @Transactional
    public void toggleFeature(UUID uuid, String userEmail) {
        Board board = getBoard(uuid);
        validateOwnership(board, userEmail);

        if (board.getIsFeatured()) {
            board.unfeature();
        } else {
            board.feature();
        }
    }
}
