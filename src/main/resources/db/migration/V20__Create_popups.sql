-- 홈페이지 팝업 테이블
CREATE TABLE popups (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),

    -- 팝업 기본 정보
    title VARCHAR(200) NOT NULL,
    content TEXT,
    image_uuid UUID, -- files 테이블 uuid 참조 (S3 이미지)
    link_url VARCHAR(500),

    -- 노출 기간
    display_start_date TIMESTAMP,
    display_end_date TIMESTAMP,

    -- 노출 설정
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- 통계
    view_count BIGINT NOT NULL DEFAULT 0,
    click_count BIGINT NOT NULL DEFAULT 0,

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    metadata JSONB DEFAULT '{}'::jsonb,

    CONSTRAINT uk_popups_uuid UNIQUE (uuid)
);

-- 인덱스 생성
CREATE INDEX idx_popups_is_active_is_deleted_display_order
    ON popups(is_active, is_deleted, display_order);
CREATE INDEX idx_popups_display_start_date ON popups(display_start_date);
CREATE INDEX idx_popups_display_end_date ON popups(display_end_date);
CREATE INDEX idx_popups_created_at ON popups(created_at DESC);

-- 코멘트 추가
COMMENT ON TABLE popups IS '홈페이지 팝업';
COMMENT ON COLUMN popups.title IS '팝업 제목';
COMMENT ON COLUMN popups.content IS '팝업 내용 (HTML 지원)';
COMMENT ON COLUMN popups.image_uuid IS 'files 테이블 uuid (팝업 이미지)';
COMMENT ON COLUMN popups.link_url IS '클릭 시 이동할 URL';
COMMENT ON COLUMN popups.display_start_date IS '노출 시작일시 (null이면 제한 없음)';
COMMENT ON COLUMN popups.display_end_date IS '노출 종료일시 (null이면 제한 없음)';
COMMENT ON COLUMN popups.display_order IS '노출 순서 (낮을수록 먼저 표시)';
COMMENT ON COLUMN popups.is_active IS '활성화 여부';
COMMENT ON COLUMN popups.view_count IS '조회수';
COMMENT ON COLUMN popups.click_count IS '클릭수';
