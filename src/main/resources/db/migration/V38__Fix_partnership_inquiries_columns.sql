-- V38: Fix partnership_inquiries table to match Entity structure

-- 1. Add missing columns for Entity mapping
ALTER TABLE partnership_inquiries
    ADD COLUMN IF NOT EXISTS partnership_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS name VARCHAR(100);

-- 2. Copy existing data to new columns
UPDATE partnership_inquiries
SET
    partnership_type = CASE
        WHEN inquiry_type = 'PARTNERSHIP' THEN 'PARTNERSHIP'
        WHEN inquiry_type = 'ADVERTISEMENT' THEN 'ADVERTISEMENT'
        ELSE 'OTHER'
    END,
    name = contact_person
WHERE partnership_type IS NULL OR name IS NULL;

-- 3. Rename message to content if needed (content already exists in table)
-- content column already exists, so no need to add

-- 4. Make new columns NOT NULL after data migration
ALTER TABLE partnership_inquiries
    ALTER COLUMN partnership_type SET NOT NULL,
    ALTER COLUMN name SET NOT NULL;

-- 5. Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_partnership_inquiries_partnership_type
    ON partnership_inquiries(partnership_type)
    WHERE is_deleted = false;

-- 6. Add comment on columns
COMMENT ON COLUMN partnership_inquiries.partnership_type IS 'Partnership type (PARTNERSHIP, ADVERTISEMENT, OTHER)';
COMMENT ON COLUMN partnership_inquiries.name IS 'Contact person name (mapped from contact_person for compatibility)';