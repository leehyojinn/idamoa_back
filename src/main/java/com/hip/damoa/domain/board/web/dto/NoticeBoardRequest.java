package com.hip.damoa.domain.board.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Notice/Event 게시글 생성/수정 Request DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeBoardRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 10000, message = "내용은 10000자 이내여야 합니다")
    private String content;

    // NOTICE/EVENT는 카테고리를 사용하지 않음 (무시됨)
    @Deprecated
    private Long categoryId;

    private String[] tags;

    private Boolean isPublished;

    private Boolean isPinned; // 상단 고정 여부

    // 썸네일 이미지
    private UUID thumbnailUuid;

    // 첨부파일 목록 (여러 파일 첨부 가능)
    private List<UUID> attachmentUuids;

    // 이벤트 날짜 (EVENT 타입일 경우)
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
}
