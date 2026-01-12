-- =====================================================
-- 客户上传文件表
-- 存储客服系统推送的客户上传文件元数据
-- =====================================================

-- 创建表
CREATE TABLE IF NOT EXISTS public.openapi_client_file (
    id BIGSERIAL PRIMARY KEY,
    matter_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    client_name VARCHAR(100),
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255),
    file_size BIGINT,
    file_type VARCHAR(100),
    file_category VARCHAR(50) DEFAULT 'OTHER',
    description TEXT,
    external_file_id VARCHAR(255) NOT NULL,
    external_file_url VARCHAR(1000) NOT NULL,
    uploaded_by VARCHAR(100),
    uploaded_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING',
    local_document_id BIGINT,
    target_dossier_id BIGINT,
    synced_at TIMESTAMP,
    synced_by BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE
);

-- 添加注释
COMMENT ON TABLE public.openapi_client_file IS '客户上传文件表 - 存储客服系统推送的客户上传文件';
COMMENT ON COLUMN public.openapi_client_file.matter_id IS '项目ID';
COMMENT ON COLUMN public.openapi_client_file.client_id IS '客户ID';
COMMENT ON COLUMN public.openapi_client_file.client_name IS '客户姓名';
COMMENT ON COLUMN public.openapi_client_file.file_name IS '文件名';
COMMENT ON COLUMN public.openapi_client_file.original_file_name IS '原始文件名';
COMMENT ON COLUMN public.openapi_client_file.file_size IS '文件大小（字节）';
COMMENT ON COLUMN public.openapi_client_file.file_type IS '文件类型（MIME类型）';
COMMENT ON COLUMN public.openapi_client_file.file_category IS '文件类别：EVIDENCE/CONTRACT/ID_CARD/OTHER';
COMMENT ON COLUMN public.openapi_client_file.description IS '文件描述';
COMMENT ON COLUMN public.openapi_client_file.external_file_id IS '客服系统中的文件ID';
COMMENT ON COLUMN public.openapi_client_file.external_file_url IS '客服系统中的文件下载URL';
COMMENT ON COLUMN public.openapi_client_file.uploaded_by IS '上传人';
COMMENT ON COLUMN public.openapi_client_file.uploaded_at IS '上传时间';
COMMENT ON COLUMN public.openapi_client_file.status IS '状态：PENDING/SYNCED/DELETED/FAILED';
COMMENT ON COLUMN public.openapi_client_file.local_document_id IS '同步后的本地文档ID';
COMMENT ON COLUMN public.openapi_client_file.target_dossier_id IS '同步到的卷宗目录ID';
COMMENT ON COLUMN public.openapi_client_file.synced_at IS '同步时间';
COMMENT ON COLUMN public.openapi_client_file.synced_by IS '同步操作人';
COMMENT ON COLUMN public.openapi_client_file.error_message IS '错误信息';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_client_file_matter ON public.openapi_client_file(matter_id);
CREATE INDEX IF NOT EXISTS idx_client_file_status ON public.openapi_client_file(status);
CREATE INDEX IF NOT EXISTS idx_client_file_external ON public.openapi_client_file(external_file_id);
CREATE INDEX IF NOT EXISTS idx_client_file_created ON public.openapi_client_file(created_at DESC);

-- 序列设置（如果需要）
SELECT setval('openapi_client_file_id_seq', 1, false);
