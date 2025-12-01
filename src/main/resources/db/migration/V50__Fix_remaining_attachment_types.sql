-- V50: Fix remaining attachment types by board type
-- V49에서 파일 확장자 기준으로 처리했던 것을 게시판 타입 기준으로 재처리

-- 1. 자료실(DOCUMENT) 게시판: 남아있는 IMAGE 타입 → DOCUMENT 타입으로 변경
UPDATE board_attachments ba
SET attachment_type = 'DOCUMENT',
    updated_at = CURRENT_TIMESTAMP
FROM boards b
WHERE ba.board_id = b.id
  AND b.board_type = 'DOCUMENT'
  AND ba.attachment_type = 'IMAGE';

-- 2. 사진(GALLERY) 게시판: 남아있는 DOCUMENT 타입 → IMAGE 타입으로 변경
UPDATE board_attachments ba
SET attachment_type = 'IMAGE',
    updated_at = CURRENT_TIMESTAMP
FROM boards b
WHERE ba.board_id = b.id
  AND b.board_type = 'GALLERY'
  AND ba.attachment_type = 'DOCUMENT';

-- 3. 마이그레이션 결과 로깅
DO $$
DECLARE
    document_board_attachments INTEGER;
    gallery_board_attachments INTEGER;
BEGIN
    -- 자료실 게시판의 DOCUMENT 타입 첨부파일 수
    SELECT COUNT(*) INTO document_board_attachments
    FROM board_attachments ba
    JOIN boards b ON ba.board_id = b.id
    WHERE b.board_type = 'DOCUMENT'
      AND ba.attachment_type = 'DOCUMENT';

    -- 사진 게시판의 IMAGE 타입 첨부파일 수
    SELECT COUNT(*) INTO gallery_board_attachments
    FROM board_attachments ba
    JOIN boards b ON ba.board_id = b.id
    WHERE b.board_type = 'GALLERY'
      AND ba.attachment_type = 'IMAGE';

    RAISE NOTICE 'Migration V50 completed:';
    RAISE NOTICE '- Document board attachments with DOCUMENT type: %', document_board_attachments;
    RAISE NOTICE '- Gallery board attachments with IMAGE type: %', gallery_board_attachments;
END $$;
