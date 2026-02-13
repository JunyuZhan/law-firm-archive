package com.lawfirm.application.finance.service;

import com.lawfirm.application.document.service.FileAccessService;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.repository.ContractRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 合同文件服务
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractFileService {

  /** 合同仓储 */
  private final ContractRepository contractRepository;

  /** 文件访问服务 */
  private final FileAccessService fileAccessService;

  /**
   * 上传合同文件
   *
   * @param file 文件
   * @param contractId 合同ID
   * @return 文件URL
   */
  @Transactional
  public String uploadContractFile(final MultipartFile file, final Long contractId) {
    Contract contract = contractRepository.getByIdOrThrow(contractId, "合同不存在");

    // 使用FileAccessService上传文件
    Map<String, String> storageInfo =
        fileAccessService.uploadFile(
            file, MinioPathGenerator.FileType.CONTRACT, contract.getMatterId(), "合同文件");

    // 设置存储信息
    contract.setFileUrl(storageInfo.get("fileUrl"));
    contract.setBucketName(storageInfo.get("bucketName"));
    contract.setStoragePath(storageInfo.get("storagePath"));
    contract.setPhysicalName(storageInfo.get("physicalName"));
    contract.setFileHash(storageInfo.get("fileHash"));

    contractRepository.updateById(contract);

    log.info(
        "合同文件上传成功: contractId={}, fileName={}, storagePath={}",
        contractId,
        file.getOriginalFilename(),
        storageInfo.get("storagePath"));

    return storageInfo.get("fileUrl");
  }

  /**
   * 设置文件存储信息（新字段） 如果fileUrl是MinIO URL，尝试解析并设置新字段；否则只设置fileUrl（向后兼容）
   *
   * @param contract 合同实体
   * @param fileUrl 文件URL
   */
  public void setFileStorageInfo(final Contract contract, final String fileUrl) {
    contract.setFileUrl(fileUrl);

    // 尝试从URL解析存储信息
    Map<String, String> storageInfo = fileAccessService.parseStorageInfoFromUrl(fileUrl);
    if (storageInfo != null) {
      contract.setBucketName(storageInfo.get("bucketName"));
      contract.setStoragePath(storageInfo.get("storagePath"));
      contract.setPhysicalName(storageInfo.get("physicalName"));
      // fileHash无法从URL解析，保持为null
      log.debug(
          "从URL解析存储信息成功: contractId={}, storagePath={}",
          contract.getId(),
          storageInfo.get("storagePath"));
    } else {
      // 不是MinIO URL或无法解析，只设置fileUrl（向后兼容）
      log.debug("无法从URL解析存储信息，仅设置fileUrl: fileUrl={}", fileUrl);
    }
  }
}
