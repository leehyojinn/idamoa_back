package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardAttachment;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.model.EventStatus;
import com.hip.damoa.domain.board.repository.BoardAttachmentRepository;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.board.web.dto.FileInfo;
import com.hip.damoa.domain.board.web.dto.NoticeBoardRequest;
import com.hip.damoa.domain.board.web.dto.NoticeBoardResponse;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Notice/Event 게시판 Service
 *
 * 공지사항 및 이벤트 게시판 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeBoardService {

    private final BoardService boardService;
    private final BoardRepository boardRepository;
    private final BoardAttachmentRepository boardAttachmentRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;

    // 공지사항 관련 타입 (NOTICE, EVENT, FAQ)
    private static final List<String> NOTICE_BOARD_TYPES = Arrays.asList("NOTICE", "EVENT", "FAQ");

    /**
     * 공지사항/이벤트 게시글 생성
     */
    @Transactional
    public NoticeBoardResponse createNotice(String userEmail, String boardType, NoticeBoardRequest request) {
        log.info("{} 게시글 생성 시작: userEmail={}, title={}", boardType, userEmail, request.getTitle());

        // BoardType 검증
        BoardType type = BoardType.fromString(boardType);
        if (!type.isNotice() && !type.isEvent()) {
            throw new BusinessException(ErrorCode.INVALID_BOARD_TYPE);
        }

        // typeData 구성 (이벤트 날짜 저장)
        Map<String, Object> typeData = new HashMap<>();
        if (request.getEventStartDate() != null) {
            typeData.put("eventStartDate", request.getEventStartDate().toString());
        }
        if (request.getEventEndDate() != null) {
            typeData.put("eventEndDate", request.getEventEndDate().toString());
        }

        // Board 생성 (NOTICE/EVENT는 categoryId 무시)
        Board board = boardService.createBoard(
                userEmail,
                boardType,
                null,  // NOTICE/EVENT는 카테고리를 사용하지 않음
                request.getTitle(),
                request.getContent(),
                typeData,
                request.getTags(),
                request.getIsPublished(),
                false,
                null
        );

        // 고정 여부 설정
        if (request.getIsPinned() != null && request.getIsPinned()) {
            board.pin();
        }

        // 이벤트인 경우 초기 상태 설정
        if (BoardType.EVENT.name().equals(boardType)) {
            // 이벤트 종료일 확인하여 상태 설정
            if (request.getEventEndDate() != null && request.getEventEndDate().isBefore(LocalDateTime.now())) {
                board.endEvent();
            } else {
                board.activateEvent();
            }
        }

        // 썸네일 저장
        if (request.getThumbnailUuid() != null) {
            File thumbnailFile = fileRepository.findByUuidAndIsDeletedFalse(request.getThumbnailUuid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            // Files 테이블의 entity 정보 업데이트 (스케줄러 삭제 방지)
            thumbnailFile.updateEntityInfo("BOARD_THUMBNAIL", board.getId());
            fileRepository.save(thumbnailFile);

            BoardAttachment attachment = BoardAttachment.builder()
                    .board(board)
                    .fileId(thumbnailFile.getId())
                    .attachmentType(BoardAttachment.AttachmentType.THUMBNAIL)
                    .displayOrder(0)
                    .description("썸네일")
                    .build();

            boardAttachmentRepository.save(attachment);
            log.info("썸네일 첨부파일 저장 완료: fileId={}", thumbnailFile.getId());
        }

        // 일반 첨부파일들 저장
        if (request.getAttachmentUuids() != null && !request.getAttachmentUuids().isEmpty()) {
            int orderIndex = 1;
            for (UUID fileUuid : request.getAttachmentUuids()) {
                File attachmentFile = fileRepository.findByUuidAndIsDeletedFalse(fileUuid)
                        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

                // Files 테이블의 entity 정보 업데이트 (스케줄러 삭제 방지)
                attachmentFile.updateEntityInfo("BOARD_ATTACHMENT", board.getId());
                fileRepository.save(attachmentFile);

                BoardAttachment attachment = BoardAttachment.builder()
                        .board(board)
                        .fileId(attachmentFile.getId())
                        .attachmentType(BoardAttachment.AttachmentType.OTHER)
                        .displayOrder(orderIndex++)
                        .description("첨부파일")
                        .build();

                boardAttachmentRepository.save(attachment);
                log.info("첨부파일 저장 완료: fileId={}, order={}", attachmentFile.getId(), attachment.getDisplayOrder());
            }
            log.info("총 {}개의 첨부파일 저장 완료", request.getAttachmentUuids().size());
        }

        log.info("{} 게시글 생성 완료: uuid={}", boardType, board.getUuid());

        // 썸네일과 첨부파일과 함께 응답 생성
        FileInfo thumbnail = loadThumbnail(board);
        List<FileInfo> attachments = loadAttachments(board);
        String userName = getUserName(board);
        return NoticeBoardResponse.from(board, thumbnail, attachments, userName);
    }

    /**
     * 공지사항/이벤트 게시글 조회
     */
    @Transactional
    public NoticeBoardResponse getNotice(UUID uuid, String expectedBoardType) {
        log.info("게시글 조회: uuid={}", uuid);

        Board board = boardService.getBoard(uuid);

        // BoardType 검증 (expectedBoardType이 null이면 검증 생략 - 통합 조회)
        if (expectedBoardType != null && !board.getBoardType().equals(expectedBoardType)) {
            throw new BusinessException(ErrorCode.BOARD_TYPE_MISMATCH);
        }

        // 조회수 증가
        boardService.incrementViewCount(uuid);

        // 썸네일과 첨부파일과 함께 응답
        FileInfo thumbnail = loadThumbnail(board);
        List<FileInfo> attachments = loadAttachments(board);
        String userName = getUserName(board);
        return NoticeBoardResponse.from(board, thumbnail, attachments, userName);
    }

    /**
     * 공지사항/이벤트 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<NoticeBoardResponse> getNoticeList(String boardType, EventStatus eventStatus, Pageable pageable) {
        Page<Board> boards;

        if (boardType == null) {
            // boardType이 null이면 NOTICE, EVENT, FAQ 모두 조회
            boards = boardRepository.findByBoardTypeInAndIsDeletedFalseAndIsPublishedTrue(
                    NOTICE_BOARD_TYPES, pageable);
        } else {
            boards = boardService.getBoardsByType(boardType, pageable);
        }

        // 각 게시글의 썸네일 로드
        Page<NoticeBoardResponse> responses = boards.map(board -> {
            FileInfo thumbnail = loadThumbnail(board);
            String userName = getUserName(board);
            return NoticeBoardResponse.from(board, thumbnail, userName);
        });

        // EventStatus 필터링
        if (eventStatus != null) {
            List<NoticeBoardResponse> filteredList = responses.getContent().stream()
                    .filter(response -> filterByEventStatus(response, eventStatus))
                    .collect(Collectors.toList());
            return new PageImpl<>(filteredList, pageable, filteredList.size());
        }

        return responses;
    }

    /**
     * 공지사항/이벤트 게시글 검색
     */
    @Transactional(readOnly = true)
    public Page<NoticeBoardResponse> searchNotice(String boardType, String keyword, EventStatus eventStatus, Pageable pageable) {
        Page<Board> boards;

        if (boardType == null) {
            // boardType이 null이면 NOTICE, EVENT, FAQ 모두 검색
            boards = boardRepository.searchByKeywordAndBoardTypes(
                    NOTICE_BOARD_TYPES, keyword, pageable);
        } else {
            boards = boardService.searchBoards(boardType, keyword, pageable);
        }

        // 각 게시글의 썸네일 로드
        Page<NoticeBoardResponse> responses = boards.map(board -> {
            FileInfo thumbnail = loadThumbnail(board);
            String userName = getUserName(board);
            return NoticeBoardResponse.from(board, thumbnail, userName);
        });

        // EventStatus 필터링
        if (eventStatus != null) {
            List<NoticeBoardResponse> filteredList = responses.getContent().stream()
                    .filter(response -> filterByEventStatus(response, eventStatus))
                    .collect(Collectors.toList());
            return new PageImpl<>(filteredList, pageable, filteredList.size());
        }

        return responses;
    }

    /**
     * EventStatus에 따라 필터링
     */
    private boolean filterByEventStatus(NoticeBoardResponse response, EventStatus eventStatus) {
        if (response.getIsEventEnded() == null) {
            // 이벤트가 아니면 필터링 제외
            return true;
        }

        return switch (eventStatus) {
            case ACTIVE -> !response.getIsEventEnded();  // 진행 중
            case ENDED -> response.getIsEventEnded();     // 종료
        };
    }

    /**
     * 게시글의 썸네일 조회
     */
    private FileInfo loadThumbnail(Board board) {
        return boardAttachmentRepository.findByBoardAndTypeOrderByDisplayOrder(
                        board, BoardAttachment.AttachmentType.THUMBNAIL)
                .stream()
                .findFirst()
                .flatMap(attachment -> fileRepository.findById(attachment.getFileId()))
                .filter(file -> !file.getIsDeleted())
                .map(FileInfo::from)
                .orElse(null);
    }

    /**
     * 게시글의 첨부파일 목록 조회
     */
    private List<FileInfo> loadAttachments(Board board) {
        return boardAttachmentRepository.findByBoardAndTypeOrderByDisplayOrder(
                        board, BoardAttachment.AttachmentType.OTHER)
                .stream()
                .map(attachment -> fileRepository.findById(attachment.getFileId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(file -> !file.getIsDeleted())
                .map(FileInfo::from)
                .collect(Collectors.toList());
    }

    /**
     * 공지사항/이벤트 게시글 수정
     */
    @Transactional
    public NoticeBoardResponse updateNotice(UUID uuid, String userEmail, NoticeBoardRequest request) {
        log.info("게시글 수정 시작: uuid={}, userEmail={}", uuid, userEmail);

        // 이벤트 날짜 업데이트
        Map<String, Object> typeData = new HashMap<>();
        if (request.getEventStartDate() != null) {
            typeData.put("eventStartDate", request.getEventStartDate().toString());
        }
        if (request.getEventEndDate() != null) {
            typeData.put("eventEndDate", request.getEventEndDate().toString());
        }

        Board board = boardService.updateBoard(
                uuid,
                userEmail,
                request.getTitle(),
                request.getContent(),
                typeData.isEmpty() ? null : typeData,
                request.getTags()
        );

        // 고정 여부 업데이트
        if (request.getIsPinned() != null) {
            if (request.getIsPinned()) {
                board.pin();
            } else {
                board.unpin();
            }
        }

        // 썸네일 업데이트
        if (request.getThumbnailUuid() != null) {
            // 기존 썸네일 삭제
            List<BoardAttachment> existingThumbnails = boardAttachmentRepository.findByBoardAndTypeOrderByDisplayOrder(
                    board, BoardAttachment.AttachmentType.THUMBNAIL);
            existingThumbnails.forEach(attachment -> {
                attachment.softDelete();
                log.info("기존 썸네일 삭제: attachmentId={}", attachment.getId());
            });

            // 새 썸네일 추가
            File thumbnailFile = fileRepository.findByUuidAndIsDeletedFalse(request.getThumbnailUuid())
                    .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

            // Files 테이블의 entity 정보 업데이트 (스케줄러 삭제 방지)
            thumbnailFile.updateEntityInfo("BOARD_THUMBNAIL", board.getId());
            fileRepository.save(thumbnailFile);

            BoardAttachment newAttachment = BoardAttachment.builder()
                    .board(board)
                    .fileId(thumbnailFile.getId())
                    .attachmentType(BoardAttachment.AttachmentType.THUMBNAIL)
                    .displayOrder(0)
                    .description("썸네일")
                    .build();

            boardAttachmentRepository.save(newAttachment);
            log.info("새 썸네일 저장 완료: fileId={}", thumbnailFile.getId());
        }

        // 첨부파일 업데이트
        if (request.getAttachmentUuids() != null) {
            // 기존 첨부파일 삭제 (썸네일 제외)
            List<BoardAttachment> existingAttachments = boardAttachmentRepository.findByBoardAndTypeOrderByDisplayOrder(
                    board, BoardAttachment.AttachmentType.OTHER);
            existingAttachments.forEach(attachment -> {
                attachment.softDelete();
                log.info("기존 첨부파일 삭제: attachmentId={}", attachment.getId());
            });

            // 새 첨부파일들 추가
            if (!request.getAttachmentUuids().isEmpty()) {
                int orderIndex = 1;
                for (UUID fileUuid : request.getAttachmentUuids()) {
                    File attachmentFile = fileRepository.findByUuidAndIsDeletedFalse(fileUuid)
                            .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

                    // Files 테이블의 entity 정보 업데이트 (스케줄러 삭제 방지)
                    attachmentFile.updateEntityInfo("BOARD_ATTACHMENT", board.getId());
                    fileRepository.save(attachmentFile);

                    BoardAttachment newAttachment = BoardAttachment.builder()
                            .board(board)
                            .fileId(attachmentFile.getId())
                            .attachmentType(BoardAttachment.AttachmentType.OTHER)
                            .displayOrder(orderIndex++)
                            .description("첨부파일")
                            .build();

                    boardAttachmentRepository.save(newAttachment);
                    log.info("새 첨부파일 저장 완료: fileId={}, order={}", attachmentFile.getId(), newAttachment.getDisplayOrder());
                }
                log.info("총 {}개의 첨부파일 저장 완료", request.getAttachmentUuids().size());
            }
        }

        log.info("게시글 수정 완료: uuid={}", uuid);

        // 썸네일과 첨부파일과 함께 응답
        FileInfo thumbnail = loadThumbnail(board);
        List<FileInfo> attachments = loadAttachments(board);
        String userName = getUserName(board);
        return NoticeBoardResponse.from(board, thumbnail, attachments, userName);
    }

    /**
     * 공지사항/이벤트 게시글 삭제
     */
    @Transactional
    public void deleteNotice(UUID uuid, String userEmail) {
        log.info("게시글 삭제: uuid={}, userEmail={}", uuid, userEmail);
        boardService.deleteBoard(uuid, userEmail);
    }

    /**
     * 이벤트 상태 업데이트 (관리자 수동 종료/활성화)
     *
     * @param uuid 이벤트 UUID
     * @param userEmail 관리자 이메일
     * @param status 변경할 상태 (ACTIVE, ENDED)
     * @return 업데이트된 이벤트 정보
     */
    @Transactional
    public NoticeBoardResponse updateEventStatus(UUID uuid, String userEmail, String status) {
        log.info("이벤트 상태 업데이트 시작: uuid={}, userEmail={}, status={}", uuid, userEmail, status);

        // 사용자 확인
        User user = userRepository.findByEmailAndIsDeletedFalse(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 게시글 조회
        Board board = boardRepository.findByUuidAndIsDeletedFalse(uuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOARD_NOT_FOUND));

        // EVENT 타입인지 확인
        if (!BoardType.EVENT.name().equals(board.getBoardType())) {
            throw new BusinessException(ErrorCode.EVENT_NOT_EDITABLE);
        }

        // 상태 업데이트
        if ("ENDED".equals(status)) {
            board.endEvent();
            log.info("이벤트 종료 처리: uuid={}, title={}", uuid, board.getTitle());
        } else if ("ACTIVE".equals(status)) {
            board.activateEvent();
            log.info("이벤트 활성화 처리: uuid={}, title={}", uuid, board.getTitle());
        } else {
            log.error("유효하지 않은 이벤트 상태값: {}", status);
            throw new BusinessException(ErrorCode.INVALID_EVENT_STATUS);
        }

        // 수정자 정보 업데이트
        board.setUpdatedBy(user.getId());
        boardRepository.save(board);

        // 응답 생성
        FileInfo thumbnail = loadThumbnail(board);
        List<FileInfo> attachments = loadAttachments(board);
        String userName = getUserName(board);

        log.info("이벤트 상태 업데이트 완료: uuid={}, newStatus={}", uuid, board.getEventStatus());
        return NoticeBoardResponse.from(board, thumbnail, attachments, userName);
    }

    /**
     * Pinned 게시글 목록
     */
    @Transactional(readOnly = true)
    public List<NoticeBoardResponse> getPinnedNotices(String boardType) {
        List<Board> boards;

        if (boardType == null) {
            // boardType이 null이면 NOTICE, EVENT, FAQ 모두 조회
            boards = boardRepository.findByBoardTypeInAndIsPinnedTrueAndIsDeletedFalseAndIsPublishedTrueOrderByPublishedAtDesc(
                    NOTICE_BOARD_TYPES);
        } else {
            boards = boardService.getPinnedBoards(boardType);
        }

        // 각 게시글의 썸네일 로드
        return boards.stream()
                .map(board -> {
                    FileInfo thumbnail = loadThumbnail(board);
                    String userName = getUserName(board);
                    return NoticeBoardResponse.from(board, thumbnail, userName);
                })
                .toList();
    }

    /**
     * Board의 User로부터 userName 조회
     * UserProfile이 있으면 name 반환, 없으면 email 반환
     */
    private String getUserName(Board board) {
        if (board.getUser() == null) {
            return null;
        }

        return userProfileRepository.findByUserId(board.getUser().getId())
                .map(com.hip.damoa.domain.user.model.UserProfile::getName)
                .orElse(board.getUser().getEmail());
    }
}
