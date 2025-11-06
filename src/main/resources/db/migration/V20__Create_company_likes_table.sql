-- V20: Create company_likes table for company like functionality

CREATE TABLE company_likes (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_company_likes_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_company_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraint: one user can only like a company once
    CONSTRAINT uk_company_likes_company_user UNIQUE (company_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_company_likes_company_id ON company_likes(company_id);
CREATE INDEX idx_company_likes_user_id ON company_likes(user_id);

-- Comment
COMMENT ON TABLE company_likes IS '업체 좋아요';
COMMENT ON COLUMN company_likes.company_id IS '업체 ID';
COMMENT ON COLUMN company_likes.user_id IS '사용자 ID';
