package com.lawfirm.application.archive.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.archive.command.CreateBorrowCommand;
import com.lawfirm.application.archive.command.ReturnArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveBorrowDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.constant.ArchiveBorrowStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.archive.entity.Archive;
import com.lawfirm.domain.archive.entity.ArchiveBorrow;
import com.lawfirm.domain.archive.repository.ArchiveBorrowRepository;
import com.lawfirm.domain.archive.repository.ArchiveOperationLogRepository;
import com.lawfirm.domain.archive.repository.ArchiveRepository;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveBorrowMapper;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** ArchiveBorrowAppService 单元测试 测试档案借阅服务的核心功能 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveBorrowAppService 档案借阅服务测试")
class ArchiveBorrowAppServiceTest {

  private static final Long TEST_BORROW_ID = 100L;
  private static final Long TEST_ARCHIVE_ID = 200L;
  private static final Long TEST_USER_ID = 1L;

  @Mock private ArchiveBorrowRepository borrowRepository;

  @Mock private ArchiveBorrowMapper borrowMapper;

  @Mock private ArchiveRepository archiveRepository;

  @Mock private ArchiveOperationLogRepository operationLogRepository;

  @InjectMocks private ArchiveBorrowAppService borrowAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  @Nested
  @DisplayName("查询借阅记录测试")
  class QueryBorrowTests {

    @Test
    @DisplayName("应该成功分页查询借阅记录")
    @SuppressWarnings("unchecked")
    void listBorrows_shouldSuccess() {
      // Given
      ArchiveBorrow borrow =
          ArchiveBorrow.builder()
              .id(TEST_BORROW_ID)
              .borrowNo("BRW2024001")
              .archiveId(TEST_ARCHIVE_ID)
              .borrowerId(TEST_USER_ID)
              .status(ArchiveBorrowStatus.PENDING)
              .build();

      Page<ArchiveBorrow> page = new Page<>(1, 10);
      page.setRecords(Collections.singletonList(borrow));
      page.setTotal(1L);

      when(borrowRepository.page(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

      PageQuery query = new PageQuery();
      query.setPageNum(1);
      query.setPageSize(10);

      // When
      PageResult<ArchiveBorrowDTO> result = borrowAppService.listBorrows(query, null, null);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getTotal()).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("创建借阅申请测试")
  class CreateBorrowTests {

    @Test
    @DisplayName("应该成功创建借阅申请")
    void createBorrow_shouldSuccess() {
      // Given
      CreateBorrowCommand command = new CreateBorrowCommand();
      command.setArchiveId(TEST_ARCHIVE_ID);
      command.setBorrowReason("工作需要");
      command.setBorrowDate(LocalDate.now());
      command.setExpectedReturnDate(LocalDate.now().plusDays(7));

      Archive archive =
          Archive.builder()
              .id(TEST_ARCHIVE_ID)
              .archiveNo("ARC2024001")
              .status(ArchiveBorrowStatus.ARCHIVE_STORED) // 已入库
              .build();

      when(archiveRepository.getByIdOrThrow(eq(TEST_ARCHIVE_ID), anyString())).thenReturn(archive);
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveBorrow> wrapper1 = any(LambdaQueryWrapper.class);
      when(borrowRepository.count(wrapper1)).thenReturn(0L);
      when(borrowRepository.save(any(ArchiveBorrow.class)))
          .thenAnswer(
              invocation -> {
                ArchiveBorrow borrow = invocation.getArgument(0);
                borrow.setId(TEST_BORROW_ID);
                borrow.setBorrowNo("BRW2024001");
                return true;
              });

      // When
      ArchiveBorrowDTO result = borrowAppService.createBorrow(command);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ArchiveBorrowStatus.PENDING);
      assertThat(result.getBorrowerId()).isEqualTo(TEST_USER_ID);
      verify(borrowRepository).save(any(ArchiveBorrow.class));
    }

    @Test
    @DisplayName("未入库的档案不能借阅")
    void createBorrow_shouldFail_whenArchiveNotStored() {
      // Given
      CreateBorrowCommand command = new CreateBorrowCommand();
      command.setArchiveId(TEST_ARCHIVE_ID);

      Archive archive =
          Archive.builder()
              .id(TEST_ARCHIVE_ID)
              .status("DRAFT") // 未入库
              .build();

      when(archiveRepository.getByIdOrThrow(eq(TEST_ARCHIVE_ID), anyString())).thenReturn(archive);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> borrowAppService.createBorrow(command));
      assertThat(exception.getMessage()).contains("已入库");
    }

    @Test
    @DisplayName("已有未归还借阅的档案不能重复借阅")
    void createBorrow_shouldFail_whenHasUnreturnedBorrow() {
      // Given
      CreateBorrowCommand command = new CreateBorrowCommand();
      command.setArchiveId(TEST_ARCHIVE_ID);

      Archive archive =
          Archive.builder().id(TEST_ARCHIVE_ID).status(ArchiveBorrowStatus.ARCHIVE_STORED).build();

      when(archiveRepository.getByIdOrThrow(eq(TEST_ARCHIVE_ID), anyString())).thenReturn(archive);
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<ArchiveBorrow> wrapper2 = any(LambdaQueryWrapper.class);
      when(borrowRepository.count(wrapper2)).thenReturn(1L); // 有未归还记录

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> borrowAppService.createBorrow(command));
      assertThat(exception.getMessage()).contains("未归还的借阅记录");
    }
  }

