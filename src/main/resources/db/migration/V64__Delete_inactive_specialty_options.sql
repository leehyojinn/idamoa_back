--
-- V64: 비활성화된 specialty 옵션 삭제
--
-- 목적:
--   V63에서 통합 후 비활성화된 분할 옵션들을 완전히 삭제
--   company_filter_options 연결이 없는 옵션만 삭제
--

-- 비활성화되고 사용되지 않는 specialty 옵션 삭제
DELETE FROM filter_options
WHERE category_id = (SELECT id FROM filter_categories WHERE code = 'specialty' AND is_deleted = false)
  AND is_active = false
  AND NOT EXISTS (
      SELECT 1 FROM company_filter_options cfo
      WHERE cfo.filter_option_id = filter_options.id
  );
