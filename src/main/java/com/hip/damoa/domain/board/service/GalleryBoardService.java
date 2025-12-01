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
import com.hip.damoa.domain.board.web.dto.FileInfo;
import com.hip.damoa.domain.board.web.dto.GalleryCreateRequest;
import com.hip.damoa.domain.board.web.dto.GalleryResponse;
import com.hip.damoa.domain.board.web.dto.GalleryUpdateRequest;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
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
 * Gallery 게시판 Service
 *
 * 사진 게시판 전용 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GalleryBoardService {

    private final BoardService boardService;
    private final BoardBookmarkService boardBookmarkService;
    private final BoardRepository boardRepository;
    private final BoardFilterOptionRepository boardFilterOptionRepository;
    private final BoardAttachmentRepository boardAttachmentRepository;
    private final FileRepository fileRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;

    /**
     * Gallery 게시글 생성
     */
    @Transactional
    public GalleryResponse createGallery(String userEmail, GalleryCreateRequest request) {
        log.info("Gallery 게시글 생성 시작: userEmail={}, title={}", userEmail, request.getTitle());

        // 이미지 검증
        if (request.getImageUuids() == null || request.getImageUuids().isEmpty()) {
            throw new BusinessException(ErrorCode.GALLERY_IMAGE_REQUIRED);
        }

        // Board 생성
        Board board = boardService.createBoard(
                userEmail,
                BoardType.GALLERY.name(),
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

        // 이미지 첨부파일 생성
        processGalleryImages(board, request.getImageUuids());

        log.info("Gallery 게시글 생성 완료: uuid={}", board.getUuid());

        // Response 생성
        List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
        List<FileInfo> images = getFileInfos(board);
        String userName = getUserName(board);
        return GalleryResponse.from(board, filterOptions, images, false, userName);
    }

    /**
     * Gallery 게시글 조회
     */
    @Transactional
    public GalleryResponse getGallery(UUID uuid, String userEmail) {
        log.info("Gallery 게시글 조회: uuid={}", uuid);

        Board board = boardService.getBoard(uuid);

        // BoardType 검증
        if (!BoardType.GALLERY.name().equals(board.getBoardType())) {
            throw new BusinessException(ErrorCode.BOARD_TYPE_MISMATCH);
        }

        // 조회수 증가
        boardService.incrementViewCount(uuid);

        // 필터 옵션 조회
        List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());

        // 파일 정보 조회
        List<FileInfo> images = getFileInfos(board);

        // 북마크 여부 확인
        boolean isBookmarked = false;
        if (userEmail != null) {
            isBookmarked = boardBookmarkService.isBookmarked(uuid, userEmail);
        }

        String userName = getUserName(board);
        return GalleryResponse.from(board, filterOptions, images, isBookmarked, userName);
    }

    /**
     * Gallery 게시글 검색 (통합)
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
    public Page<GalleryResponse> searchGalleries(String keyword, String[] tags, List<Long> filterOptionIds,
                                                  Boolean onlyBookmarked, Boolean onlyMyPosts,
                                                  String userEmail, Pageable pageable) {

        log.info("Gallery 게시글 검색 시작: keyword={}, tags={}, filterOptions={}, onlyBookmarked={}, onlyMyPosts={}, userEmail={}",
                keyword, tags != null ? String.join(",", tags) : null, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail);

        // 로그인이 필요한 기능 체크
        if ((Boolean.TRUE.equals(onlyBookmarked) || Boolean.TRUE.equals(onlyMyPosts)) && userEmail == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        // Specification 구성 (CompanyService와 동일한 패턴)
        Specification<Board> spec = BoardSpecifications.searchBoards(
                BoardType.GALLERY.name(),
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
            List<FileInfo> images = getFileInfos(board);
            boolean isBookmarked = checkBookmark && boardBookmarkService.isBookmarked(board.getUuid(), userEmail);
            String userName = getUserName(board);
            return GalleryResponse.from(board, filterOptions, images, isBookmarked, userName);
        });
    }

    /**
     * Gallery 게시글 수정
     */
    @Transactional
    public GalleryResponse updateGallery(UUID uuid, String userEmail, GalleryUpdateRequest request) {
        log.info("Gallery 게시글 수정 시작: uuid={}, userEmail={}", uuid, userEmail);

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

        // 이미지 첨부파일 업데이트
        if (request.getImageUuids() != null) {
            // 기존 첨부파일 Soft delete
            List<BoardAttachment> existingAttachments = boardAttachmentRepository.findByBoardOrderByDisplayOrder(board);
            existingAttachments.forEach(attachment -> attachment.softDelete());

            // 새 첨부파일 추가
            if (!request.getImageUuids().isEmpty()) {
                processGalleryImages(board, request.getImageUuids());
            }
        }

        log.info("Gallery 게시글 수정 완료: uuid={}", uuid);

        List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
        List<FileInfo> images = getFileInfos(board);
        boolean isBookmarked = boardBookmarkService.isBookmarked(uuid, userEmail);
        String userName = getUserName(board);
        return GalleryResponse.from(board, filterOptions, images, isBookmarked, userName);
    }

    /**
     * Gallery 게시글 삭제
     */
    @Transactional
    public void deleteGallery(UUID uuid, String userEmail) {
        log.info("Gallery 게시글 삭제: uuid={}, userEmail={}", uuid, userEmail);
        boardService.deleteBoard(uuid, userEmail);
    }

    /**
     * Featured Gallery 목록
     */
    @Transactional(readOnly = true)
    public List<GalleryResponse> getFeaturedGalleries() {
        List<Board> boards = boardService.getFeaturedBoards();

        return boards.stream()
                .filter(board -> BoardType.GALLERY.name().equals(board.getBoardType()))
                .map(board -> {
                    List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
                    List<FileInfo> images = getFileInfos(board);
                    String userName = getUserName(board);
                    return GalleryResponse.from(board, filterOptions, images, false, userName);
                })
                .toList();
    }

    /**
     * Pinned Gallery 목록
     */
    @Transactional(readOnly = true)
    public List<GalleryResponse> getPinnedGalleries() {
        List<Board> boards = boardService.getPinnedBoards(BoardType.GALLERY.name());

        return boards.stream()
                .map(board -> {
                    List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
                    List<FileInfo> images = getFileInfos(board);
                    String userName = getUserName(board);
                    return GalleryResponse.from(board, filterOptions, images, false, userName);
                })
                .toList();
    }

    /**
     * 내가 작성한 Gallery 목록 (마이페이지)
     */
    @Transactional(readOnly = true)
    public Page<GalleryResponse> getMyGalleries(String userEmail, Pageable pageable) {
        log.info("내 Gallery 목록 조회: userEmail={}", userEmail);

        // Specification 사용하여 내가 작성한 게시글만 조회
        Specification<Board> spec = BoardSpecifications.searchBoards(
                BoardType.GALLERY.name(),
                null,  // keyword 없음
                null,  // filterOptions 없음
                false, // onlyBookmarked = false
                true,  // onlyMyPosts = true
                userEmail
        );

        Page<Board> boards = boardRepository.findAll(spec, pageable);

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = boardFilterOptionRepository.findByBoardId(board.getId());
            List<FileInfo> images = getFileInfos(board);
            boolean isBookmarked = boardBookmarkService.isBookmarked(board.getUuid(), userEmail);
            String userName = getUserName(board);
            return GalleryResponse.from(board, filterOptions, images, isBookmarked, userName);
        });
    }

    /**
     * Gallery 이미지 첨부파일 처리
     */
    private void processGalleryImages(Board board, List<String> imageUuids) {
        if (imageUuids == null || imageUuids.isEmpty()) {
            return;
        }

        log.info("Gallery 이미지 첨부파일 처리 시작: boardId={}, imageCount={}", board.getId(), imageUuids.size());

        int displayOrder = 0;
        for (String uuidString : imageUuids) {
            try {
                UUID imageUuid = UUID.fromString(uuidString);
                saveGalleryImage(board, imageUuid, displayOrder++);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format: {}", uuidString);
                throw new BusinessException(ErrorCode.INVALID_UUID_FORMAT);
            }
        }

        log.info("Gallery 이미지 첨부파일 처리 완료: boardId={}, attachmentCount={}", board.getId(), imageUuids.size());
    }

    /**
     * Gallery 이미지 저장
     */
    private void saveGalleryImage(Board board, UUID imageUuid, int displayOrder) {
        // UUID로 파일 조회
        File file = fileRepository.findByUuidAndIsDeletedFalse(imageUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));

        // Files 테이블의 entityId 업데이트 (스케줄러 삭제 방지)
        file.updateEntityInfo("BOARD_IMAGE", board.getId());
        fileRepository.save(file);

        // BoardAttachment 생성
        BoardAttachment attachment = BoardAttachment.builder()
                .board(board)
                .fileId(file.getId())
                .attachmentType(BoardAttachment.AttachmentType.IMAGE)
                .displayOrder(displayOrder)
                .build();

        boardAttachmentRepository.save(attachment);
        log.debug("Gallery 이미지 첨부파일 저장: fileId={}, displayOrder={}", file.getId(), displayOrder);
    }

    /**
     * Board의 파일 정보 조회 (BoardAttachment → File → FileInfo)
     */
    private List<FileInfo> getFileInfos(Board board) {
        List<BoardAttachment> attachments = boardAttachmentRepository.findByBoardOrderByDisplayOrder(board);

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
