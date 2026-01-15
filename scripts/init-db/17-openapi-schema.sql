-- =====================================================
-- 律师事务所管理系统 - 客户服务 OpenAPI 模块
-- =====================================================
-- 版本: 1.2.0
-- 日期: 2026-01-12
-- 描述: 
--   1. 向客户服务系统推送项目数据
--   2. 为客户服务系统提供数据拉取接口
--   3. 接收客户上传的文件
-- =====================================================

-- =====================================================
-- 1. 数据推送记录表 - 记录向客户服务系统推送的数据
-- =====================================================
CREATE TABLE IF NOT EXISTS public.openapi_push_record (
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联信息
    matter_id BIGINT NOT NULL,                    -- 项目ID
    client_id BIGINT NOT NULL,                    -- 客户ID
    
    -- 推送信息
    push_type VARCHAR(20) NOT NULL,               -- 推送类型: MANUAL-手动, AUTO-自动, UPDATE-更新
    scopes VARCHAR(500) NOT NULL,                 -- 推送范围（逗号分隔）
    data_snapshot JSONB,                          -- 推送的数据快照（脱敏后的数据）
    
    -- 客户服务系统返回
    external_id VARCHAR(100),                     -- 客户服务系统返回的数据ID
    external_url VARCHAR(500),                    -- 客户服务系统返回的客户访问链接
    
    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 状态: PENDING-待推送, SUCCESS-成功, FAILED-失败
    error_message TEXT,                           -- 错误信息
    retry_count INTEGER DEFAULT 0,                -- 重试次数
    
    -- 有效期
    expires_at TIMESTAMP,                         -- 数据在客户服务系统中的有效期
    
    -- 创建信息
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0                       -- 乐观锁版本号
);

CREATE INDEX IF NOT EXISTS idx_push_record_matter ON public.openapi_push_record(matter_id);
CREATE INDEX IF NOT EXISTS idx_push_record_client ON public.openapi_push_record(client_id);
CREATE INDEX IF NOT EXISTS idx_push_record_status ON public.openapi_push_record(status);

COMMENT ON TABLE public.openapi_push_record IS '数据推送记录表 - 记录向客户服务系统推送的项目数据';
COMMENT ON COLUMN public.openapi_push_record.external_id IS '客户服务系统返回的数据ID，用于后续更新或撤销';
COMMENT ON COLUMN public.openapi_push_record.external_url IS '客户服务系统返回的客户访问链接，可发送给客户';
COMMENT ON COLUMN public.openapi_push_record.data_snapshot IS '推送时的数据快照，JSON格式，用于审计和对比';

-- =====================================================
-- 2. 推送配置表 - 项目级别的推送设置
-- =====================================================
CREATE TABLE IF NOT EXISTS public.openapi_push_config (
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联信息
    matter_id BIGINT NOT NULL UNIQUE,             -- 项目ID（一个项目一条配置）
    client_id BIGINT NOT NULL,                    -- 客户ID
    
    -- 推送设置
    enabled BOOLEAN DEFAULT FALSE,                -- 是否启用推送
    scopes VARCHAR(500),                          -- 默认推送范围
    auto_push_on_update BOOLEAN DEFAULT FALSE,    -- 项目更新时自动推送
    valid_days INTEGER DEFAULT 30,                -- 数据有效期（天）
    
    -- 创建信息
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0                       -- 乐观锁版本号
);

CREATE INDEX IF NOT EXISTS idx_push_config_matter ON public.openapi_push_config(matter_id);
COMMENT ON TABLE public.openapi_push_config IS '推送配置表 - 项目级别的客户服务推送设置';

