package com.lawfirm.interfaces.rest.evidence;

import com.lawfirm.application.evidence.command.CreateCrossExamCommand;
import com.lawfirm.application.evidence.command.CreateEvidenceCommand;
import com.lawfirm.application.evidence.command.UpdateEvidenceCommand;
import com.lawfirm.application.evidence.dto.EvidenceCrossExamDTO;
import com.lawfirm.application.evidence.dto.EvidenceDTO;
import com.lawfirm.application.evidence.dto.EvidenceQueryDTO;
import com.lawfirm.application.evidence.service.EvidenceAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.document.DocumentContentExtractor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 证据管理接口
 */
@Slf4j
@RestController
@RequestMapping("/evidence")
@RequiredArgsConstructor
public class EvidenceController {

    private final EvidenceAppService evidenceAppService;
    private final MinioService minioService;
    private final DocumentContentExtractor documentContentExtractor;

    /**
     * 分页查询证据
     */
    @GetMapping
    @RequirePermission("evidence:view")
    public Result<PageResult<EvidenceDTO>> list(EvidenceQueryDTO query) {
        return Result.success(evidenceAppService.listEvidence(query));
    }

    /**
     * 获取证据详情
     */
    @GetMapping("/{id}")
    @RequirePermission("evidence:view")
    public Result<EvidenceDTO> getById(@PathVariable Long id) {
        return Result.success(evidenceAppService.getEvidenceById(id));
    }

    /**
     * 创建证据
     */
    @PostMapping
    @RequirePermission("evidence:create")
    @OperationLog(module = "证据管理", action = "添加证据")
    public Result<EvidenceDTO> create(@Valid @RequestBody CreateEvidenceCommand command) {
        return Result.success(evidenceAppService.createEvidence(command));
    }

    /**
     * 更新证据
     */
    @PutMapping("/{id}")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据管理", action = "更新证据")
    public Result<EvidenceDTO> update(@PathVariable Long id,
                                      @Valid @RequestBody UpdateEvidenceCommand command) {
        return Result.success(evidenceAppService.updateEvidence(id, command));
    }

    /**
     * 删除证据
     */
    @DeleteMapping("/{id}")
    @RequirePermission("evidence:delete")
    @OperationLog(module = "证据管理", action = "删除证据")
    public Result<Void> delete(@PathVariable Long id) {
        evidenceAppService.deleteEvidence(id);
        return Result.success();
    }

