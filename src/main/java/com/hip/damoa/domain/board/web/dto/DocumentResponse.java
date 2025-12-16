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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Document 게시글 Response DTO
 */
@Slf4j
@Schema(description = "자료실 게시글 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    @Schema(description = "게시글 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "제목", example = "병원 인테리어 설계도면 모음")
    private String title;

    @Schema(description = "내용", example = "치과, 안과, 피부과 인테리어 설계도면입니다.")
    private String content;

    @Schema(description = "게시판 타입", example = "DOCUMENT")
    private String boardType;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "카테고리명", example = "설계도면")
    private String categoryName;

    // Document 특화 데이터
    @Schema(description = """
            첨부 파일 목록 (가격 정보 포함)

            각 파일에 isPaid, price 정보가 포함됩니다:
            - isPaid: 유료 파일 여부
            - price: 파일 가격 (원)
            """)
    private List<FileInfo> files;

    @Schema(description = "썸네일 파일 정보")
    private FileInfo thumbnail;

    @Schema(description = "[레거시] 유료 게시글 여부 (일괄 가격 방식). 개별 가격 방식에서는 files[].isPaid 사용", example = "true")
    private Boolean isPaid;

    @Schema(description = "[레거시] 게시글 기본 가격 (원). 개별 가격 방식에서는 files[].price 사용", example = "5000")
    private Integer price;

    @Schema(description = """
            파일별 가격 맵 (uuid -> price)

            예시:
            {
              "550e8400-e29b-41d4-a716-446655440000": 5000,
              "550e8400-e29b-41d4-a716-446655440001": 3000
            }

            - 맵에 없는 파일은 무료
            - files[].isPaid, files[].price로도 확인 가능
            """, example = "{\"550e8400-e29b-41d4-a716-446655440000\": 5000}")
    private Map<String, Integer> filePrices;

    // 통계
    @Schema(description = "조회수", example = "150")
    private Integer viewCount;

    @Schema(description = "좋아요 수", example = "25")
    private Integer likeCount;

    @Schema(description = "댓글 수", example = "10")
    private Integer commentCount;

    @Schema(description = "다운로드 횟수", example = "45")
    private Long downloadCount;

    // 상태
    @Schema(description = "고정 여부", example = "false")
    private Boolean isPinned;

    @Schema(description = "추천 여부", example = "false")
    private Boolean isFeatured;

    @Schema(description = "게시 여부", example = "true")
    private Boolean isPublished;

    @Schema(description = "게시일시", example = "2024-01-15T10:30:00")
    private LocalDateTime publishedAt;

    // 필터
    @Schema(description = "필터 옵션 목록")
    private List<GalleryResponse.FilterOptionSummary> filterOptions;

    // 태그
    @Schema(description = "태그 배열", example = "[\"인테리어\", \"설계도면\", \"병원\"]")
    private String[] tags;

    // 작성자
    @Schema(description = "작성자 ID", example = "1")
    private Long userId;

    @Schema(description = "작성자 이메일", example = "user@example.com")
    private String userEmail;

    @Schema(description = "작성자 이름", example = "홍길동")
    private String userName;

    // 시간
    @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2024-01-15T10:30:00")
    private LocalDateTime updatedAt;

    // 북마크 여부 (로그인 사용자용)
    @Schema(description = "현재 사용자의 북마크 여부 (로그인 시)", example = "false")
    private Boolean isBookmarked;

    // 다운로드 여부 (로그인 사용자용)
    @Schema(description = "현재 사용자의 다운로드 여부 (로그인 시)", example = "false")
    private Boolean hasDownloaded;

    // 삭제 상태 (관리자용)
    @Schema(description = "삭제 여부 (관리자용)", example = "false")
    private Boolean isDeleted;

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
     * Entity to DTO
     */
    public static DocumentResponse from(Board board) {
        return from(board, List.of(), List.of(), null, false, false, 0L, getUserEmailSafe(board));
    }

    /**
     * Entity to DTO (전체 정보 포함)
     * 삭제된 User의 경우 안전하게 처리
     */
    public static DocumentResponse from(Board board, List<BoardFilterOption> filterOptions,
                                        List<FileInfo> files, FileInfo thumbnail,
                                        boolean isBookmarked, boolean hasDownloaded, Long downloadCount, String userName) {
        Map<String, Object> typeData = board.getTypeData();

        // typeData에서 가격 정보 추출
        Boolean isPaid = (Boolean) typeData.getOrDefault("isPaid", false);
        Integer price = ((Number) typeData.getOrDefault("price", 0)).intValue();

        // 파일별 가격 정보 추출 (개별 가격 방식)
        Map<String, Integer> filePrices = extractFilePrices(typeData, isPaid, price);

        // User 정보 안전하게 조회
        Long userId = getUserIdSafe(board);
        String userEmail = getUserEmailSafe(board);
        String resolvedUserName = userName != null ? userName : userEmail;

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
                .filePrices(filePrices)
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
                .userId(userId)
                .userEmail(userEmail)
                .userName(resolvedUserName)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .isBookmarked(isBookmarked)
                .hasDownloaded(hasDownloaded)
                .isDeleted(board.getIsDeleted())
                .build();
    }

    /**
     * typeData에서 파일별 가격 정보 추출
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> extractFilePrices(Map<String, Object> typeData, Boolean isPaid, Integer defaultPrice) {
        Map<String, Integer> filePrices = new java.util.HashMap<>();

        // 개별 가격 방식 확인
        Boolean individualPricing = (Boolean) typeData.getOrDefault("individualPricing", false);

        if (Boolean.TRUE.equals(individualPricing)) {
            // 개별 가격 방식: filePrices 맵에서 추출
            Object filePricesObj = typeData.get("filePrices");
            if (filePricesObj instanceof Map) {
                Map<String, Object> prices = (Map<String, Object>) filePricesObj;
                for (Map.Entry<String, Object> entry : prices.entrySet()) {
                    if (entry.getValue() instanceof Number) {
                        filePrices.put(entry.getKey(), ((Number) entry.getValue()).intValue());
                    }
                }
            }
        } else if (Boolean.TRUE.equals(isPaid) && defaultPrice != null && defaultPrice > 0) {
            // 일괄 가격 방식: 모든 파일에 동일 가격 적용
            Object filesObj = typeData.get("files");
            if (filesObj instanceof List) {
                List<String> fileUuids = (List<String>) filesObj;
                for (String uuid : fileUuids) {
                    filePrices.put(uuid, defaultPrice);
                }
            }
        }

        return filePrices;
    }
}
