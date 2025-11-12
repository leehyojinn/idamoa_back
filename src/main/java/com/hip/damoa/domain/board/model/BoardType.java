package com.hip.damoa.domain.board.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시판 타입 Enum
 *
 * <p>시스템에서 지원하는 게시판 타입:</p>
 * <ul>
 *   <li><b>NOTICE</b> - 공지사항 게시판 (기본 CRUD, 핀 고정)</li>
 *   <li><b>EVENT</b> - 이벤트 게시판 (기본 CRUD, 핀 고정)</li>
 *   <li><b>FAQ</b> - 자주 묻는 질문 (기본 CRUD, 핀 고정)</li>
 *   <li><b>GALLERY</b> - 사진 게시판 (이미지 업로드, 필터 7개, 북마크, 저작권 정보, 관련 링크)</li>
 *   <li><b>DOCUMENT</b> - 자료실 (파일 업로드/다운로드, 필터 3개, 북마크, 유료 파일 지원, 다운로드 통계)</li>
 * </ul>
 *
 * <p>필터 지원: GALLERY (7개), DOCUMENT (3개)</p>
 * <p>북마크 지원: GALLERY, DOCUMENT</p>
 * <p>파일 다운로드: DOCUMENT</p>
 */
@Getter
@RequiredArgsConstructor
public enum BoardType {

    NOTICE("공지사항", "공지사항 게시판"),
    EVENT("이벤트", "이벤트 게시판"),
    FAQ("FAQ", "자주 묻는 질문"),
    GALLERY("사진 게시판", "인테리어 사진 갤러리"),
    DOCUMENT("자료실", "다운로드 자료실");

    private final String displayName;
    private final String description;

    public static BoardType fromString(String type) {
        for (BoardType boardType : BoardType.values()) {
            if (boardType.name().equalsIgnoreCase(type)) {
                return boardType;
            }
        }
        throw new IllegalArgumentException("Unknown board type: " + type);
    }

    public boolean isGallery() {
        return this == GALLERY;
    }

    public boolean isDocument() {
        return this == DOCUMENT;
    }

    public boolean isNotice() {
        return this == NOTICE;
    }

    public boolean isEvent() {
        return this == EVENT;
    }

    public boolean supportsFilters() {
        return this == GALLERY || this == DOCUMENT;
    }

    public boolean supportsBookmarks() {
        return this == GALLERY || this == DOCUMENT;
    }

    public boolean supportsDownload() {
        return this == DOCUMENT;
    }
}
