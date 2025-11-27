-- V39: Recreate partnership_inquiries table to match Entity structure
-- Drop and recreate since there's no data

DROP TABLE IF EXISTS partnership_inquiries CASCADE;

-- Create table matching PartnershipInquiry Entity
CREATE TABLE partnership_inquiries (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    user_id BIGINT,  -- nullable (비회원 가능)
    partnership_type VARCHAR(20) NOT NULL,  -- PARTNERSHIP, ADVERTISEMENT, OTHER
    name VARCHAR(100) NOT NULL,  -- 작성자명
    email VARCHAR(100) NOT NULL,  -- 이메일
    phone VARCHAR(20) NOT NULL,  -- 연락처
    content TEXT NOT NULL,  -- 문의 내용
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP,
    metadata JSONB,

    CONSTRAINT fk_partnership_inquiries_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX idx_partnership_inquiries_user_id ON partnership_inquiries(user_id);
CREATE INDEX idx_partnership_inquiries_status ON partnership_inquiries(status);
CREATE INDEX idx_partnership_inquiries_type ON partnership_inquiries(partnership_type);
CREATE INDEX idx_partnership_inquiries_created_at ON partnership_inquiries(created_at DESC);
CREATE INDEX idx_partnership_inquiries_is_deleted ON partnership_inquiries(is_deleted);

-- Comments
COMMENT ON TABLE partnership_inquiries IS '제휴/광고 문의';
COMMENT ON COLUMN partnership_inquiries.partnership_type IS '문의 유형 (PARTNERSHIP, ADVERTISEMENT, OTHER)';
COMMENT ON COLUMN partnership_inquiries.name IS '작성자명';
COMMENT ON COLUMN partnership_inquiries.status IS '처리 상태 (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)';
