-- V23: Refactor company_images.image_url to file_id
-- Purpose: Establish FK relationship between company_images and files table
-- Impact: Better data integrity, file metadata access

-- =====================================================
-- Step 1: Delete existing data (개발 환경, 데이터 손실 허용)
-- =====================================================
-- 기존 데이터는 image_url을 file_id로 변환할 수 없으므로 모두 삭제
DELETE FROM company_images;

-- =====================================================
-- Step 2: Add file_id column
-- =====================================================
ALTER TABLE company_images
    ADD COLUMN IF NOT EXISTS file_id BIGINT;

-- =====================================================
-- Step 3: Drop image_url column
-- =====================================================
ALTER TABLE company_images
    DROP COLUMN IF EXISTS image_url;

-- =====================================================
-- Step 4: Make file_id NOT NULL and add index
-- =====================================================
ALTER TABLE company_images
    ALTER COLUMN file_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_company_images_file_id ON company_images(file_id);

-- =====================================================
-- Step 4: Add foreign key constraint (Optional)
-- =====================================================
-- FK 제약조건 추가 (files 삭제 시 company_images도 삭제)
-- ALTER TABLE company_images
--     ADD CONSTRAINT fk_company_images_file_id
--     FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE;

COMMENT ON COLUMN company_images.file_id IS 'FK to files.id for image file reference';

-- =====================================================
-- Notes:
-- =====================================================
-- 1. OLD: company_images.image_url = "https://s3.../img.jpg"
--    NEW: company_images.file_id = 123 (File ID)
--
-- 2. To get URL, join with files table or use service layer conversion
--
-- 3. FK constraint is commented out for flexibility
--    Enable it if you want strict referential integrity
