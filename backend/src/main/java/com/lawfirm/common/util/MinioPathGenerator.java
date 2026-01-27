package com.lawfirm.common.util;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * MinIO 路径生成器
 * 统一生成标准化存储路径和文件名
 */
@Slf4j
public class MinioPathGenerator {
    
    /**
     * 文件类型常量
     */
    public static class FileType {
        public static final String MATTERS = "matters";      // 项目文档
        public static final String EVIDENCE = "evidence";    // 证据文件
        public static final String DOSSIER = "dossier";      // 卷宗文件
        public static final String PERSONAL = "personal";    // 个人文档
        public static final String REPORTS = "reports";      // 报表文件
        public static final String CONTRACT = "contracts";   // 合同文件
        public static final String EXPENSE = "expense";      // 费用凭证
        public static final String INVOICE = "invoice";      // 发票文件
        public static final String APPROVAL = "approval";    // 审批附件
        public static final String SEAL = "seal";            // 用印附件
        public static final String TASK = "tasks";           // 任务附件
    }
    public static String generateStandardPath(String fileType, Long matterId, String folder) {
        if (fileType == null || fileType.isEmpty()) {
            throw new IllegalArgumentException("fileType不能为空");
        }
        StringBuilder path = new StringBuilder();
        path.append(fileType).append("/");
        
        // 审批附件需要特殊处理：包含业务类型
        if (FileType.APPROVAL.equals(fileType) && folder != null && !folder.isEmpty()) {
            // folder参数在审批场景中代表businessType
            path.append(sanitizeFolderName(folder)).append("/");
            if (matterId != null) {
                path.append("M_").append(matterId).append("/");
            }
        } else {
            // 其他文件类型：先项目ID，再年月，再文件夹
            if (matterId != null) {
                path.append("M_").append(matterId).append("/");
            }
            String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            path.append(yearMonth).append("/");
            if (folder != null && !folder.isEmpty()) {
                path.append(sanitizeFolderName(folder)).append("/");
            }
        }
        
        return path.toString();
    }
    
    /**
     * 生成审批附件的存储路径
     * 格式：approval/{businessType}/M_{matterId}/{YYYY-MM}/审批附件/
     */
    public static String generateApprovalPath(String businessType, Long matterId) {
        if (businessType == null || businessType.isEmpty()) {
            throw new IllegalArgumentException("businessType不能为空");
        }
        StringBuilder path = new StringBuilder();
        path.append(FileType.APPROVAL).append("/");
        path.append(sanitizeFolderName(businessType)).append("/");
        if (matterId != null) {
            path.append("M_").append(matterId).append("/");
        }
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        path.append(yearMonth).append("/");
        path.append("审批附件").append("/");
        return path.toString();
    }

    /**
     * 生成兼容路径（与现有buildStoragePath逻辑完全一致）
     * 格式：matters/{matterId}/{folder}/ 或 personal/{userId}/{folder}/
     * 
     * 注意：此方法必须与 DocumentAppService.buildStoragePath() 逻辑完全一致
     */
    public static String generateCompatiblePath(Long matterId, String folder) {
        StringBuilder path = new StringBuilder();
        if (matterId != null) {
            path.append("matters/").append(matterId).append("/");
        } else {
            // 个人文档：需要从SecurityUtils获取userId
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new IllegalArgumentException("项目ID和用户ID不能同时为空");
            }
            path.append("personal/").append(userId).append("/");
        }
        if (folder != null && !folder.isEmpty() && !"root".equals(folder)) {
            path.append(folder).append("/");
        }
        return path.toString();
    }

    public static String generatePersonalPath(Long userId, String folder) {
        if (userId == null) {
            throw new IllegalArgumentException("userId不能为空");
        }
        StringBuilder path = new StringBuilder();
        path.append("personal/").append("U_").append(userId).append("/");
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        path.append(yearMonth).append("/");
        path.append(sanitizeFolderName(folder)).append("/");
        return path.toString();
    }

    public static String generatePhysicalName(String originalFilename) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String cleanFilename = sanitizeFilename(originalFilename);
        return String.format("%s_%s_%s", datePrefix, shortUuid, cleanFilename);
    }

    public static String buildObjectName(String storagePath, String physicalName) {
        return storagePath + physicalName;
    }

    /**
     * 验证存储路径是否包含项目ID（M_{matterId}）
     * 
     * 规则：
     * 1. 项目相关文件（matters, evidence, dossier）必须包含 M_{matterId}
     * 2. 个人文档（personal）必须包含 U_{userId} 或 personal/{userId}/
     * 3. 报表文件（reports）可以不包含项目ID
     * 
     * @param storagePath 存储路径
     * @param fileType 文件类型
     * @param matterId 项目ID（如果应该包含）
     * @return true 如果路径格式正确
     * @throws IllegalArgumentException 如果路径格式不正确
     */
    public static void validateStoragePath(String storagePath, String fileType, Long matterId) {
        if (storagePath == null || storagePath.isEmpty()) {
            throw new IllegalArgumentException("存储路径不能为空");
        }
        
        // 个人文档和报表可以不包含项目ID
        if (FileType.PERSONAL.equals(fileType) || FileType.REPORTS.equals(fileType)) {
            return; // 个人文档和报表不强制要求项目ID
        }
        
        // 审批附件可以不包含项目ID（某些审批可能不关联项目）
        if (FileType.APPROVAL.equals(fileType)) {
            return; // 审批附件不强制要求项目ID
        }
        
        // 项目相关文件必须包含项目ID
        if (matterId == null) {
            throw new IllegalArgumentException("项目相关文件必须指定项目ID");
        }
        
        // 验证路径中是否包含 M_{matterId}
        String expectedPattern = "M_" + matterId;
        if (!storagePath.contains(expectedPattern)) {
            throw new IllegalArgumentException(
                String.format("存储路径格式错误：路径 '%s' 必须包含项目ID '%s'", storagePath, expectedPattern)
            );
        }
        
        // 额外验证：确保路径格式正确（包含文件类型前缀）
        if (!storagePath.startsWith(fileType + "/")) {
            throw new IllegalArgumentException(
                String.format("存储路径格式错误：路径 '%s' 必须以 '%s/' 开头", storagePath, fileType)
            );
        }
    }

    private static String sanitizeFolderName(String folder) {
        if (folder == null || folder.isEmpty() || "root".equals(folder)) {
            return "others";
        }
        String clean = folder.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
        if (clean.length() > 50) {
            clean = clean.substring(0, 50);
        }
        return clean;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unknown";
        }
        String clean = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        int maxLength = 200;
        if (clean.length() > maxLength) {
            int lastDot = clean.lastIndexOf('.');
            if (lastDot > 0) {
                String name = clean.substring(0, lastDot);
                String ext = clean.substring(lastDot);
                int maxNameLen = Math.max(1, maxLength - ext.length());
                if (name.length() > maxNameLen) {
                    name = name.substring(0, maxNameLen);
                }
                clean = name + ext;
            } else {
                clean = clean.substring(0, maxLength);
            }
        }
        return clean;
    }
}
