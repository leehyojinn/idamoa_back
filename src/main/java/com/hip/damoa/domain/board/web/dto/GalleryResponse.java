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
 * Gallery 게시글 Response DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryResponse {

    private UUID uuid;
    private String title;
    private String content;
    private String boardType;
    private Long categoryId;
    private String categoryName;

    // Gallery 특화 데이터
    private List<FileInfo> images;  // 이미지 파일 정보
    private String relatedLink;
    private CopyrightInfo copyright;

    // 통계
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;

    // 상태
    private Boolean isPinned;
    private Boolean isFeatured;
    private Boolean isPublished;
    private LocalDateTime publishedAt;

    // 필터
    private List<FilterOptionSummary> filterOptions;

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

    // 삭제 상태 (관리자용)
    private Boolean isDeleted;

    /**
     * 저작권 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CopyrightInfo {
        private String owner;
        private String license;
        private String attribution;
    }

    /**
     * 필터 옵션 요약
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterOptionSummary {
        private Long id;
        private UUID uuid;
        private String categoryCode;
        private String categoryName;
        private String code;
        private String name;
        private String shortName;
        private String color;
        private String icon;
    }

    /**
     * Entity to DTO (필터 옵션 없이)
     */
    public static GalleryResponse from(Board board) {
        return from(board, List.of(), List.of(), false, board.getUser() != null ? board.getUser().getEmail() : null);
    }

    /**
     * Entity to DTO (전체 정보 포함)
     */
    public static GalleryResponse from(Board board, List<BoardFilterOption> filterOptions,
                                       List<FileInfo> images, boolean isBookmarked, String userName) {
        Map<String, Object> typeData = board.getTypeData();

        // typeData에서 링크 정보 추출 (relatedLink 또는 link 필드 확인)
        String relatedLink = "";
        Object relatedLinkObj = typeData.get("relatedLink");
        if (relatedLinkObj instanceof String && !((String) relatedLinkObj).isEmpty()) {
            relatedLink = (String) relatedLinkObj;
        } else {
            Object linkObj = typeData.get("link");
            if (linkObj instanceof String) {
                relatedLink = (String) linkObj;
            }
        }

        // copyright 필드 안전 처리 (Map 또는 String일 수 있음)
        CopyrightInfo copyright;
        Object copyrightObj = typeData.get("copyright");
        if (copyrightObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> copyrightMap = (Map<String, String>) copyrightObj;
            copyright = CopyrightInfo.builder()
                    .owner(copyrightMap.getOrDefault("owner", ""))
                    .license(copyrightMap.getOrDefault("license", "All Rights Reserved"))
                    .attribution(copyrightMap.getOrDefault("attribution", "선택"))
                    .build();
        } else if (copyrightObj instanceof String && !((String) copyrightObj).isEmpty()) {
            // copyright가 문자열인 경우 owner로 사용
            copyright = CopyrightInfo.builder()
                    .owner((String) copyrightObj)
                    .license("All Rights Reserved")
                    .attribution("선택")
                    .build();
        } else {
            copyright = CopyrightInfo.builder()
                    .owner("")
                    .license("All Rights Reserved")
                    .attribution("선택")
                    .build();
        }

        return GalleryResponse.builder()
                .uuid(board.getUuid())
                .title(board.getTitle())
                .content(board.getContent())
                .boardType(board.getBoardType())
                .categoryId(board.getCategory() != null ? board.getCategory().getId() : null)
                .categoryName(board.getCategory() != null ? board.getCategory().getName() : null)
                .images(images)  // FileInfo 리스트
                .relatedLink(relatedLink)
                .copyright(copyright)
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .isPinned(board.getIsPinned())
                .isFeatured(board.getIsFeatured())
                .isPublished(board.getIsPublished())
                .publishedAt(board.getPublishedAt())
                .filterOptions(filterOptions.stream()
                        .map(bfo -> {
                            FilterOption fo = bfo.getFilterOption();
                            return FilterOptionSummary.builder()
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
                .userId(board.getUser() != null ? board.getUser().getId() : null)
                .userEmail(board.getUser() != null ? board.getUser().getEmail() : null)
                .userName(userName != null ? userName : (board.getUser() != null ? board.getUser().getEmail() : null))
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .isBookmarked(isBookmarked)
                .isDeleted(board.getIsDeleted())
                .build();
    }
}