-- =====================================================
-- 3. 客户访问令牌表 - 供客户服务系统拉取数据（可选）
-- =====================================================
CREATE TABLE IF NOT EXISTS public.openapi_client_token (
    id BIGSERIAL PRIMARY KEY,
    
    -- 令牌信息
    token VARCHAR(128) NOT NULL UNIQUE,           -- 访问令牌（安全随机生成）
    token_type VARCHAR(20) DEFAULT 'BEARER',      -- 令牌类型
    
    -- 关联信息
    client_id BIGINT NOT NULL,                    -- 关联的客户ID
    matter_id BIGINT,                             -- 关联的项目ID（可选，限定到具体项目）
    
    -- 授权范围
    scope VARCHAR(500),                           -- 授权范围：MATTER_INFO,MATTER_PROGRESS,DOCUMENT_LIST,LAWYER_INFO 等
    
    -- 有效期控制
    expires_at TIMESTAMP NOT NULL,                -- 过期时间
    max_access_count INTEGER,                     -- 最大访问次数（NULL表示不限制）
    access_count INTEGER DEFAULT 0,               -- 已访问次数
    
    -- 安全控制
    ip_whitelist VARCHAR(500),                    -- IP白名单（逗号分隔，NULL表示不限制）
    last_access_ip VARCHAR(50),                   -- 最后访问IP
    last_access_at TIMESTAMP,                     -- 最后访问时间
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',          -- 状态：ACTIVE-有效, REVOKED-已撤销, EXPIRED-已过期
    revoked_at TIMESTAMP,                         -- 撤销时间
    revoked_by BIGINT,                            -- 撤销人
    revoke_reason VARCHAR(200),                   -- 撤销原因
    
    -- 创建信息
    created_by BIGINT,                            -- 创建人（律师）
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- 备注
    remark VARCHAR(500),                          -- 备注说明
    
    deleted BOOLEAN DEFAULT FALSE
);

-- 索引
CREATE INDEX idx_openapi_token_client ON public.openapi_client_token(client_id);
CREATE INDEX idx_openapi_token_matter ON public.openapi_client_token(matter_id);
CREATE INDEX idx_openapi_token_expires ON public.openapi_client_token(expires_at);
CREATE INDEX idx_openapi_token_status ON public.openapi_client_token(status);

-- 注释
COMMENT ON TABLE public.openapi_client_token IS '客户访问令牌表 - 管理客户门户的访问授权';
COMMENT ON COLUMN public.openapi_client_token.token IS '访问令牌，安全随机生成的128位字符串';
COMMENT ON COLUMN public.openapi_client_token.scope IS '授权范围，多个用逗号分隔：MATTER_INFO,MATTER_PROGRESS,DOCUMENT_LIST,LAWYER_INFO,DEADLINE_INFO';
COMMENT ON COLUMN public.openapi_client_token.max_access_count IS '最大访问次数，NULL表示不限制';
COMMENT ON COLUMN public.openapi_client_token.ip_whitelist IS 'IP白名单，多个用逗号分隔，NULL表示不限制';