    /**
     * 调整排序
     */
    @PutMapping("/{id}/sort")
    @RequirePermission("evidence:edit")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        evidenceAppService.updateSortOrder(id, sortOrder);
        return Result.success();
    }

    /**
     * 批量调整分组
     */
    @PostMapping("/batch-group")
    @RequirePermission("evidence:edit")
    @OperationLog(module = "证据管理", action = "批量调整分组")
    public Result<Void> batchUpdateGroup(@RequestBody List<Long> ids, @RequestParam String groupName) {
        evidenceAppService.batchUpdateGroup(ids, groupName);
        return Result.success();
    }

    /**
     * 添加质证记录
     */
    @PostMapping("/{id}/cross-exam")
    @RequirePermission("evidence:crossExam")
    @OperationLog(module = "证据管理", action = "添加质证记录")
    public Result<EvidenceCrossExamDTO> addCrossExam(@PathVariable Long id,
                                                     @Valid @RequestBody CreateCrossExamCommand command) {
        command.setEvidenceId(id);
        return Result.success(evidenceAppService.addCrossExam(command));
    }

    /**
     * 完成质证
     */
    @PostMapping("/{id}/complete-cross-exam")
    @RequirePermission("evidence:crossExam")
    @OperationLog(module = "证据管理", action = "完成质证")
    public Result<Void> completeCrossExam(@PathVariable Long id) {
        evidenceAppService.completeCrossExam(id);
        return Result.success();
    }

    /**
     * 按案件获取证据列表
     */
    @GetMapping("/matter/{matterId}")
    @RequirePermission("evidence:view")
    public Result<List<EvidenceDTO>> getByMatter(@PathVariable Long matterId) {
        return Result.success(evidenceAppService.getEvidenceByMatter(matterId));
    }

    /**
     * 获取案件的证据分组
     */
    @GetMapping("/matter/{matterId}/groups")
    @RequirePermission("evidence:view")
    public Result<List<String>> getGroups(@PathVariable Long matterId) {
        return Result.success(evidenceAppService.getEvidenceGroups(matterId));
    }

    /**
     * 上传证据文件
     */
    @PostMapping("/upload")
    @RequirePermission("evidence:create")
    @OperationLog(module = "证据管理", action = "上传证据文件")
    public Result<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = minioService.uploadFile(file, "evidence/");
            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            return Result.success(result);
        } catch (Exception e) {
            throw new com.lawfirm.common.exception.BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件预览URL
     */
    @GetMapping("/{id}/preview")
    @RequirePermission("evidence:view")
    public Result<Map<String, Object>> getPreviewUrl(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("fileUrl", evidence.getFileUrl());
        result.put("fileName", evidence.getFileName());
        result.put("fileType", getFileType(evidence.getFileName()));
        result.put("canPreview", canPreview(evidence.getFileName()));
        return Result.success(result);
    }

    /**
     * 获取文件缩略图URL
     */
    @GetMapping("/{id}/thumbnail")
    @RequirePermission("evidence:view")
    public Result<Map<String, Object>> getThumbnailUrl(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        Map<String, Object> result = new HashMap<>();
        String fileName = evidence.getFileName();
        if (isImageFile(fileName)) {
            // 图片文件直接返回原文件URL作为缩略图
            result.put("thumbnailUrl", evidence.getFileUrl());
        } else {
            // 非图片文件返回null，前端显示默认图标
            result.put("thumbnailUrl", null);
        }
        result.put("fileType", getFileType(fileName));
        return Result.success(result);
    }

    /**
     * 判断文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") 
            || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp")) {
            return "image";
        } else if (lowerName.endsWith(".pdf")) {
            return "pdf";
        } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "word";
        } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return "excel";
        } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") || lowerName.endsWith(".mov")) {
            return "video";
        } else if (lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a")) {
            return "audio";
        } else {
            return "other";
        }
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") 
            || lowerName.endsWith(".gif") || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp");
    }

    /**
     * 判断是否可以预览
     */
    private boolean canPreview(String fileName) {
        String fileType = getFileType(fileName);
        return "image".equals(fileType) || "pdf".equals(fileType);
    }

    /**
     * 获取文件文本内容（用于Word/Excel等文档预览）
     */
    @GetMapping("/{id}/content")
    @RequirePermission("evidence:view")
    @OperationLog(module = "证据管理", action = "获取文件内容")
    public Result<Map<String, Object>> getFileContent(@PathVariable Long id) {
        EvidenceDTO evidence = evidenceAppService.getEvidenceById(id);
        if (evidence.getFileUrl() == null) {
            throw new com.lawfirm.common.exception.BusinessException("该证据没有关联文件");
        }
        
        Map<String, Object> result = new HashMap<>();
        String fileType = getFileType(evidence.getFileName());
        
        try {
            // Word/Excel文档：使用Apache POI提取文本内容
            if ("word".equals(fileType) || "excel".equals(fileType)) {
                // 从MinIO下载文件
                String objectName = minioService.extractObjectName(evidence.getFileUrl());
                if (objectName == null) {
                    result.put("content", "无法解析文件URL");
                    result.put("supported", false);
                    return Result.success(result);
                }
                
                byte[] fileBytes = minioService.downloadFileAsBytes(objectName);
                String content = documentContentExtractor.extractText(fileBytes, evidence.getFileName());
                
                result.put("content", content);
                result.put("supported", true);
            } else if ("pdf".equals(fileType)) {
                // PDF文本提取可以通过OCR服务实现，暂时返回提示
                result.put("content", "PDF文本提取功能需要OCR服务支持，当前版本暂不支持。");
                result.put("supported", false);
            } else {
                result.put("content", "");
                result.put("supported", false);
            }
        } catch (Exception e) {
            log.error("提取文件内容失败", e);
            result.put("content", "文档内容提取失败: " + e.getMessage());
            result.put("supported", false);
        }
        
        return Result.success(result);
    }
}
