-- V10: Create chat tables for real-time messaging between users and companies

-- =====================================================
-- chat_rooms: 채팅방 (견적 요청자와 업체 간 1:1 채팅)
-- =====================================================
CREATE TABLE chat_rooms (
    id                      BIGSERIAL PRIMARY KEY,
    uuid                    UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),

    -- 관계 (견적 요청, 사용자, 업체)
    estimate_request_id     BIGINT NOT NULL REFERENCES estimate_requests(id) ON DELETE CASCADE,
    user_id                 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id              BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- 마지막 메시지 정보
    last_message_at         TIMESTAMP,
    last_message            TEXT,

    -- 미읽음 개수
    unread_count_user       INTEGER NOT NULL DEFAULT 0,
    unread_count_company    INTEGER NOT NULL DEFAULT 0,

    -- Soft delete
    is_deleted              BOOLEAN NOT NULL DEFAULT false,
    deleted_at              TIMESTAMP,

    -- 메타데이터 (확장 가능)
    metadata                JSONB,

    -- 타임스탬프
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 제약 조건: 한 견적 요청당 한 업체와의 채팅방은 하나만 존재
    CONSTRAINT uq_chat_room_estimate_company UNIQUE (estimate_request_id, company_id)
);

-- =====================================================
-- chat_messages: 채팅 메시지
-- =====================================================
CREATE TABLE chat_messages (
    id                  BIGSERIAL PRIMARY KEY,
    uuid                UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),

    -- 관계 (채팅방)
    chat_room_id        BIGINT NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,

    -- 발신자 정보
    sender_type         VARCHAR(20) NOT NULL CHECK (sender_type IN ('USER', 'COMPANY')),
    sender_id           BIGINT NOT NULL, -- USER ID or COMPANY ID

    -- 메시지 내용
    message             TEXT NOT NULL,

    -- 읽음 상태
    is_read             BOOLEAN NOT NULL DEFAULT false,
    read_at             TIMESTAMP,

    -- Soft delete
    is_deleted          BOOLEAN NOT NULL DEFAULT false,
    deleted_at          TIMESTAMP,

    -- 메타데이터 (확장 가능)
    metadata            JSONB,

    -- 타임스탬프
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- Indexes for performance
-- =====================================================

-- chat_rooms indexes
CREATE INDEX idx_chat_rooms_estimate_request_id ON chat_rooms(estimate_request_id);
CREATE INDEX idx_chat_rooms_user_id ON chat_rooms(user_id);
CREATE INDEX idx_chat_rooms_company_id ON chat_rooms(company_id);
CREATE INDEX idx_chat_rooms_last_message_at ON chat_rooms(last_message_at DESC);
CREATE INDEX idx_chat_rooms_uuid ON chat_rooms(uuid);
CREATE INDEX idx_chat_rooms_is_deleted ON chat_rooms(is_deleted);

-- chat_messages indexes
CREATE INDEX idx_chat_messages_chat_room_id ON chat_messages(chat_room_id);
CREATE INDEX idx_chat_messages_sender_type ON chat_messages(sender_type);
CREATE INDEX idx_chat_messages_is_read ON chat_messages(is_read);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX idx_chat_messages_uuid ON chat_messages(uuid);
CREATE INDEX idx_chat_messages_is_deleted ON chat_messages(is_deleted);

-- Composite index for efficient unread message queries
CREATE INDEX idx_chat_messages_room_read ON chat_messages(chat_room_id, is_read, is_deleted);

-- =====================================================
-- Comments for documentation
-- =====================================================

COMMENT ON TABLE chat_rooms IS '채팅방 - 견적 요청자와 업체 간 1:1 채팅';
COMMENT ON COLUMN chat_rooms.estimate_request_id IS '견적 요청 ID';
COMMENT ON COLUMN chat_rooms.user_id IS '사용자 ID (견적 요청 작성자)';
COMMENT ON COLUMN chat_rooms.company_id IS '업체 ID';
COMMENT ON COLUMN chat_rooms.last_message_at IS '마지막 메시지 시각';
COMMENT ON COLUMN chat_rooms.last_message IS '마지막 메시지 내용 (미리보기용)';
COMMENT ON COLUMN chat_rooms.unread_count_user IS '사용자 미읽음 개수';
COMMENT ON COLUMN chat_rooms.unread_count_company IS '업체 미읽음 개수';

COMMENT ON TABLE chat_messages IS '채팅 메시지';
COMMENT ON COLUMN chat_messages.chat_room_id IS '채팅방 ID';
COMMENT ON COLUMN chat_messages.sender_type IS '발신자 타입 (USER or COMPANY)';
COMMENT ON COLUMN chat_messages.sender_id IS '발신자 ID (User ID 또는 Company ID)';
COMMENT ON COLUMN chat_messages.message IS '메시지 내용';
COMMENT ON COLUMN chat_messages.is_read IS '읽음 여부';
COMMENT ON COLUMN chat_messages.read_at IS '읽은 시각';
