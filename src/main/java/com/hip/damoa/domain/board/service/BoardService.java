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
     * 필터 옵션 추가 - JSONB 배열 방식
     */
    @Transactional
    public void addFilterOptions(UUID boardUuid, List<Long> filterOptionIds) {
        log.info("필터 옵션 추가: boardUuid={}, filterOptionIds={}", boardUuid, filterOptionIds);

        if (filterOptionIds == null || filterOptionIds.isEmpty()) {
            return;
        }

        Board board = getBoard(boardUuid);

        // BoardType 확인
        BoardType boardType = BoardType.fromString(board.getBoardType());
        if (!boardType.supportsFilters()) {
            throw new BusinessException(ErrorCode.BOARD_TYPE_NOT_SUPPORT_FILTER);
        }

        // 유효한 필터 옵션만 추출
        List<FilterOption> validOptions = filterOptionRepository.findByIdInAndIsDeletedFalse(filterOptionIds);
        List<Long> validIds = validOptions.stream()
                .map(FilterOption::getId)
                .collect(java.util.stream.Collectors.toList());

        // JSONB 배열 업데이트 (UPDATE 1회)
        board.updateFilterOptionIds(validIds);
        boardRepository.save(board);

        log.info("필터 옵션 추가 완료 (JSONB): boardUuid={}, 추가된 개수={}", boardUuid, validIds.size());
    }

    /**
     * 필터 옵션 제거 - JSONB 배열 방식
     */
    @Transactional
    public void removeFilterOption(UUID boardUuid, Long filterOptionId) {
        log.info("필터 옵션 제거: boardUuid={}, filterOptionId={}", boardUuid, filterOptionId);

        Board board = getBoard(boardUuid);

        List<Long> currentIds = board.getFilterOptionIds();
        if (currentIds == null || currentIds.isEmpty()) {
            return;
        }

        // 해당 ID 제거
        List<Long> updatedIds = currentIds.stream()
                .filter(id -> !id.equals(filterOptionId))
                .collect(java.util.stream.Collectors.toList());

        board.updateFilterOptionIds(updatedIds);
        boardRepository.save(board);

        log.info("필터 옵션 제거 완료 (JSONB): 이전={}, 이후={}", currentIds.size(), updatedIds.size());
    }

    /**
     * 게시글의 모든 필터 옵션 조회 - JSONB 배열 방식
     */
    @Transactional(readOnly = true)
    public List<FilterOption> getBoardFilterOptions(UUID boardUuid) {
        Board board = getBoard(boardUuid);
        List<Long> ids = board.getFilterOptionIds();
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return filterOptionRepository.findByIdInAndIsDeletedFalse(ids);
    }

    /**
     * 게시글의 필터 옵션 ID 목록 조회 - JSONB 배열 방식
     */
    @Transactional(readOnly = true)
    public List<Long> getBoardFilterOptionIds(UUID boardUuid) {
        Board board = getBoard(boardUuid);
        List<Long> ids = board.getFilterOptionIds();
        return ids != null ? ids : List.of();
    }

    /**
     * 게시글의 필터 옵션 상세 조회 - JSONB 배열 방식
     */
    @Transactional(readOnly = true)
    public List<FilterOption> getBoardFilterOptionDetails(UUID boardUuid) {
        Board board = getBoard(boardUuid);
        List<Long> ids = board.getFilterOptionIds();
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return filterOptionRepository.findByIdInAndIsDeletedFalse(ids);
    }

    /**
     * 권한 확인 (작성자 본인인지)
     */
    private void validateOwnership(Board board, String userEmail) {
        // user가 null인 경우 (시스템 생성 게시글 등) - 일반 사용자는 삭제 불가
        if (board.getUser() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!board.getUser().getEmail().equals(userEmail)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /**
     * 게시글 삭제 (관리자용 - 권한 검증 없이)
     */
    @Transactional
    public void deleteBoardByAdmin(UUID uuid) {
        log.info("게시글 삭제 (관리자): uuid={}", uuid);

        Board board = getBoard(uuid);
        board.softDelete();
        log.info("게시글 삭제 완료 (관리자): uuid={}", uuid);
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
