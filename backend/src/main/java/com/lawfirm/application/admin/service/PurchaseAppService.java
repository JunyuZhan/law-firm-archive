package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreatePurchaseRequestCommand;
import com.lawfirm.application.admin.command.PurchaseReceiveCommand;
import com.lawfirm.application.admin.dto.PurchaseItemDTO;
import com.lawfirm.application.admin.dto.PurchaseReceiveDTO;
import com.lawfirm.application.admin.dto.PurchaseRequestDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.constant.PurchaseStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.Asset;
import com.lawfirm.domain.admin.entity.PurchaseItem;
import com.lawfirm.domain.admin.entity.PurchaseReceive;
import com.lawfirm.domain.admin.entity.PurchaseRequest;
import com.lawfirm.domain.admin.entity.Supplier;
import com.lawfirm.domain.admin.repository.AssetRepository;
import com.lawfirm.domain.admin.repository.PurchaseItemRepository;
import com.lawfirm.domain.admin.repository.PurchaseReceiveRepository;
import com.lawfirm.domain.admin.repository.PurchaseRequestRepository;
import com.lawfirm.domain.admin.repository.SupplierRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

/** 采购管理应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseAppService {

  /** 采购申请仓储 */
  private final PurchaseRequestRepository requestRepository;

  /** 采购明细仓储 */
  private final PurchaseItemRepository itemRepository;

  /** 采购入库仓储 */
  private final PurchaseReceiveRepository receiveRepository;

  /** 供应商仓储 */
  private final SupplierRepository supplierRepository;

  /** 资产仓储 */
  private final AssetRepository assetRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 问题430/433/441/442修复：序号生成器防止并发重复 */
  private final AtomicLong sequence = new AtomicLong(0);

  /** 序号最大值 */
  private static final int MAX_SEQUENCE = 10000;

  /**
   * 分页查询采购申请 问题425修复：使用批量加载避免N+1查询
   *
   * @param query 分页查询条件
   * @param keyword 关键词
   * @param purchaseType 采购类型
   * @param status 状态
   * @param applicantId 申请人ID
   * @param departmentId 部门ID
   * @return 分页结果
   */
  public PageResult<PurchaseRequestDTO> listRequests(
      final PageQuery query,
      final String keyword,
      final String purchaseType,
      final String status,
      final Long applicantId,
      final Long departmentId) {
    Page<PurchaseRequest> page = new Page<>(query.getPageNum(), query.getPageSize());
    IPage<PurchaseRequest> result =
        requestRepository.findPage(page, keyword, purchaseType, status, applicantId, departmentId);

    List<PurchaseRequestDTO> items = convertRequestsToDTOs(result.getRecords());

    return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取采购申请详情
   *
   * @param id 申请ID
   * @return 采购申请DTO
   */
  public PurchaseRequestDTO getRequestById(final Long id) {
    PurchaseRequest request = requestRepository.getById(id);
    if (request == null) {
      throw new BusinessException("采购申请不存在");
    }
    PurchaseRequestDTO dto = toRequestDTO(request);
    dto.setItems(
        itemRepository.findByRequestId(id).stream()
            .map(this::toItemDTO)
            .collect(Collectors.toList()));
    return dto;
  }

  /**
   * 创建采购申请 问题428修复：使用批量保存明细 问题441修复：使用安全的编号生成
   *
   * @param command 创建命令
   * @return 采购申请DTO
   */
  @Transactional
  public PurchaseRequestDTO createRequest(final CreatePurchaseRequestCommand command) {
    Long userId = SecurityUtils.getCurrentUserId();
    String requestNo = generateRequestNo();

    BigDecimal estimatedAmount = BigDecimal.ZERO;
    if (command.getItems() != null) {
      for (var item : command.getItems()) {
        BigDecimal itemAmount =
            item.getEstimatedPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        estimatedAmount = estimatedAmount.add(itemAmount);
      }
    }

    PurchaseRequest request =
        PurchaseRequest.builder()
            .requestNo(requestNo)
            .title(command.getTitle())
            .applicantId(userId)
            .purchaseType(command.getPurchaseType())
            .estimatedAmount(estimatedAmount)
            .expectedDate(command.getExpectedDate())
            .reason(command.getReason())
            .supplierId(command.getSupplierId())
            .status(PurchaseStatus.DRAFT)
            .remarks(command.getRemarks())
            .build();

    requestRepository.save(request);

    // 问题428修复：批量保存明细
    if (command.getItems() != null && !command.getItems().isEmpty()) {
      List<PurchaseItem> items =
          command.getItems().stream()
              .map(
                  itemCmd -> {
                    BigDecimal itemAmount =
                        itemCmd
                            .getEstimatedPrice()
                            .multiply(BigDecimal.valueOf(itemCmd.getQuantity()));
                    return PurchaseItem.builder()
                        .requestId(request.getId())
                        .itemName(itemCmd.getItemName())
                        .specification(itemCmd.getSpecification())
                        .unit(itemCmd.getUnit())
                        .quantity(itemCmd.getQuantity())
                        .estimatedPrice(itemCmd.getEstimatedPrice())
                        .estimatedAmount(itemAmount)
                        .receivedQuantity(0)
                        .remarks(itemCmd.getRemarks())
                        .build();
                  })
              .collect(Collectors.toList());

      itemRepository.saveBatch(items);
    }

    log.info("创建采购申请: {}", requestNo);
    return getRequestById(request.getId());
  }

  /**
   * 提交采购申请 问题435修复：验证只能提交自己的申请
   *
   * @param id 申请ID
   */
  @Transactional
  public void submitRequest(final Long id) {
    PurchaseRequest request = requestRepository.getById(id);
    if (request == null) {
      throw new BusinessException("采购申请不存在");
    }
    if (!PurchaseStatus.DRAFT.equals(request.getStatus())) {
      throw new BusinessException("只有草稿状态的申请可以提交");
    }

    Long currentUserId = SecurityUtils.getCurrentUserId();

    // 问题435修复：验证权限，只能提交自己的申请
    if (!request.getApplicantId().equals(currentUserId)) {
      throw new BusinessException("权限不足：只能提交自己的采购申请");
    }

    request.setStatus(PurchaseStatus.PENDING);
    requestRepository.updateById(request);
    log.info("提交采购申请: requestNo={}, applicant={}", request.getRequestNo(), currentUserId);
  }

  /**
   * 审批采购申请 问题431修复：添加权限验证，防止自己审批自己
   *
   * @param id 申请ID
   * @param approved 是否批准
   * @param comment 审批意见
   */
  @Transactional
  public void approveRequest(final Long id, final boolean approved, final String comment) {
    PurchaseRequest request = requestRepository.getById(id);
    if (request == null) {
      throw new BusinessException("采购申请不存在");
    }
    if (!PurchaseStatus.PENDING.equals(request.getStatus())) {
      throw new BusinessException("只有待审批的申请可以审批");
    }

    Long approverId = SecurityUtils.getCurrentUserId();

    // 问题431修复：验证审批权限
    if (!SecurityUtils.hasRole("ADMIN")
        && !SecurityUtils.hasRole("FINANCE_MANAGER")
        && !SecurityUtils.hasRole("PURCHASE_MANAGER")) {
      throw new BusinessException("权限不足：只有管理员或财务/采购主管才能审批");
    }

    // 问题431修复：防止自己审批自己
    if (request.getApplicantId().equals(approverId)) {
      throw new BusinessException("不能审批自己的采购申请");
    }

    request.setStatus(approved ? PurchaseStatus.APPROVED : PurchaseStatus.REJECTED);
    request.setApproverId(approverId);
    request.setApprovalDate(LocalDate.now());
    request.setApprovalComment(comment);
    requestRepository.updateById(request);

    log.info(
        "审批采购申请: requestNo={}, approved={}, approver={}",
        request.getRequestNo(),
        approved,
        approverId);
  }

  /**
   * 开始采购
   *
   * @param id 申请ID
   * @param supplierId 供应商ID
   */
  @Transactional
  public void startPurchasing(final Long id, final Long supplierId) {
    PurchaseRequest request = requestRepository.getById(id);
    if (request == null) {
      throw new BusinessException("采购申请不存在");
    }
    if (!PurchaseStatus.APPROVED.equals(request.getStatus())) {
      throw new BusinessException("只有已批准的申请可以开始采购");
    }

    request.setStatus(PurchaseStatus.PURCHASING);
    request.setSupplierId(supplierId);
    requestRepository.updateById(request);
    log.info("开始采购: {}", request.getRequestNo());
  }

  /**
   * 采购入库 问题433/442修复：使用安全的编号生成
   *
   * @param command 入库命令
   * @return 入库记录DTO
   */
  @Transactional
  public PurchaseReceiveDTO receiveItem(final PurchaseReceiveCommand command) {
    PurchaseRequest request = requestRepository.getById(command.getRequestId());
    if (request == null) {
      throw new BusinessException("采购申请不存在");
    }
    if (!PurchaseStatus.PURCHASING.equals(request.getStatus())
        && !PurchaseStatus.APPROVED.equals(request.getStatus())) {
      throw new BusinessException("当前状态不允许入库");
    }

    PurchaseItem item = itemRepository.getById(command.getItemId());
    if (item == null || !item.getRequestId().equals(command.getRequestId())) {
      throw new BusinessException("采购明细不存在");
    }

    // 检查入库数量
    int alreadyReceived = receiveRepository.sumQuantityByItemId(command.getItemId());
    if (alreadyReceived + command.getQuantity() > item.getQuantity()) {
      throw new BusinessException("入库数量超过采购数量");
    }

    String receiveNo = generateReceiveNo();
    Long userId = SecurityUtils.getCurrentUserId();

    PurchaseReceive receive =
        PurchaseReceive.builder()
            .receiveNo(receiveNo)
            .requestId(command.getRequestId())
            .itemId(command.getItemId())
            .quantity(command.getQuantity())
            .receiveDate(
                command.getReceiveDate() != null ? command.getReceiveDate() : LocalDate.now())
            .receiverId(userId)
            .location(command.getLocation())
            .convertToAsset(command.getConvertToAsset() != null && command.getConvertToAsset())
            .remarks(command.getRemarks())
            .build();

    // 问题433修复：如果转为资产，使用安全的编号生成
    if (Boolean.TRUE.equals(command.getConvertToAsset())) {
      String assetNo = generateAssetNo();
      Asset asset =
          Asset.builder()
              .assetNo(assetNo)
              .name(item.getItemName())
              .category(request.getPurchaseType())
              .specification(item.getSpecification())
              .purchaseDate(LocalDate.now())
              .purchasePrice(
                  item.getActualPrice() != null ? item.getActualPrice() : item.getEstimatedPrice())
              .location(command.getLocation())
              .status(PurchaseStatus.ASSET_IDLE)
              .createdBy(userId)
              .createdAt(LocalDateTime.now())
              .build();
      assetRepository.save(asset);
      receive.setAssetId(asset.getId());
      log.info("采购物品转为资产: assetNo={}, itemName={}", assetNo, item.getItemName());
    }

    receiveRepository.save(receive);

    // 更新明细已入库数量
    item.setReceivedQuantity(alreadyReceived + command.getQuantity());
    itemRepository.updateById(item);

    // 检查是否全部入库完成
    checkAndCompleteRequest(request.getId());

    log.info("采购入库: {} - {}", request.getRequestNo(), item.getItemName());
    return toReceiveDTO(receive);
  }

  /**
   * 取消采购申请 问题436修复：添加权限验证
   *
   * @param id 申请ID
   */
  @Transactional
  public void cancelRequest(final Long id) {
    PurchaseRequest request = requestRepository.getById(id);
    if (request == null) {
      throw new BusinessException("采购申请不存在");
    }
    if (PurchaseStatus.COMPLETED.equals(request.getStatus())
        || PurchaseStatus.CANCELLED.equals(request.getStatus())) {
      throw new BusinessException("当前状态不允许取消");
    }

    Long currentUserId = SecurityUtils.getCurrentUserId();

    // 问题436修复：验证权限，只有申请人或管理员可以取消
    if (!request.getApplicantId().equals(currentUserId)) {
      if (!SecurityUtils.hasRole("ADMIN") && !SecurityUtils.hasRole("PURCHASE_MANAGER")) {
        throw new BusinessException("权限不足：只有申请人或管理员才能取消");
      }
      log.warn(
          "管理员取消他人的采购申请: requestId={}, operator={}, applicant={}",
          id,
          currentUserId,
          request.getApplicantId());
    }

    request.setStatus(PurchaseStatus.CANCELLED);
    requestRepository.updateById(request);
    log.info("取消采购申请: requestNo={}, cancelBy={}", request.getRequestNo(), currentUserId);
  }

  /**
   * 获取入库记录 问题426修复：使用批量加载避免N+1查询
   *
   * @param requestId 申请ID
   * @return 入库记录列表
   */
  public List<PurchaseReceiveDTO> getReceiveRecords(final Long requestId) {
    List<PurchaseReceive> receives = receiveRepository.findByRequestId(requestId);
    return convertReceivesToDTOs(receives);
  }

  /**
   * 获取我的采购申请 问题425修复：使用批量加载避免N+1查询
   *
   * @return 采购申请列表
   */
  public List<PurchaseRequestDTO> getMyRequests() {
    Long userId = SecurityUtils.getCurrentUserId();
    Page<PurchaseRequest> page = new Page<>(1, 100);
    IPage<PurchaseRequest> result =
        requestRepository.findPage(page, null, null, null, userId, null);
    return convertRequestsToDTOs(result.getRecords());
  }

  /**
   * 获取待审批的采购申请 问题425修复：使用批量加载避免N+1查询
   *
   * @return 采购申请列表
   */
  public List<PurchaseRequestDTO> getPendingApproval() {
    return convertRequestsToDTOs(requestRepository.findPendingApproval());
  }

  /**
   * 获取采购统计
   *
   * @return 统计数据
   */
  public Map<String, Object> getStatistics() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("byStatus", requestRepository.countByStatus());
    stats.put("amountByType", requestRepository.sumAmountByType());
    return stats;
  }

  private void checkAndCompleteRequest(final Long requestId) {
    List<PurchaseItem> items = itemRepository.findByRequestId(requestId);
    boolean allReceived =
        items.stream().allMatch(item -> item.getReceivedQuantity() >= item.getQuantity());

    if (allReceived) {
      PurchaseRequest request = requestRepository.getById(requestId);
      request.setStatus(PurchaseStatus.COMPLETED);
      requestRepository.updateById(request);
      log.info("采购申请完成: {}", request.getRequestNo());
    }
  }

  // ==================== 编号生成方法 ====================

  /**
   * 问题441修复：生成采购申请编号（防止并发重复）
   *
   * @return 申请编号
   */
  private String generateRequestNo() {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    long seq = sequence.incrementAndGet() % MAX_SEQUENCE;
    return String.format("PUR%s%04d", date, seq);
  }

  /**
   * 问题442修复：生成入库编号（防止并发重复）
   *
   * @return 入库编号
   */
  private String generateReceiveNo() {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    long seq = sequence.incrementAndGet() % MAX_SEQUENCE;
    return String.format("RCV%s%04d", date, seq);
  }

  /**
   * 问题433修复：生成资产编号（防止并发重复）
   *
   * @return 资产编号
   */
  private String generateAssetNo() {
    String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    long seq = sequence.incrementAndGet() % MAX_SEQUENCE;
    return String.format("AST%s%04d", date, seq);
  }

  // ==================== 批量转换方法 ====================

  /**
   * 问题425修复：批量转换采购申请DTO，避免N+1查询
   *
   * @param requests 采购申请列表
   * @return DTO列表
   */
  private List<PurchaseRequestDTO> convertRequestsToDTOs(final List<PurchaseRequest> requests) {
    if (requests.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载申请人信息
    Set<Long> applicantIds =
        requests.stream()
            .map(PurchaseRequest::getApplicantId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, User> userMap =
        applicantIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(new ArrayList<>(applicantIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    // 批量加载供应商信息
    Set<Long> supplierIds =
        requests.stream()
            .map(PurchaseRequest::getSupplierId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, Supplier> supplierMap =
        supplierIds.isEmpty()
            ? Collections.emptyMap()
            : supplierRepository.listByIds(new ArrayList<>(supplierIds)).stream()
                .collect(Collectors.toMap(Supplier::getId, s -> s));

    // 转换DTO（从Map获取）
    return requests.stream()
        .map(r -> toRequestDTO(r, userMap, supplierMap))
        .collect(Collectors.toList());
  }

  /**
   * 问题426修复：批量转换入库记录DTO，避免N+1查询
   *
   * @param receives 入库记录列表
   * @return DTO列表
   */
  private List<PurchaseReceiveDTO> convertReceivesToDTOs(final List<PurchaseReceive> receives) {
    if (receives.isEmpty()) {
      return Collections.emptyList();
    }

    // 批量加载明细信息
    Set<Long> itemIds =
        receives.stream()
            .map(PurchaseReceive::getItemId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, PurchaseItem> itemMap =
        itemIds.isEmpty()
            ? Collections.emptyMap()
            : itemRepository.listByIds(new ArrayList<>(itemIds)).stream()
                .collect(Collectors.toMap(PurchaseItem::getId, i -> i));

    // 批量加载入库人信息
    Set<Long> receiverIds =
        receives.stream()
            .map(PurchaseReceive::getReceiverId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, User> userMap =
        receiverIds.isEmpty()
            ? Collections.emptyMap()
            : userRepository.listByIds(new ArrayList<>(receiverIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    // 转换DTO（从Map获取）
    return receives.stream()
        .map(r -> toReceiveDTO(r, itemMap, userMap))
        .collect(Collectors.toList());
  }

  // ==================== DTO转换方法 ====================

  private PurchaseRequestDTO toRequestDTO(final PurchaseRequest request) {
    // 单个转换时仍查询关联数据
    Map<Long, User> userMap = Collections.emptyMap();
    Map<Long, Supplier> supplierMap = Collections.emptyMap();

    if (request.getApplicantId() != null) {
      User user = userRepository.getById(request.getApplicantId());
      if (user != null) {
        userMap = Collections.singletonMap(user.getId(), user);
      }
    }
    if (request.getSupplierId() != null) {
      Supplier supplier = supplierRepository.getById(request.getSupplierId());
      if (supplier != null) {
        supplierMap = Collections.singletonMap(supplier.getId(), supplier);
      }
    }

    return toRequestDTO(request, userMap, supplierMap);
  }

  /**
   * 问题425修复：带Map参数的toRequestDTO方法，避免重复查询
   *
   * @param request 采购申请
   * @param userMap 用户映射
   * @param supplierMap 供应商映射
   * @return 采购申请DTO
   */
  private PurchaseRequestDTO toRequestDTO(
      final PurchaseRequest request,
      final Map<Long, User> userMap,
      final Map<Long, Supplier> supplierMap) {
    PurchaseRequestDTO dto =
        PurchaseRequestDTO.builder()
            .id(request.getId())
            .requestNo(request.getRequestNo())
            .title(request.getTitle())
            .applicantId(request.getApplicantId())
            .departmentId(request.getDepartmentId())
            .purchaseType(request.getPurchaseType())
            .purchaseTypeName(getPurchaseTypeName(request.getPurchaseType()))
            .estimatedAmount(request.getEstimatedAmount())
            .actualAmount(request.getActualAmount())
            .expectedDate(request.getExpectedDate())
            .reason(request.getReason())
            .status(request.getStatus())
            .statusName(getStatusName(request.getStatus()))
            .approverId(request.getApproverId())
            .approvalDate(request.getApprovalDate())
            .approvalComment(request.getApprovalComment())
            .supplierId(request.getSupplierId())
            .remarks(request.getRemarks())
            .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
            .build();

    // 从Map获取申请人
    if (request.getApplicantId() != null) {
      User user = userMap.get(request.getApplicantId());
      if (user != null) {
        dto.setApplicantName(user.getRealName());
      }
    }

    // 从Map获取供应商
    if (request.getSupplierId() != null) {
      Supplier supplier = supplierMap.get(request.getSupplierId());
      if (supplier != null) {
        dto.setSupplierName(supplier.getName());
      }
    }

    return dto;
  }

  private PurchaseItemDTO toItemDTO(final PurchaseItem item) {
    return PurchaseItemDTO.builder()
        .id(item.getId())
        .requestId(item.getRequestId())
        .itemName(item.getItemName())
        .specification(item.getSpecification())
        .unit(item.getUnit())
        .quantity(item.getQuantity())
        .estimatedPrice(item.getEstimatedPrice())
        .actualPrice(item.getActualPrice())
        .estimatedAmount(item.getEstimatedAmount())
        .actualAmount(item.getActualAmount())
        .receivedQuantity(item.getReceivedQuantity())
        .remarks(item.getRemarks())
        .fullyReceived(item.getReceivedQuantity() >= item.getQuantity())
        .build();
  }

  private PurchaseReceiveDTO toReceiveDTO(final PurchaseReceive receive) {
    // 单个转换时仍查询关联数据
    Map<Long, PurchaseItem> itemMap = Collections.emptyMap();
    Map<Long, User> userMap = Collections.emptyMap();

    if (receive.getItemId() != null) {
      PurchaseItem item = itemRepository.getById(receive.getItemId());
      if (item != null) {
        itemMap = Collections.singletonMap(item.getId(), item);
      }
    }
    if (receive.getReceiverId() != null) {
      User user = userRepository.getById(receive.getReceiverId());
      if (user != null) {
        userMap = Collections.singletonMap(user.getId(), user);
      }
    }

    return toReceiveDTO(receive, itemMap, userMap);
  }

  /**
   * 问题426修复：带Map参数的toReceiveDTO方法，避免重复查询
   *
   * @param receive 入库记录
   * @param itemMap 明细映射
   * @param userMap 用户映射
   * @return 入库记录DTO
   */
  private PurchaseReceiveDTO toReceiveDTO(
      final PurchaseReceive receive,
      final Map<Long, PurchaseItem> itemMap,
      final Map<Long, User> userMap) {
    PurchaseReceiveDTO dto =
        PurchaseReceiveDTO.builder()
            .id(receive.getId())
            .receiveNo(receive.getReceiveNo())
            .requestId(receive.getRequestId())
            .itemId(receive.getItemId())
            .quantity(receive.getQuantity())
            .receiveDate(receive.getReceiveDate())
            .receiverId(receive.getReceiverId())
            .location(receive.getLocation())
            .convertToAsset(receive.getConvertToAsset())
            .assetId(receive.getAssetId())
            .remarks(receive.getRemarks())
            .createdAt(receive.getCreatedAt())
            .build();

    // 从Map获取明细信息
    if (receive.getItemId() != null) {
      PurchaseItem item = itemMap.get(receive.getItemId());
      if (item != null) {
        dto.setItemName(item.getItemName());
      }
    }

    // 从Map获取入库人
    if (receive.getReceiverId() != null) {
      User user = userMap.get(receive.getReceiverId());
      if (user != null) {
        dto.setReceiverName(user.getRealName());
      }
    }

    return dto;
  }

  private String getPurchaseTypeName(final String type) {
    if (type == null) {
      return "其他";
    }
    return switch (type) {
      case "OFFICE" -> "办公用品";
      case "IT" -> "IT设备";
      case "FURNITURE" -> "家具";
      case "SERVICE" -> "服务";
      default -> "其他";
    };
  }

  private String getStatusName(final String status) {
    return PurchaseStatus.getStatusName(status);
  }
}
