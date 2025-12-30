-- V82: 시스템 메시지를 위해 sender_id NULL 허용
-- 시스템 메시지 (입장/퇴장 알림 등)는 발신자가 없을 수 있음

ALTER TABLE direct_chat_messages
    ALTER COLUMN sender_id DROP NOT NULL;

-- 코멘트 추가
COMMENT ON COLUMN direct_chat_messages.sender_id IS '발신자 ID (시스템 메시지인 경우 NULL)';
