-- V34 마이그레이션이 이미 실행되었는지 확인 (title 컬럼 존재 여부로 체크)
-- title 컬럼이 이미 존재한다면 V34가 이미 적용된 것으로 간주하고 스킵
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'inquiries' AND column_name = 'title'
    ) THEN
        RAISE NOTICE 'V34 migration already applied (title column exists), skipping entire migration...';
    ELSE
        -- V34 마이그레이션 실행
        RAISE NOTICE 'Running V34 migration...';

        -- 1. 기존 제휴/광고 문의 데이터를 partnership_inquiries 테이블로 이동
        -- 이 부분은 partnership_inquiries 테이블 구조가 다르므로 스킵

        -- 2. inquiries 테이블 구조 수정 (일반 문의용으로 변경)
        -- 2-1. 기존 컬럼 삭제
        ALTER TABLE inquiries DROP COLUMN IF EXISTS name;
        ALTER TABLE inquiries DROP COLUMN IF EXISTS email;
        ALTER TABLE inquiries DROP COLUMN IF EXISTS phone;

        -- 2-2. 새 컬럼 추가
        ALTER TABLE inquiries ADD COLUMN title VARCHAR(200);
        UPDATE inquiries SET title = SUBSTRING(content FROM 1 FOR 200) WHERE title IS NULL;
        ALTER TABLE inquiries ALTER COLUMN title SET NOT NULL;

        -- 2-3. user_id를 NOT NULL로 변경 (일반 문의는 로그인 필수)
        -- user_id가 null인 레코드가 있다면 삭제 (또는 적절한 사용자로 할당)
        DELETE FROM inquiries WHERE user_id IS NULL;
        ALTER TABLE inquiries ALTER COLUMN user_id SET NOT NULL;

        RAISE NOTICE 'V34 migration completed successfully';
    END IF;
END $$;