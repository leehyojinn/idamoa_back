-- ===========================================
-- V3: Add missing BaseEntity fields
-- ===========================================

-- 1. notification_logs: Add uuid and updated_at
ALTER TABLE public.notification_logs
    ADD COLUMN IF NOT EXISTS uuid UUID DEFAULT gen_random_uuid() NOT NULL,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

ALTER TABLE public.notification_logs
    ADD CONSTRAINT notification_logs_uuid_key UNIQUE (uuid);

-- 2. notifications: Add soft delete fields
ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

-- 3. Create indexes
CREATE INDEX IF NOT EXISTS idx_notification_logs_uuid ON notification_logs(uuid);
CREATE INDEX IF NOT EXISTS idx_notifications_is_deleted ON notifications(is_deleted) WHERE is_deleted = false;
