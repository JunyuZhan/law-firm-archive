-- =====================================================
-- 数据库优化脚本 - 全文搜索索引
-- =====================================================
-- 版本: 1.0.0
-- 日期: 2026-01-15
-- 描述: 为需要模糊搜索的字段添加全文搜索和三元组索引
-- 优先级: P2 (中优先级)
-- 依赖: pg_trgm 扩展（在 00-extensions.sql 中已安装）
-- =====================================================

-- 确保 pg_trgm 扩展已安装
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- =====================================================
-- 1. 系统管理模块全文搜索
-- =====================================================

-- 用户表：姓名和用户名搜索
CREATE INDEX IF NOT EXISTS idx_user_name_trgm
    ON public.sys_user USING gin (real_name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_user_username_trgm
    ON public.sys_user USING gin (username gin_trgm_ops)
    WHERE deleted = false;

-- 菜单表：菜单名称搜索
CREATE INDEX IF NOT EXISTS idx_menu_name_trgm
    ON public.sys_menu USING gin (name gin_trgm_ops)
    WHERE deleted = false;

-- 配置表：配置名称搜索
CREATE INDEX IF NOT EXISTS idx_config_name_trgm
    ON public.sys_config USING gin (config_name gin_trgm_ops)
    WHERE deleted = false;

-- 通知表：标题和内容搜索
CREATE INDEX IF NOT EXISTS idx_notification_title_trgm
    ON public.sys_notification USING gin (title gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_notification_content_trgm
    ON public.sys_notification USING gin (content gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 2. 客户管理模块全文搜索
-- =====================================================

-- 客户表：客户名称搜索
CREATE INDEX IF NOT EXISTS idx_client_name_trgm
    ON public.crm_client USING gin (name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_client_name_cn_trgm
    ON public.crm_client USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_client_legal_rep_trgm
    ON public.crm_client USING gin (legal_representative gin_trgm_ops)
    WHERE deleted = false;

-- 客户表：注册地址搜索
CREATE INDEX IF NOT EXISTS idx_client_address_trgm
    ON public.crm_client USING gin (registered_address gin_trgm_ops)
    WHERE deleted = false;

-- 联系人表：联系人姓名和手机号搜索
CREATE INDEX IF NOT EXISTS idx_contact_name_trgm
    ON public.crm_contact USING gin (contact_name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_contact_phone_trgm
    ON public.crm_contact USING gin (mobile_phone gin_trgm_ops)
    WHERE deleted = false;

-- 案源表：案源名称搜索
CREATE INDEX IF NOT EXISTS idx_lead_name_trgm
    ON public.crm_lead USING gin (lead_name gin_trgm_ops)
    WHERE deleted = false;

-- 案源表：联系人信息搜索
CREATE INDEX IF NOT EXISTS idx_lead_contact_name_trgm
    ON public.crm_lead USING gin (contact_name gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_lead_phone_trgm
    ON public.crm_lead USING gin (contact_phone gin_trgm_ops)
    WHERE deleted = false;

-- 利冲检查表：当事人名称搜索
CREATE INDEX IF NOT EXISTS idx_conflict_party_name_trgm
    ON public.crm_conflict_check_item USING gin (party_name gin_trgm_ops)
    WHERE deleted = false;

-- 客户变更历史：变更内容搜索
CREATE INDEX IF NOT EXISTS idx_client_change_desc_trgm
    ON public.crm_client_change_history USING gin (change_description gin_trgm_ops)
    WHERE deleted = false;

-- 客户标签：标签名称搜索
CREATE INDEX IF NOT EXISTS idx_client_tag_name_trgm
    ON public.crm_client_tag USING gin (tag_name gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 3. 项目管理模块全文搜索
-- =====================================================

-- 项目表：项目名称搜索
CREATE INDEX IF NOT EXISTS idx_matter_title_trgm
    ON public.matter USING gin (title gin_trgm_ops)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_matter_title_cn_trgm
    ON public.matter USING gin (title gin_trgm_ops);

-- 项目表：案号搜索
CREATE INDEX IF NOT EXISTS idx_matter_case_no_trgm
    ON public.matter USING gin (case_no gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：案件名称搜索
CREATE INDEX IF NOT EXISTS idx_matter_court_name_trgm
    ON public.matter USING gin (court_name gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：对方当事人搜索
CREATE INDEX IF NOT EXISTS idx_matter_opposing_party_trgm
    ON public.matter USING gin (opposing_party gin_trgm_ops)
    WHERE deleted = false;

-- 项目表：审理法院搜索
CREATE INDEX IF NOT EXISTS idx_matter_court_trgm
    ON public.matter USING gin (trial_court gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 4. 财务管理模块全文搜索
-- =====================================================

-- 合同表：合同名称搜索
CREATE INDEX IF NOT EXISTS idx_contract_name_trgm
    ON public.finance_contract USING gin (contract_name gin_trgm_ops)
    WHERE deleted = false;

-- 合同表：合同编号搜索
CREATE INDEX IF NOT EXISTS idx_contract_no_trgm
    ON public.finance_contract USING gin (contract_no gin_trgm_ops)
    WHERE deleted = false;

-- 收款表：备注搜索
CREATE INDEX IF NOT EXISTS idx_payment_remark_trgm
    ON public.finance_payment USING gin (remark gin_trgm_ops)
    WHERE deleted = false;

-- 发票表：发票号码搜索
CREATE INDEX IF NOT EXISTS idx_invoice_no_trgm
    ON public.finance_invoice USING gin (invoice_no gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 5. 行政管理模块全文搜索
-- =====================================================

-- 会议室：位置搜索
CREATE INDEX IF NOT EXISTS idx_meeting_location_trgm
    ON public.admin_meeting_room USING gin (location gin_trgm_ops)
    WHERE deleted = false;

-- 印章申请：申请事由搜索
CREATE INDEX IF NOT EXISTS idx_seal_reason_trgm
    ON public.admin_seal_application USING gin (application_reason gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 6. 任务管理模块全文搜索
-- =====================================================

-- 任务表：任务标题搜索
CREATE INDEX IF NOT EXISTS idx_task_title_trgm
    ON public.task USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- 任务表：任务描述搜索
CREATE INDEX IF NOT EXISTS idx_task_desc_trgm
    ON public.task USING gin (description gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 7. 人力资源模块全文搜索
-- =====================================================

-- 员工表：姓名搜索
CREATE INDEX IF NOT EXISTS idx_employee_name_trgm
    ON public.hr_employee USING gin (name gin_trgm_ops)
    WHERE deleted = false;

-- 培训表：培训主题搜索
CREATE INDEX IF NOT EXISTS idx_training_topic_trgm
    ON public.hr_training USING gin (topic gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 8. 档案管理模块全文搜索
-- =====================================================

-- 档案表：档案编号搜索
CREATE INDEX IF NOT EXISTS idx_archive_no_trgm
    ON public.archive USING gin (archive_no gin_trgm_ops)
    WHERE deleted = false;

-- 档案表：案卷名称搜索
CREATE INDEX IF NOT EXISTS idx_archive_name_trgm
    ON public.archive USING gin (dossier_name gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 9. 文档管理模块全文搜索
-- =====================================================

-- 文档表：文件名搜索
CREATE INDEX IF NOT EXISTS idx_doc_filename_trgm
    ON public.doc_file USING gin (file_name gin_trgm_ops)
    WHERE deleted = false;

-- 文档表：文档标题搜索
CREATE INDEX IF NOT EXISTS idx_doc_title_trgm
    ON public.doc_file USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 10. 知识库模块全文搜索
-- =====================================================

-- 文章表：标题搜索
CREATE INDEX IF NOT EXISTS idx_article_title_trgm
    ON public.kb_article USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- 文章表：内容搜索（使用 tsvector）
CREATE INDEX IF NOT EXISTS idx_article_content_fts
    ON public.kb_article USING gin (to_tsvector('chinese', coalesce(title, '') || ' ' || coalesce(content, '')))
    WHERE deleted = false;

-- 文章表：标签搜索
CREATE INDEX IF NOT EXISTS idx_article_tags_trgm
    ON public.kb_article USING gin (tags gin_trgm_ops)
    WHERE deleted = false;

-- 案例表：案例名称搜索
CREATE INDEX IF NOT EXISTS idx_kb_case_name_trgm
    ON public.kb_case USING gin (case_name gin_trgm_ops)
    WHERE deleted = false;

-- 案例表：案由搜索
CREATE INDEX IF NOT EXISTS idx_kb_case_cause_trgm
    ON public.kb_case USING gin (cause_of_action gin_trgm_ops)
    WHERE deleted = false;

-- 法规表：法规名称搜索
CREATE INDEX IF NOT EXISTS idx_regulation_name_trgm
    ON public.kb_regulation USING gin (regulation_name gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 11. 质量管理模块全文搜索
-- =====================================================

-- 风险预警表：风险描述搜索
CREATE INDEX IF NOT EXISTS idx_risk_desc_trgm
    ON public.quality_risk USING gin (risk_description gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 12. 工作台模块全文搜索
-- =====================================================

-- 审批表：审批事项搜索
CREATE INDEX IF NOT EXISTS idx_approval_title_trgm
    ON public.wb_approval USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- 日程表：日程内容搜索
CREATE INDEX IF NOT EXISTS idx_schedule_content_trgm
    ON public.wb_schedule USING gin (title gin_trgm_ops)
    WHERE deleted = false;

-- =====================================================
-- 13. 创建全文搜索辅助函数
-- =====================================================

-- 创建中文全文搜索配置（如果 zhparser 可用）
-- CREATE TEXT SEARCH CONFIGURATION chinese (COPY = simple);
-- ALTER TEXT SEARCH CONFIGURATION chinese ALTER MAPPING FOR n,v,a,i,e,l WITH zhparser;

-- 创建通用搜索函数
CREATE OR REPLACE FUNCTION public.fulltext_search(query_text TEXT, table_name TEXT, search_columns TEXT[])
RETURNS TABLE (id bigint, rank REAL) AS $$
DECLARE
    sql_query TEXT;
    search_col TEXT;
BEGIN
    sql_query := 'SELECT id, ts_rank_cd(to_tsvector('' || quote_literal('chinese') || '', ';

    FOREACH search_col IN ARRAY search_columns LOOP
        sql_query := sql_query || 'coalesce(' || quote_ident(search_col) || ', '''') || '' '' || ';
    END LOOP;

    sql_query := sql_query || ''''), query) AS rank FROM ' || quote_ident(table_name);
    sql_query := sql_query || ' WHERE deleted = false AND ';
    sql_query := sql_query || 'to_tsvector('' || quote_literal('chinese') || '', ';

    FOREACH search_col IN ARRAY search_columns LOOP
        sql_query := sql_query || 'coalesce(' || quote_ident(search_col) || ', '''') || '' '' || ';
    END LOOP;

    sql_query := sql_query || ''') @@ to_tsquery('' || quote_literal('chinese') || ', replace(' || quote_literal(query_text) || ', '' '', '' | ''))';
    sql_query := sql_query || ' ORDER BY rank DESC LIMIT 100;';

    RETURN QUERY EXECUTE sql_query;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.fulltext_search(text, text, text[]) IS '通用全文搜索函数';

-- =====================================================
-- 14. 创建搜索使用示例函数
-- =====================================================

-- 客户模糊搜索函数
CREATE OR REPLACE FUNCTION public.search_clients(search_term TEXT, limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    id bigint,
    name character varying,
    client_type character varying,
    status character varying,
    match_rank REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.name,
        c.client_type,
        c.status,
        CASE
            WHEN c.name % search_term THEN 0.9 * similarity(c.name, search_term)
            WHEN c.name ILIKE ('%' || search_term || '%') THEN 0.5
            ELSE 0.1
        END AS match_rank
    FROM public.crm_client c
    WHERE c.deleted = false
      AND (
          c.name % search_term
          OR c.name ILIKE ('%' || search_term || '%')
          OR c.legal_representative ILIKE ('%' || search_term || '%')
          OR c.credit_code ILIKE ('%' || search_term || '%')
      )
    ORDER BY match_rank DESC, c.name
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.search_clients(text, integer) IS '客户模糊搜索函数';

-- 项目模糊搜索函数
CREATE OR REPLACE FUNCTION public.search_matters(search_term TEXT, limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    id bigint,
    title character varying,
    matter_type character varying,
    matter_status character varying,
    match_rank REAL
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        m.id,
        m.title,
        m.matter_type,
        m.matter_status,
        CASE
            WHEN m.title % search_term THEN 0.9 * similarity(m.title, search_term)
            WHEN m.title ILIKE ('%' || search_term || '%') THEN 0.5
            ELSE 0.1
        END AS match_rank
    FROM public.matter m
    WHERE m.deleted = false
      AND (
          m.title % search_term
          OR m.title ILIKE ('%' || search_term || '%')
          OR m.case_no ILIKE ('%' || search_term || '%')
          OR m.opposing_party ILIKE ('%' || search_term || '%')
      )
    ORDER BY match_rank DESC, m.title
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.search_matters(text, integer) IS '项目模糊搜索函数';

-- =====================================================
-- 完成标记
-- =====================================================

DO $$
DECLARE
    trgm_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO trgm_count
    FROM pg_indexes
    WHERE schemaname = 'public'
    AND indexdef LIKE '%USING gin%gin_trgm_ops%';

    RAISE NOTICE '========================================';
    RAISE NOTICE '全文搜索索引优化完成';
    RAISE NOTICE '========================================';
    RAISE NOTICE '已创建 % 个三元组/全文搜索索引', trgm_count;
    RAISE NOTICE '';
    RAISE NOTICE '使用示例:';
    RAISE NOTICE '  -- 客户搜索';
    RAISE NOTICE '  SELECT * FROM search_clients(''贵州公司'');';
    RAISE NOTICE '';
    RAISE NOTICE '  -- 项目搜索';
    RAISE NOTICE '  SELECT * FROM search_matters(''合同纠纷'');';
    RAISE NOTICE '';
    RAISE NOTICE '三元组索引支持:';
    RAISE NOTICE '  1. 模糊匹配 (LIKE ''%term%'')';
    RAISE NOTICE '  2. 相似度搜索 (name % ''term'')';
    RAISE NOTICE '  3. 拼音搜索 (需要额外配置)';
END $$;
