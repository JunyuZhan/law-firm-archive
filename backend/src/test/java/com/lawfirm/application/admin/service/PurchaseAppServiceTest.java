package com.lawfirm.application.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.command.CreatePurchaseRequestCommand;
import com.lawfirm.application.admin.command.PurchaseReceiveCommand;
import com.lawfirm.application.admin.dto.PurchaseReceiveDTO;
import com.lawfirm.application.admin.dto.PurchaseRequestDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.constant.PurchaseStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.admin.entity.*;
import com.lawfirm.domain.admin.repository.*;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** PurchaseAppService 单元测试 测试采购管理服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PurchaseAppService 采购服务测试")
class PurchaseAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_REQUEST_ID = 100L;
  private static final Long TEST_ITEM_ID = 200L;

  @SuppressWarnings("unused")
  private static final Long TEST_SUPPLIER_ID = 300L;

  private static final Long OTHER_USER_ID = 999L;

  @Mock private PurchaseRequestRepository requestRepository;

  @Mock private PurchaseItemRepository itemRepository;

  @Mock private PurchaseReceiveRepository receiveRepository;

  @Mock private SupplierRepository supplierRepository;

  @Mock private AssetRepository assetRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private PurchaseAppService purchaseAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(TEST_USER_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("创建采购申请测试")
  class CreateRequestTests {

    @Test
    @DisplayName("应该成功创建采购申请")
    void createRequest_shouldSuccess() {
      // Given
      CreatePurchaseRequestCommand command = new CreatePurchaseRequestCommand();
      command.setTitle("采购办公用品");
      command.setPurchaseType("OFFICE_SUPPLIES");
      command.setExpectedDate(LocalDate.now().plusDays(7));
      command.setReason("日常办公需要");

      CreatePurchaseRequestCommand.ItemCommand itemCmd =
          new CreatePurchaseRequestCommand.ItemCommand();
      itemCmd.setItemName("A4纸");
      itemCmd.setSpecification("80g");
      itemCmd.setUnit("包");
      itemCmd.setQuantity(10);
      itemCmd.setEstimatedPrice(new BigDecimal("25.00"));
      command.setItems(Collections.singletonList(itemCmd));

      PurchaseRequest savedRequest =
          PurchaseRequest.builder().id(TEST_REQUEST_ID).requestNo("PR2024001").build();

      when(requestRepository.save(any(PurchaseRequest.class)))
          .thenAnswer(
              invocation -> {
                PurchaseRequest req = invocation.getArgument(0);
                req.setId(TEST_REQUEST_ID);
                req.setRequestNo("PR2024001");
                return true;
              });
      when(itemRepository.saveBatch(anyList())).thenReturn(true);
      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(savedRequest);
      when(itemRepository.findByRequestId(TEST_REQUEST_ID)).thenReturn(Collections.emptyList());

      // When
      PurchaseRequestDTO result = purchaseAppService.createRequest(command);

      // Then
      assertThat(result).isNotNull();
      verify(requestRepository).save(any(PurchaseRequest.class));
      verify(itemRepository).saveBatch(anyList());
    }

    @Test
    @DisplayName("应该正确计算预估总金额")
    void createRequest_shouldCalculateAmount() {
      // Given
      CreatePurchaseRequestCommand command = new CreatePurchaseRequestCommand();
      command.setTitle("采购测试");

      CreatePurchaseRequestCommand.ItemCommand item1 =
          new CreatePurchaseRequestCommand.ItemCommand();
      item1.setQuantity(5);
      item1.setEstimatedPrice(new BigDecimal("10.00"));

      CreatePurchaseRequestCommand.ItemCommand item2 =
          new CreatePurchaseRequestCommand.ItemCommand();
      item2.setQuantity(3);
      item2.setEstimatedPrice(new BigDecimal("20.00"));

      command.setItems(Arrays.asList(item1, item2));

      PurchaseRequest savedRequest =
          PurchaseRequest.builder().id(TEST_REQUEST_ID).requestNo("PR2024001").build();
      when(requestRepository.save(any(PurchaseRequest.class)))
          .thenAnswer(
              invocation -> {
                PurchaseRequest req = invocation.getArgument(0);
                req.setId(TEST_REQUEST_ID);
                req.setRequestNo("PR2024001");
                return true;
              });
      when(itemRepository.saveBatch(anyList())).thenReturn(true);
      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(savedRequest);
      when(itemRepository.findByRequestId(TEST_REQUEST_ID)).thenReturn(Collections.emptyList());

      // When
      PurchaseRequestDTO result = purchaseAppService.createRequest(command);

      // Then
      assertThat(result).isNotNull();
      verify(requestRepository)
          .save(argThat(req -> req.getEstimatedAmount().compareTo(new BigDecimal("110.00")) == 0));
    }
  }

  @Nested
  @DisplayName("提交采购申请测试")
  class SubmitRequestTests {

    @Test
    @DisplayName("应该成功提交采购申请")
    void submitRequest_shouldSuccess() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .requestNo("PR2024001")
              .applicantId(TEST_USER_ID)
              .status(PurchaseStatus.DRAFT)
              .build();

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);
      when(requestRepository.updateById(any(PurchaseRequest.class))).thenReturn(true);

      // When
      purchaseAppService.submitRequest(TEST_REQUEST_ID);

      // Then
      assertThat(request.getStatus()).isEqualTo(PurchaseStatus.PENDING);
      verify(requestRepository).updateById(request);
    }

    @Test
    @DisplayName("非草稿状态不能提交")
    void submitRequest_shouldFail_whenNotDraft() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .status(PurchaseStatus.PENDING) // 已提交
              .build();

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> purchaseAppService.submitRequest(TEST_REQUEST_ID));
      assertThat(exception.getMessage()).contains("只有草稿状态");
    }

    @Test
    @DisplayName("只能提交自己的申请")
    void submitRequest_shouldFail_whenNotOwner() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .applicantId(OTHER_USER_ID) // 其他用户
              .status(PurchaseStatus.DRAFT)
              .build();

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> purchaseAppService.submitRequest(TEST_REQUEST_ID));
      assertThat(exception.getMessage()).contains("只能提交自己的");
    }
  }

  @Nested
  @DisplayName("审批采购申请测试")
  class ApproveRequestTests {

    @Test
    @DisplayName("应该成功审批通过")
    void approveRequest_shouldSuccess_whenApproved() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .applicantId(OTHER_USER_ID)
              .status(PurchaseStatus.PENDING)
              .build();

      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);
      when(requestRepository.updateById(any(PurchaseRequest.class))).thenReturn(true);

      // When
      purchaseAppService.approveRequest(TEST_REQUEST_ID, true, "同意");

      // Then
      assertThat(request.getStatus()).isEqualTo(PurchaseStatus.APPROVED);
      assertThat(request.getApproverId()).isEqualTo(TEST_USER_ID);
      assertThat(request.getApprovalDate()).isNotNull();
    }

    @Test
    @DisplayName("应该成功审批拒绝")
    void approveRequest_shouldSuccess_whenRejected() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .applicantId(OTHER_USER_ID)
              .status(PurchaseStatus.PENDING)
              .build();

      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);
      when(requestRepository.updateById(any(PurchaseRequest.class))).thenReturn(true);

      // When
      purchaseAppService.approveRequest(TEST_REQUEST_ID, false, "不符合规定");

      // Then
      assertThat(request.getStatus()).isEqualTo(PurchaseStatus.REJECTED);
    }

    @Test
    @DisplayName("不能审批自己的申请")
    void approveRequest_shouldFail_whenSelfApproval() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .applicantId(TEST_USER_ID) // 自己
              .status(PurchaseStatus.PENDING)
              .build();

      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> purchaseAppService.approveRequest(TEST_REQUEST_ID, true, ""));
      assertThat(exception.getMessage()).contains("不能审批自己");
    }

    @Test
    @DisplayName("非管理员无权审批")
    void approveRequest_shouldFail_whenNoPermission() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder().id(TEST_REQUEST_ID).status(PurchaseStatus.PENDING).build();

      securityUtilsMock.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("FINANCE_MANAGER")).thenReturn(false);
      securityUtilsMock.when(() -> SecurityUtils.hasRole("PURCHASE_MANAGER")).thenReturn(false);

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class,
              () -> purchaseAppService.approveRequest(TEST_REQUEST_ID, true, ""));
      assertThat(exception.getMessage()).contains("权限不足");
    }
  }

  @Nested
  @DisplayName("采购入库测试")
  class ReceiveItemTests {

    @Test
    @DisplayName("应该成功采购入库")
    void receiveItem_shouldSuccess() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .requestNo("PR2024001")
              .status(PurchaseStatus.APPROVED)
              .purchaseType("OFFICE_SUPPLIES")
              .build();

      PurchaseItem item =
          PurchaseItem.builder()
              .id(TEST_ITEM_ID)
              .requestId(TEST_REQUEST_ID)
              .itemName("A4纸")
              .quantity(10)
              .receivedQuantity(0)
              .estimatedPrice(new BigDecimal("25.00"))
              .build();

      PurchaseReceiveCommand command = new PurchaseReceiveCommand();
      command.setRequestId(TEST_REQUEST_ID);
      command.setItemId(TEST_ITEM_ID);
      command.setQuantity(5);
      command.setReceiveDate(LocalDate.now());
      command.setConvertToAsset(false);

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);
      when(itemRepository.getById(TEST_ITEM_ID)).thenReturn(item);
      when(receiveRepository.sumQuantityByItemId(TEST_ITEM_ID)).thenReturn(0);
      when(receiveRepository.save(any(PurchaseReceive.class))).thenReturn(true);
      when(itemRepository.updateById(any(PurchaseItem.class))).thenReturn(true);

      // When
      PurchaseReceiveDTO result = purchaseAppService.receiveItem(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(item.getReceivedQuantity()).isEqualTo(5);
      verify(receiveRepository).save(any(PurchaseReceive.class));
    }

    @Test
    @DisplayName("入库数量超过采购数量应该失败")
    void receiveItem_shouldFail_whenQuantityExceeds() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder().id(TEST_REQUEST_ID).status(PurchaseStatus.APPROVED).build();

      PurchaseItem item =
          PurchaseItem.builder().id(TEST_ITEM_ID).requestId(TEST_REQUEST_ID).quantity(10).build();

      PurchaseReceiveCommand command = new PurchaseReceiveCommand();
      command.setRequestId(TEST_REQUEST_ID);
      command.setItemId(TEST_ITEM_ID);
      command.setQuantity(15); // 超过采购数量

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);
      when(itemRepository.getById(TEST_ITEM_ID)).thenReturn(item);
      when(receiveRepository.sumQuantityByItemId(TEST_ITEM_ID)).thenReturn(0);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> purchaseAppService.receiveItem(command));
      assertThat(exception.getMessage()).contains("超过采购数量");
    }
  }

  @Nested
  @DisplayName("取消采购申请测试")
  class CancelRequestTests {

    @Test
    @DisplayName("应该成功取消采购申请")
    void cancelRequest_shouldSuccess() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .applicantId(TEST_USER_ID)
              .status(PurchaseStatus.DRAFT)
              .build();

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);
      when(requestRepository.updateById(any(PurchaseRequest.class))).thenReturn(true);

      // When
      purchaseAppService.cancelRequest(TEST_REQUEST_ID);

      // Then
      assertThat(request.getStatus()).isEqualTo(PurchaseStatus.CANCELLED);
      verify(requestRepository).updateById(request);
    }

    @Test
    @DisplayName("已完成或已取消的申请不能取消")
    void cancelRequest_shouldFail_whenCompleted() {
      // Given
      PurchaseRequest request =
          PurchaseRequest.builder().id(TEST_REQUEST_ID).status(PurchaseStatus.COMPLETED).build();

      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(request);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> purchaseAppService.cancelRequest(TEST_REQUEST_ID));
      assertThat(exception.getMessage()).contains("不允许取消");
    }
  }

  @Nested
  @DisplayName("查询采购申请测试")
  class QueryRequestTests {

    @Test
    @DisplayName("应该成功分页查询采购申请")
    void listRequests_shouldSuccess() {
      // Given
      PageQuery query = new PageQuery();
      query.setPageNum(1);
      query.setPageSize(10);

      PurchaseRequest request =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .requestNo("PR2024001")
              .title("采购申请1")
              .build();

      Page<PurchaseRequest> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(request));
      page.setTotal(1L);

      @SuppressWarnings("unchecked")
      Page<PurchaseRequest> pageParam = any(Page.class);
      when(requestRepository.findPage(pageParam, any(), any(), any(), any(), any()))
          .thenReturn(page);
      when(userRepository.listByIds(anyList())).thenReturn(Collections.emptyList());

      // When
      PageResult<PurchaseRequestDTO> result =
          purchaseAppService.listRequests(query, null, null, null, null, null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getRequestNo()).isEqualTo("PR2024001");
    }

    @Test
    @DisplayName("应该成功获取采购申请详情")
    void getRequestById_shouldSuccess() {
      // Given
      PurchaseItem item =
          PurchaseItem.builder()
              .id(TEST_ITEM_ID)
              .requestId(TEST_REQUEST_ID)
              .itemName("A4纸")
              .quantity(10)
              .receivedQuantity(0)
              .build();

      PurchaseRequest requestWithId =
          PurchaseRequest.builder()
              .id(TEST_REQUEST_ID)
              .requestNo("PR2024001")
              .title("采购申请1")
              .build();
      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(requestWithId);
      when(itemRepository.findByRequestId(TEST_REQUEST_ID))
          .thenReturn(Collections.singletonList(item));

      // When
      PurchaseRequestDTO result = purchaseAppService.getRequestById(TEST_REQUEST_ID);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRequestNo()).isEqualTo("PR2024001");
      assertThat(result.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("申请不存在应该失败")
    void getRequestById_shouldFail_whenNotFound() {
      // Given
      when(requestRepository.getById(TEST_REQUEST_ID)).thenReturn(null);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> purchaseAppService.getRequestById(TEST_REQUEST_ID));
      assertThat(exception.getMessage()).contains("采购申请不存在");
    }
  }
}
