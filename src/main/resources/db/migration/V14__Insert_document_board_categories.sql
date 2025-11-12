-- V14: Insert document board categories
-- Created: 2025-11-12
-- Purpose: 자료실(DOCUMENT) 게시판 카테고리 초기 데이터 추가

INSERT INTO board_categories (
    board_type,
    name,
    slug,
    description,
    parent_id,
    depth,
    display_order,
    is_active,
    is_deleted,
    created_at,
    updated_at,
    metadata
) VALUES
-- 1. 전체
('DOCUMENT', '전체', 'all', '모든 자료 보기', NULL, 0, 1, true, false, NOW(), NOW(), '{}'::jsonb),

-- 2. 병원 관련 법규
('DOCUMENT', '병원 관련 법규', 'medical-regulations', '병원 운영에 필요한 법규 및 규정', NULL, 0, 2, true, false, NOW(), NOW(), '{}'::jsonb),

-- 3. 인테리어 가이드북
('DOCUMENT', '인테리어 가이드북', 'interior-guidebook', '병원 인테리어 가이드 및 체크리스트', NULL, 0, 3, true, false, NOW(), NOW(), '{}'::jsonb),

-- 4. 개원 체크리스트
('DOCUMENT', '개원 체크리스트', 'opening-checklist', '병원 개원 시 필요한 체크리스트', NULL, 0, 4, true, false, NOW(), NOW(), '{}'::jsonb),

-- 5. 표준 서류
('DOCUMENT', '표준 서류', 'standard-documents', '병원 운영 표준 서류 양식', NULL, 0, 5, true, false, NOW(), NOW(), '{}'::jsonb),

-- 6. 오픈&참조도면
('DOCUMENT', '오픈&참조도면', 'reference-drawings', '오픈소스 및 참조 도면 자료', NULL, 0, 6, true, false, NOW(), NOW(), '{}'::jsonb);
