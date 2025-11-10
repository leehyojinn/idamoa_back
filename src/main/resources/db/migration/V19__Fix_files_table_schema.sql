-- Fix files table schema to match File entity

-- Drop old files table and recreate with correct schema
DROP TABLE IF EXISTS file_uploads CASCADE;
DROP TABLE IF EXISTS files CASCADE;

-- Create files table with correct schema matching File entity
CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,

    -- File information
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    file_extension VARCHAR(20),

    -- User relationship
    uploader_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- Category (optional, for future categorization)
    category_id BIGINT,

    -- Entity relationship (polymorphic)
    entity_type VARCHAR(50), -- USER_PROFILE, COMPANY_IMAGE, PORTFOLIO, ESTIMATE, REVIEW, etc.
    entity_id BIGINT,

    -- Display
    display_order INT,
    is_public BOOLEAN DEFAULT FALSE NOT NULL,

    -- Statistics
    download_count BIGINT DEFAULT 0 NOT NULL,

    -- Metadata (JSONB for image width, height, etc.)
    image_metadata JSONB,
    metadata JSONB DEFAULT '{}'::JSONB,

    -- Description
    description TEXT,

    -- Base entity fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_files_uploader_id ON files(uploader_id);
CREATE INDEX idx_files_category_id ON files(category_id);
CREATE INDEX idx_files_entity ON files(entity_type, entity_id);
CREATE INDEX idx_files_created_at ON files(created_at);
CREATE INDEX idx_files_is_deleted ON files(is_deleted);

-- Comments
COMMENT ON TABLE files IS '파일 관리 (Presigned URL 업로드)';
COMMENT ON COLUMN files.entity_type IS '연관 엔티티 타입 (USER_PROFILE, COMPANY_IMAGE, PORTFOLIO, etc.)';
COMMENT ON COLUMN files.entity_id IS '연관 엔티티 ID';
COMMENT ON COLUMN files.image_metadata IS '이미지 메타데이터 (width, height 등)';
