-- V36: Fix inquiry tables schema issues

-- 1. Fix inquiries table (ensure V34 migration was properly applied)
DO $$
BEGIN
    -- Check if 'name' column still exists (it shouldn't after V34)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'inquiries' AND column_name = 'name'
    ) THEN
        -- Remove old columns if they still exist
        ALTER TABLE inquiries DROP COLUMN IF EXISTS name;
        ALTER TABLE inquiries DROP COLUMN IF EXISTS email;
        ALTER TABLE inquiries DROP COLUMN IF EXISTS phone;
        RAISE NOTICE 'Removed old columns from inquiries table';
    END IF;

    -- Ensure 'title' column exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'inquiries' AND column_name = 'title'
    ) THEN
        ALTER TABLE inquiries ADD COLUMN title VARCHAR(200);
        UPDATE inquiries SET title = SUBSTRING(content FROM 1 FOR 200) WHERE title IS NULL;
        ALTER TABLE inquiries ALTER COLUMN title SET NOT NULL;
        RAISE NOTICE 'Added title column to inquiries table';
    END IF;

    -- Ensure user_id is NOT NULL (general inquiries require authentication)
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'inquiries'
        AND column_name = 'user_id'
        AND is_nullable = 'YES'
    ) THEN
        -- Delete any inquiries without user_id
        DELETE FROM inquiries WHERE user_id IS NULL;
        ALTER TABLE inquiries ALTER COLUMN user_id SET NOT NULL;
        RAISE NOTICE 'Made user_id NOT NULL in inquiries table';
    END IF;
END $$;

-- 2. Ensure partnership_inquiries table has all required columns
DO $$
BEGIN
    -- Check if 'content' column exists (it should based on V32)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'partnership_inquiries' AND column_name = 'content'
    ) THEN
        ALTER TABLE partnership_inquiries ADD COLUMN content TEXT NOT NULL DEFAULT 'No content';
        ALTER TABLE partnership_inquiries ALTER COLUMN content DROP DEFAULT;
        RAISE NOTICE 'Added content column to partnership_inquiries table';
    END IF;
END $$;

-- 3. Update table comments to clarify their purposes
COMMENT ON TABLE inquiries IS '일반 문의 (로그인 필수) - 버그, 결제 오류, 계정 문제 등';
COMMENT ON TABLE partnership_inquiries IS '제휴/광고 문의 (로그인 선택) - 비즈니스 파트너십 문의';

-- 4. Add missing indexes if they don't exist
-- Using standard text search indexes (remove 'korean' configuration which may not exist)
CREATE INDEX IF NOT EXISTS idx_inquiries_title_text ON inquiries(title);
CREATE INDEX IF NOT EXISTS idx_inquiries_content_text ON inquiries(content);
CREATE INDEX IF NOT EXISTS idx_partnership_inquiries_content_text ON partnership_inquiries(content);