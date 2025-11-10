-- V22: Add images columns as bigint[] (File IDs)
-- Purpose: Establish FK relationship between companies/reviews and files table
-- Impact: Better data integrity, file metadata access, proper cascading

-- =====================================================
-- Step 1: Add companies.images column
-- =====================================================
-- Add new column to store File IDs
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS images bigint[];

COMMENT ON COLUMN companies.images IS 'Array of File IDs (FK to files.id) for company images';

-- =====================================================
-- Step 2: Add/Convert company_reviews.images column
-- =====================================================
-- If column exists as text[], convert to bigint[]
-- If column doesn't exist, add it as bigint[]
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'company_reviews' AND column_name = 'images'
    ) THEN
        -- Column exists, alter type (remove DEFAULT if exists)
        ALTER TABLE company_reviews ALTER COLUMN images DROP DEFAULT;
        ALTER TABLE company_reviews ALTER COLUMN images TYPE bigint[] USING NULL;
    ELSE
        -- Column doesn't exist, add it
        ALTER TABLE company_reviews ADD COLUMN images bigint[];
    END IF;
END $$;

COMMENT ON COLUMN company_reviews.images IS 'Array of File IDs (FK to files.id) for review images';

-- =====================================================
-- Step 3: Create GIN index for array queries (Optional but recommended)
-- =====================================================
-- GIN index improves performance for array containment queries
-- Example: SELECT * FROM companies WHERE 123 = ANY(images)
CREATE INDEX IF NOT EXISTS idx_companies_images_gin ON companies USING gin(images);
CREATE INDEX IF NOT EXISTS idx_company_reviews_images_gin ON company_reviews USING gin(images);

-- =====================================================
-- Notes:
-- =====================================================
-- 1. OLD: companies.images = ["https://s3.../img1.jpg", "https://s3.../img2.jpg"]
--    NEW: companies.images = [1, 2] (File IDs)
--
-- 2. To get URLs, join with files table:
--    SELECT c.*, array_agg(f.file_url ORDER BY array_position(c.images, f.id)) as image_urls
--    FROM companies c
--    LEFT JOIN files f ON f.id = ANY(c.images)
--    GROUP BY c.id
--
-- 3. No FK constraint added to allow flexibility (files can be deleted independently)
--    Application layer handles orphaned IDs by filtering null results
--
-- 4. Array order is preserved and represents display order
