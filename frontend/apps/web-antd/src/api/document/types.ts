/**
 * 文档管理模块类型定义
 */

export interface DocumentDTO {
  id: number;
  name: string;
  title?: string;
  matterId?: number;
  matterName?: string;
  categoryId?: number;
  categoryName?: string;
  filePath?: string;
  fileName?: string;
  fileSize?: number;
  fileType?: string;
  version?: number;
  uploaderId?: number;
  uploaderName?: string;
  uploadTime?: string;
  description?: string;
  createdAt?: string;
  /** 文件分类（evidence/contract/litigation/other） */
  fileCategory?: string;
  /** 卷宗目录路径 */
  folderPath?: string;
  /** 关联的卷宗目录项ID */
  dossierItemId?: number;
  /** 显示排序顺序 */
  displayOrder?: number;
  /** 缩略图URL（图片和PDF文件） */
  thumbnailUrl?: string;
  /**
   * MinIO桶名称，默认law-firm
   */
  bucketName?: string;
  /**
   * 存储路径：document/{matterId}/{folderPath}/
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
  /** 文档来源类型: SYSTEM_GENERATED, SYSTEM_LINKED, USER_UPLOADED, SIGNED_VERSION */
  sourceType?:
    | 'SIGNED_VERSION'
    | 'SYSTEM_GENERATED'
    | 'SYSTEM_LINKED'
    | 'USER_UPLOADED';
  /** 文档来源类型名称 */
  sourceTypeName?: string;
  /** 来源数据ID */
  sourceId?: number;
  /** 来源模块: CONTRACT, APPROVAL, INVOICE, MATTER */
  sourceModule?: string;
}

/** 文档来源类型常量 */
export const SOURCE_TYPE_OPTIONS = [
  { color: 'blue', label: '系统生成', value: 'SYSTEM_GENERATED' },
  { color: 'cyan', label: '系统关联', value: 'SYSTEM_LINKED' },
  { color: 'default', label: '用户上传', value: 'USER_UPLOADED' },
  { color: 'green', label: '签字版本', value: 'SIGNED_VERSION' },
];

export interface DocumentQuery {
  name?: string;
  matterId?: number;
  categoryId?: number;
  uploaderId?: number;
  /** 创建时间开始 */
  createdAtFrom?: string;
  /** 创建时间结束 */
  createdAtTo?: string;
  pageNum?: number;
  pageSize?: number;
}

export interface CreateDocumentCommand {
  name: string;
  matterId?: number;
  categoryId?: number;
  filePath: string;
  fileName: string;
  fileSize: number;
  fileType?: string;
  description?: string;
}

export interface UpdateDocumentCommand {
  id: number;
  title?: string; // 文档标题（重命名）
  categoryId?: number;
  description?: string;
}

export interface UploadNewVersionCommand {
  documentId: number;
  filePath: string;
  fileName: string;
  fileSize: number;
  fileType?: string;
}
