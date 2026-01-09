-- =============================================
-- V86: 팝업 모바일 지원 및 다양한 단위 추가
-- =============================================
-- 지원 단위: px, %, vw, vh, em, rem

-- PC 설정 단위 필드 추가
ALTER TABLE popups ADD COLUMN width_unit VARCHAR(10) DEFAULT 'px';
ALTER TABLE popups ADD COLUMN height_unit VARCHAR(10) DEFAULT 'px';
ALTER TABLE popups ADD COLUMN offset_x_unit VARCHAR(10) DEFAULT 'px';
ALTER TABLE popups ADD COLUMN offset_y_unit VARCHAR(10) DEFAULT 'px';

-- 모바일 전용 설정 추가
ALTER TABLE popups ADD COLUMN mobile_enabled BOOLEAN DEFAULT true;
ALTER TABLE popups ADD COLUMN mobile_width INTEGER;
ALTER TABLE popups ADD COLUMN mobile_width_unit VARCHAR(10) DEFAULT 'px';
ALTER TABLE popups ADD COLUMN mobile_height INTEGER;
ALTER TABLE popups ADD COLUMN mobile_height_unit VARCHAR(10) DEFAULT 'px';
ALTER TABLE popups ADD COLUMN mobile_position VARCHAR(20) DEFAULT 'CENTER';
ALTER TABLE popups ADD COLUMN mobile_offset_x INTEGER DEFAULT 0;
ALTER TABLE popups ADD COLUMN mobile_offset_x_unit VARCHAR(10) DEFAULT 'px';
ALTER TABLE popups ADD COLUMN mobile_offset_y INTEGER DEFAULT 0;
ALTER TABLE popups ADD COLUMN mobile_offset_y_unit VARCHAR(10) DEFAULT 'px';

-- 컬럼 코멘트
COMMENT ON COLUMN popups.width_unit IS '너비 단위 (px, %, vw, vh, em, rem)';
COMMENT ON COLUMN popups.height_unit IS '높이 단위 (px, %, vw, vh, em, rem)';
COMMENT ON COLUMN popups.offset_x_unit IS 'X 오프셋 단위 (px, %, vw, vh, em, rem)';
COMMENT ON COLUMN popups.offset_y_unit IS 'Y 오프셋 단위 (px, %, vw, vh, em, rem)';
COMMENT ON COLUMN popups.mobile_enabled IS '모바일 설정 활성화 여부';
COMMENT ON COLUMN popups.mobile_width IS '모바일 너비';
COMMENT ON COLUMN popups.mobile_width_unit IS '모바일 너비 단위';
COMMENT ON COLUMN popups.mobile_height IS '모바일 높이';
COMMENT ON COLUMN popups.mobile_height_unit IS '모바일 높이 단위';
COMMENT ON COLUMN popups.mobile_position IS '모바일 위치 (CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM)';
COMMENT ON COLUMN popups.mobile_offset_x IS '모바일 X 오프셋';
COMMENT ON COLUMN popups.mobile_offset_x_unit IS '모바일 X 오프셋 단위';
COMMENT ON COLUMN popups.mobile_offset_y IS '모바일 Y 오프셋';
COMMENT ON COLUMN popups.mobile_offset_y_unit IS '모바일 Y 오프셋 단위';
