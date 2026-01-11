/**
 * AI 文书生成 API
 */
import { requestClient } from '#/api/request';

// ========== 项目上下文相关类型 ==========

/**
 * 项目基本信息
 */
export interface MatterInfo {
  id?: number;
  matterNo?: string;
  name?: string;
  matterType?: string;
  caseType?: string;
  status?: string;
  description?: string;
  court?: string;
  caseNo?: string;
  opposingParty?: string;
  claimAmount?: string;
  disputeAmount?: string;
}

/**
 * 客户信息
 */
export interface ClientInfo {
  id?: number;
  name?: string;
  clientType?: string;
  creditCode?: string;
  idCard?: string;
  legalRepresentative?: string;
  contactPerson?: string;
  contactPhone?: string;
  contactEmail?: string;
  registeredAddress?: string;
  role?: string;
  isPrimary?: boolean;
}

/**
 * 参与人信息
 */
export interface ParticipantInfo {
  userId?: number;
  name?: string;
  role?: string;
  phone?: string;
  email?: string;
  lawyerLicenseNo?: string;
}

/**
 * 文档信息
 */
export interface DocumentInfo {
  id?: number;
  title?: string;
  fileName?: string;
  fileType?: string;
  category?: string;
  description?: string;
  content?: string;
  /** 是否支持内容提取 */
  extractable?: boolean;
}

/**
 * 项目上下文 DTO
 */
export interface MatterContextDTO {
  matter?: MatterInfo;
  clients?: ClientInfo[];
  participants?: ParticipantInfo[];
  documents?: DocumentInfo[];
  masked?: boolean;
}

/**
 * 脱敏映射条目
 */
export interface MaskingMappingEntry {
  fieldName: string;
  originalValue: string;
  maskedValue: string;
}

/**
 * 脱敏映射 DTO
 */
export interface MaskingMappingDTO {
  mappings: MaskingMappingEntry[];
}

/**
 * 收集选项
 */
export interface CollectOptions {
  includeMatterInfo?: boolean;
  includeClients?: boolean;
  includeParticipants?: boolean;
  includeDocuments?: boolean;
  extractDocumentContent?: boolean;
  selectedDocumentIds?: number[];
}

/**
 * 脱敏结果（带映射）
 */
export interface MaskingResultResponse {
  maskedContext: MatterContextDTO;
  mapping: MaskingMappingDTO;
}

// ========== AI 生成相关类型 ==========

/**
 * AI 生成文书命令
 */
export interface AiGenerateDocumentCommand {
  /** 文书类型 */
  documentType: string;
  /** 需求描述 */
  requirement: string;
  /** 项目ID（可选） */
  matterId?: number;
  /** 卷宗目录项ID（可选） */
  dossierItemId?: number;
  /** 文件名称（可选） */
  fileName?: string;
  /** 是否仅预览 */
  previewOnly?: boolean;
  /** 额外上下文 */
  additionalContext?: string;
  /** 语气风格 */
  tone?: string;
  /** AI 集成配置 ID（可选，指定使用哪个大模型） */
  aiIntegrationId?: number;
  /** 是否使用收集的项目上下文 */
  useMatterContext?: boolean;
  /** 收集的项目上下文信息 */
  matterContext?: MatterContextDTO;
  /** 是否对数据进行脱敏 */
  enableMasking?: boolean;
}

/**
 * AI 模型信息
 */
export interface AiModelInfo {
  id: number;
  name: string;
  code: string;
  description?: string;
  modelName?: string;
}

/**
 * AI 服务状态
 */
export interface AiStatus {
  available: boolean;
  message?: string;
  model?: string;
  code?: string;
  defaultId?: number;
  /** 所有可用的 AI 模型列表 */
  models?: AiModelInfo[];
}

/**
 * AI 生成结果
 */
export interface AiGenerateResult {
  content: string;
  preview: boolean;
  document?: any;
}

// AI 请求超时时间：6 分钟（DeepSeek R1 等推理模型需要较长时间）
const AI_TIMEOUT = 360_000;

/**
 * 检查 AI 服务状态
 */
export function checkAiStatus() {
  return requestClient.get<AiStatus>('/document/ai/status');
}

/**
 * AI 生成文书
 */
export function aiGenerateDocument(command: AiGenerateDocumentCommand) {
  return requestClient.post<AiGenerateResult>(
    '/document/ai/generate',
    command,
    {
      timeout: AI_TIMEOUT,
    },
  );
}

/**
 * AI 预览文书
 */
export function aiPreviewDocument(command: AiGenerateDocumentCommand) {
  return requestClient.post<string>('/document/ai/preview', command, {
    timeout: AI_TIMEOUT,
  });
}

// ========== 项目上下文收集 ==========

/**
 * 收集项目上下文信息
 */
export function collectMatterContext(
  matterId: number,
  includeDocuments = true,
  extractContent = false,
) {
  return requestClient.get<MatterContextDTO>(
    `/document/ai/context/${matterId}`,
    {
      params: { includeDocuments, extractContent },
    },
  );
}

/**
 * 选择性收集项目上下文
 */
export function collectSelectiveContext(
  matterId: number,
  options: CollectOptions,
) {
  return requestClient.post<MatterContextDTO>(
    `/document/ai/context/${matterId}/selective`,
    options,
  );
}

/**
 * 获取项目可选文档列表
 */
export function getAvailableDocuments(matterId: number) {
  return requestClient.get<DocumentInfo[]>(
    `/document/ai/context/${matterId}/documents`,
  );
}

// ========== 脱敏处理 ==========

/**
 * 脱敏项目上下文信息
 */
export function maskMatterContext(context: MatterContextDTO) {
  return requestClient.post<MatterContextDTO>(
    '/document/ai/context/mask',
    context,
  );
}

/**
 * 脱敏并返回映射关系（用于还原）
 */
export function maskWithMapping(context: MatterContextDTO) {
  return requestClient.post<MaskingResultResponse>(
    '/document/ai/context/mask-with-mapping',
    context,
  );
}

/**
 * 收集并脱敏项目上下文（一键操作）
 */
export function collectAndMaskContext(
  matterId: number,
  includeDocuments = true,
  extractContent = false,
) {
  return requestClient.get<MatterContextDTO>(
    `/document/ai/context/${matterId}/masked`,
    {
      params: { includeDocuments, extractContent },
    },
  );
}

/**
 * 收集并脱敏（带映射，用于还原）
 */
export function collectAndMaskWithMapping(
  matterId: number,
  options?: CollectOptions,
) {
  return requestClient.post<MaskingResultResponse>(
    `/document/ai/context/${matterId}/masked-with-mapping`,
    options || {},
  );
}

// ========== 脱敏还原 ==========

/**
 * 脱敏还原
 * 将 AI 生成的脱敏文书还原为包含真实信息的文书
 */
export function restoreMaskedText(
  maskedText: string,
  mapping: MaskingMappingDTO,
) {
  return requestClient.post<string>('/document/ai/restore', {
    maskedText,
    mapping,
  });
}
