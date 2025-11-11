-- ===========================================
-- V4: Update notifications table structure
-- ===========================================

-- 1. Rename send_error to error_message (if exists)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'notifications' AND column_name = 'send_error'
    ) THEN
        ALTER TABLE public.notifications RENAME COLUMN send_error TO error_message;
    END IF;
END $$;

-- 2. Make user_id nullable (for pre-registration notifications like email verification)
ALTER TABLE public.notifications
    ALTER COLUMN user_id DROP NOT NULL;

-- 3. Add missing columns
ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS recipient_id BIGINT,
    ADD COLUMN IF NOT EXISTS recipient_email VARCHAR(100),
    ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    ADD COLUMN IF NOT EXISTS failed_at TIMESTAMP WITHOUT TIME ZONE;

-- 4. Update existing rows status based on is_sent (only if status exists and is PENDING)
UPDATE public.notifications
SET status = CASE
    WHEN is_sent = true THEN 'SENT'
    WHEN error_message IS NOT NULL THEN 'FAILED'
    ELSE 'PENDING'
END
WHERE status = 'PENDING';

-- 5. Add foreign key for recipient_id (if not exists)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'notifications_recipient_id_fkey'
    ) THEN
        ALTER TABLE public.notifications
            ADD CONSTRAINT notifications_recipient_id_fkey
            FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 6. Create indexes
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_email ON notifications(recipient_email);
