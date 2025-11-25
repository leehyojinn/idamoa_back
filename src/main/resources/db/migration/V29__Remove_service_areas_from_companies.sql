-- ======================================================================
-- V29: Remove service_areas column from companies table
-- ======================================================================
-- Description: serviceAreas is now managed through filter options
-- Date: 2025-11-25
-- ======================================================================

-- 1. Drop the view that depends on service_areas column
DROP VIEW IF EXISTS v_company_rankings CASCADE;

-- 2. Drop the service_areas column
ALTER TABLE companies DROP COLUMN IF EXISTS service_areas;

-- 3. Recreate the view without service_areas column
-- (뷰가 필요한 경우, service_areas 없이 재생성)
CREATE OR REPLACE VIEW v_company_rankings AS
SELECT
    c.id,
    c.uuid,
    c.name,
    c.slug,
    c.description,
    c.avg_rating,
    c.review_count,
    c.like_count,
    c.view_count,
    c.portfolio_count,
    c.completed_projects,
    c.verified,
    c.featured,
    c.premium_tier,
    c.created_at,
    c.updated_at
FROM companies c
WHERE c.is_deleted = false
  AND c.status = 'ACTIVE';

-- Add comment explaining the change
COMMENT ON TABLE companies IS 'Company information table. Service areas are now managed through filter options in company_filter_options table (since V29)';
COMMENT ON VIEW v_company_rankings IS 'Company rankings view without service_areas (service areas now managed via filters)';

-- Log the migration
DO $$
BEGIN
    RAISE NOTICE 'V29 Migration: service_areas column removed from companies table. v_company_rankings view recreated. Service areas should now be managed through filter options.';
END $$;