package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardAttachment;
import com.hip.damoa.domain.board.model.BoardFilterOption;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.repository.BoardAttachmentRepository;
import com.hip.damoa.domain.board.repository.BoardFilterOptionRepository;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.board.repository.BoardSpecifications;
import com.hip.damoa.domain.board.web.dto.DocumentCreateRequest;
import com.hip.damoa.domain.board.web.dto.DocumentResponse;
import com.hip.damoa.domain.board.web.dto.DocumentUpdateRequest;
import com.hip.damoa.domain.board.web.dto.FileInfo;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileDownloadRepository;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Document 게시판 Service
 *
 * 자료실 전용 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentBoardService {

    private final BoardService boardService;
    private final BoardBookmarkService boardBookmarkService;
    private final BoardFilterOptionRepository boardFilterOptionRepository;
    private final BoardAttachmentRepository boardAttachmentRepository;
    private final BoardRepository boardRepository;
    private final FileRepository fileRepository;
    private final FileDownloadRepository fileDownloadRepository;
    private final UserRepository userRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;

    /**
     * Document 게시글 생성
     */
    @Transactional
    public DocumentResponse createDocument(String userEmail, DocumentCreateRequest request) {
        log.info("Document 게시글 생성 시작: userEmail={}, title={}", userEmail, request.getTitle());

        // 파일 검증
        if (request.getFileUuids() == null || request.getFileUuids().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // Board 생성
        Board board = boardService.createBoard(
                userEmail,
                BoardType.DOCUMENT.name(),
                request.getCategoryId(),
                request.getTitle(),
                request.getContent(),
                request.toTypeData(),
                request.getTags(),
                request.getIsPublished(),
                request.getIsPrivate(),
                null
        );

        // 필터 옵션 추가
        if (request.getFilterOptionIds() != null && !request.getFilterOptionIds().isEmpty()) {
            boardService.addFilterOptions(board.getUuid(), request.getFilterOptionIds());
        }

        // 문서 첨부파일 생성
        processDocumentFiles(board, request.getFileUuids(), request.getThumbnailUuid());

        log.info("Document 게시글 생성 완료: uuid={}", board.getUuid());

        // Response 생성
        List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
        List<FileInfo> files = getDocumentFileInfos(board);
        FileInfo thumbnail = getThumbnailFileInfo(board);
        String userName = getUserName(board);
        return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, 0L, userName);
    }

    /**
     * Document 게시글 조회
     */
    @Transactional
    public DocumentResponse getDocument(UUID uuid, String userEmail) {
        log.info("Document 게시글 조회: uuid={}", uuid);

        Board board = boardService.getBoard(uuid);

        // BoardType 검증
        if (!BoardType.DOCUMENT.name().equals(board.getBoardType())) {
            throw new BusinessException(ErrorCode.BOARD_TYPE_MISMATCH);
        }

        // 조회수 증가
        boardService.incrementViewCount(uuid);

        // 필터 옵션 조회
        List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());

        // 북마크 여부 확인
        boolean isBookmarked = false;
        boolean hasDownloaded = false;
        if (userEmail != null) {
            isBookmarked = boardBookmarkService.isBookmarked(uuid, userEmail);

            // 다운로드 여부 확인 (첫 번째 파일 기준)
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                @SuppressWarnings("unchecked")
                List<String> fileUuids = (List<String>) board.getTypeData().get("files");
                if (fileUuids != null && !fileUuids.isEmpty()) {
                    String firstFileUuid = fileUuids.get(0);
                    File file = fileRepository.findByUuid(UUID.fromString(firstFileUuid)).orElse(null);
                    if (file != null) {
                        hasDownloaded = fileDownloadRepository.existsByFileIdAndUserId(file.getId(), user.getId());
                    }
                }
            }
        }

        // 다운로드 횟수 조회 (모든 파일의 합계)
        long totalDownloadCount = getTotalDownloadCount(board);

        // 파일 정보 조회
        List<FileInfo> files = getDocumentFileInfos(board);
        FileInfo thumbnail = getThumbnailFileInfo(board);
        String userName = getUserName(board);

        return DocumentResponse.from(board, filterOptions, files, thumbnail, isBookmarked, hasDownloaded, totalDownloadCount, userName);
    }

    /**
     * Document 게시글 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentList(Pageable pageable) {
        Page<Board> boards = boardService.getBoardsByType(BoardType.DOCUMENT.name(), pageable);

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
            List<FileInfo> files = getDocumentFileInfos(board);
            FileInfo thumbnail = getThumbnailFileInfo(board);
            long downloadCount = getTotalDownloadCount(board);
            String userName = getUserName(board);
            return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
        });
    }

    /**
     * Document 게시글 검색
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocument(String keyword, Pageable pageable) {
        Page<Board> boards = boardService.searchBoards(BoardType.DOCUMENT.name(), keyword, pageable);

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
            List<FileInfo> files = getDocumentFileInfos(board);
            FileInfo thumbnail = getThumbnailFileInfo(board);
            long downloadCount = getTotalDownloadCount(board);
            String userName = getUserName(board);
            return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
        });
    }

    /**
     * Document 게시글 검색 (통합)
     *
     * @param keyword 검색 키워드 (nullable)
     * @param tags 태그 필터 (nullable)
     * @param filterOptionIds 필터 옵션 IDs (nullable)
     * @param onlyBookmarked 북마크된 게시글만 조회 (nullable, userEmail 필요)
     * @param onlyMyPosts 내가 쓴 게시글만 조회 (nullable, userEmail 필요)
     * @param userEmail 사용자 이메일 (북마크 조회 시 필요)
     * @param pageable 페이지 정보
     * @return 검색 결과
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocuments(String keyword, String[] tags, List<Long> filterOptionIds,
                                                   Boolean onlyBookmarked, Boolean onlyMyPosts,
                                                   String userEmail, Pageable pageable) {

        log.info("Document 게시글 검색 시작: keyword={}, tags={}, filterOptions={}, onlyBookmarked={}, onlyMyPosts={}, userEmail={}",
                keyword, tags != null ? String.join(",", tags) : null, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail);

        // 로그인이 필요한 기능 체크
        if ((Boolean.TRUE.equals(onlyBookmarked) || Boolean.TRUE.equals(onlyMyPosts)) && userEmail == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        // Specification 구성 (GalleryBoardService와 동일한 패턴)
        Specification<Board> spec = BoardSpecifications.searchBoards(
                BoardType.DOCUMENT.name(),
                keyword,
                tags,
                filterOptionIds,
                onlyBookmarked,
                onlyMyPosts,
                userEmail
        );

        // Specification을 사용한 조회
        Page<Board> boards = boardRepository.findAll(spec, pageable);

        // 북마크 여부 확인을 위한 userEmail 존재 여부
        boolean checkBookmark = userEmail != null;

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
            List<FileInfo> files = getDocumentFileInfos(board);
            FileInfo thumbnail = getThumbnailFileInfo(board);
            long downloadCount = getTotalDownloadCount(board);

            boolean isBookmarked = checkBookmark && boardBookmarkService.isBookmarked(board.getUuid(), userEmail);

            // hasDownloaded 확인
            boolean hasDownloaded = false;
            if (userEmail != null) {
                User user = userRepository.findByEmail(userEmail).orElse(null);
                if (user != null) {
                    @SuppressWarnings("unchecked")
                    List<String> fileUuids = (List<String>) board.getTypeData().get("files");
                    if (fileUuids != null && !fileUuids.isEmpty()) {
                        String firstFileUuid = fileUuids.get(0);
                        File file = fileRepository.findByUuid(UUID.fromString(firstFileUuid)).orElse(null);
                        if (file != null) {
                            hasDownloaded = fileDownloadRepository.existsByFileIdAndUserId(file.getId(), user.getId());
                        }
                    }
                }
            }

            String userName = getUserName(board);
            return DocumentResponse.from(board, filterOptions, files, thumbnail, isBookmarked, hasDownloaded, downloadCount, userName);
        });
    }

    /**
     * Document 게시글 수정
     */
    @Transactional
    public DocumentResponse updateDocument(UUID uuid, String userEmail, DocumentUpdateRequest request) {
        log.info("Document 게시글 수정 시작: uuid={}, userEmail={}", uuid, userEmail);

        Board board = boardService.updateBoard(
                uuid,
                userEmail,
                request.getTitle(),
                request.getContent(),
                request.toTypeData(),
                request.getTags()
        );

        // 필터 옵션 업데이트
        if (request.getFilterOptionIds() != null) {
            // 기존 필터 삭제
            boardFilterOptionRepository.deleteByBoardId(board.getId());
            // 새 필터 추가
            if (!request.getFilterOptionIds().isEmpty()) {
                boardService.addFilterOptions(uuid, request.getFilterOptionIds());
            }
        }

        // 문서 첨부파일 업데이트
        if (request.getFileUuids() != null) {
            // 기존 첨부파일 Soft delete
            List<BoardAttachment> existingAttachments = boardAttachmentRepository.findByBoardOrderByDisplayOrder(board);
            existingAttachments.forEach(attachment -> attachment.softDelete());

            // 새 첨부파일 추가
            if (!request.getFileUuids().isEmpty()) {
                processDocumentFiles(board, request.getFileUuids(), request.getThumbnailUuid());
            }
        }

        log.info("Document 게시글 수정 완료: uuid={}", uuid);

        List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
        List<FileInfo> files = getDocumentFileInfos(board);
        FileInfo thumbnail = getThumbnailFileInfo(board);
        boolean isBookmarked = boardBookmarkService.isBookmarked(uuid, userEmail);
        long downloadCount = getTotalDownloadCount(board);

        User user = userRepository.findByEmail(userEmail).orElse(null);
        boolean hasDownloaded = false;
        if (user != null) {
            @SuppressWarnings("unchecked")
            List<String> fileUuids = (List<String>) board.getTypeData().get("files");
            if (fileUuids != null && !fileUuids.isEmpty()) {
                String firstFileUuid = fileUuids.get(0);
                File file = fileRepository.findByUuid(UUID.fromString(firstFileUuid)).orElse(null);
                if (file != null) {
                    hasDownloaded = fileDownloadRepository.existsByFileIdAndUserId(file.getId(), user.getId());
                }
            }
        }

        String userName = getUserName(board);
        return DocumentResponse.from(board, filterOptions, files, thumbnail, isBookmarked, hasDownloaded, downloadCount, userName);
    }

    /**
     * Document 게시글 삭제
     */
    @Transactional
    public void deleteDocument(UUID uuid, String userEmail) {
        log.info("Document 게시글 삭제: uuid={}, userEmail={}", uuid, userEmail);
        boardService.deleteBoard(uuid, userEmail);
    }

    /**
     * Featured Document 목록
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getFeaturedDocuments() {
        List<Board> boards = boardService.getFeaturedBoards();

        return boards.stream()
                .filter(board -> BoardType.DOCUMENT.name().equals(board.getBoardType()))
                .map(board -> {
                    List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
                    List<FileInfo> files = getDocumentFileInfos(board);
                    FileInfo thumbnail = getThumbnailFileInfo(board);
                    long downloadCount = getTotalDownloadCount(board);
                    String userName = getUserName(board);
                    return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
                })
                .toList();
    }

    /**
     * Pinned Document 목록
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getPinnedDocuments() {
        List<Board> boards = boardService.getPinnedBoards(BoardType.DOCUMENT.name());

        return boards.stream()
                .map(board -> {
                    List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
                    List<FileInfo> files = getDocumentFileInfos(board);
                    FileInfo thumbnail = getThumbnailFileInfo(board);
                    long downloadCount = getTotalDownloadCount(board);
                    String userName = getUserName(board);
                    return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
                })
                .toList();
    }

    /**
     * 내가 작성한 Document 목록 (마이페이지)
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getMyDocuments(String userEmail, Pageable pageable) {
        log.info("내 Document 목록 조회: userEmail={}", userEmail);

        // Specification 사용하여 내가 작성한 게시글만 조회
        Specification<Board> spec = BoardSpecifications.searchBoards(
                BoardType.DOCUMENT.name(),
                null,  // keyword 없음
                null,  // filterOptions 없음
                false, // onlyBookmarked = false
                true,  // onlyMyPosts = true
                userEmail
        );

        Page<Board> boards = boardRepository.findAll(spec, pageable);

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
            List<FileInfo> files = getDocumentFileInfos(board);
            FileInfo thumbnail = getThumbnailFileInfo(board);
            long downloadCount = getTotalDownloadCount(board);

            boolean isBookmarked = boardBookmarkService.isBookmarked(board.getUuid(), userEmail);

            // hasDownloaded 확인
            boolean hasDownloaded = false;
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                @SuppressWarnings("unchecked")
                List<String> fileUuids = (List<String>) board.getTypeData().get("files");
                if (fileUuids != null && !fileUuids.isEmpty()) {
                    String firstFileUuid = fileUuids.get(0);
                    File file = fileRepository.findByUuid(UUID.fromString(firstFileUuid)).orElse(null);
                    if (file != null) {
                        hasDownloaded = fileDownloadRepository.existsByFileIdAndUserId(file.getId(), user.getId());
                    }
                }
            }

            String userName = getUserName(board);
            return DocumentResponse.from(board, filterOptions, files, thumbnail, isBookmarked, hasDownloaded, downloadCount, userName);
        });
    }

    /**
     * 전체 다운로드 횟수 계산 (게시글의 모든 파일)
     */
    private long getTotalDownloadCount(Board board) {
        @SuppressWarnings("unchecked")
        List<String> fileUuids = (List<String>) board.getTypeData().get("files");

        if (fileUuids == null || fileUuids.isEmpty()) {
            return 0L;
        }

        long total = 0L;
        for (String fileUuidStr : fileUuids) {
            try {
                UUID fileUuid = UUID.fromString(fileUuidStr);
                File file = fileRepository.findByUuid(fileUuid).orElse(null);
                if (file != null) {
                    total += fileDownloadRepository.countByFileId(file.getId());
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid file UUID: {}", fileUuidStr);
            }
        }

        return total;
    }

    /**
     * Document 첨부파일 처리
     */
    private void processDocumentFiles(Board board, List<String> fileUuids, String thumbnailUuid) {
        if (fileUuids == null || fileUuids.isEmpty()) {
            return;
        }

        log.info("Document 첨부파일 처리 시작: boardId={}, fileCount={}", board.getId(), fileUuids.size());

        int displayOrder = 0;

        // 썸네일 처리 (있는 경우)
        if (thumbnailUuid != null && !thumbnailUuid.isEmpty()) {
            try {
                UUID thumbUuid = UUID.fromString(thumbnailUuid);
                saveDocumentFile(board, thumbUuid, BoardAttachment.AttachmentType.THUMBNAIL, displayOrder++);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid thumbnail UUID format: {}", thumbnailUuid);
            }
        }

        // 문서 파일 처리
        for (String uuidString : fileUuids) {
            try {
                UUID fileUuid = UUID.fromString(uuidString);
                saveDocumentFile(board, fileUuid, BoardAttachment.AttachmentType.DOCUMENT, displayOrder++);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid file UUID format: {}", uuidString);
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        log.info("Document 첨부파일 처리 완료: boardId={}, attachmentCount={}", board.getId(), displayOrder);
    }

    /**
     * Document 파일 저장
     */
    private void saveDocumentFile(Board board, UUID fileUuid, BoardAttachment.AttachmentType type, int displayOrder) {
        // UUID로 파일 조회
        File file = fileRepository.findByUuidAndIsDeletedFalse(fileUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // Files 테이블의 entityId 업데이트 (스케줄러 삭제 방지)
        String entityType = (type == BoardAttachment.AttachmentType.THUMBNAIL) ? "BOARD_THUMBNAIL" : "BOARD_DOCUMENT";
        file.updateEntityInfo(entityType, board.getId());
        fileRepository.save(file);

        // BoardAttachment 생성
        BoardAttachment attachment = BoardAttachment.builder()
                .board(board)
                .fileId(file.getId())
                .attachmentType(type)
                .displayOrder(displayOrder)
                .build();

        boardAttachmentRepository.save(attachment);
        log.debug("Document 첨부파일 저장: fileId={}, type={}, displayOrder={}", file.getId(), type, displayOrder);
    }

    /**
     * Board의 문서 파일 정보 조회 (DOCUMENT 타입만)
     */
    private List<FileInfo> getDocumentFileInfos(Board board) {
        List<BoardAttachment> attachments = boardAttachmentRepository.findByBoardAndTypeOrderByDisplayOrder(
                board, BoardAttachment.AttachmentType.DOCUMENT);

        return attachments.stream()
                .map(attachment -> {
                    File file = fileRepository.findById(attachment.getFileId())
                            .orElse(null);
                    return file != null ? FileInfo.from(file) : null;
                })
                .filter(fileInfo -> fileInfo != null)
                .toList();
    }

    /**
     * Board의 썸네일 파일 정보 조회
     */
    private FileInfo getThumbnailFileInfo(Board board) {
        List<BoardAttachment> thumbnailAttachments = boardAttachmentRepository.findByBoardAndTypeOrderByDisplayOrder(
                board, BoardAttachment.AttachmentType.THUMBNAIL);

        if (thumbnailAttachments.isEmpty()) {
            return null;
        }

        BoardAttachment thumbnailAttachment = thumbnailAttachments.get(0);  // 첫 번째 썸네일
        File file = fileRepository.findById(thumbnailAttachment.getFileId())
                .orElse(null);

        return file != null ? FileInfo.from(file) : null;
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
