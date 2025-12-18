package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.board.model.Board;
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

/**
 * Notice/Event 게시글 Response DTO
 */
@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항/이벤트 게시글 응답")
public class NoticeBoardResponse {

    @Schema(description = "게시글 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID uuid;

    @Schema(description = "제목", example = "2025년 신년 이벤트 안내")
    private String title;

    @Schema(description = "내용 (HTML 지원)", example = "<p>2025년 신년 이벤트에 참여하세요!</p>")
    private String content;

    @Schema(description = "게시판 타입 (NOTICE, EVENT, FAQ)", example = "EVENT")
    private String boardType;

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "카테고리명", example = "일반공지")
    private String categoryName;

    // 썸네일 이미지
    @Schema(description = "썸네일 이미지 정보")
    private FileInfo thumbnail;

    // 첨부파일 목록
    @Schema(description = "첨부파일 목록")
    @Builder.Default
    private List<FileInfo> attachments = List.of();

    // 이벤트 날짜 (EVENT 타입일 경우)
    @Schema(description = "이벤트 시작일시 (EVENT 타입)", example = "2025-01-01T00:00:00")
    private LocalDateTime eventStartDate;

    @Schema(description = "이벤트 종료일시 (EVENT 타입)", example = "2025-01-31T23:59:59")
    private LocalDateTime eventEndDate;

    @Schema(description = "이벤트 상태 (ACTIVE, ENDED)", example = "ACTIVE")
    private String eventStatus;

    @Schema(description = "이벤트 종료 여부", example = "false")
    private Boolean isEventEnded;

    // 통계
    @Schema(description = "조회수", example = "1500")
    private Integer viewCount;

    @Schema(description = "좋아요 수", example = "120")
    private Integer likeCount;

    @Schema(description = "댓글 수", example = "25")
    private Integer commentCount;

    // 상태
    @Schema(description = "고정 여부 (상단 고정)", example = "true")
    private Boolean isPinned;

    @Schema(description = "추천 여부", example = "false")
    private Boolean isFeatured;

    @Schema(description = "게시 여부", example = "true")
    private Boolean isPublished;

    @Schema(description = "게시일시", example = "2025-01-01T09:00:00")
    private LocalDateTime publishedAt;

    // 태그
    @Schema(description = "태그 배열", example = "[\"이벤트\", \"할인\", \"신년\"]")
    @Builder.Default
    private String[] tags = new String[0];

    // 작성자
    @Schema(description = "작성자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userUuid;

    @Schema(description = "작성자 이메일", example = "admin@example.com")
    private String userEmail;

    @Schema(description = "작성자 이름", example = "관리자")
    private String userName;

    // 시간
    @Schema(description = "생성일시", example = "2025-01-01T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-01T10:00:00")
    private LocalDateTime updatedAt;

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
     * 사용자 UUID 안전하게 조회 (삭제된 사용자 처리)
     */
    private static UUID getUserUuidSafe(Board board) {
        try {
            return board.getUser() != null ? board.getUser().getUuid() : null;
        } catch (Exception e) {
            log.warn("User not found for board: {}", board.getId());
            return null;
        }
    }

    /**
     * Entity to DTO (기본 - 썸네일, 첨부파일 없음)
     */
    public static NoticeBoardResponse from(Board board) {
        return from(board, null, null, getUserEmailSafe(board));
    }

    /**
     * Entity to DTO (썸네일 포함, 첨부파일 없음)
     */
    public static NoticeBoardResponse from(Board board, FileInfo thumbnail, String userName) {
        return from(board, thumbnail, null, userName);
    }

    /**
     * Entity to DTO (썸네일, 첨부파일 포함)
     * 삭제된 User의 경우 안전하게 처리
     */
    public static NoticeBoardResponse from(Board board, FileInfo thumbnail, List<FileInfo> attachments, String userName) {
        // typeData에서 이벤트 날짜 추출
        Map<String, Object> typeData = board.getTypeData();
        LocalDateTime eventStartDate = null;
        LocalDateTime eventEndDate = null;
        Boolean isEventEnded = null;

        if (typeData != null && "EVENT".equals(board.getBoardType())) {
            if (typeData.get("eventStartDate") != null) {
                eventStartDate = LocalDateTime.parse(typeData.get("eventStartDate").toString());
            }
            if (typeData.get("eventEndDate") != null) {
                eventEndDate = LocalDateTime.parse(typeData.get("eventEndDate").toString());
                // 현재 시각과 비교하여 종료 여부 판단
                isEventEnded = eventEndDate.isBefore(LocalDateTime.now());
            }
        }

        // User 정보 안전하게 조회
        UUID userUuid = getUserUuidSafe(board);
        String userEmail = getUserEmailSafe(board);
        String resolvedUserName = userName != null ? userName : userEmail;

        return NoticeBoardResponse.builder()
                .uuid(board.getUuid())
                .title(board.getTitle())
                .content(board.getContent())
                .boardType(board.getBoardType())
                .categoryId(board.getCategory() != null ? board.getCategory().getId() : null)
                .categoryName(board.getCategory() != null ? board.getCategory().getName() : null)
                .thumbnail(thumbnail)
                .attachments(attachments)
                .eventStartDate(eventStartDate)
                .eventEndDate(eventEndDate)
                .eventStatus(board.getEventStatus())
                .isEventEnded(isEventEnded)
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .isPinned(board.getIsPinned())
                .isFeatured(board.getIsFeatured())
                .isPublished(board.getIsPublished())
                .publishedAt(board.getPublishedAt())
                .tags(board.getTags())
                .userUuid(userUuid)
                .userEmail(userEmail)
                .userName(resolvedUserName)
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .isDeleted(board.getIsDeleted())
                .build();
    }
}
