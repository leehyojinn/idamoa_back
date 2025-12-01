package com.hip.damoa.domain.board.web.dto;

import com.hip.damoa.domain.board.model.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Notice/Event 게시글 Response DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeBoardResponse {

    private UUID uuid;
    private String title;
    private String content;
    private String boardType;
    private Long categoryId;
    private String categoryName;

    // 썸네일 이미지
    private FileInfo thumbnail;

    // 첨부파일 목록
    private List<FileInfo> attachments;

    // 이벤트 날짜 (EVENT 타입일 경우)
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
    private String eventStatus;  // 이벤트 상태 (ACTIVE, ENDED)
    private Boolean isEventEnded;  // 이벤트 종료 여부

    // 통계
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;

    // 상태
    private Boolean isPinned;
    private Boolean isFeatured;
    private Boolean isPublished;
    private LocalDateTime publishedAt;

    // 태그
    private String[] tags;

    // 작성자
    private Long userId;
    private String userEmail;
    private String userName;

    // 시간
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 삭제 상태 (관리자용)
    private Boolean isDeleted;

    /**
     * Entity to DTO (기본 - 썸네일, 첨부파일 없음)
     */
    public static NoticeBoardResponse from(Board board) {
        return from(board, null, null, board.getUser() != null ? board.getUser().getEmail() : null);
    }

    /**
     * Entity to DTO (썸네일 포함, 첨부파일 없음)
     */
    public static NoticeBoardResponse from(Board board, FileInfo thumbnail, String userName) {
        return from(board, thumbnail, null, userName);
    }

    /**
     * Entity to DTO (썸네일, 첨부파일 포함)
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
                .userId(board.getUser() != null ? board.getUser().getId() : null)
                .userEmail(board.getUser() != null ? board.getUser().getEmail() : null)
                .userName(userName != null ? userName : (board.getUser() != null ? board.getUser().getEmail() : null))
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .isDeleted(board.getIsDeleted())
                .build();
    }
}