-- =====================================================
-- 2. 客户访问日志表 - 记录所有外部访问
-- =====================================================
CREATE TABLE IF NOT EXISTS public.openapi_access_log (
    id BIGSERIAL PRIMARY KEY,
    
    -- 访问信息
    token_id BIGINT NOT NULL,                     -- 令牌ID
    client_id BIGINT NOT NULL,                    -- 客户ID
    matter_id BIGINT,                             -- 项目ID
    
    -- 请求信息
    request_path VARCHAR(200) NOT NULL,           -- 请求路径
    request_method VARCHAR(10) NOT NULL,          -- 请求方法
    request_params TEXT,                          -- 请求参数（脱敏）
    
    -- 响应信息
    response_code INTEGER,                        -- 响应状态码
    response_time_ms INTEGER,                     -- 响应时间(ms)
    
    -- 客户端信息
    client_ip VARCHAR(50),                        -- 客户端IP
    user_agent VARCHAR(500),                      -- User-Agent
    
    -- 访问结果
    access_result VARCHAR(20),                    -- 访问结果：SUCCESS, DENIED, ERROR
    error_message VARCHAR(500),                   -- 错误信息
    
    -- 时间
    access_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_openapi_log_token ON public.openapi_access_log(token_id);
CREATE INDEX idx_openapi_log_client ON public.openapi_access_log(client_id);
CREATE INDEX idx_openapi_log_matter ON public.openapi_access_log(matter_id);
CREATE INDEX idx_openapi_log_access_at ON public.openapi_access_log(access_at);

-- 注释
COMMENT ON TABLE public.openapi_access_log IS '客户访问日志表 - 记录所有 OpenAPI 访问';

-- =====================================================
-- 3. 项目共享配置表 - 控制项目哪些信息可以对外共享
-- =====================================================
CREATE TABLE IF NOT EXISTS public.openapi_matter_share_config (
    id BIGSERIAL PRIMARY KEY,
    
    matter_id BIGINT NOT NULL UNIQUE,             -- 项目ID
    
    -- 可共享的信息范围
    share_basic_info BOOLEAN DEFAULT TRUE,        -- 基本信息（项目名称、类型、状态）
    share_progress BOOLEAN DEFAULT TRUE,          -- 进度信息
    share_task_list BOOLEAN DEFAULT FALSE,        -- 任务列表
    share_deadline BOOLEAN DEFAULT TRUE,          -- 关键期限
    share_document_list BOOLEAN DEFAULT FALSE,    -- 文档列表（仅标题，不含内容）
    share_team_info BOOLEAN DEFAULT TRUE,         -- 团队信息（律师姓名、联系方式）
    share_fee_info BOOLEAN DEFAULT FALSE,         -- 费用信息（已收款/待收款）
    
    -- 自定义隐藏字段
    hidden_fields VARCHAR(1000),                  -- 需要隐藏的字段列表（JSON数组）
    
    -- 配置状态
    enabled BOOLEAN DEFAULT FALSE,                -- 是否启用共享
    
    -- 创建信息
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    deleted BOOLEAN DEFAULT FALSE
);

-- 注释
COMMENT ON TABLE public.openapi_matter_share_config IS '项目共享配置表 - 控制项目信息的对外共享范围';

-- =====================================================
-- 4. 公开验证码表 - 用于函件、合同等的二维码验证
-- =====================================================
CREATE TABLE IF NOT EXISTS public.openapi_verification_code (
    id BIGSERIAL PRIMARY KEY,
    
    -- 验证信息
    verification_code VARCHAR(32) NOT NULL UNIQUE, -- 验证码
    verification_type VARCHAR(30) NOT NULL,        -- 类型：LETTER-函件, CONTRACT-合同, CERTIFICATE-证书
    
    -- 关联业务
    business_id BIGINT NOT NULL,                   -- 业务对象ID
    business_no VARCHAR(50),                       -- 业务编号
    
    -- 验证控制
    expires_at TIMESTAMP,                          -- 过期时间（NULL表示永久有效）
    max_verify_count INTEGER,                      -- 最大验证次数
    verify_count INTEGER DEFAULT 0,                -- 已验证次数
    
    -- 状态
    status VARCHAR(20) DEFAULT 'ACTIVE',           -- 状态：ACTIVE, REVOKED, EXPIRED
    
    -- 创建信息
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    deleted BOOLEAN DEFAULT FALSE
);

-- 索引
CREATE INDEX idx_openapi_verify_type ON public.openapi_verification_code(verification_type);
CREATE INDEX idx_openapi_verify_business ON public.openapi_verification_code(business_id);

-- 注释
COMMENT ON TABLE public.openapi_verification_code IS '公开验证码表 - 用于二维码真伪验证';

-- =====================================================
-- 5. 客户上传文件表 - 存储客服系统推送的客户上传文件
-- =====================================================
CREATE TABLE IF NOT EXISTS public.openapi_client_file (
    id BIGSERIAL PRIMARY KEY,
    
    -- 关联信息
    matter_id BIGINT NOT NULL,                      -- 项目ID
    client_id BIGINT NOT NULL,                      -- 客户ID
    client_name VARCHAR(100),                       -- 客户姓名
    
    -- 文件信息
    file_name VARCHAR(255) NOT NULL,                -- 文件名
    original_file_name VARCHAR(255),                -- 原始文件名
    file_size BIGINT,                               -- 文件大小（字节）
    file_type VARCHAR(100),                         -- 文件类型（MIME类型）
    file_category VARCHAR(50) DEFAULT 'OTHER',     -- 文件类别：EVIDENCE/CONTRACT/ID_CARD/OTHER
    description TEXT,                               -- 文件描述
    
    -- 外部系统信息
    external_file_id VARCHAR(255) NOT NULL,         -- 客服系统中的文件ID
    external_file_url VARCHAR(1000) NOT NULL,       -- 客服系统中的文件下载URL
    uploaded_by VARCHAR(100),                       -- 上传人
    uploaded_at TIMESTAMP,                          -- 上传时间
    
    -- 同步状态
    status VARCHAR(20) DEFAULT 'PENDING',           -- 状态：PENDING/SYNCED/DELETED/FAILED
    local_document_id BIGINT,                       -- 同步后的本地文档ID
    target_dossier_id BIGINT,                       -- 同步到的卷宗目录ID
    synced_at TIMESTAMP,                            -- 同步时间
    synced_by BIGINT,                               -- 同步操作人
    error_message TEXT,                             -- 错误信息
    
    -- 审计字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 0                        -- 乐观锁版本号
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_client_file_matter ON public.openapi_client_file(matter_id);
CREATE INDEX IF NOT EXISTS idx_client_file_client ON public.openapi_client_file(client_id);
CREATE INDEX IF NOT EXISTS idx_client_file_status ON public.openapi_client_file(status);
CREATE INDEX IF NOT EXISTS idx_client_file_external ON public.openapi_client_file(external_file_id);
CREATE INDEX IF NOT EXISTS idx_client_file_created ON public.openapi_client_file(created_at DESC);

-- 注释
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

-- =====================================================
-- 6. 初始化权限菜单
-- =====================================================
-- 项目级别客户服务权限（律师使用）
-- 添加到项目管理菜单下（parent_id=4 是项目管理）
INSERT INTO public.sys_menu (id, parent_id, name, menu_type, permission, sort_order, visible, status, is_external, is_cache)
VALUES 
    (1815, 4, '客户服务-查看', 'BUTTON', 'matter:clientService:list', 20, true, 'ENABLED', false, false),
    (1816, 4, '客户服务-推送', 'BUTTON', 'matter:clientService:create', 21, true, 'ENABLED', false, false)
ON CONFLICT (id) DO NOTHING;

-- =====================================================
-- 7. 分配角色权限
-- =====================================================
-- 修复序列（先执行，避免插入冲突）
DO $$
BEGIN
    PERFORM setval('sys_role_menu_id_seq', COALESCE((SELECT MAX(id) FROM sys_role_menu), 0) + 1, false);
END $$;

-- 为管理员角色分配客户服务权限
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM public.sys_role r, public.sys_menu m
WHERE r.role_code = 'ADMIN' AND m.id IN (1815, 1816)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- 为律师角色分配项目级别的客户服务权限
INSERT INTO public.sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM public.sys_role r, public.sys_menu m
WHERE r.role_code = 'LAWYER' AND m.id IN (1815, 1816)
ON CONFLICT (role_id, menu_id) DO NOTHING;

-- =====================================================
-- 8. 添加客户服务系统的外部集成配置
-- =====================================================
-- 修复序列
DO $$
BEGIN
    PERFORM setval('sys_external_integration_id_seq', COALESCE((SELECT MAX(id) FROM sys_external_integration), 0) + 1, false);
END $$;

-- 插入客户服务系统配置（使用 ID=101 避免与 20-init-data.sql 中的数据冲突）
INSERT INTO public.sys_external_integration (
    id, integration_code, integration_name, integration_type, description, 
    api_url, auth_type, extra_config, enabled
) VALUES (
    101,
    'CLIENT_SERVICE',
    '客户服务系统',
    'CLIENT_SERVICE',
    '向客户推送项目信息，支持短信、公众号等多渠道通知客户查看项目进度。需要另行部署客户服务系统。',
    'https://client-service.example.com/api',
    'API_KEY',
    '{"notifyChannels": ["SMS", "WECHAT", "EMAIL"], "defaultValidDays": 30, "apiEndpoints": {"push": "/matter/receive", "revoke": "/matter/revoke"}}',
    false
) ON CONFLICT (integration_code) DO UPDATE SET
    integration_name = EXCLUDED.integration_name,
    description = EXCLUDED.description,
    extra_config = EXCLUDED.extra_config;

-- 更新菜单序列（保留 1816 作为最大值，虽然 1810-1814 已废弃）
SELECT setval('sys_menu_id_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM sys_menu), 1816));

