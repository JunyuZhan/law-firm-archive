package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateSealCommand;
import com.lawfirm.application.document.command.UpdateSealCommand;
import com.lawfirm.application.document.dto.SealDTO;
import com.lawfirm.application.document.dto.SealQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.document.entity.Seal;
import com.lawfirm.domain.document.repository.SealApplicationRepository;
import com.lawfirm.domain.document.repository.SealRepository;
import com.lawfirm.infrastructure.persistence.mapper.SealMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 印章应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SealAppService {

  /** 印章仓储. */
  private final SealRepository sealRepository;

  /** 用印申请仓储. */
  private final SealApplicationRepository applicationRepository;

  /** 印章Mapper. */
  private final SealMapper sealMapper;

  /**
   * 分页查询印章。
   *
   * @param query 查询条件
   * @return 印章分页结果
   */
  public PageResult<SealDTO> listSeals(final SealQueryDTO query) {
    IPage<Seal> page =
        sealMapper.selectSealPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getName(),
            query.getSealType(),
            query.getKeeperId(),
            query.getStatus());

    List<SealDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 创建印章。
   *
   * @param command 创建印章命令
   * @return 创建的印章DTO
   */
  @Transactional
  public SealDTO createSeal(final CreateSealCommand command) {
    String sealNo = generateSealNo();

    Seal seal =
        Seal.builder()
            .sealNo(sealNo)
            .name(command.getName())
            .sealType(command.getSealType())
            .keeperId(command.getKeeperId())
            .keeperName(command.getKeeperName())
            .imageUrl(command.getImageUrl())
            .description(command.getDescription())
            .status("ACTIVE")
            .build();

    sealRepository.save(seal);
    log.info("印章创建成功: {} ({})", seal.getName(), seal.getSealNo());
    return toDTO(seal);
  }

  /**
   * 获取印章详情。
   *
   * @param id 印章ID
   * @return 印章DTO
   */
  public SealDTO getSealById(final Long id) {
    Seal seal = sealRepository.getByIdOrThrow(id, "印章不存在");
    SealDTO dto = toDTO(seal);
    dto.setUsageCount(applicationRepository.countUsageBySealId(id));
    return dto;
  }

  /**
   * 更新印章。
   *
   * @param id 印章ID
   * @param command 更新印章命令
   * @return 更新后的印章DTO
   */
  @Transactional
  public SealDTO updateSeal(final Long id, final UpdateSealCommand command) {
    Seal seal = sealRepository.getByIdOrThrow(id, "印章不存在");

    if (StringUtils.hasText(command.getName())) {
      seal.setName(command.getName());
    }
    if (command.getKeeperId() != null) {
      seal.setKeeperId(command.getKeeperId());
    }
    if (command.getKeeperName() != null) {
      seal.setKeeperName(command.getKeeperName());
    }
    if (command.getImageUrl() != null) {
      seal.setImageUrl(command.getImageUrl());
    }
    if (command.getDescription() != null) {
      seal.setDescription(command.getDescription());
    }

    sealRepository.updateById(seal);
    log.info("印章更新成功: {}", seal.getName());
    return toDTO(seal);
  }

  /**
   * 变更印章状态。
   *
   * @param id 印章ID
   * @param status 新状态
   */
  @Transactional
  public void changeSealStatus(final Long id, final String status) {
    Seal seal = sealRepository.getByIdOrThrow(id, "印章不存在");

    // 验证状态转换
    if ("DESTROYED".equals(seal.getStatus())) {
      throw new BusinessException("已销毁的印章无法变更状态");
    }

    seal.setStatus(status);
    sealRepository.updateById(seal);
    log.info("印章状态变更成功: {} -> {}", seal.getName(), status);
  }

  /**
   * 删除印章。
   *
   * @param id 印章ID
   */
  @Transactional
  public void deleteSeal(final Long id) {
    Seal seal = sealRepository.getByIdOrThrow(id, "印章不存在");

    // 检查是否有待处理的用印申请
    int pendingCount = applicationRepository.countPendingBySealId(id);
    if (pendingCount > 0) {
      throw new BusinessException("该印章存在待处理或已批准的用印申请，无法删除");
    }

    sealRepository.removeById(id);
    log.info("印章删除成功: {}", seal.getName());
  }

  /**
   * 生成印章编号。
   *
   * @return 印章编号
   */
  private String generateSealNo() {
    String prefix = "SEAL" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  /**
   * 获取印章类型名称。
   *
   * @param type 印章类型代码
   * @return 印章类型名称
   */
  private String getSealTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "OFFICIAL" -> "公章";
      case "CONTRACT" -> "合同章";
      case "FINANCE" -> "财务章";
      case "LEGAL" -> "法人章";
      case "OTHER" -> "其他";
      default -> type;
    };
  }

  /**
   * 获取状态名称。
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "ACTIVE" -> "在用";
      case "DISABLED" -> "停用";
      case "LOST" -> "遗失";
      case "DESTROYED" -> "销毁";
      default -> status;
    };
  }

  /**
   * Entity 转 DTO。
   *
   * @param seal 印章实体
   * @return 印章DTO
   */
  private SealDTO toDTO(final Seal seal) {
    SealDTO dto = new SealDTO();
    dto.setId(seal.getId());
    dto.setSealNo(seal.getSealNo());
    dto.setName(seal.getName());
    dto.setSealType(seal.getSealType());
    dto.setSealTypeName(getSealTypeName(seal.getSealType()));
    dto.setKeeperId(seal.getKeeperId());
    dto.setKeeperName(seal.getKeeperName());
    dto.setImageUrl(seal.getImageUrl());
    dto.setStatus(seal.getStatus());
    dto.setStatusName(getStatusName(seal.getStatus()));
    dto.setDescription(seal.getDescription());
    dto.setCreatedAt(seal.getCreatedAt());
    dto.setUpdatedAt(seal.getUpdatedAt());
    return dto;
  }
}
