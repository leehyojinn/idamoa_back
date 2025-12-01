-- V48: Migrate file URLs from S3 to CloudFront
-- S3 URL을 CloudFront URL로 변경하여 CDN 캐싱 및 보안 강화

-- 1. files 테이블의 file_url 변경
UPDATE files
SET file_url = REPLACE(
    file_url,
    'https://hip-damoa-uploads-local.s3.ap-northeast-2.amazonaws.com',
    'https://diuqq6anej0c9.cloudfront.net'
)
WHERE file_url LIKE '%hip-damoa-uploads-local.s3.ap-northeast-2.amazonaws.com%';

-- 2. 마이그레이션 결과 로깅 (영향받은 행 수 확인용)
DO $$
DECLARE
    affected_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO affected_count
    FROM files
    WHERE file_url LIKE '%diuqq6anej0c9.cloudfront.net%';

    RAISE NOTICE 'CloudFront URL migration completed. Total files with CloudFront URL: %', affected_count;
END $$;
