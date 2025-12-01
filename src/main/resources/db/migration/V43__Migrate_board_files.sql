--
-- V43: Board File 덤프 데이터 마이그레이션
-- 이전 시스템(damoa_board_file)에서 현재 시스템(files + board_attachments)으로 데이터 이전
--

-- ============================================
-- 1. 임시 테이블 생성 (덤프 데이터 저장용)
-- ============================================

CREATE TEMPORARY TABLE temp_damoa_board_file (
    seq INTEGER,
    uuid UUID,
    original_name TEXT,
    stored_name TEXT,
    file_path TEXT,
    file_size BIGINT,
    mime_type TEXT,
    download_count INTEGER,
    description TEXT,
    sort_order INTEGER,
    create_datetime TIMESTAMPTZ,
    update_datetime TIMESTAMPTZ,
    board_uuid UUID,
    file_subpath TEXT,
    delete_datetime TIMESTAMPTZ
);

-- ============================================
-- 2. 덤프 데이터 INSERT (board_dump_file.sql에서 복사)
-- ============================================

-- TODO: 실제 덤프 파일의 모든 INSERT 문을 여기에 추가해야 합니다.
-- 예시 데이터:
INSERT INTO temp_damoa_board_file (seq, uuid, original_name, stored_name, file_path, file_size, mime_type, download_count, description, sort_order, create_datetime, update_datetime, board_uuid, file_subpath, delete_datetime) VALUES
(12, 'd3e0321a-d554-4e01-9ad1-2ad88cfc9af9', 'q0rpZO15ntvA06mcNmt5L6RkUdO7w5pnKzISO6fX.jpg', '099f3d33-6493-41f8-9df9-c9046ec3945c-lg.jpg', 'images/250910/099f3d33-6493-41f8-9df9-c9046ec3945c-lg.jpg', 129076, 'image/jpeg', 0, NULL, 0, '2025-09-10 06:19:46.607947+00', '2025-09-10 06:19:46.607947+00', NULL, 'images/250910/099f3d33-6493-41f8-9df9-c9046ec3945c-pre.jpg', NULL);

-- ============================================
-- 3. files 테이블에 파일 메타데이터 삽입
-- ============================================

INSERT INTO files (
    uuid,
    original_filename,
    stored_filename,
    file_path,
    file_url,
    file_size,
    mime_type,
    file_extension,
    entity_type,
    entity_id,
    description,
    display_order,
    is_public,
    created_at,
    updated_at,
    is_deleted,
    deleted_at
)
SELECT
    t.uuid,
    t.original_name,
    t.stored_name,
    -- file_path: public/ prefix 제거
    CASE WHEN t.file_path LIKE 'public/%' THEN SUBSTRING(t.file_path FROM 8) ELSE t.file_path END,
    -- file_url: S3 URL 생성 (public/ prefix 제거)
    'https://hip-damoa-uploads-local.s3.ap-northeast-2.amazonaws.com/' ||
    CASE WHEN t.file_path LIKE 'public/%' THEN SUBSTRING(t.file_path FROM 8) ELSE t.file_path END,
    t.file_size,
    t.mime_type,
    -- 확장자 추출
    CASE
        WHEN t.original_name LIKE '%.%'
            THEN LOWER(SUBSTRING(t.original_name FROM '\.([^.]+)$'))
        ELSE ''
    END,
    CASE
        WHEN t.board_uuid IS NOT NULL THEN 'BOARD'
        ELSE 'TEMP'
    END,
    NULL,  -- entity_id는 board_attachments에서 처리
    t.description,
    t.sort_order,
    true,  -- is_public
    t.create_datetime AT TIME ZONE 'UTC',
    t.update_datetime AT TIME ZONE 'UTC',
    CASE WHEN t.delete_datetime IS NOT NULL THEN true ELSE false END,
    t.delete_datetime AT TIME ZONE 'UTC'
FROM temp_damoa_board_file t
WHERE NOT EXISTS (
    SELECT 1 FROM files f WHERE f.uuid = t.uuid
);

-- ============================================
-- 4. board_attachments 테이블에 board-file 연결 삽입
-- (board_uuid가 있는 파일만)
-- ============================================

INSERT INTO board_attachments (
    board_id,
    file_id,
    attachment_type,
    display_order,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    b.id,
    f.id,
    'IMAGE',  -- 대부분 이미지
    t.sort_order,
    t.create_datetime AT TIME ZONE 'UTC',
    t.update_datetime AT TIME ZONE 'UTC',
    CASE WHEN t.delete_datetime IS NOT NULL THEN true ELSE false END
FROM temp_damoa_board_file t
JOIN boards b ON b.uuid = t.board_uuid
JOIN files f ON f.uuid = t.uuid
WHERE t.board_uuid IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM board_attachments ba
    WHERE ba.board_id = b.id AND ba.file_id = f.id
);

-- ============================================
-- 5. 시퀀스 업데이트
-- ============================================

SELECT setval('files_id_seq', COALESCE((SELECT MAX(id) FROM files), 1), true);
SELECT setval('board_attachments_id_seq', COALESCE((SELECT MAX(id) FROM board_attachments), 1), true);

-- ============================================
-- 6. 통계 출력
-- ============================================

DO $$
DECLARE
    v_files_imported INTEGER;
    v_attachments_imported INTEGER;
    v_total INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM temp_damoa_board_file;
    SELECT COUNT(*) INTO v_files_imported FROM files WHERE uuid IN (SELECT uuid FROM temp_damoa_board_file);
    SELECT COUNT(*) INTO v_attachments_imported FROM board_attachments ba
        JOIN files f ON f.id = ba.file_id
        WHERE f.uuid IN (SELECT uuid FROM temp_damoa_board_file);

    RAISE NOTICE 'Board File Migration Complete:';
    RAISE NOTICE '  - Files: % / % imported', v_files_imported, v_total;
    RAISE NOTICE '  - Attachments: % linked', v_attachments_imported;
END $$;

-- 임시 테이블 정리
DROP TABLE IF EXISTS temp_damoa_board_file;
