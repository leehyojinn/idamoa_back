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
import com.hip.damoa.domain.board.web.dto.FileInfo;
import com.hip.damoa.domain.board.web.dto.GalleryCreateRequest;
import com.hip.damoa.domain.board.web.dto.GalleryResponse;
import com.hip.damoa.domain.board.web.dto.GalleryUpdateRequest;
import com.hip.damoa.domain.company.model.Company;
import com.hip.damoa.domain.company.model.CompanyReview;
import com.hip.damoa.domain.company.model.CompanyReviewImage;
import com.hip.damoa.domain.company.repository.CompanyRepository;
import com.hip.damoa.domain.company.repository.CompanyReviewRepository;
import com.hip.damoa.domain.board.repository.BoardLikeRepository;
import com.hip.damoa.domain.file.model.File;
import com.hip.damoa.domain.file.repository.FileRepository;
import com.hip.damoa.domain.user.model.User;
import com.hip.damoa.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final BoardBookmarkRepository boardBookmarkRepository;
    private final BoardRepository boardRepository;
    private final BoardFilterOptionRepository boardFilterOptionRepository;
    private final BoardAttachmentRepository boardAttachmentRepository;
    private final FileRepository fileRepository;
    private final com.hip.damoa.domain.user.repository.UserProfileRepository userProfileRepository;
    private final CompanyRepository companyRepository;
    private final CompanyReviewRepository companyReviewRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final com.hip.damoa.domain.user.repository.UserRepository userRepository;

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
        GalleryResponse.CompanySummary company = getCompanySummary(board);
        return GalleryResponse.from(board, filterOptions, images, false, false, userName, company, null);
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
        GalleryResponse.CompanySummary company = getCompanySummary(board);
        List<GalleryResponse.ReviewSummary> reviews = getReviews(board);
        boolean liked = isLiked(board, userEmail);
        return GalleryResponse.from(board, filterOptions, images, isBookmarked, liked, userName, company, reviews);
    }

    /**
     * Gallery 게시글 검색 (통합) - N+1 최적화 버전
     *
     * @param keyword 검색 키워드 (nullable)
     * @param tags 태그 필터 (nullable)
     * @param filterOptionIds 필터 옵션 IDs (nullable)
     * @param onlyBookmarked 북마크된 게시글만 조회 (nullable, userEmail 필요)
     * @param onlyMyPosts 내가 쓴 게시글만 조회 (nullable, userEmail 필요)
     * @param userEmail 사용자 이메일 (북마크 조회 시 필요)
     * @param companyUuid 회사 UUID (특정 회사의 갤러리만 조회, nullable)
     * @param pageable 페이지 정보
     * @return 검색 결과
     */
    @Transactional(readOnly = true)
    public Page<GalleryResponse> searchGalleries(String keyword, String[] tags, List<Long> filterOptionIds,
                                                  Boolean onlyBookmarked, Boolean onlyMyPosts,
                                                  String userEmail, UUID companyUuid, Pageable pageable) {

        log.info("Gallery 게시글 검색 시작: keyword={}, tags={}, filterOptions={}, onlyBookmarked={}, onlyMyPosts={}, userEmail={}, companyUuid={}",
                keyword, tags != null ? String.join(",", tags) : null, filterOptionIds, onlyBookmarked, onlyMyPosts, userEmail, companyUuid);

        // 로그인이 필요한 기능 체크
        if ((Boolean.TRUE.equals(onlyBookmarked) || Boolean.TRUE.equals(onlyMyPosts)) && userEmail == null) {
            throw new BusinessException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        // Specification 구성
        Specification<Board> spec = BoardSpecifications.searchBoards(
                BoardType.GALLERY.name(),
                keyword,
                tags,
                filterOptionIds,
                onlyBookmarked,
                onlyMyPosts,
                userEmail,
                companyUuid
        );

        // 1. Board 목록 조회
        Page<Board> boards = boardRepository.findAll(spec, pageable);
        List<Board> boardList = boards.getContent();

        if (boardList.isEmpty()) {
            return boards.map(b -> null);
        }

        // 2. 필요한 ID 목록 추출
        List<Long> boardIds = boardList.stream().map(Board::getId).toList();
        List<Long> userIds = boardList.stream()
                .map(b -> b.getUser() != null ? b.getUser().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3. FilterOption 일괄 조회 (IN 절 - 1번 쿼리)
        Map<Long, List<BoardFilterOption>> filterOptionsMap = boardFilterOptionRepository.findByBoardIdIn(boardIds)
                .stream()
                .collect(Collectors.groupingBy(bfo -> bfo.getBoard().getId()));

        // 4. Attachment 일괄 조회 (IN 절 - 1번 쿼리)
        Map<Long, List<BoardAttachment>> attachmentsMap = boardAttachmentRepository.findByBoardIdIn(boardIds)
                .stream()
                .collect(Collectors.groupingBy(ba -> ba.getBoard().getId()));

        // 5. File 일괄 조회 (IN 절 - 1번 쿼리)
        List<Long> fileIds = attachmentsMap.values().stream()
                .flatMap(List::stream)
                .map(BoardAttachment::getFileId)
                .distinct()
                .toList();
        Map<Long, File> filesMap = fileIds.isEmpty() ? Map.of() :
                fileRepository.findByIdIn(fileIds).stream()
                        .collect(Collectors.toMap(File::getId, f -> f));

        // 6. UserProfile 일괄 조회 (IN 절 - 1번 쿼리)
        Map<Long, UserProfile> profilesMap = userIds.isEmpty() ? Map.of() :
                userProfileRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(up -> up.getUser().getId(), up -> up, (a, b) -> a));

        // 7. Company 일괄 조회 (IN 절 - 1번 쿼리)
        List<Company> companies = userIds.isEmpty() ? List.of() : companyRepository.findByOwnerIdIn(userIds);
        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(c -> c.getOwner().getId(), c -> c, (a, b) -> a));

        // 8. Rating/ReviewCount 일괄 조회 (IN 절 - 2번 쿼리)
        List<Long> companyIds = companies.stream().map(Company::getId).toList();
        Map<Long, Double> ratingsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getAverageRatingsRaw(companyIds).stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Double) row[1]
                        ));
        Map<Long, Long> reviewCountsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getReviewCountsRaw(companyIds).stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> (Long) row[1]
                        ));

        // 9. Bookmark/Like 일괄 조회 (IN 절 - 2번 쿼리, 로그인한 경우만)
        Set<Long> bookmarkedBoardIds = Set.of();
        Set<Long> likedBoardIds = Set.of();
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                bookmarkedBoardIds = new HashSet<>(
                        boardBookmarkRepository.findBookmarkedBoardIds(boardIds, user.getId()));
                likedBoardIds = new HashSet<>(
                        boardLikeRepository.findLikedBoardIds(boardIds, user.getId()));
            }
        }

        // 10. 최종 매핑 (추가 쿼리 없음 - 메모리에서 처리)
        final Set<Long> finalBookmarked = bookmarkedBoardIds;
        final Set<Long> finalLiked = likedBoardIds;

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = filterOptionsMap.getOrDefault(board.getId(), List.of());
            List<FileInfo> images = getFileInfosFromMap(board, attachmentsMap, filesMap);
            boolean isBookmarked = finalBookmarked.contains(board.getId());
            boolean liked = finalLiked.contains(board.getId());
            String userName = getUserNameFromMap(board, profilesMap);
            GalleryResponse.CompanySummary company = getCompanySummaryFromMap(board, companyMap, ratingsMap, reviewCountsMap);

            return GalleryResponse.from(board, filterOptions, images, isBookmarked, liked, userName, company, null);
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
        GalleryResponse.CompanySummary company = getCompanySummary(board);
        return GalleryResponse.from(board, filterOptions, images, isBookmarked, false, userName, company, null);
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
     * Featured Gallery 목록 - N+1 최적화 버전
     */
    @Transactional(readOnly = true)
    public List<GalleryResponse> getFeaturedGalleries() {
        List<Board> boards = boardService.getFeaturedBoards();
        List<Board> galleryBoards = boards.stream()
                .filter(board -> BoardType.GALLERY.name().equals(board.getBoardType()))
                .toList();

        if (galleryBoards.isEmpty()) {
            return List.of();
        }

        // Bulk 조회
        List<Long> boardIds = galleryBoards.stream().map(Board::getId).toList();
        List<Long> userIds = galleryBoards.stream()
                .map(b -> b.getUser() != null ? b.getUser().getId() : null)
                .filter(Objects::nonNull).distinct().toList();

        Map<Long, List<BoardFilterOption>> filterOptionsMap = boardFilterOptionRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(bfo -> bfo.getBoard().getId()));
        Map<Long, List<BoardAttachment>> attachmentsMap = boardAttachmentRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(ba -> ba.getBoard().getId()));
        List<Long> fileIds = attachmentsMap.values().stream().flatMap(List::stream)
                .map(BoardAttachment::getFileId).distinct().toList();
        Map<Long, File> filesMap = fileIds.isEmpty() ? Map.of() :
                fileRepository.findByIdIn(fileIds).stream().collect(Collectors.toMap(File::getId, f -> f));
        Map<Long, UserProfile> profilesMap = userIds.isEmpty() ? Map.of() :
                userProfileRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(up -> up.getUser().getId(), up -> up, (a, b) -> a));
        List<Company> companies = userIds.isEmpty() ? List.of() : companyRepository.findByOwnerIdIn(userIds);
        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(c -> c.getOwner().getId(), c -> c, (a, b) -> a));
        List<Long> companyIds = companies.stream().map(Company::getId).toList();
        Map<Long, Double> ratingsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getAverageRatingsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Double) row[1]));
        Map<Long, Long> reviewCountsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getReviewCountsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return galleryBoards.stream()
                .map(board -> {
                    List<BoardFilterOption> filterOptions = filterOptionsMap.getOrDefault(board.getId(), List.of());
                    List<FileInfo> images = getFileInfosFromMap(board, attachmentsMap, filesMap);
                    String userName = getUserNameFromMap(board, profilesMap);
                    GalleryResponse.CompanySummary company = getCompanySummaryFromMap(board, companyMap, ratingsMap, reviewCountsMap);
                    return GalleryResponse.from(board, filterOptions, images, false, false, userName, company, null);
                })
                .toList();
    }

    /**
     * Pinned Gallery 목록 - N+1 최적화 버전
     */
    @Transactional(readOnly = true)
    public List<GalleryResponse> getPinnedGalleries() {
        List<Board> boards = boardService.getPinnedBoards(BoardType.GALLERY.name());

        if (boards.isEmpty()) {
            return List.of();
        }

        // Bulk 조회
        List<Long> boardIds = boards.stream().map(Board::getId).toList();
        List<Long> userIds = boards.stream()
                .map(b -> b.getUser() != null ? b.getUser().getId() : null)
                .filter(Objects::nonNull).distinct().toList();

        Map<Long, List<BoardFilterOption>> filterOptionsMap = boardFilterOptionRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(bfo -> bfo.getBoard().getId()));
        Map<Long, List<BoardAttachment>> attachmentsMap = boardAttachmentRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(ba -> ba.getBoard().getId()));
        List<Long> fileIds = attachmentsMap.values().stream().flatMap(List::stream)
                .map(BoardAttachment::getFileId).distinct().toList();
        Map<Long, File> filesMap = fileIds.isEmpty() ? Map.of() :
                fileRepository.findByIdIn(fileIds).stream().collect(Collectors.toMap(File::getId, f -> f));
        Map<Long, UserProfile> profilesMap = userIds.isEmpty() ? Map.of() :
                userProfileRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(up -> up.getUser().getId(), up -> up, (a, b) -> a));
        List<Company> companies = userIds.isEmpty() ? List.of() : companyRepository.findByOwnerIdIn(userIds);
        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(c -> c.getOwner().getId(), c -> c, (a, b) -> a));
        List<Long> companyIds = companies.stream().map(Company::getId).toList();
        Map<Long, Double> ratingsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getAverageRatingsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Double) row[1]));
        Map<Long, Long> reviewCountsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getReviewCountsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return boards.stream()
                .map(board -> {
                    List<BoardFilterOption> filterOptions = filterOptionsMap.getOrDefault(board.getId(), List.of());
                    List<FileInfo> images = getFileInfosFromMap(board, attachmentsMap, filesMap);
                    String userName = getUserNameFromMap(board, profilesMap);
                    GalleryResponse.CompanySummary company = getCompanySummaryFromMap(board, companyMap, ratingsMap, reviewCountsMap);
                    return GalleryResponse.from(board, filterOptions, images, false, false, userName, company, null);
                })
                .toList();
    }

    /**
     * 내가 작성한 Gallery 목록 (마이페이지) - N+1 최적화 버전
     */
    @Transactional(readOnly = true)
    public Page<GalleryResponse> getMyGalleries(String userEmail, Pageable pageable) {
        log.info("내 Gallery 목록 조회: userEmail={}", userEmail);

        Specification<Board> spec = BoardSpecifications.searchBoards(
                BoardType.GALLERY.name(), null, null, false, true, userEmail);

        Page<Board> boards = boardRepository.findAll(spec, pageable);
        List<Board> boardList = boards.getContent();

        if (boardList.isEmpty()) {
            return boards.map(b -> null);
        }

        // Bulk 조회
        List<Long> boardIds = boardList.stream().map(Board::getId).toList();
        List<Long> userIds = boardList.stream()
                .map(b -> b.getUser() != null ? b.getUser().getId() : null)
                .filter(Objects::nonNull).distinct().toList();

        Map<Long, List<BoardFilterOption>> filterOptionsMap = boardFilterOptionRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(bfo -> bfo.getBoard().getId()));
        Map<Long, List<BoardAttachment>> attachmentsMap = boardAttachmentRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(ba -> ba.getBoard().getId()));
        List<Long> fileIds = attachmentsMap.values().stream().flatMap(List::stream)
                .map(BoardAttachment::getFileId).distinct().toList();
        Map<Long, File> filesMap = fileIds.isEmpty() ? Map.of() :
                fileRepository.findByIdIn(fileIds).stream().collect(Collectors.toMap(File::getId, f -> f));
        Map<Long, UserProfile> profilesMap = userIds.isEmpty() ? Map.of() :
                userProfileRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(up -> up.getUser().getId(), up -> up, (a, b) -> a));
        List<Company> companies = userIds.isEmpty() ? List.of() : companyRepository.findByOwnerIdIn(userIds);
        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(c -> c.getOwner().getId(), c -> c, (a, b) -> a));
        List<Long> companyIds = companies.stream().map(Company::getId).toList();
        Map<Long, Double> ratingsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getAverageRatingsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Double) row[1]));
        Map<Long, Long> reviewCountsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getReviewCountsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        // Bookmark/Like
        User user = userRepository.findByEmail(userEmail).orElse(null);
        Set<Long> bookmarkedBoardIds = user != null ?
                new HashSet<>(boardBookmarkRepository.findBookmarkedBoardIds(boardIds, user.getId())) : Set.of();
        Set<Long> likedBoardIds = user != null ?
                new HashSet<>(boardLikeRepository.findLikedBoardIds(boardIds, user.getId())) : Set.of();

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = filterOptionsMap.getOrDefault(board.getId(), List.of());
            List<FileInfo> images = getFileInfosFromMap(board, attachmentsMap, filesMap);
            boolean isBookmarked = bookmarkedBoardIds.contains(board.getId());
            boolean liked = likedBoardIds.contains(board.getId());
            String userName = getUserNameFromMap(board, profilesMap);
            GalleryResponse.CompanySummary company = getCompanySummaryFromMap(board, companyMap, ratingsMap, reviewCountsMap);
            return GalleryResponse.from(board, filterOptions, images, isBookmarked, liked, userName, company, null);
        });
    }

    /**
     * 특정 회사의 Gallery 목록 조회 (포트폴리오) - N+1 최적화 버전
     */
    @Transactional(readOnly = true)
    public Page<GalleryResponse> getGalleriesByCompanyUuid(UUID companyUuid, String currentUserEmail, Pageable pageable) {
        log.info("회사별 Gallery 목록 조회: companyUuid={}", companyUuid);

        Company targetCompany = companyRepository.findByUuid(companyUuid)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPANY_NOT_FOUND));

        Page<Board> boards = boardRepository.findByUserIdAndBoardTypeAndIsPublishedTrueAndIsDeletedFalse(
                targetCompany.getOwner().getId(), BoardType.GALLERY.name(), pageable);
        List<Board> boardList = boards.getContent();

        if (boardList.isEmpty()) {
            return boards.map(b -> null);
        }

        // Bulk 조회
        List<Long> boardIds = boardList.stream().map(Board::getId).toList();
        List<Long> userIds = boardList.stream()
                .map(b -> b.getUser() != null ? b.getUser().getId() : null)
                .filter(Objects::nonNull).distinct().toList();

        Map<Long, List<BoardFilterOption>> filterOptionsMap = boardFilterOptionRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(bfo -> bfo.getBoard().getId()));
        Map<Long, List<BoardAttachment>> attachmentsMap = boardAttachmentRepository.findByBoardIdIn(boardIds)
                .stream().collect(Collectors.groupingBy(ba -> ba.getBoard().getId()));
        List<Long> fileIds = attachmentsMap.values().stream().flatMap(List::stream)
                .map(BoardAttachment::getFileId).distinct().toList();
        Map<Long, File> filesMap = fileIds.isEmpty() ? Map.of() :
                fileRepository.findByIdIn(fileIds).stream().collect(Collectors.toMap(File::getId, f -> f));
        Map<Long, UserProfile> profilesMap = userIds.isEmpty() ? Map.of() :
                userProfileRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(up -> up.getUser().getId(), up -> up, (a, b) -> a));
        List<Company> companies = userIds.isEmpty() ? List.of() : companyRepository.findByOwnerIdIn(userIds);
        Map<Long, Company> companyMap = companies.stream()
                .collect(Collectors.toMap(c -> c.getOwner().getId(), c -> c, (a, b) -> a));
        List<Long> companyIds = companies.stream().map(Company::getId).toList();
        Map<Long, Double> ratingsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getAverageRatingsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Double) row[1]));
        Map<Long, Long> reviewCountsMap = companyIds.isEmpty() ? Map.of() :
                companyReviewRepository.getReviewCountsRaw(companyIds).stream()
                        .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        // Bookmark/Like
        Set<Long> bookmarkedBoardIds = Set.of();
        Set<Long> likedBoardIds = Set.of();
        if (currentUserEmail != null) {
            User user = userRepository.findByEmail(currentUserEmail).orElse(null);
            if (user != null) {
                bookmarkedBoardIds = new HashSet<>(boardBookmarkRepository.findBookmarkedBoardIds(boardIds, user.getId()));
                likedBoardIds = new HashSet<>(boardLikeRepository.findLikedBoardIds(boardIds, user.getId()));
            }
        }

        final Set<Long> finalBookmarked = bookmarkedBoardIds;
        final Set<Long> finalLiked = likedBoardIds;

        return boards.map(board -> {
            List<BoardFilterOption> filterOptions = filterOptionsMap.getOrDefault(board.getId(), List.of());
            List<FileInfo> images = getFileInfosFromMap(board, attachmentsMap, filesMap);
            boolean isBookmarked = finalBookmarked.contains(board.getId());
            boolean liked = finalLiked.contains(board.getId());
            String userName = getUserNameFromMap(board, profilesMap);
            GalleryResponse.CompanySummary companySummary = getCompanySummaryFromMap(board, companyMap, ratingsMap, reviewCountsMap);
            return GalleryResponse.from(board, filterOptions, images, isBookmarked, liked, userName, companySummary, null);
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
     * Board의 파일 정보 조회 (BoardAttachment → File → FileInfo) [N+1 최적화]
     */
    private List<FileInfo> getFileInfos(Board board) {
        List<BoardAttachment> attachments = boardAttachmentRepository.findByBoardOrderByDisplayOrder(board);

        if (attachments.isEmpty()) {
            return List.of();
        }

        // [N+1 최적화] 파일 ID 목록으로 일괄 조회
        List<Long> fileIds = attachments.stream()
                .map(BoardAttachment::getFileId)
                .toList();

        Map<Long, File> fileMap = fileRepository.findByIdIn(fileIds).stream()
                .collect(Collectors.toMap(File::getId, f -> f));

        return attachments.stream()
                .map(attachment -> fileMap.get(attachment.getFileId()))
                .filter(Objects::nonNull)
                .map(FileInfo::from)
                .toList();
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
     * 사용자의 게시글 좋아요 여부 확인
     */
    private boolean isLiked(Board board, String userEmail) {
        if (userEmail == null || board == null) {
            return false;
        }
        try {
            return userRepository.findByEmail(userEmail)
                    .map(user -> boardLikeRepository.existsByBoardIdAndUserId(board.getId(), user.getId()))
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Failed to check like status: boardId={}, userEmail={}", board.getId(), userEmail);
            return false;
        }
    }

    /**
     * Board의 User로부터 Company 정보 조회
     * User가 소유한 Company가 있으면 CompanySummary 반환, 없으면 null 반환
     */
    private GalleryResponse.CompanySummary getCompanySummary(Board board) {
        if (board.getUser() == null) {
            return null;
        }

        try {
            return companyRepository.findByOwnerId(board.getUser().getId())
                    .map(company -> {
                        Double avgRating = companyReviewRepository.getAverageRatingByCompany(company);
                        long reviewCount = companyReviewRepository.countByCompanyAndIsDeletedFalse(company);
                        return GalleryResponse.CompanySummary.builder()
                                .companyUuid(company.getUuid())
                                .companyName(company.getName())
                                .phone(company.getPrimaryPhone())
                                .averageRating(avgRating)
                                .reviewCount((int) reviewCount)
                                .build();
                    })
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get company for board: boardId={}", board.getId());
            return null;
        }
    }

    /**
     * Company의 리뷰 목록 조회 (상세 조회용) [N+1 최적화]
     * 승인된 리뷰만 조회
     */
    private List<GalleryResponse.ReviewSummary> getReviews(Board board) {
        if (board.getUser() == null) {
            return List.of();
        }

        try {
            Company company = companyRepository.findByOwnerId(board.getUser().getId())
                    .orElse(null);
            if (company == null) {
                return List.of();
            }

            List<CompanyReview> reviews = companyReviewRepository.findByCompanyAndStatus(company, "PUBLISHED");
            if (reviews.isEmpty()) {
                return List.of();
            }

            // [N+1 최적화] 사용자 프로필 일괄 조회
            List<Long> userIds = reviews.stream()
                    .filter(r -> r.getUser() != null)
                    .map(r -> r.getUser().getId())
                    .distinct()
                    .toList();
            Map<Long, UserProfile> profileMap = Collections.emptyMap();
            if (!userIds.isEmpty()) {
                profileMap = userProfileRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(up -> up.getUser().getId(), up -> up));
            }

            // [N+1 최적화] 리뷰 이미지 파일 ID 수집 및 일괄 조회
            List<Long> allFileIds = reviews.stream()
                    .flatMap(r -> r.getReviewImages().stream())
                    .filter(img -> !img.getIsDeleted())
                    .map(CompanyReviewImage::getFileId)
                    .distinct()
                    .toList();
            Map<Long, File> fileMap = Collections.emptyMap();
            if (!allFileIds.isEmpty()) {
                fileMap = fileRepository.findByIdIn(allFileIds).stream()
                        .collect(Collectors.toMap(File::getId, f -> f));
            }

            final Map<Long, UserProfile> finalProfileMap = profileMap;
            final Map<Long, File> finalFileMap = fileMap;

            return reviews.stream()
                    .map(review -> {
                        String reviewUserName = "익명";
                        if (review.getUser() != null) {
                            UserProfile profile = finalProfileMap.get(review.getUser().getId());
                            if (profile != null) {
                                reviewUserName = profile.getName();
                            }
                        }

                        // 리뷰 이미지 조회 (Map에서)
                        List<FileInfo> reviewImages = review.getReviewImages().stream()
                                .filter(img -> !img.getIsDeleted())
                                .sorted((a, b) -> a.getDisplayOrder().compareTo(b.getDisplayOrder()))
                                .map(img -> finalFileMap.get(img.getFileId()))
                                .filter(Objects::nonNull)
                                .map(FileInfo::from)
                                .toList();

                        return GalleryResponse.ReviewSummary.builder()
                                .reviewUuid(review.getUuid())
                                .userName(reviewUserName)
                                .rating(review.getRating())
                                .content(review.getContent())
                                .createdAt(review.getCreatedAt())
                                .reply(review.getReply())
                                .repliedAt(review.getRepliedAt())
                                .images(reviewImages)
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to get reviews for board: boardId={}", board.getId());
            return List.of();
        }
    }

    // ==================== N+1 최적화용 헬퍼 메서드 ====================

    /**
     * [N+1 최적화] Map에서 파일 정보 조회
     */
    private List<FileInfo> getFileInfosFromMap(Board board,
            Map<Long, List<BoardAttachment>> attachmentsMap,
            Map<Long, File> filesMap) {
        List<BoardAttachment> attachments = attachmentsMap.getOrDefault(board.getId(), List.of());
        return attachments.stream()
                .map(att -> filesMap.get(att.getFileId()))
                .filter(Objects::nonNull)
                .map(FileInfo::from)
                .toList();
    }

    /**
     * [N+1 최적화] Map에서 사용자 이름 조회
     */
    private String getUserNameFromMap(Board board, Map<Long, UserProfile> profilesMap) {
        if (board.getUser() == null) return null;
        UserProfile profile = profilesMap.get(board.getUser().getId());
        if (profile != null) {
            return profile.getName();
        }
        return board.getUser().getEmail();
    }

    /**
     * [N+1 최적화] Map에서 회사 정보 조회
     */
    private GalleryResponse.CompanySummary getCompanySummaryFromMap(Board board,
            Map<Long, Company> companyMap,
            Map<Long, Double> ratingsMap,
            Map<Long, Long> reviewCountsMap) {
        if (board.getUser() == null) return null;
        Company company = companyMap.get(board.getUser().getId());
        if (company == null) return null;

        return GalleryResponse.CompanySummary.builder()
                .companyUuid(company.getUuid())
                .companyName(company.getName())
                .phone(company.getPrimaryPhone())
                .averageRating(ratingsMap.getOrDefault(company.getId(), 0.0))
                .reviewCount(reviewCountsMap.getOrDefault(company.getId(), 0L).intValue())
                .build();
    }
}
