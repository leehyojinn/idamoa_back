package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.board.model.Board;
import com.hip.damoa.domain.board.model.BoardFilterOption;
import com.hip.damoa.domain.filter.model.FilterOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gallery 게시글 Response DTO
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "갤러리 게시글 응답")
public class GalleryResponse {

    @Schema(description = "게시글 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "제목", example = "모던 카페 인테리어 시공 사례")
    private String title;

    @Schema(description = "내용 (HTML 지원)", example = "<p>고급스러운 카페 인테리어 시공 사례입니다.</p>")
    private String content;

    @Schema(description = "게시판 타입", example = "GALLERY")
    private String boardType;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "카테고리명", example = "카페/음식점")
    private String categoryName;

    // Gallery 특화 데이터
    @Schema(description = "갤러리 이미지 파일 목록")
    private List<FileInfo> images;

    @Schema(description = "관련 링크", example = "https://example.com/portfolio")
    private String relatedLink;

    @Schema(description = "저작권 정보")
    private CopyrightInfo copyright;

    // 통계
    @Schema(description = "조회수", example = "1500")
    private Integer viewCount;

    @Schema(description = "좋아요 수", example = "120")
    private Integer likeCount;

    @Schema(description = "댓글 수", example = "25")
    private Integer commentCount;

    // 상태
    @Schema(description = "상단 고정 여부", example = "false")
    private Boolean isPinned;

    @Schema(description = "추천 여부", example = "true")
    private Boolean isFeatured;

    @Schema(description = "게시 여부", example = "true")
    private Boolean isPublished;

    @Schema(description = "게시일시", example = "2025-01-01T09:00:00")
    private LocalDateTime publishedAt;

    // 필터
    @Schema(description = "적용된 필터 옵션 목록")
    private List<FilterOptionSummary> filterOptions;

    // 태그
    @Schema(description = "태그 배열", example = "[\"인테리어\", \"카페\", \"모던\"]")
    private String[] tags;

    // 작성자
    @Schema(description = "작성자 ID", example = "1")
    private Long userId;

