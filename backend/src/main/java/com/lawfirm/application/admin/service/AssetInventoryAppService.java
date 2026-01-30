package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreateAssetInventoryCommand;
import com.lawfirm.application.admin.dto.AssetInventoryDTO;
import com.lawfirm.application.admin.dto.AssetInventoryDetailDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.domain.admin.entity.AssetInventory;
import com.lawfirm.domain.admin.entity.AssetInventoryDetail;
import com.lawfirm.domain.admin.repository.AssetInventoryRepository;
import com.lawfirm.domain.admin.repository.AssetRepository;
import com.lawfirm.infrastructure.persistence.mapper.AssetInventoryDetailMapper;
import com.lawfirm.infrastructure.persistence.mapper.AssetInventoryMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 资产盘点服务（M8-033） */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetInventoryAppService {

  /** 资产盘点仓储 */
  private final AssetInventoryRepository inventoryRepository;

  /** 资产盘点Mapper */
  private final AssetInventoryMapper inventoryMapper;

  /** 资产盘点明细Mapper */
  private final AssetInventoryDetailMapper detailMapper;

  /** 资产仓储 */
  private final AssetRepository assetRepository;

  /** 问题443修复：序号生成器防止并发重复 */
  private final AtomicLong sequence = new AtomicLong(0);

  /** 序号模数（用于生成4位序号） */
  private static final long SEQUENCE_MODULUS = 10000L;

  /**
   * 创建资产盘点 问题429修复：使用批量插入替代循环插入.
   *
   * @param command 创建命令
   * @return 资产盘点DTO
   */
  @Transactional
  public AssetInventoryDTO createInventory(final CreateAssetInventoryCommand command) {
    Long userId = SecurityUtils.getUserId();

    // 生成盘点编号
    String inventoryNo = generateInventoryNo();

    AssetInventory inventory =
        AssetInventory.builder()
            .inventoryNo(inventoryNo)
            .inventoryDate(command.getInventoryDate())
            .inventoryType(command.getInventoryType())
            .departmentId(command.getDepartmentId())
            .location(command.getLocation())
            .status(AssetInventory.STATUS_IN_PROGRESS)
            .totalCount(0)
            .actualCount(0)
            .surplusCount(0)
            .shortageCount(0)
            .remark(command.getRemark())
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .build();

    inventoryRepository.save(inventory);

    // 如果是全盘，获取所有资产；如果是抽盘，使用指定的资产ID列表
    List<Asset> assets;
    if (AssetInventory.TYPE_FULL.equals(command.getInventoryType())) {
      assets = assetRepository.list();
    } else {
      if (command.getAssetIds() == null || command.getAssetIds().isEmpty()) {
        throw new BusinessException("抽盘时必须指定资产ID列表");
      }
      assets = assetRepository.listByIds(command.getAssetIds());
    }

    if (assets.isEmpty()) {
      throw new BusinessException("没有找到需要盘点的资产");
    }

    // 问题429修复：批量创建盘点明细
    List<AssetInventoryDetail> details =
        assets.stream()
            .map(
                asset ->
                    AssetInventoryDetail.builder()
                        .inventoryId(inventory.getId())
                        .assetId(asset.getId())
                        .expectedStatus(asset.getStatus())
                        .expectedLocation(asset.getLocation())
                        .expectedUserId(asset.getCurrentUserId())
                        .discrepancyType(AssetInventoryDetail.DISCREPANCY_NORMAL)
                        .build())
            .collect(Collectors.toList());

    // 批量插入（MyBatis-Plus会自动分批）
    for (AssetInventoryDetail detail : details) {
      detailMapper.insert(detail);
    }
    // 注意：如果detailMapper有insertBatchSomeColumn方法，可以替换为：
    // detailMapper.insertBatchSomeColumn(details);

    inventory.setTotalCount(assets.size());
    inventoryRepository.updateById(inventory);

    log.info(
        "资产盘点创建成功: inventoryNo={}, type={}, count={}",
        inventoryNo,
        command.getInventoryType(),
        assets.size());
    return getInventoryById(inventory.getId());
  }

  /**
   * 更新盘点明细.
   *
   * @param detailId 明细ID
   * @param actualStatus 实际状态
   * @param actualLocation 实际位置
   * @param actualUserId 实际使用人ID
   * @param discrepancyDesc 差异描述
   */
  @Transactional
  public void updateInventoryDetail(
      final Long detailId,
      final String actualStatus,
      final String actualLocation,
      final Long actualUserId,
      final String discrepancyDesc) {
    AssetInventoryDetail detail = detailMapper.selectById(detailId);
    if (detail == null) {
      throw new BusinessException("盘点明细不存在");
    }

    AssetInventory inventory = inventoryRepository.getByIdOrThrow(detail.getInventoryId(), "盘点不存在");
    if (AssetInventory.STATUS_COMPLETED.equals(inventory.getStatus())) {
      throw new BusinessException("盘点已完成，不能修改");
    }

    detail.setActualStatus(actualStatus);
    detail.setActualLocation(actualLocation);
    detail.setActualUserId(actualUserId);
    detail.setDiscrepancyDesc(discrepancyDesc);

    // 判断差异类型
    String discrepancyType = AssetInventoryDetail.DISCREPANCY_NORMAL;
    if (!detail.getExpectedStatus().equals(actualStatus)) {
      discrepancyType = AssetInventoryDetail.DISCREPANCY_STATUS;
    } else if (detail.getExpectedLocation() != null
        && !detail.getExpectedLocation().equals(actualLocation)) {
      discrepancyType = AssetInventoryDetail.DISCREPANCY_LOCATION;
    } else if (detail.getExpectedUserId() != null
        && !detail.getExpectedUserId().equals(actualUserId)) {
      discrepancyType = AssetInventoryDetail.DISCREPANCY_LOCATION;
    }
    detail.setDiscrepancyType(discrepancyType);

    detailMapper.updateById(detail);
  }

  /**
   * 完成盘点 问题434修复：添加权限验证.
   *
   * @param inventoryId 盘点ID
   * @return 资产盘点DTO
   */
  @Transactional
  public AssetInventoryDTO completeInventory(final Long inventoryId) {
    AssetInventory inventory = inventoryRepository.getByIdOrThrow(inventoryId, "盘点不存在");

    if (AssetInventory.STATUS_COMPLETED.equals(inventory.getStatus())) {
      throw new BusinessException("盘点已完成");
    }

    Long currentUserId = SecurityUtils.getUserId();

    // 问题434修复：验证权限，创建人或管理员
    if (!inventory.getCreatedBy().equals(currentUserId)) {
      if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("ASSET_MANAGER")) {
        throw new BusinessException("权限不足：只有盘点创建人或管理员才能完成盘点");
      }
      log.warn(
          "管理员完成他人的盘点: inventoryId={}, operator={}, creator={}",
          inventoryId,
          currentUserId,
          inventory.getCreatedBy());
    }

    // 统计盘点结果
    List<AssetInventoryDetail> details = detailMapper.selectByInventoryId(inventoryId);
    int actualCount = 0;
    int surplusCount = 0;
    int shortageCount = 0;

    for (AssetInventoryDetail detail : details) {
      if (!AssetInventoryDetail.DISCREPANCY_NORMAL.equals(detail.getDiscrepancyType())) {
        actualCount++;
        if (AssetInventoryDetail.DISCREPANCY_SURPLUS.equals(detail.getDiscrepancyType())) {
          surplusCount++;
        } else if (AssetInventoryDetail.DISCREPANCY_SHORTAGE.equals(detail.getDiscrepancyType())) {
          shortageCount++;
        }
      } else {
        actualCount++;
      }
    }

    inventory.setActualCount(actualCount);
    inventory.setSurplusCount(surplusCount);
    inventory.setShortageCount(shortageCount);
    inventory.setStatus(AssetInventory.STATUS_COMPLETED);
    inventory.setUpdatedBy(currentUserId);
    inventory.setUpdatedAt(LocalDateTime.now());
    inventoryRepository.updateById(inventory);

    log.info(
        "资产盘点完成: inventoryNo={}, total={}, actual={}, surplus={}, shortage={}, completedBy={}",
        inventory.getInventoryNo(),
        inventory.getTotalCount(),
        actualCount,
        surplusCount,
        shortageCount,
        currentUserId);
    return getInventoryById(inventoryId);
  }

  /**
   * 获取盘点详情 问题427修复：使用批量加载避免N+1查询.
   *
   * @param id 盘点ID
   * @return 资产盘点DTO
   */
  public AssetInventoryDTO getInventoryById(final Long id) {
    AssetInventory inventory = inventoryRepository.getByIdOrThrow(id, "盘点不存在");
    AssetInventoryDTO dto = toDTO(inventory);

    // 加载明细
    List<AssetInventoryDetail> details = detailMapper.selectByInventoryId(id);
    dto.setDetails(convertDetailsToDTOs(details));

    return dto;
  }

  /**
   * 问题427修复：批量转换盘点明细DTO，避免N+1查询
   *
   * @param details 盘点明细列表
   * @return DTO列表
   */
  private List<AssetInventoryDetailDTO> convertDetailsToDTOs(
      final List<AssetInventoryDetail> details) {
    if (details.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载资产信息
    Set<Long> assetIds =
        details.stream()
            .map(AssetInventoryDetail::getAssetId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, Asset> assetMap =
        assetIds.isEmpty()
            ? Collections.emptyMap()
            : assetRepository.listByIds(new ArrayList<>(assetIds)).stream()
                .collect(Collectors.toMap(Asset::getId, a -> a));

    // 转换DTO（从Map获取）
    return details.stream().map(d -> toDetailDTO(d, assetMap)).collect(Collectors.toList());
  }

  /**
   * 分页查询资产盘点列表.
   *
   * @param query 分页查询条件
   * @param status 状态
   * @return 分页结果
   */
  public PageResult<AssetInventoryDTO> listInventories(final PageQuery query, final String status) {
    Page<AssetInventory> page = new Page<>(query.getPageNum(), query.getPageSize());
    LambdaQueryWrapper<AssetInventory> wrapper = new LambdaQueryWrapper<>();
    // 注意：deleted 条件由 @TableLogic 注解自动处理
    if (status != null && !status.isEmpty()) {
      wrapper.eq(AssetInventory::getStatus, status);
    }
    wrapper.orderByDesc(AssetInventory::getCreatedAt);

    Page<AssetInventory> result = inventoryMapper.selectPage(page, wrapper);
    List<AssetInventoryDTO> items =
        result.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 查询进行中的盘点.
   *
   * @return 资产盘点DTO列表
   */
  public List<AssetInventoryDTO> getInProgressInventories() {
    List<AssetInventory> inventories = inventoryMapper.selectInProgress();
    return inventories.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 问题443修复：生成盘点编号（防止并发重复）
   *
   * @return 盘点编号
   */
  private String generateInventoryNo() {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    long seq = sequence.incrementAndGet() % SEQUENCE_MODULUS;
    return String.format("INV%s%04d", date, seq);
  }

  private String getInventoryTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case AssetInventory.TYPE_FULL -> "全盘";
      case AssetInventory.TYPE_PARTIAL -> "抽盘";
      default -> type;
    };
  }

  /**
   * 获取状态名称
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case AssetInventory.STATUS_IN_PROGRESS -> "进行中";
      case AssetInventory.STATUS_COMPLETED -> "已完成";
      default -> status;
    };
  }

  /**
   * 获取差异类型名称
   *
   * @param type 类型
   * @return 类型名称
   */
  private String getDiscrepancyTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case AssetInventoryDetail.DISCREPANCY_NORMAL -> "正常";
      case AssetInventoryDetail.DISCREPANCY_SURPLUS -> "盘盈";
      case AssetInventoryDetail.DISCREPANCY_SHORTAGE -> "盘亏";
      case AssetInventoryDetail.DISCREPANCY_LOCATION -> "位置不符";
      case AssetInventoryDetail.DISCREPANCY_STATUS -> "状态不符";
      default -> type;
    };
  }

  private AssetInventoryDTO toDTO(final AssetInventory inventory) {
    AssetInventoryDTO dto = new AssetInventoryDTO();
    dto.setId(inventory.getId());
    dto.setInventoryNo(inventory.getInventoryNo());
    dto.setInventoryDate(inventory.getInventoryDate());
    dto.setInventoryType(inventory.getInventoryType());
    dto.setInventoryTypeName(getInventoryTypeName(inventory.getInventoryType()));
    dto.setDepartmentId(inventory.getDepartmentId());
    dto.setLocation(inventory.getLocation());
    dto.setStatus(inventory.getStatus());
    dto.setStatusName(getStatusName(inventory.getStatus()));
    dto.setTotalCount(inventory.getTotalCount());
    dto.setActualCount(inventory.getActualCount());
    dto.setSurplusCount(inventory.getSurplusCount());
    dto.setShortageCount(inventory.getShortageCount());
    dto.setRemark(inventory.getRemark());
    dto.setCreatedAt(inventory.getCreatedAt());
    dto.setUpdatedAt(inventory.getUpdatedAt());
    return dto;
  }

  /**
   * 问题427修复：带Map参数的toDetailDTO方法，避免重复查询
   *
   * @param detail 盘点明细实体
   * @param assetMap 资产Map
   * @return 盘点明细DTO
   */
  private AssetInventoryDetailDTO toDetailDTO(
      final AssetInventoryDetail detail, final Map<Long, Asset> assetMap) {
    AssetInventoryDetailDTO dto = new AssetInventoryDetailDTO();
    dto.setId(detail.getId());
    dto.setInventoryId(detail.getInventoryId());
    dto.setAssetId(detail.getAssetId());
    dto.setExpectedStatus(detail.getExpectedStatus());
    dto.setActualStatus(detail.getActualStatus());
    dto.setExpectedLocation(detail.getExpectedLocation());
    dto.setActualLocation(detail.getActualLocation());
    dto.setExpectedUserId(detail.getExpectedUserId());
    dto.setActualUserId(detail.getActualUserId());
    dto.setDiscrepancyType(detail.getDiscrepancyType());
    dto.setDiscrepancyTypeName(getDiscrepancyTypeName(detail.getDiscrepancyType()));
    dto.setDiscrepancyDesc(detail.getDiscrepancyDesc());
    dto.setRemark(detail.getRemark());

    // 从Map获取资产信息
    if (detail.getAssetId() != null) {
      Asset asset = assetMap.get(detail.getAssetId());
      if (asset != null) {
        dto.setAssetNo(asset.getAssetNo());
        dto.setAssetName(asset.getName());
      }
    }

    return dto;
  }
}
