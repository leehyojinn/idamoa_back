-- =====================================================
-- V76: 제휴업체 관리 테이블 생성
-- =====================================================

-- 제휴업체 테이블
CREATE TABLE company_partnerships (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    display_order INTEGER NOT NULL DEFAULT 0,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    admin_memo TEXT,
    registered_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb
);

-- 회사당 1개 활성 제휴만 허용 (Partial Unique Index)
CREATE UNIQUE INDEX idx_company_partnerships_company_active
    ON company_partnerships(company_id)
    WHERE status = 'ACTIVE' AND is_deleted = FALSE;

-- 인덱스
CREATE INDEX idx_company_partnerships_uuid ON company_partnerships(uuid);
CREATE INDEX idx_company_partnerships_status ON company_partnerships(status);
CREATE INDEX idx_company_partnerships_end_date ON company_partnerships(end_date);
CREATE INDEX idx_company_partnerships_display_order ON company_partnerships(display_order);
CREATE INDEX idx_company_partnerships_company_id ON company_partnerships(company_id);

-- 코멘트
COMMENT ON TABLE company_partnerships IS '제휴업체 관리';
COMMENT ON COLUMN company_partnerships.display_order IS '노출 순서 (낮을수록 먼저 노출, 0이 가장 상위)';
COMMENT ON COLUMN company_partnerships.start_date IS '제휴 시작일';
COMMENT ON COLUMN company_partnerships.end_date IS '제휴 만료일';
COMMENT ON COLUMN company_partnerships.status IS '상태: ACTIVE(활성), EXPIRED(만료), CANCELLED(취소)';
COMMENT ON COLUMN company_partnerships.admin_memo IS '관리자 메모';
