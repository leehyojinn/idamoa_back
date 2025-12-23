package com.hip.damoa.domain.board.service;

import com.hip.damoa.core.exception.BusinessException;
import com.hip.damoa.core.exception.ErrorCode;
import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardAttachment;
import com.hip.damoa.domain.board.model.BoardFilterOption;
import com.hip.damoa.domain.board.model.BoardType;
import com.hip.damoa.domain.board.repository.BoardAttachmentRepository;
import com.hip.damoa.domain.board.repository.BoardBookmarkRepository;
import com.hip.damoa.domain.board.repository.BoardFilterOptionRepository;
import com.hip.damoa.domain.board.repository.BoardRepository;
import com.hip.damoa.domain.board.repository.BoardSpecifications;
import com.hip.damoa.domain.board.web.dto.DocumentCreateRequest;
import com.hip.damoa.domain.board.web.dto.DocumentResponse;
import com.hip.damoa.domain.board.web.dto.DocumentUpdateRequest;
import com.hip.damoa.domain.board.web.dto.FileInfo;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.model.FilePricing;
import com.hip.damoa.domain.file.repository.FileDownloadRepository;
import com.hip.damoa.domain.file.repository.FilePricingRepository;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final BoardBookmarkRepository boardBookmarkRepository;
    private final BoardRepository boardRepository;
    private final FileRepository fileRepository;
    private final FileDownloadRepository fileDownloadRepository;
    private final FilePricingRepository filePricingRepository;
    private final UserRepository userRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;

    /**
     * Document 게시글 생성
     */
    @Transactional
    public DocumentResponse createDocument(String userEmail, DocumentCreateRequest request) {
        log.info("Document 게시글 생성 시작: userEmail={}, title={}", userEmail, request.getTitle());

        // 파일 검증
        List<String> effectiveFileUuids = request.getEffectiveFileUuids();
        if (effectiveFileUuids == null || effectiveFileUuids.isEmpty()) {
            throw new BusinessException(ErrorCode.DOCUMENT_FILE_REQUIRED);
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
        processDocumentFiles(board, effectiveFileUuids, request.getThumbnailUuid());

        // 유료 파일 가격 설정 (개별 또는 일괄)
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Map<String, Integer> filePriceMap = request.getFilePriceMap();
        processFilePricingWithMap(effectiveFileUuids, filePriceMap, user.getId());

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
     * Document 게시글 목록 조회 [N+1 최적화]
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocumentList(Pageable pageable) {
        Page<Board> boards = boardService.getBoardsByType(BoardType.DOCUMENT.name(), pageable);

        // [N+1 최적화] Bulk 데이터 준비
        DocumentBulkData bulkData = prepareBulkData(boards.getContent(), null);

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = bulkData.filterOptionMap().getOrDefault(board.getId(), Collections.emptyList());
            List<FileInfo> files = getFileInfosFromMap(board, bulkData.attachmentMap(), bulkData.fileMap(), bulkData.pricingMap());
            FileInfo thumbnail = getThumbnailFromMap(board, bulkData.attachmentMap(), bulkData.fileMap());
            long downloadCount = getDownloadCountFromMap(board, bulkData.attachmentMap(), bulkData.downloadCountMap());
            String userName = getUserNameFromMap(board, bulkData.profileMap());
            return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
        });
    }

    /**
     * Document 게시글 검색 [N+1 최적화]
     */
    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocument(String keyword, Pageable pageable) {
        Page<Board> boards = boardService.searchBoards(BoardType.DOCUMENT.name(), keyword, pageable);

        // [N+1 최적화] Bulk 데이터 준비
        DocumentBulkData bulkData = prepareBulkData(boards.getContent(), null);

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = bulkData.filterOptionMap().getOrDefault(board.getId(), Collections.emptyList());
            List<FileInfo> files = getFileInfosFromMap(board, bulkData.attachmentMap(), bulkData.fileMap(), bulkData.pricingMap());
            FileInfo thumbnail = getThumbnailFromMap(board, bulkData.attachmentMap(), bulkData.fileMap());
            long downloadCount = getDownloadCountFromMap(board, bulkData.attachmentMap(), bulkData.downloadCountMap());
            String userName = getUserNameFromMap(board, bulkData.profileMap());
            return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
        });
    }

    /**
     * Document 게시글 검색 (통합) [N+1 최적화]
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

        // [N+1 최적화] 사용자 ID 조회 (한 번만)
        Long userId = null;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        // [N+1 최적화] Bulk 데이터 준비
        DocumentBulkData bulkData = prepareBulkData(boards.getContent(), userId);

        final Long finalUserId = userId;
        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = bulkData.filterOptionMap().getOrDefault(board.getId(), Collections.emptyList());
            List<FileInfo> files = getFileInfosFromMap(board, bulkData.attachmentMap(), bulkData.fileMap(), bulkData.pricingMap());
            FileInfo thumbnail = getThumbnailFromMap(board, bulkData.attachmentMap(), bulkData.fileMap());
            long downloadCount = getDownloadCountFromMap(board, bulkData.attachmentMap(), bulkData.downloadCountMap());

            boolean isBookmarked = finalUserId != null && bulkData.bookmarkedIds().contains(board.getId());
            boolean hasDownloaded = hasDownloadedFirstFile(board, bulkData.attachmentMap(), bulkData.downloadedFileIds());

            String userName = getUserNameFromMap(board, bulkData.profileMap());
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

        // 필터 옵션 업데이트 - JSONB 배열 방식
        if (request.getFilterOptionIds() != null) {
            boardService.addFilterOptions(uuid, request.getFilterOptionIds());
        }

        // 문서 첨부파일 업데이트
        List<String> effectiveFileUuids = request.getEffectiveFileUuids();
        if (effectiveFileUuids != null) {
            // 기존 첨부파일 Soft delete
            List<BoardAttachment> existingAttachments = boardAttachmentRepository.findByBoardOrderByDisplayOrder(board);
            existingAttachments.forEach(attachment -> attachment.softDelete());

            // 새 첨부파일 추가
            if (!effectiveFileUuids.isEmpty()) {
                processDocumentFiles(board, effectiveFileUuids, request.getThumbnailUuid());
            }
        }

        // 유료 파일 가격 업데이트 (파일 목록이 변경되거나 가격 정보가 변경된 경우)
        if (effectiveFileUuids != null || request.getIsPaid() != null || request.getPrice() != null || request.hasIndividualPricing()) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // 현재 파일 UUID 목록 조회
            @SuppressWarnings("unchecked")
            List<String> currentFileUuids = effectiveFileUuids != null ? effectiveFileUuids :
                    (List<String>) board.getTypeData().get("files");
            if (currentFileUuids != null && !currentFileUuids.isEmpty()) {
                Map<String, Integer> filePriceMap = request.getFilePriceMap();
                processFilePricingWithMap(currentFileUuids, filePriceMap, user.getId());
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
     * Featured Document 목록 [N+1 최적화]
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getFeaturedDocuments() {
        List<Board> boards = boardService.getFeaturedBoards();

        // Document 타입만 필터링
        List<Board> documentBoards = boards.stream()
                .filter(board -> BoardType.DOCUMENT.name().equals(board.getBoardType()))
                .toList();

        // [N+1 최적화] Bulk 데이터 준비
        DocumentBulkData bulkData = prepareBulkData(documentBoards, null);

        return documentBoards.stream()
                .map(board -> {
                    List<BoardFilterOption> filterOptions = bulkData.filterOptionMap().getOrDefault(board.getId(), Collections.emptyList());
                    List<FileInfo> files = getFileInfosFromMap(board, bulkData.attachmentMap(), bulkData.fileMap(), bulkData.pricingMap());
                    FileInfo thumbnail = getThumbnailFromMap(board, bulkData.attachmentMap(), bulkData.fileMap());
                    long downloadCount = getDownloadCountFromMap(board, bulkData.attachmentMap(), bulkData.downloadCountMap());
                    String userName = getUserNameFromMap(board, bulkData.profileMap());
                    return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
                })
                .toList();
    }

    /**
     * Pinned Document 목록 [N+1 최적화]
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getPinnedDocuments() {
        List<Board> boards = boardService.getPinnedBoards(BoardType.DOCUMENT.name());

        // [N+1 최적화] Bulk 데이터 준비
        DocumentBulkData bulkData = prepareBulkData(boards, null);

        return boards.stream()
                .map(board -> {
                    List<BoardFilterOption> filterOptions = bulkData.filterOptionMap().getOrDefault(board.getId(), Collections.emptyList());
                    List<FileInfo> files = getFileInfosFromMap(board, bulkData.attachmentMap(), bulkData.fileMap(), bulkData.pricingMap());
                    FileInfo thumbnail = getThumbnailFromMap(board, bulkData.attachmentMap(), bulkData.fileMap());
                    long downloadCount = getDownloadCountFromMap(board, bulkData.attachmentMap(), bulkData.downloadCountMap());
                    String userName = getUserNameFromMap(board, bulkData.profileMap());
                    return DocumentResponse.from(board, filterOptions, files, thumbnail, false, false, downloadCount, userName);
                })
                .toList();
    }

    /**
     * 내가 작성한 Document 목록 (마이페이지) [N+1 최적화]
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

        // [N+1 최적화] 사용자 ID 조회 (한 번만)
        Long userId = null;
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user != null) {
            userId = user.getId();
        }

        // [N+1 최적화] Bulk 데이터 준비
        DocumentBulkData bulkData = prepareBulkData(boards.getContent(), userId);

        final Long finalUserId = userId;
        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = bulkData.filterOptionMap().getOrDefault(board.getId(), Collections.emptyList());
            List<FileInfo> files = getFileInfosFromMap(board, bulkData.attachmentMap(), bulkData.fileMap(), bulkData.pricingMap());
            FileInfo thumbnail = getThumbnailFromMap(board, bulkData.attachmentMap(), bulkData.fileMap());
            long downloadCount = getDownloadCountFromMap(board, bulkData.attachmentMap(), bulkData.downloadCountMap());

            boolean isBookmarked = finalUserId != null && bulkData.bookmarkedIds().contains(board.getId());
            boolean hasDownloaded = hasDownloadedFirstFile(board, bulkData.attachmentMap(), bulkData.downloadedFileIds());

            String userName = getUserNameFromMap(board, bulkData.profileMap());
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
                throw new BusinessException(ErrorCode.INVALID_UUID_FORMAT);
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
     * Board의 문서 파일 정보 조회 (DOCUMENT 타입만, 가격 정보 포함)
     */
    private List<FileInfo> getDocumentFileInfos(Board board) {
        List<BoardAttachment> attachments = boardAttachmentRepository.findByBoardAndTypeOrderByDisplayOrder(
                board, BoardAttachment.AttachmentType.DOCUMENT);

        return attachments.stream()
                .map(attachment -> {
                    File file = fileRepository.findById(attachment.getFileId())
                            .orElse(null);
                    if (file == null) {
                        return null;
                    }

                    // FilePricing에서 가격 정보 조회
                    FilePricing pricing = filePricingRepository.findByFileId(file.getId()).orElse(null);
                    Boolean isPaid = pricing != null && Boolean.TRUE.equals(pricing.getIsPaid());
                    Integer price = pricing != null ? pricing.getPrice() : 0;

                    return FileInfo.from(file, isPaid, price);
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
     * 삭제된 User의 경우 "알 수 없음" 반환
     */
    private String getUserName(Board board) {
        if (board.getUser() == null) {
            return null;
        }

        try {
            return userProfileRepository.findByUserId(board.getUser().getId())
                    .map(com.hip.damoa.domain.user.model.UserProfile::getName)
                    .orElse(board.getUser().getEmail());
        } catch (Exception e) {
            log.warn("User not found for board: boardId={}", board.getId());
            return "알 수 없음";
        }
    }

    /**
     * 파일 가격 정보 처리 (파일별 개별 가격 지원)
     *
     * @param fileUuids 파일 UUID 목록
     * @param filePriceMap 파일별 가격 맵 (uuid -> price), 맵에 없으면 무료
     * @param userId 사용자 ID
     */
    private void processFilePricingWithMap(List<String> fileUuids, Map<String, Integer> filePriceMap, Long userId) {
        if (fileUuids == null || fileUuids.isEmpty()) {
            return;
        }

        log.info("파일 가격 정보 처리: fileCount={}, paidFileCount={}",
                fileUuids.size(), filePriceMap != null ? filePriceMap.size() : 0);

        for (String fileUuidStr : fileUuids) {
            try {
                UUID fileUuid = UUID.fromString(fileUuidStr);
                File file = fileRepository.findByUuid(fileUuid).orElse(null);

                if (file == null) {
                    log.warn("파일을 찾을 수 없음: fileUuid={}", fileUuidStr);
                    continue;
                }

                // 이 파일의 가격 조회 (맵에 있으면 유료, 없으면 무료)
                Integer price = (filePriceMap != null) ? filePriceMap.get(fileUuidStr) : null;
                boolean shouldBePaid = price != null && price > 0;

                FilePricing pricing = filePricingRepository.findByFileId(file.getId()).orElse(null);

                if (shouldBePaid) {
                    // 유료 설정
                    if (pricing == null) {
                        pricing = FilePricing.builder()
                                .file(file)
                                .isPaid(true)
                                .price(price)
                                .isActive(true)
                                .createdBy(userId)
                                .build();
                    } else {
                        pricing.setAsPaid(price);
                        pricing.setUpdatedBy(userId);
                    }
                    filePricingRepository.save(pricing);
                    log.debug("유료 파일 가격 설정: fileId={}, price={}", file.getId(), price);
                } else {
                    // 무료 설정
                    if (pricing != null) {
                        pricing.setAsFree();
                        pricing.setUpdatedBy(userId);
                        filePricingRepository.save(pricing);
                        log.debug("파일 무료 전환: fileId={}", file.getId());
                    }
                }
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 파일 UUID 형식: {}", fileUuidStr);
            }
        }

        log.info("파일 가격 정보 처리 완료");
    }

    // ==================== [N+1 최적화] 헬퍼 메서드들 ====================

    /**
     * Board 목록에 대한 bulk 데이터 준비 (N+1 최적화)
     */
    private DocumentBulkData prepareBulkData(List<Board> boards, Long userId) {
        if (boards.isEmpty()) {
            return new DocumentBulkData(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptySet(),
                    Collections.emptySet()
            );
        }

        // 1. Board ID 목록
        List<Long> boardIds = boards.stream().map(Board::getId).toList();

        // 2. 필터 옵션 일괄 조회
        List<BoardFilterOption> allFilterOptions = boardFilterOptionRepository.findByBoardIdIn(boardIds);
        Map<Long, List<BoardFilterOption>> filterOptionMap = allFilterOptions.stream()
                .collect(Collectors.groupingBy(bfo -> bfo.getBoard().getId()));

        // 3. 첨부파일 일괄 조회 (DOCUMENT + THUMBNAIL)
        List<BoardAttachment> allAttachments = boardAttachmentRepository.findByBoardIdIn(boardIds);
        Map<Long, List<BoardAttachment>> attachmentMap = allAttachments.stream()
                .collect(Collectors.groupingBy(att -> att.getBoard().getId()));

        // 4. 파일 ID 수집 및 파일 일괄 조회
        List<Long> fileIds = allAttachments.stream()
                .map(BoardAttachment::getFileId)
                .distinct()
                .toList();

        Map<Long, File> fileMap = Collections.emptyMap();
        Map<Long, FilePricing> pricingMap = Collections.emptyMap();
        Map<Long, Long> downloadCountMap = Collections.emptyMap();
        Set<Long> downloadedFileIds = Collections.emptySet();

        if (!fileIds.isEmpty()) {
            List<File> files = fileRepository.findByIdIn(fileIds);
            fileMap = files.stream().collect(Collectors.toMap(File::getId, f -> f));

            // 5. 파일 가격 정보 일괄 조회
            List<FilePricing> pricings = filePricingRepository.findByFileIdIn(fileIds);
            pricingMap = pricings.stream().collect(Collectors.toMap(fp -> fp.getFile().getId(), fp -> fp));

            // 6. 다운로드 횟수 일괄 조회
            List<Object[]> downloadCounts = fileDownloadRepository.countByFileIdIn(fileIds);
            downloadCountMap = new HashMap<>();
            for (Object[] row : downloadCounts) {
                Long fileId = (Long) row[0];
                Long count = (Long) row[1];
                downloadCountMap.put(fileId, count);
            }

            // 7. 사용자의 다운로드 여부 일괄 조회
            if (userId != null) {
                List<Long> downloadedIds = fileDownloadRepository.findDownloadedFileIds(fileIds, userId);
                downloadedFileIds = new HashSet<>(downloadedIds);
            }
        }

        // 8. 사용자 프로필 일괄 조회
        List<Long> userIds = boards.stream()
                .filter(b -> b.getUser() != null)
                .map(b -> b.getUser().getId())
                .distinct()
                .toList();

        Map<Long, com.hip.damoa.domain.user.model.UserProfile> profileMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<com.hip.damoa.domain.user.model.UserProfile> profiles = userProfileRepository.findByUserIdIn(userIds);
            profileMap = profiles.stream().collect(Collectors.toMap(up -> up.getUser().getId(), up -> up));
        }

        // 9. 북마크 여부 일괄 조회
        Set<Long> bookmarkedIds = Collections.emptySet();
        if (userId != null) {
            List<Long> bookmarkedBoardIds = boardBookmarkRepository.findBookmarkedBoardIds(boardIds, userId);
            bookmarkedIds = new HashSet<>(bookmarkedBoardIds);
        }

        return new DocumentBulkData(filterOptionMap, attachmentMap, fileMap, pricingMap,
                downloadCountMap, profileMap, bookmarkedIds, downloadedFileIds);
    }

    /**
     * Bulk 데이터 홀더 클래스
     */
    private record DocumentBulkData(
            Map<Long, List<BoardFilterOption>> filterOptionMap,
            Map<Long, List<BoardAttachment>> attachmentMap,
            Map<Long, File> fileMap,
            Map<Long, FilePricing> pricingMap,
            Map<Long, Long> downloadCountMap,
            Map<Long, com.hip.damoa.domain.user.model.UserProfile> profileMap,
            Set<Long> bookmarkedIds,
            Set<Long> downloadedFileIds
    ) {}

    /**
     * Map에서 Board의 FileInfo 목록 추출 (Document 타입)
     */
    private List<FileInfo> getFileInfosFromMap(Board board,
                                                Map<Long, List<BoardAttachment>> attachmentMap,
                                                Map<Long, File> fileMap,
                                                Map<Long, FilePricing> pricingMap) {
        List<BoardAttachment> attachments = attachmentMap.getOrDefault(board.getId(), Collections.emptyList());
        return attachments.stream()
                .filter(att -> att.getAttachmentType() == BoardAttachment.AttachmentType.DOCUMENT)
                .map(att -> {
                    File file = fileMap.get(att.getFileId());
                    if (file == null) return null;
                    FilePricing pricing = pricingMap.get(file.getId());
                    Boolean isPaid = pricing != null && Boolean.TRUE.equals(pricing.getIsPaid());
                    Integer price = pricing != null ? pricing.getPrice() : 0;
                    return FileInfo.from(file, isPaid, price);
                })
                .filter(info -> info != null)
                .toList();
    }

    /**
     * Map에서 Board의 썸네일 FileInfo 추출
     */
    private FileInfo getThumbnailFromMap(Board board,
                                          Map<Long, List<BoardAttachment>> attachmentMap,
                                          Map<Long, File> fileMap) {
        List<BoardAttachment> attachments = attachmentMap.getOrDefault(board.getId(), Collections.emptyList());
        return attachments.stream()
                .filter(att -> att.getAttachmentType() == BoardAttachment.AttachmentType.THUMBNAIL)
                .findFirst()
                .map(att -> {
                    File file = fileMap.get(att.getFileId());
                    return file != null ? FileInfo.from(file) : null;
                })
                .orElse(null);
    }

    /**
     * Map에서 Board의 다운로드 횟수 추출
     */
    private long getDownloadCountFromMap(Board board,
                                          Map<Long, List<BoardAttachment>> attachmentMap,
                                          Map<Long, Long> downloadCountMap) {
        List<BoardAttachment> attachments = attachmentMap.getOrDefault(board.getId(), Collections.emptyList());
        return attachments.stream()
                .filter(att -> att.getAttachmentType() == BoardAttachment.AttachmentType.DOCUMENT)
                .mapToLong(att -> downloadCountMap.getOrDefault(att.getFileId(), 0L))
                .sum();
    }

    /**
     * Map에서 사용자 이름 추출
     */
    private String getUserNameFromMap(Board board, Map<Long, com.hip.damoa.domain.user.model.UserProfile> profileMap) {
        if (board.getUser() == null) {
            return null;
        }
        com.hip.damoa.domain.user.model.UserProfile profile = profileMap.get(board.getUser().getId());
        return profile != null ? profile.getName() : board.getUser().getEmail();
    }

    /**
     * 사용자가 첫 번째 파일을 다운로드했는지 확인
     */
    private boolean hasDownloadedFirstFile(Board board,
                                            Map<Long, List<BoardAttachment>> attachmentMap,
                                            Set<Long> downloadedFileIds) {
        List<BoardAttachment> attachments = attachmentMap.getOrDefault(board.getId(), Collections.emptyList());
        return attachments.stream()
                .filter(att -> att.getAttachmentType() == BoardAttachment.AttachmentType.DOCUMENT)
                .findFirst()
                .map(att -> downloadedFileIds.contains(att.getFileId()))
                .orElse(false);
    }
}
