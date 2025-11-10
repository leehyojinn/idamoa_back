-- V21: Add missing BaseEntity columns to company_likes table

-- Add BaseEntity columns if they don't exist
ALTER TABLE company_likes ADD COLUMN IF NOT EXISTS uuid UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE company_likes ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE company_likes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE company_likes ADD COLUMN IF NOT EXISTS metadata JSONB;

-- Add unique constraint for uuid if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_company_likes_uuid'
    ) THEN
        ALTER TABLE company_likes ADD CONSTRAINT uk_company_likes_uuid UNIQUE (uuid);
    END IF;
END $$;
