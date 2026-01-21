-- 포트폴리오 상담신청 테이블 생성
CREATE TABLE portfolio_consultations (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),

    -- 신청자 (로그인 유저)
    user_id BIGINT NOT NULL REFERENCES users(id),

    -- 해당 포트폴리오
    portfolio_id BIGINT NOT NULL REFERENCES company_portfolios(id),

    -- 업체 (조회 편의용)
    company_id BIGINT NOT NULL REFERENCES companies(id),

    -- 신청자 정보
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(200) NOT NULL,

    -- 상담 내용
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,

    -- 연락 방법
    contact_method VARCHAR(20) NOT NULL,

    -- 연락 가능 시간
    available_time VARCHAR(200),

    -- 상태
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- 업체 메모 (업체용)
    company_memo TEXT,

    -- 답변
    answer TEXT,
    answered_at TIMESTAMP,

    -- BaseEntity 공통 필드
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,

    CONSTRAINT uk_portfolio_consultations_uuid UNIQUE (uuid)
);

-- 인덱스 생성
CREATE INDEX idx_portfolio_consultations_user_id ON portfolio_consultations(user_id);
CREATE INDEX idx_portfolio_consultations_portfolio_id ON portfolio_consultations(portfolio_id);
CREATE INDEX idx_portfolio_consultations_company_id ON portfolio_consultations(company_id);
CREATE INDEX idx_portfolio_consultations_status ON portfolio_consultations(status);
CREATE INDEX idx_portfolio_consultations_created_at ON portfolio_consultations(created_at);

COMMENT ON TABLE portfolio_consultations IS '포트폴리오 상담신청';
COMMENT ON COLUMN portfolio_consultations.user_id IS '신청자 ID (로그인 유저)';
COMMENT ON COLUMN portfolio_consultations.portfolio_id IS '포트폴리오 ID';
COMMENT ON COLUMN portfolio_consultations.company_id IS '업체 ID';
COMMENT ON COLUMN portfolio_consultations.contact_method IS '연락 방법 (PHONE, EMAIL, KAKAO, ANY)';
COMMENT ON COLUMN portfolio_consultations.status IS '상태 (PENDING, IN_PROGRESS, ANSWERED, COMPLETED, CANCELLED)';
COMMENT ON COLUMN portfolio_consultations.company_memo IS '업체용 메모';
