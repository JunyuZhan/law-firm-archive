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
  fileType?: string; // 文件类型：image, pdf, word, excel, video, audio, other
  thumbnailUrl?: string; // 缩略图URL（仅图片文件）
  /**
   * MinIO桶名称，默认law-firm
   */
  bucketName?: string;
  /**
   * 存储路径：evidence/M_{matterId}/{YYYY-MM}/证据文件/
   */
  storagePath?: string;
  /**
   * 物理文件名：{YYYYMMDD}_{UUID}_{fileName}.{ext}
   */
  physicalName?: string;
  /**
   * 文件Hash值（SHA-256），用于去重和校验
   */
  fileHash?: string;
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
