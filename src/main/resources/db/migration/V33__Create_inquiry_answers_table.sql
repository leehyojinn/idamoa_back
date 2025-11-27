-- 일반 문의 답변 테이블 생성
CREATE TABLE IF NOT EXISTS inquiry_answers (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() NOT NULL UNIQUE,
    inquiry_id BIGINT NOT NULL,  -- 문의 ID
    admin_id BIGINT NOT NULL,  -- 답변 작성 관리자 ID
    content TEXT NOT NULL,  -- 답변 내용
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inquiry_answers_inquiry FOREIGN KEY (inquiry_id)
        REFERENCES inquiries(id) ON DELETE CASCADE,
    CONSTRAINT fk_inquiry_answers_admin FOREIGN KEY (admin_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT uk_one_answer_per_inquiry UNIQUE (inquiry_id)  -- 문의당 하나의 답변만
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_inquiry_answers_inquiry_id ON inquiry_answers(inquiry_id);
CREATE INDEX IF NOT EXISTS idx_inquiry_answers_admin_id ON inquiry_answers(admin_id);
CREATE INDEX IF NOT EXISTS idx_inquiry_answers_created_at ON inquiry_answers(created_at DESC);

-- 코멘트 추가
COMMENT ON TABLE inquiry_answers IS '일반 문의 답변';
COMMENT ON COLUMN inquiry_answers.id IS '답변 ID';
COMMENT ON COLUMN inquiry_answers.uuid IS '답변 UUID';
COMMENT ON COLUMN inquiry_answers.inquiry_id IS '문의 ID';
COMMENT ON COLUMN inquiry_answers.admin_id IS '답변 작성 관리자 ID';
COMMENT ON COLUMN inquiry_answers.content IS '답변 내용';
COMMENT ON COLUMN inquiry_answers.created_at IS '답변 작성일시';
COMMENT ON COLUMN inquiry_answers.updated_at IS '답변 수정일시';