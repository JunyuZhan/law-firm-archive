/**
 * 文书制作模块类型定义
 */
import type { MatterDTO } from '#/api/matter/types';
import type { DocumentTemplateDTO } from '#/api/document/template-types';
import type { 
  MatterContextDTO, 
  MaskingMappingDTO, 
  DocumentInfo, 
  CollectOptions,
  AiModelInfo,
} from '#/api/document/ai';

/** 制作模式 */
export type ComposeMode = 'template' | 'ai';

/** 卷宗目录项 */
export interface MatterDossierItem {
  id: number;
  matterId: number;
  parentId: number;
  name: string;
  itemType: string;
  fileType?: string;
  filePath?: string;
  fileSize?: number;
  sortOrder?: number;
}

/** 模板表单数据 */
export interface TemplateFormData {
  variables: Record<string, string>;
  fileName: string;
}

/** AI 表单数据 */
export interface AiFormData {
  documentType: string;
  requirement: string;
  fileName: string;
  additionalContext: string;
  tone: string;
}

/** AI 状态 */
export interface AiStatus {
  available: boolean;
  model?: string;
  message?: string;
  defaultId?: number;
  models?: AiModelInfo[];
}

/** 项目选择器 Props */
export interface MatterSelectorProps {
  selectedMatterId?: number;
  isPersonalDoc: boolean;
  selectedDossierId?: number;
}

/** 项目选择器 Emits */
export interface MatterSelectorEmits {
  (e: 'update:selectedMatterId', value: number | undefined): void;
  (e: 'update:isPersonalDoc', value: boolean): void;
  (e: 'update:selectedDossierId', value: number | undefined): void;
  (e: 'matterChange', matter: MatterDTO | null): void;
}

/** 上下文收集器 Props */
export interface ContextCollectorProps {
  matterId: number;
  matterContext: MatterContextDTO | null;
  enableMasking: boolean;
}

/** 上下文收集器 Emits */
export interface ContextCollectorEmits {
  (e: 'update:matterContext', value: MatterContextDTO | null): void;
  (e: 'update:enableMasking', value: boolean): void;
  (e: 'contextCollected', context: MatterContextDTO, masked: MatterContextDTO | null, mapping: MaskingMappingDTO | null): void;
}

/** 模板模式 Props */
export interface TemplateModeProps {
  templates: DocumentTemplateDTO[];
  loading: boolean;
}

/** 模板模式 Emits */
export interface TemplateModeEmits {
  (e: 'success'): void;
  (e: 'reset'): void;
}

/** AI 模式 Props */
export interface AiModeProps {
  aiStatus: AiStatus;
  availableModels: AiModelInfo[];
}

/** AI 模式 Emits */
export interface AiModeEmits {
  (e: 'success'): void;
  (e: 'reset'): void;
}

/** 变量名映射 */
export const VARIABLE_NAME_MAP: Record<string, string> = {
  'matter.name': '项目名称',
  'matter.no': '项目编号',
  'client.name': '客户名称',
  'client.contact': '联系人',
  'client.phone': '联系电话',
  'client.address': '客户地址',
  'user.name': '当前用户',
  'firm.name': '律所名称',
  'firm.address': '律所地址',
  'date': '日期',
  'today': '今日日期',
};

/** 文书类型选项 */
export const DOCUMENT_TYPE_OPTIONS = [
  { label: '起诉状', value: '起诉状' },
  { label: '答辩状', value: '答辩状' },
  { label: '上诉状', value: '上诉状' },
  { label: '法律意见书', value: '法律意见书' },
  { label: '律师函', value: '律师函' },
  { label: '合同', value: '合同' },
  { label: '授权委托书', value: '授权委托书' },
  { label: '代理词', value: '代理词' },
  { label: '辩护词', value: '辩护词' },
  { label: '其他', value: '其他' },
];

/** 语气风格选项 */
export const TONE_OPTIONS = [
  { label: '正式严谨', value: '正式严谨' },
  { label: '温和友好', value: '温和友好' },
  { label: '强硬有力', value: '强硬有力' },
  { label: '简洁明了', value: '简洁明了' },
];

/** 模板类型映射 */
export const TEMPLATE_TYPE_MAP: Record<string, string> = {
  WORD: 'Word文档',
  EXCEL: 'Excel表格',
  PDF: 'PDF文档',
};

/** 业务类型选项 */
export const BUSINESS_TYPE_OPTIONS = [
  { label: '全部', value: '' },
  { label: '诉讼文书', value: 'LITIGATION' },
  { label: '非诉文书', value: 'NON_LITIGATION' },
  { label: '合同文书', value: 'CONTRACT' },
  { label: '函件文书', value: 'LETTER' },
  { label: '其他', value: 'OTHER' },
];

