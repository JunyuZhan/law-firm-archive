package com.lawfirm.application.document.command;

import com.lawfirm.application.document.dto.MatterContextDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 生成文书命令
 */
@Data
public class AiGenerateDocumentCommand {
    
    /**
     * 文书类型/用途描述
     * 例如：起诉状、答辩状、法律意见书、合同、律师函等
     */
    @NotBlank(message = "请输入文书类型")
    private String documentType;
    
    /**
     * 详细需求描述
     * 用户输入的文书需求，AI 根据此内容生成文书
     */
    @NotBlank(message = "请输入文书需求描述")
    private String requirement;
    
    /**
     * 项目ID（可选）
     * 如果关联项目，会自动填充项目相关信息
     */
    private Long matterId;
    
    /**
     * 卷宗目录项ID（可选）
     * 指定保存到哪个卷宗文件夹
     */
    private Long dossierItemId;
    
    /**
     * 文件名称（可选）
     */
    private String fileName;
    
    /**
     * 是否仅预览（不保存）
     */
    private Boolean previewOnly = false;
    
    /**
     * 额外上下文信息（可选）
     * 可以提供案件背景、当事人信息等补充信息
     */
    private String additionalContext;
    
    /**
     * 语气风格（可选）
     * 例如：正式、温和、强硬等
     */
    private String tone;

    /**
     * AI 集成配置 ID（可选）
     * 指定使用哪个 AI 大模型，不指定则使用默认启用的模型
     */
    private Long aiIntegrationId;

    // ========== 新增：项目上下文相关字段 ==========

    /**
     * 是否使用收集的项目上下文
     * 如果为 true，将使用 matterContext 字段中的完整项目信息
     */
    private Boolean useMatterContext = false;

    /**
     * 收集的项目上下文信息
     * 包含项目、客户、参与人、文档等完整信息
     */
    private MatterContextDTO matterContext;

    /**
     * 是否对数据进行脱敏
     * 如果使用本地部署的大模型，可以不脱敏
     */
    private Boolean enableMasking = true;
}

