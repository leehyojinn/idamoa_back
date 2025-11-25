-- ======================================================================
-- V30: Fix company filter option duplicates
-- ======================================================================
-- Description: Remove duplicate entries in company_filter_options table
-- Date: 2025-11-25
-- ======================================================================

-- 1. Remove all duplicate entries (keep only the earliest one)
DELETE FROM company_filter_options a
USING company_filter_options b
WHERE a.id > b.id
AND a.company_id = b.company_id
AND a.filter_option_id = b.filter_option_id;

-- 2. Log the cleanup
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM company_filter_options;

    RAISE NOTICE 'V30 Migration: Duplicate removal complete. Total records: %', v_count;
END $$;