-- V31: Add event_status column to boards table for event status management
-- 이벤트 상태 관리를 위한 event_status 컬럼 추가
-- ACTIVE: 진행 중인 이벤트
-- ENDED: 종료된 이벤트

-- boards 테이블에 event_status 컬럼 추가
ALTER TABLE boards
    ADD COLUMN IF NOT EXISTS event_status VARCHAR(20);

-- 인덱스 추가 (이벤트 상태별 조회를 위함)
CREATE INDEX IF NOT EXISTS idx_boards_event_status
    ON boards(board_type, event_status)
    WHERE board_type = 'EVENT';

-- 기존 이벤트들의 상태 업데이트
-- 종료일이 지난 이벤트는 ENDED로, 그 외는 ACTIVE로 설정
UPDATE boards
SET event_status = CASE
    WHEN type_data->>'eventEndDate' IS NOT NULL
        AND CAST(type_data->>'eventEndDate' AS timestamp) < CURRENT_TIMESTAMP
    THEN 'ENDED'
    ELSE 'ACTIVE'
END
WHERE board_type = 'EVENT'
  AND event_status IS NULL;

-- 코멘트 추가
COMMENT ON COLUMN boards.event_status IS 'EVENT 타입 게시글의 상태 (ACTIVE: 진행중, ENDED: 종료됨)';