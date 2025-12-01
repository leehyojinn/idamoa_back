-- V51: Extract thumbnails from Document board content
-- content 안의 첫 번째 이미지를 추출하여 files + board_attachments에 썸네일로 등록

-- 임시 테이블 생성: content에서 첫 번째 이미지 URL 추출
CREATE TEMP TABLE temp_document_thumbnails AS
SELECT
    b.id as board_id,
    b.user_id,
    (regexp_match(b.content, 'src="(https://diuqq6anej0c9\.cloudfront\.net[^"]*)"'))[1] as image_url
FROM boards b
WHERE b.board_type = 'DOCUMENT'
  AND b.content LIKE '%<img%'
  AND b.id NOT IN (
    SELECT ba.board_id FROM board_attachments ba WHERE ba.attachment_type = 'THUMBNAIL'
  )
  AND (regexp_match(b.content, 'src="(https://diuqq6anej0c9\.cloudfront\.net[^"]*)"'))[1] IS NOT NULL;

-- files 테이블에 썸네일 이미지 등록
INSERT INTO files (
    original_filename,
    stored_filename,
    file_path,
    file_url,
    file_size,
    mime_type,
    file_extension,
    uploader_id,
    entity_type,
    entity_id,
    is_public,
    download_count,
    created_at,
    updated_at,
    is_deleted
)
SELECT
    -- original_filename: URL에서 파일명 추출
    CASE
        WHEN image_url LIKE '%/%' THEN split_part(image_url, '/', -1)
        ELSE 'thumbnail.jpg'
    END as original_filename,
    -- stored_filename: 동일
    CASE
        WHEN image_url LIKE '%/%' THEN split_part(image_url, '/', -1)
        ELSE 'thumbnail.jpg'
    END as stored_filename,
    -- file_path: CloudFront 도메인 이후 경로
    REPLACE(image_url, 'https://diuqq6anej0c9.cloudfront.net/', '') as file_path,
    -- file_url
    image_url,
    -- file_size: 알 수 없으므로 0
    0 as file_size,
    -- mime_type: 확장자로 추정
    CASE
        WHEN image_url LIKE '%.png' THEN 'image/png'
        WHEN image_url LIKE '%.gif' THEN 'image/gif'
        WHEN image_url LIKE '%.webp' THEN 'image/webp'
        ELSE 'image/jpeg'
    END as mime_type,
    -- file_extension
    CASE
        WHEN image_url LIKE '%.png' THEN 'png'
        WHEN image_url LIKE '%.gif' THEN 'gif'
        WHEN image_url LIKE '%.webp' THEN 'webp'
        ELSE 'jpg'
    END as file_extension,
    -- uploader_id
    t.user_id,
    -- entity_type
    'BOARD_THUMBNAIL' as entity_type,
    -- entity_id
    t.board_id as entity_id,
    -- is_public
    true,
    -- download_count
    0,
    -- timestamps
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    -- is_deleted
    false
FROM temp_document_thumbnails t;

-- board_attachments 테이블에 썸네일 연결
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
    t.board_id,
    f.id as file_id,
    'THUMBNAIL' as attachment_type,
    0 as display_order,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    false
FROM temp_document_thumbnails t
JOIN files f ON f.entity_type = 'BOARD_THUMBNAIL'
            AND f.entity_id = t.board_id
            AND f.file_url = t.image_url;

-- 임시 테이블 삭제
DROP TABLE temp_document_thumbnails;

-- 결과 로깅
DO $$
DECLARE
    new_files_count INTEGER;
    new_attachments_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO new_files_count
    FROM files
    WHERE entity_type = 'BOARD_THUMBNAIL'
      AND created_at > CURRENT_TIMESTAMP - INTERVAL '1 minute';

    SELECT COUNT(*) INTO new_attachments_count
    FROM board_attachments ba
    JOIN boards b ON ba.board_id = b.id
    WHERE b.board_type = 'DOCUMENT'
      AND ba.attachment_type = 'THUMBNAIL';

    RAISE NOTICE 'Migration V51 completed:';
    RAISE NOTICE '- New files created: %', new_files_count;
    RAISE NOTICE '- Document boards with thumbnails: %', new_attachments_count;
END $$;