    @Schema(description = "작성자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String userName;

    // 시간
    @Schema(description = "생성일시", example = "2025-01-01T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-01T10:00:00")
    private LocalDateTime updatedAt;

    // 북마크 여부 (로그인 사용자용)
    @Schema(description = "북마크 여부 (로그인 사용자용)", example = "false")
    private Boolean isBookmarked;

    // 좋아요 여부 (로그인 사용자용)
    @Schema(description = "좋아요 여부 (로그인 사용자용)", example = "false")
    private Boolean isLiked;

    // 삭제 상태 (관리자용)
    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

    // 회사 정보
    @Schema(description = "작성자 회사 정보")
    private CompanySummary company;

    // 리뷰 목록 (상세 조회용)
    @Schema(description = "회사 리뷰 목록 (상세 조회 시에만 제공)")
    private List<ReviewSummary> reviews;

    /**
     * 저작권 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "저작권 정보")
    public static class CopyrightInfo {
        @Schema(description = "저작권자", example = "다모아 인테리어")
        private String owner;

        @Schema(description = "라이선스 유형", example = "All Rights Reserved")
        private String license;

        @Schema(description = "출처 표기", example = "선택")
        private String attribution;
    }

    /**
     * 회사 정보 요약 (검색/목록용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "회사 정보 요약")
    public static class CompanySummary {
        @Schema(description = "회사 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID companyUuid;

        @Schema(description = "회사명", example = "다모아 인테리어")
        private String companyName;

        @Schema(description = "전화번호", example = "02-1234-5678")
        private String phone;

        @Schema(description = "리뷰 평균 평점 (1.0~5.0)", example = "4.5")
        private Double averageRating;

        @Schema(description = "리뷰 총 개수", example = "15")
        private Integer reviewCount;
    }

    /**
     * 리뷰 정보 요약 (상세 조회용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리뷰 정보 요약")
    public static class ReviewSummary {
        @Schema(description = "리뷰 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID reviewUuid;

        @Schema(description = "작성자 이름", example = "홍길동")
        private String userName;

        @Schema(description = "평점 (1.0~5.0)", example = "4.5")
        private BigDecimal rating;

        @Schema(description = "리뷰 내용", example = "정말 만족스러운 시공이었습니다.")
        private String content;

        @Schema(description = "작성일시", example = "2025-01-01T08:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "업체 답변", example = "감사합니다. 좋은 평가를 주셔서 감사합니다.")
        private String reply;

        @Schema(description = "업체 답변 작성일시", example = "2025-01-02T10:00:00")
        private LocalDateTime repliedAt;

        @Schema(description = "리뷰 이미지 목록")
        private List<FileInfo> images;
    }

    /**
     * 필터 옵션 요약
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "필터 옵션 요약 정보")
    public static class FilterOptionSummary {
        @Schema(description = "필터 옵션 ID", example = "1")
        private Long id;

        @Schema(description = "필터 옵션 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID uuid;

        @Schema(description = "필터 카테고리 코드", example = "SPACE_TYPE")
        private String categoryCode;

        @Schema(description = "필터 카테고리명", example = "공간 유형")
        private String categoryName;

        @Schema(description = "필터 옵션 코드", example = "CAFE")
        private String code;

        @Schema(description = "필터 옵션명", example = "카페/음식점")
        private String name;

        @Schema(description = "필터 옵션 단축명", example = "카페")
        private String shortName;

        @Schema(description = "색상 코드", example = "#FF5733")
        private String color;

        @Schema(description = "아이콘", example = "cafe-icon")
        private String icon;
    }

    /**
     * 사용자 이메일 안전하게 조회 (삭제된 사용자 처리)
     */
    private static String getUserEmailSafe(Board board) {
        try {
            return board.getUser() != null ? board.getUser().getEmail() : null;
        } catch (Exception e) {
            log.warn("User not found for board: {}", board.getId());
            return "알 수 없음";
        }
    }

    /**
     * 사용자 ID 안전하게 조회 (삭제된 사용자 처리)
     */
    private static Long getUserIdSafe(Board board) {
        try {
            return board.getUser() != null ? board.getUser().getId() : null;
        } catch (Exception e) {
            log.warn("User not found for board: {}", board.getId());
            return null;
        }
    }

    /**
     * Entity to DTO (필터 옵션 없이)
     */
    public static GalleryResponse from(Board board) {
        return from(board, List.of(), List.of(), false, false, getUserEmailSafe(board), null, null);
    }

    /**
     * Entity to DTO (기본 정보 - 하위 호환용)
     * 삭제된 User의 경우 안전하게 처리
     */
    public static GalleryResponse from(Board board, List<BoardFilterOption> filterOptions,
                                       List<FileInfo> images, boolean isBookmarked, String userName) {
        return from(board, filterOptions, images, isBookmarked, false, userName, null, null);
    }

    /**
     * Entity to DTO (전체 정보 포함 - Company + Reviews + isLiked)
     * 삭제된 User의 경우 안전하게 처리
     */
    public static GalleryResponse from(Board board, List<BoardFilterOption> filterOptions,
                                       List<FileInfo> images, boolean isBookmarked, boolean isLiked,
                                       String userName, CompanySummary company, List<ReviewSummary> reviews) {
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

        // User 정보 안전하게 조회
        Long userId = getUserIdSafe(board);
        String userEmail = getUserEmailSafe(board);
        String resolvedUserName = userName != null ? userName : userEmail;

        // 배열 필드 null-safe 처리
        List<FilterOptionSummary> filterOptionSummaries = (filterOptions != null ? filterOptions : List.<BoardFilterOption>of())
                .stream()
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
                .collect(Collectors.toList());

        return GalleryResponse.builder()
                .uuid(board.getUuid())
                .title(board.getTitle())
                .content(board.getContent())
                .boardType(board.getBoardType())
                .categoryId(board.getCategory() != null ? board.getCategory().getId() : null)
                .categoryName(board.getCategory() != null ? board.getCategory().getName() : null)
                .images(images != null ? images : List.of())
                .relatedLink(relatedLink)
                .copyright(copyright)
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .isPinned(board.getIsPinned())
                .isFeatured(board.getIsFeatured())
                .isPublished(board.getIsPublished())
                .publishedAt(board.getPublishedAt())
                .filterOptions(filterOptionSummaries)
                .tags(board.getTags() != null ? board.getTags() : new String[0])
                .userId(userId)
                .userEmail(userEmail)
                .userName(resolvedUserName)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .isBookmarked(isBookmarked)
                .isLiked(isLiked)
                .isDeleted(board.getIsDeleted())
                .company(company)
                .reviews(reviews != null ? reviews : List.of())
                .build();
    }
}
