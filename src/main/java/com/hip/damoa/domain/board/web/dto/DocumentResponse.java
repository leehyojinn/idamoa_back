package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardFilterOption;
import com.hip.damoa.domain.filter.model.FilterOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Document 게시글 Response DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID uuid;
    private String title;
    private String content;
    private String boardType;
    private Long categoryId;
    private String categoryName;

    // Document 특화 데이터
    private List<FileInfo> files;  // 문서 파일 정보
    private FileInfo thumbnail;    // 썸네일 파일 정보
    private Boolean isPaid;
    private Integer price;

    // 통계
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Long downloadCount; // 다운로드 횟수

    // 상태
    private Boolean isPinned;
    private Boolean isFeatured;
    private Boolean isPublished;
    private LocalDateTime publishedAt;

    // 필터
    private List<GalleryResponse.FilterOptionSummary> filterOptions;

    // 태그
    private String[] tags;

    // 작성자
    private Long userId;
    private String userEmail;
    private String userName;

    // 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 북마크 여부 (로그인 사용자용)
    private Boolean isBookmarked;

    // 다운로드 여부 (로그인 사용자용)
    private Boolean hasDownloaded;

    /**
     * Entity to DTO
     */
    public static DocumentResponse from(Board board) {
        return from(board, List.of(), List.of(), null, false, false, 0L, board.getUser() != null ? board.getUser().getEmail() : null);
    }

    /**
     * Entity to DTO (전체 정보 포함)
     */
    public static DocumentResponse from(Board board, List<BoardFilterOption> filterOptions,
                                        List<FileInfo> files, FileInfo thumbnail,
                                        boolean isBookmarked, boolean hasDownloaded, Long downloadCount, String userName) {
        Map<String, Object> typeData = board.getTypeData();

        // typeData에서 가격 정보 추출
        Boolean isPaid = (Boolean) typeData.getOrDefault("isPaid", false);
        Integer price = ((Number) typeData.getOrDefault("price", 0)).intValue();

        return DocumentResponse.builder()
                .uuid(board.getUuid())
                .title(board.getTitle())
                .content(board.getContent())
                .boardType(board.getBoardType())
                .categoryId(board.getCategory() != null ? board.getCategory().getId() : null)
                .categoryName(board.getCategory() != null ? board.getCategory().getName() : null)
                .files(files)      // FileInfo 리스트
                .thumbnail(thumbnail)  // FileInfo
                .isPaid(isPaid)
                .price(price)
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .downloadCount(downloadCount)
                .isPinned(board.getIsPinned())
                .isFeatured(board.getIsFeatured())
                .isPublished(board.getIsPublished())
                .publishedAt(board.getPublishedAt())
                .filterOptions(filterOptions.stream()
                        .map(bfo -> {
                            FilterOption fo = bfo.getFilterOption();
                            return GalleryResponse.FilterOptionSummary.builder()
                                    .id(fo.getId())
                                    .uuid(fo.getUuid())
                                    .categoryCode(fo.getCategory().getCode())
                                    .categoryName(fo.getCategory().getName())
                                    .code(fo.getCode())
                                    .name(fo.getName())
                                    .shortName(fo.getShortName())
                                    .color(fo.getColor())
                                    .icon(fo.getIcon())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .tags(board.getTags())
                .userId(board.getUser().getId())
                .userEmail(board.getUser().getEmail())
                .userName(userName != null ? userName : board.getUser().getEmail())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .isBookmarked(isBookmarked)
                .hasDownloaded(hasDownloaded)
                .build();
    }
}
