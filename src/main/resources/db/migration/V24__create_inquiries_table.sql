-- 제휴/광고 문의 테이블 생성
CREATE TABLE inquiries (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id BIGINT,
    inquiry_type VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    metadata JSONB,

    CONSTRAINT fk_inquiries_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

-- 인덱스 생성
CREATE INDEX idx_inquiries_user_id ON inquiries (user_id);
CREATE INDEX idx_inquiries_status ON inquiries (status);
CREATE INDEX idx_inquiries_inquiry_type ON inquiries (inquiry_type);
CREATE INDEX idx_inquiries_created_at ON inquiries (created_at DESC);
CREATE INDEX idx_inquiries_is_deleted ON inquiries (is_deleted);

-- 코멘트 추가
COMMENT ON TABLE inquiries IS '제휴/광고 문의';
COMMENT ON COLUMN inquiries.id IS '문의 ID';
COMMENT ON COLUMN inquiries.uuid IS '문의 UUID';
COMMENT ON COLUMN inquiries.user_id IS '작성자 ID (회원인 경우, nullable)';
COMMENT ON COLUMN inquiries.inquiry_type IS '문의 유형 (PARTNERSHIP, ADVERTISEMENT, OTHER)';
COMMENT ON COLUMN inquiries.name IS '작성자명';
COMMENT ON COLUMN inquiries.email IS '이메일주소';
COMMENT ON COLUMN inquiries.phone IS '연락처';
COMMENT ON COLUMN inquiries.content IS '문의내용';
COMMENT ON COLUMN inquiries.status IS '상태 (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)';
COMMENT ON COLUMN inquiries.created_at IS '생성일시';
COMMENT ON COLUMN inquiries.updated_at IS '수정일시';
COMMENT ON COLUMN inquiries.is_deleted IS '삭제 여부';
COMMENT ON COLUMN inquiries.deleted_at IS '삭제일시';
COMMENT ON COLUMN inquiries.metadata IS '확장 데이터 (JSONB)';
