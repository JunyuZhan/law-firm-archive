/**
 * 证据管理模块类型定义
 */

export interface EvidenceDTO {
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
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  fileSizeDisplay?: string;
  fileType?: string;  // 文件类型：image, pdf, word, excel, video, audio, other
  thumbnailUrl?: string;  // 缩略图URL（仅图片文件）
  submitterId?: number;
  submitterName?: string;
  submitTime?: string;
  status?: string;
  statusName?: string;
  sortOrder?: number;
  createdAt?: string;
}

export interface EvidenceQuery {
  evidenceNo?: string;
  name?: string;
  matterId?: number;
  evidenceType?: string;
  status?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateEvidenceCommand {
  name: string;
  matterId: number;
  evidenceType: string;
  source?: string;
  groupName?: string;
  provePurpose?: string;
  description?: string;
  isOriginal?: boolean;
  originalCount?: number;
  copyCount?: number;
  pageStart?: number;
  pageEnd?: number;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
}

export interface UpdateEvidenceCommand {
  name?: string;
  evidenceType?: string;
  source?: string;
  groupName?: string;
  provePurpose?: string;
  description?: string;
  isOriginal?: boolean;
  originalCount?: number;
  copyCount?: number;
  pageStart?: number;
  pageEnd?: number;
}

