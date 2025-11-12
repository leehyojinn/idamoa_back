-- V30: Add CHECK constraints for NotificationChannel enum values
-- Ensures only valid channel values (EMAIL, SMS, KAKAO, FCM) are stored in the database

-- Add CHECK constraint to notifications table
ALTER TABLE notifications
    ADD CONSTRAINT check_notifications_channel
    CHECK (channel IN ('EMAIL', 'SMS', 'KAKAO', 'FCM'));

-- Add CHECK constraint to notification_logs table
ALTER TABLE notification_logs
    ADD CONSTRAINT check_notification_logs_channel
    CHECK (channel IN ('EMAIL', 'SMS', 'KAKAO', 'FCM'));

-- Add CHECK constraint to notification_templates table
ALTER TABLE notification_templates
    ADD CONSTRAINT check_notification_templates_channel
    CHECK (channel IN ('EMAIL', 'SMS', 'KAKAO', 'FCM'));
