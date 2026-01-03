package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreatePurchaseRequestCommand;
import com.lawfirm.application.admin.command.PurchaseReceiveCommand;
import com.lawfirm.application.admin.dto.PurchaseItemDTO;
import com.lawfirm.application.admin.dto.PurchaseReceiveDTO;
import com.lawfirm.application.admin.dto.PurchaseRequestDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.*;
import com.lawfirm.domain.admin.repository.*;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购管理应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseAppService {

    private final PurchaseRequestRepository requestRepository;
    private final PurchaseItemRepository itemRepository;
    private final PurchaseReceiveRepository receiveRepository;
    private final SupplierRepository supplierRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    /**
     * 分页查询采购申请
     */
    public PageResult<PurchaseRequestDTO> listRequests(PageQuery query, String keyword, String purchaseType,
                                                        String status, Long applicantId, Long departmentId) {
        Page<PurchaseRequest> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<PurchaseRequest> result = requestRepository.findPage(page, keyword, purchaseType, status, applicantId, departmentId);
        
        List<PurchaseRequestDTO> items = result.getRecords().stream()
                .map(this::toRequestDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取采购申请详情
     */
    public PurchaseRequestDTO getRequestById(Long id) {
        PurchaseRequest request = requestRepository.getById(id);
        if (request == null) {
            throw new BusinessException("采购申请不存在");
        }
        PurchaseRequestDTO dto = toRequestDTO(request);
        dto.setItems(itemRepository.findByRequestId(id).stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList()));
        return dto;
    }

    /**
     * 创建采购申请
     */
    @Transactional
    public PurchaseRequestDTO createRequest(CreatePurchaseRequestCommand command) {
        Long userId = SecurityUtils.getCurrentUserId();
        String requestNo = "PUR" + System.currentTimeMillis();
        
        BigDecimal estimatedAmount = BigDecimal.ZERO;
        if (command.getItems() != null) {
            for (var item : command.getItems()) {
                BigDecimal itemAmount = item.getEstimatedPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                estimatedAmount = estimatedAmount.add(itemAmount);
            }
        }
        
        PurchaseRequest request = PurchaseRequest.builder()
                .requestNo(requestNo)
                .title(command.getTitle())
                .applicantId(userId)
                .purchaseType(command.getPurchaseType())
                .estimatedAmount(estimatedAmount)
                .expectedDate(command.getExpectedDate())
                .reason(command.getReason())
                .supplierId(command.getSupplierId())
                .status("DRAFT")
                .remarks(command.getRemarks())
                .build();
        
        requestRepository.save(request);
        
        // 保存明细
        if (command.getItems() != null) {
            for (var itemCmd : command.getItems()) {
                BigDecimal itemAmount = itemCmd.getEstimatedPrice().multiply(BigDecimal.valueOf(itemCmd.getQuantity()));
                PurchaseItem item = PurchaseItem.builder()
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
                itemRepository.save(item);
            }
        }
        
        log.info("创建采购申请: {}", requestNo);
        return getRequestById(request.getId());
    }


    /**
     * 提交采购申请
     */
    @Transactional
    public void submitRequest(Long id) {
        PurchaseRequest request = requestRepository.getById(id);
        if (request == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (!"DRAFT".equals(request.getStatus())) {
            throw new BusinessException("只有草稿状态的申请可以提交");
        }
        request.setStatus("PENDING");
        requestRepository.updateById(request);
        log.info("提交采购申请: {}", request.getRequestNo());
    }

    /**
     * 审批采购申请
     */
    @Transactional
    public void approveRequest(Long id, boolean approved, String comment) {
        PurchaseRequest request = requestRepository.getById(id);
        if (request == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException("只有待审批的申请可以审批");
        }
        
        request.setStatus(approved ? "APPROVED" : "REJECTED");
        request.setApproverId(SecurityUtils.getCurrentUserId());
        request.setApprovalDate(LocalDate.now());
        request.setApprovalComment(comment);
        requestRepository.updateById(request);
        
        log.info("审批采购申请: {} -> {}", request.getRequestNo(), approved ? "批准" : "拒绝");
    }

    /**
     * 开始采购
     */
    @Transactional
    public void startPurchasing(Long id, Long supplierId) {
        PurchaseRequest request = requestRepository.getById(id);
        if (request == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (!"APPROVED".equals(request.getStatus())) {
            throw new BusinessException("只有已批准的申请可以开始采购");
        }
        
        request.setStatus("PURCHASING");
        request.setSupplierId(supplierId);
        requestRepository.updateById(request);
        log.info("开始采购: {}", request.getRequestNo());
    }

    /**
     * 采购入库
     */
    @Transactional
    public PurchaseReceiveDTO receiveItem(PurchaseReceiveCommand command) {
        PurchaseRequest request = requestRepository.getById(command.getRequestId());
        if (request == null) {
            throw new BusinessException("采购申请不存在");
        }
        if (!"PURCHASING".equals(request.getStatus()) && !"APPROVED".equals(request.getStatus())) {
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
        
        String receiveNo = "RCV" + System.currentTimeMillis();
        Long userId = SecurityUtils.getCurrentUserId();
        
        PurchaseReceive receive = PurchaseReceive.builder()
                .receiveNo(receiveNo)
                .requestId(command.getRequestId())
                .itemId(command.getItemId())
                .quantity(command.getQuantity())
                .receiveDate(command.getReceiveDate() != null ? command.getReceiveDate() : LocalDate.now())
                .receiverId(userId)
                .location(command.getLocation())
                .convertToAsset(command.getConvertToAsset() != null && command.getConvertToAsset())
                .remarks(command.getRemarks())
                .build();
        
        // 如果转为资产
        if (Boolean.TRUE.equals(command.getConvertToAsset())) {
            Asset asset = Asset.builder()
                    .assetNo("AST" + System.currentTimeMillis())
                    .name(item.getItemName())
                    .category(request.getPurchaseType())
                    .specification(item.getSpecification())
                    .purchaseDate(LocalDate.now())
                    .purchasePrice(item.getActualPrice() != null ? item.getActualPrice() : item.getEstimatedPrice())
                    .location(command.getLocation())
                    .status("IDLE")
                    .build();
            assetRepository.save(asset);
            receive.setAssetId(asset.getId());
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
     * 取消采购申请
     */
    @Transactional
    public void cancelRequest(Long id) {
        PurchaseRequest request = requestRepository.getById(id);
        if (request == null) {
            throw new BusinessException("采购申请不存在");
        }
        if ("COMPLETED".equals(request.getStatus()) || "CANCELLED".equals(request.getStatus())) {
            throw new BusinessException("当前状态不允许取消");
        }
        request.setStatus("CANCELLED");
        requestRepository.updateById(request);
        log.info("取消采购申请: {}", request.getRequestNo());
    }

    /**
     * 获取入库记录
     */
    public List<PurchaseReceiveDTO> getReceiveRecords(Long requestId) {
        return receiveRepository.findByRequestId(requestId).stream()
                .map(this::toReceiveDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取我的采购申请
     */
    public List<PurchaseRequestDTO> getMyRequests() {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<PurchaseRequest> page = new Page<>(1, 100);
        IPage<PurchaseRequest> result = requestRepository.findPage(page, null, null, null, userId, null);
        return result.getRecords().stream()
                .map(this::toRequestDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取待审批的采购申请
     */
    public List<PurchaseRequestDTO> getPendingApproval() {
        return requestRepository.findPendingApproval().stream()
                .map(this::toRequestDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取采购统计
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("byStatus", requestRepository.countByStatus());
        stats.put("amountByType", requestRepository.sumAmountByType());
        return stats;
    }

    private void checkAndCompleteRequest(Long requestId) {
        List<PurchaseItem> items = itemRepository.findByRequestId(requestId);
        boolean allReceived = items.stream()
                .allMatch(item -> item.getReceivedQuantity() >= item.getQuantity());
        
        if (allReceived) {
            PurchaseRequest request = requestRepository.getById(requestId);
            request.setStatus("COMPLETED");
            requestRepository.updateById(request);
            log.info("采购申请完成: {}", request.getRequestNo());
        }
    }


    private PurchaseRequestDTO toRequestDTO(PurchaseRequest request) {
        PurchaseRequestDTO dto = PurchaseRequestDTO.builder()
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
        
        // 查询申请人
        if (request.getApplicantId() != null) {
            User user = userRepository.getById(request.getApplicantId());
            if (user != null) {
                dto.setApplicantName(user.getRealName());
            }
        }
        
        // 查询供应商
        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.getById(request.getSupplierId());
            if (supplier != null) {
                dto.setSupplierName(supplier.getName());
            }
        }
        
        return dto;
    }

    private PurchaseItemDTO toItemDTO(PurchaseItem item) {
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

    private PurchaseReceiveDTO toReceiveDTO(PurchaseReceive receive) {
        PurchaseReceiveDTO dto = PurchaseReceiveDTO.builder()
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
        
        // 查询明细信息
        if (receive.getItemId() != null) {
            PurchaseItem item = itemRepository.getById(receive.getItemId());
            if (item != null) {
                dto.setItemName(item.getItemName());
            }
        }
        
        // 查询入库人
        if (receive.getReceiverId() != null) {
            User user = userRepository.getById(receive.getReceiverId());
            if (user != null) {
                dto.setReceiverName(user.getRealName());
            }
        }
        
        return dto;
    }

    private String getPurchaseTypeName(String type) {
        return switch (type) {
            case "OFFICE" -> "办公用品";
            case "IT" -> "IT设备";
            case "FURNITURE" -> "家具";
            case "SERVICE" -> "服务";
            default -> "其他";
        };
    }

    private String getStatusName(String status) {
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            case "PURCHASING" -> "采购中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
