/**
 * 证据管理组件类型定义
 */

export interface EvidenceItem {
  id: number;
  evidenceNo: string;
  name: string;
  matterId?: number;
  matterName?: string;
  evidenceType?: string;
  evidenceTypeName?: string;
  source?: string;
  groupName?: string;
  provePurpose?: string;
  description?: string;
  isOriginal?: boolean;
  originalCount?: number;
  copyCount?: number;
  pageStart?: number;
  pageEnd?: number;
  pageRange?: string;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  fileSizeDisplay?: string;
  fileType?: string;  // image, pdf, word, excel, video, audio, other
  thumbnailUrl?: string;
  crossExamStatus?: string;
  crossExamStatusName?: string;
  status?: string;
  sortOrder?: number;
  createdAt?: string;
  createdBy?: number;
  createdByName?: string;
}

export interface EvidenceGroup {
  key: string;
  title: string;
  count: number;
  children?: EvidenceGroup[];
}

export type ViewMode = 'grid' | 'list';

export interface FileTypeInfo {
  type: string;
  icon: string;
  color: string;
  canPreview: boolean;
}

// 文件类型映射
export const FILE_TYPE_MAP: Record<string, FileTypeInfo> = {
  image: { type: 'image', icon: 'FileImageOutlined', color: '#52c41a', canPreview: true },
  pdf: { type: 'pdf', icon: 'FilePdfOutlined', color: '#ff4d4f', canPreview: true },
  word: { type: 'word', icon: 'FileWordOutlined', color: '#1890ff', canPreview: true },
  excel: { type: 'excel', icon: 'FileExcelOutlined', color: '#52c41a', canPreview: true },
  ppt: { type: 'ppt', icon: 'FilePptOutlined', color: '#fa8c16', canPreview: true },
  video: { type: 'video', icon: 'VideoCameraOutlined', color: '#722ed1', canPreview: true },
  audio: { type: 'audio', icon: 'AudioOutlined', color: '#13c2c2', canPreview: true },
  other: { type: 'other', icon: 'FileOutlined', color: '#8c8c8c', canPreview: false },
};

// 证据类型选项
export const EVIDENCE_TYPE_OPTIONS = [
  { value: 'DOCUMENTARY', label: '书证' },
  { value: 'PHYSICAL', label: '物证' },
  { value: 'AUDIO_VISUAL', label: '视听资料' },
  { value: 'ELECTRONIC', label: '电子数据' },
  { value: 'WITNESS', label: '证人证言' },
  { value: 'EXPERT', label: '鉴定意见' },
  { value: 'INSPECTION', label: '勘验笔录' },
];

// 获取文件类型信息
export function getFileTypeInfo(fileType?: string): FileTypeInfo {
  return FILE_TYPE_MAP[fileType || 'other'] ?? FILE_TYPE_MAP.other!;
}

// 获取文件图标（emoji版本，用于简单展示）
export function getFileEmoji(fileType?: string): string {
  const emojiMap: Record<string, string> = {
    image: '🖼️',
    pdf: '📄',
    word: '📝',
    excel: '📊',
    ppt: '📽️',
    video: '🎥',
    audio: '🎵',
    other: '📎',
  };
  return emojiMap[fileType || 'other'] || '📎';
}

// 格式化文件大小
export function formatFileSize(size?: number): string {
  if (!size) return '-';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
}