  @Nested
  @DisplayName("审批借阅申请测试")
  class ApproveBorrowTests {

    @Test
    @DisplayName("应该成功审批通过借阅申请")
    void approveBorrow_shouldSuccess() {
      // Given
      ArchiveBorrow borrow =
          ArchiveBorrow.builder()
              .id(TEST_BORROW_ID)
              .borrowNo("BRW2024001")
              .archiveId(TEST_ARCHIVE_ID)
              .status(ArchiveBorrowStatus.PENDING)
              .build();

      when(borrowRepository.getByIdOrThrow(eq(TEST_BORROW_ID), anyString())).thenReturn(borrow);
      when(borrowRepository.updateById(any(ArchiveBorrow.class))).thenReturn(true);

      // When
      borrowAppService.approveBorrow(TEST_BORROW_ID);

      // Then
      assertThat(borrow.getStatus()).isEqualTo(ArchiveBorrowStatus.APPROVED);
      verify(borrowRepository).updateById(borrow);
    }

    @Test
    @DisplayName("已处理的申请不能重复审批")
    void approveBorrow_shouldFail_whenAlreadyProcessed() {
      // Given
      ArchiveBorrow borrow =
          ArchiveBorrow.builder()
              .id(TEST_BORROW_ID)
              .status(ArchiveBorrowStatus.APPROVED) // 已审批
              .build();

      when(borrowRepository.getByIdOrThrow(eq(TEST_BORROW_ID), anyString())).thenReturn(borrow);

      // When & Then
      BusinessException exception =
          assertThrows(
              BusinessException.class, () -> borrowAppService.approveBorrow(TEST_BORROW_ID));
      assertThat(exception.getMessage()).contains("不允许审批");
    }
  }

  @Nested
  @DisplayName("归还档案测试")
  class ReturnArchiveTests {

    @Test
    @DisplayName("应该成功归还档案")
    void returnArchive_shouldSuccess() {
      // Given
      ArchiveBorrow borrow =
          ArchiveBorrow.builder()
              .id(TEST_BORROW_ID)
              .borrowNo("BRW2024001")
              .archiveId(TEST_ARCHIVE_ID)
              .borrowerId(TEST_USER_ID)
              .status(ArchiveBorrowStatus.BORROWED)
              .build();

      Archive archive =
          Archive.builder()
              .id(TEST_ARCHIVE_ID)
              .status(ArchiveBorrowStatus.ARCHIVE_BORROWED)
              .build();

      ReturnArchiveCommand command = new ReturnArchiveCommand();
      command.setBorrowId(TEST_BORROW_ID);
      command.setReturnCondition("完好");
      command.setReturnRemarks("备注");

      when(borrowRepository.getByIdOrThrow(eq(TEST_BORROW_ID), anyString())).thenReturn(borrow);
      when(archiveRepository.getByIdOrThrow(eq(TEST_ARCHIVE_ID), anyString())).thenReturn(archive);
      when(borrowRepository.updateById(any(ArchiveBorrow.class))).thenReturn(true);
      when(archiveRepository.updateById(any(Archive.class))).thenReturn(true);
      when(operationLogRepository.save(any())).thenReturn(true);

      // When
      borrowAppService.returnArchive(command);

      // Then
      assertThat(borrow.getStatus()).isEqualTo(ArchiveBorrowStatus.RETURNED);
      assertThat(borrow.getActualReturnDate()).isNotNull();
      verify(borrowRepository).updateById(borrow);
    }

    @Test
    @DisplayName("未借出的档案不能归还")
    void returnArchive_shouldFail_whenNotBorrowed() {
      // Given
      ArchiveBorrow borrow =
          ArchiveBorrow.builder()
              .id(TEST_BORROW_ID)
              .status(ArchiveBorrowStatus.PENDING) // 待审批
              .build();

      ReturnArchiveCommand command = new ReturnArchiveCommand();
      command.setBorrowId(TEST_BORROW_ID);

      when(borrowRepository.getByIdOrThrow(eq(TEST_BORROW_ID), anyString())).thenReturn(borrow);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> borrowAppService.returnArchive(command));
      assertThat(exception.getMessage()).contains("借出或逾期");
    }
  }
}
