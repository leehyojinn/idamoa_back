-- V49: Fix Document content URLs and attachment types
-- 1. content의 이전 CloudFront URL을 새 CloudFront URL로 변경
-- 2. attachment_type 수정 (PDF → DOCUMENT, JPG/PNG → IMAGE)

-- 1. boards 테이블의 content에서 이전 CloudFront URL 변경
UPDATE boards
SET content = REPLACE(
    content,
    'https://d3jwbzkv7b53n2.cloudfront.net',
    'https://diuqq6anej0c9.cloudfront.net'
),
    updated_at = CURRENT_TIMESTAMP
WHERE content LIKE '%d3jwbzkv7b53n2.cloudfront.net%';

-- 2. board_attachments의 attachment_type 수정
-- PDF 파일이 IMAGE로 분류된 것을 DOCUMENT로 변경
UPDATE board_attachments ba
SET attachment_type = 'DOCUMENT',
    updated_at = CURRENT_TIMESTAMP
FROM files f
WHERE ba.file_id = f.id
  AND ba.attachment_type = 'IMAGE'
  AND f.file_extension = 'pdf';

-- 이미지 파일이 DOCUMENT로 분류된 것을 IMAGE로 변경
UPDATE board_attachments ba
SET attachment_type = 'IMAGE',
    updated_at = CURRENT_TIMESTAMP
FROM files f
WHERE ba.file_id = f.id
  AND ba.attachment_type = 'DOCUMENT'
  AND f.file_extension IN ('jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp');

-- 3. 마이그레이션 결과 로깅
DO $$
DECLARE
    content_updated_count INTEGER;
    pdf_to_document_count INTEGER;
    image_to_image_count INTEGER;
BEGIN
    -- content URL 변경 결과
    SELECT COUNT(*) INTO content_updated_count
    FROM boards
    WHERE content LIKE '%diuqq6anej0c9.cloudfront.net%';

    -- attachment_type 변경 결과
    SELECT COUNT(*) INTO pdf_to_document_count
    FROM board_attachments ba
    JOIN files f ON ba.file_id = f.id
    WHERE ba.attachment_type = 'DOCUMENT'
      AND f.file_extension = 'pdf';

    SELECT COUNT(*) INTO image_to_image_count
    FROM board_attachments ba
    JOIN files f ON ba.file_id = f.id
    WHERE ba.attachment_type = 'IMAGE'
      AND f.file_extension IN ('jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp');

    RAISE NOTICE 'Migration V49 completed:';
    RAISE NOTICE '- Boards with new CloudFront URL in content: %', content_updated_count;
    RAISE NOTICE '- PDF files with DOCUMENT type: %', pdf_to_document_count;
    RAISE NOTICE '- Image files with IMAGE type: %', image_to_image_count;
END $$;
