-- ===========================================
-- V5: Fix email_verifications and notification_logs tables
-- ===========================================

-- 1. email_verifications: Add BaseEntity fields
ALTER TABLE public.email_verifications
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS verification_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING' NOT NULL;

-- Migrate is_verified to status for existing rows
UPDATE public.email_verifications
SET status = CASE
    WHEN is_verified = true THEN 'VERIFIED'
    ELSE 'PENDING'
END
WHERE status = 'PENDING';

-- Drop is_verified column (replaced by status)
ALTER TABLE public.email_verifications
    DROP COLUMN IF EXISTS is_verified;

-- 2. notification_logs: Add request_data
ALTER TABLE public.notification_logs
    ADD COLUMN IF NOT EXISTS request_data JSONB DEFAULT '{}'::jsonb;

-- 3. Create indexes
CREATE INDEX IF NOT EXISTS idx_email_verifications_email ON email_verifications(email);
CREATE INDEX IF NOT EXISTS idx_email_verifications_token ON email_verifications(verification_token);
CREATE INDEX IF NOT EXISTS idx_email_verifications_status ON email_verifications(status);
CREATE INDEX IF NOT EXISTS idx_email_verifications_is_deleted ON email_verifications(is_deleted) WHERE is_deleted = false;
