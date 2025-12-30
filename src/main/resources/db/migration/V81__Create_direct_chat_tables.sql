-- =====================================================
-- V81: 범용 1:1 채팅 테이블 생성
-- =====================================================

-- 1. 1:1 채팅방 테이블
CREATE TABLE direct_chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user1_id BIGINT NOT NULL REFERENCES users(id),
    user2_id BIGINT NOT NULL REFERENCES users(id),
    last_message TEXT,
    last_message_at TIMESTAMP,
    last_sender_id BIGINT REFERENCES users(id),
    user1_active BOOLEAN NOT NULL DEFAULT true,
    user2_active BOOLEAN NOT NULL DEFAULT true,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_different_users CHECK (user1_id <> user2_id),
    CONSTRAINT uq_direct_chat_room_users UNIQUE (user1_id, user2_id)
);

COMMENT ON TABLE direct_chat_rooms IS '1:1 채팅방';
COMMENT ON COLUMN direct_chat_rooms.user1_id IS '참여자1 (항상 user1_id < user2_id 규칙)';
COMMENT ON COLUMN direct_chat_rooms.user2_id IS '참여자2';
COMMENT ON COLUMN direct_chat_rooms.last_message IS '마지막 메시지 내용';
COMMENT ON COLUMN direct_chat_rooms.last_message_at IS '마지막 메시지 시간';
COMMENT ON COLUMN direct_chat_rooms.last_sender_id IS '마지막 메시지 발신자';
COMMENT ON COLUMN direct_chat_rooms.user1_active IS 'user1 활성 상태 (나가기 여부)';
COMMENT ON COLUMN direct_chat_rooms.user2_active IS 'user2 활성 상태 (나가기 여부)';
COMMENT ON COLUMN direct_chat_rooms.metadata IS '추가 메타데이터';

CREATE INDEX idx_direct_chat_rooms_user1_id ON direct_chat_rooms(user1_id) WHERE is_deleted = false;
CREATE INDEX idx_direct_chat_rooms_user2_id ON direct_chat_rooms(user2_id) WHERE is_deleted = false;
CREATE INDEX idx_direct_chat_rooms_last_message_at ON direct_chat_rooms(last_message_at DESC) WHERE is_deleted = false;
CREATE INDEX idx_direct_chat_rooms_uuid ON direct_chat_rooms(uuid);

-- 2. 채팅 메시지 테이블
CREATE TABLE direct_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    room_id BIGINT NOT NULL REFERENCES direct_chat_rooms(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    content TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE direct_chat_messages IS '채팅 메시지';
COMMENT ON COLUMN direct_chat_messages.message_type IS 'TEXT, IMAGE, FILE, SYSTEM';
COMMENT ON COLUMN direct_chat_messages.content IS '메시지 내용';
COMMENT ON COLUMN direct_chat_messages.metadata IS '추가 메타데이터';

CREATE INDEX idx_direct_chat_messages_room_id ON direct_chat_messages(room_id, created_at DESC) WHERE is_deleted = false;
CREATE INDEX idx_direct_chat_messages_sender_id ON direct_chat_messages(sender_id) WHERE is_deleted = false;
CREATE INDEX idx_direct_chat_messages_uuid ON direct_chat_messages(uuid);

-- 3. 채팅 첨부파일 테이블
CREATE TABLE direct_chat_attachments (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    message_id BIGINT NOT NULL REFERENCES direct_chat_messages(id),
    file_id BIGINT NOT NULL REFERENCES files(id),
    original_filename VARCHAR(500) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    thumbnail_url VARCHAR(1000),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE direct_chat_attachments IS '채팅 첨부파일';
COMMENT ON COLUMN direct_chat_attachments.file_id IS '파일 테이블 참조';
COMMENT ON COLUMN direct_chat_attachments.thumbnail_url IS '이미지 썸네일 URL';
COMMENT ON COLUMN direct_chat_attachments.metadata IS '추가 메타데이터';

CREATE INDEX idx_direct_chat_attachments_message_id ON direct_chat_attachments(message_id) WHERE is_deleted = false;
CREATE INDEX idx_direct_chat_attachments_file_id ON direct_chat_attachments(file_id);

-- 4. 읽음 상태 테이블
CREATE TABLE direct_chat_read_states (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    room_id BIGINT NOT NULL REFERENCES direct_chat_rooms(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    last_read_message_id BIGINT REFERENCES direct_chat_messages(id),
    last_read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_direct_chat_read_state UNIQUE (room_id, user_id)
);

COMMENT ON TABLE direct_chat_read_states IS '채팅 읽음 상태';
COMMENT ON COLUMN direct_chat_read_states.last_read_message_id IS '마지막으로 읽은 메시지 ID';
COMMENT ON COLUMN direct_chat_read_states.last_read_at IS '마지막 읽은 시간';

CREATE INDEX idx_direct_chat_read_states_room_user ON direct_chat_read_states(room_id, user_id);

-- 5. 알림 발송 Outbox 테이블
CREATE TABLE notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    recipient_phone VARCHAR(20),
    channel VARCHAR(30) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    template_code VARCHAR(50),
    title VARCHAR(200),
    content TEXT NOT NULL,
    template_data JSONB DEFAULT '{}',
    entity_type VARCHAR(50),
    entity_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    last_error TEXT,
    debounce_key VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notification_outbox IS '알림 발송 Outbox (Transactional Outbox Pattern)';
COMMENT ON COLUMN notification_outbox.channel IS 'KAKAO_ALIMTALK, SMS, EMAIL, PUSH 등';
COMMENT ON COLUMN notification_outbox.notification_type IS 'CHAT_MESSAGE, ESTIMATE_REQUEST 등';
COMMENT ON COLUMN notification_outbox.template_code IS '카카오 알림톡 템플릿 코드';
COMMENT ON COLUMN notification_outbox.template_data IS '템플릿 변수 데이터';
COMMENT ON COLUMN notification_outbox.entity_type IS '관련 엔티티 타입';
COMMENT ON COLUMN notification_outbox.entity_id IS '관련 엔티티 ID';
COMMENT ON COLUMN notification_outbox.status IS 'PENDING, PROCESSING, SENT, FAILED, CANCELLED';
COMMENT ON COLUMN notification_outbox.scheduled_at IS '발송 예정 시간';
COMMENT ON COLUMN notification_outbox.processed_at IS '실제 처리 시간';
COMMENT ON COLUMN notification_outbox.debounce_key IS '중복 발송 방지 키';

CREATE INDEX idx_notification_outbox_status ON notification_outbox(status, scheduled_at) WHERE status = 'PENDING';
CREATE INDEX idx_notification_outbox_debounce ON notification_outbox(debounce_key, status) WHERE debounce_key IS NOT NULL;
CREATE INDEX idx_notification_outbox_recipient ON notification_outbox(recipient_id, status);
