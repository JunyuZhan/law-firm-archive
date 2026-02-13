package com.lawfirm.application.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.client.command.CreateClientCommand;
import com.lawfirm.application.client.command.UpdateClientCommand;
import com.lawfirm.application.client.dto.ClientDTO;
import com.lawfirm.application.client.dto.ClientQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/** ClientAppService 单元测试 测试客户管理核心业务逻辑 */
@ExtendWith(MockitoExtension.class)
class ClientAppServiceTest {

  @Mock private ClientRepository clientRepository;

  @Mock private ClientMapper clientMapper;

  @Mock private UserRepository userRepository;

  @Mock private MatterMapper matterMapper;

  @InjectMocks private ClientAppService clientAppService;

  private MockedStatic<SecurityUtils> securityUtilsMock;

  @BeforeEach
  void setUp() {
    securityUtilsMock = mockStatic(SecurityUtils.class);
    securityUtilsMock.when(SecurityUtils::getUserId).thenReturn(1L);
    securityUtilsMock.when(SecurityUtils::getDepartmentId).thenReturn(1L);
    securityUtilsMock.when(SecurityUtils::getDataScope).thenReturn("ALL");
  }

  @AfterEach
  void tearDown() {
    if (securityUtilsMock != null) {
      securityUtilsMock.close();
    }
  }

  // ==================== 创建客户测试 ====================

  @Nested
  @DisplayName("创建客户测试")
  class CreateClientTests {

    @Test
    @DisplayName("成功创建企业客户")
    void createClient_Enterprise_Success() {
      // Given
      CreateClientCommand command = new CreateClientCommand();
      command.setName("测试企业有限公司");
      command.setClientType("ENTERPRISE");
      command.setCreditCode("91110000123456789X");
      command.setLegalRepresentative("张三");
      command.setContactPerson("李四");
      command.setContactPhone("13800138000");
      command.setLevel("A");
      command.setCategory("VIP");

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Client> wrapper1 = any(LambdaQueryWrapper.class);
      when(clientRepository.count(wrapper1)).thenReturn(0L);
      when(clientRepository.save(any(Client.class))).thenReturn(true);

      // When
      ClientDTO result = clientAppService.createClient(command);

      // Then
      assertNotNull(result);
      assertEquals("测试企业有限公司", result.getName());
      assertEquals("ENTERPRISE", result.getClientType());
      assertEquals("91110000123456789X", result.getCreditCode());
      assertEquals("POTENTIAL", result.getStatus());
      assertNotNull(result.getClientNo());
      assertTrue(result.getClientNo().startsWith("C"));
      verify(clientRepository).save(any(Client.class));
    }

    @Test
    @DisplayName("成功创建个人客户")
    void createClient_Individual_Success() {
      // Given
      CreateClientCommand command = new CreateClientCommand();
      command.setName("王五");
      command.setClientType("INDIVIDUAL");
      command.setIdCard("110101199001011234");
      command.setContactPhone("13900139000");

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Client> wrapper2 = any(LambdaQueryWrapper.class);
      when(clientRepository.count(wrapper2)).thenReturn(0L);
      when(clientRepository.save(any(Client.class))).thenReturn(true);

      // When
      ClientDTO result = clientAppService.createClient(command);

      // Then
      assertNotNull(result);
      assertEquals("王五", result.getName());
      assertEquals("INDIVIDUAL", result.getClientType());
      assertEquals("110101199001011234", result.getIdCard());
      verify(clientRepository).save(any(Client.class));
    }

    @Test
    @DisplayName("客户名称重复时创建失败")
    void createClient_DuplicateName() {
      // Given
      CreateClientCommand command = new CreateClientCommand();
      command.setName("重复名称公司");
      command.setClientType("ENTERPRISE");
      command.setCreditCode("91110000123456789X");

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Client> wrapper3 = any(LambdaQueryWrapper.class);
      when(clientRepository.count(wrapper3)).thenReturn(1L);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> clientAppService.createClient(command));
      assertEquals("客户名称已存在", exception.getMessage());
    }

    @Test
    @DisplayName("企业客户缺少信用代码时创建失败")
    void createClient_Enterprise_MissingCreditCode() {
      // Given
      CreateClientCommand command = new CreateClientCommand();
      command.setName("测试企业");
      command.setClientType("ENTERPRISE");
      // 缺少信用代码

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Client> wrapper4 = any(LambdaQueryWrapper.class);
      when(clientRepository.count(wrapper4)).thenReturn(0L);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> clientAppService.createClient(command));
      assertEquals("企业客户必须填写统一社会信用代码", exception.getMessage());
    }

    @Test
    @DisplayName("个人客户缺少身份证时创建失败")
    void createClient_Individual_MissingIdCard() {
      // Given
      CreateClientCommand command = new CreateClientCommand();
      command.setName("张三");
      command.setClientType("INDIVIDUAL");
      // 缺少身份证

      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Client> wrapper5 = any(LambdaQueryWrapper.class);
      when(clientRepository.count(wrapper5)).thenReturn(0L);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> clientAppService.createClient(command));
      assertEquals("个人客户必须填写身份证号", exception.getMessage());
    }
  }

  // ==================== 更新客户测试 ====================

  @Nested
  @DisplayName("更新客户测试")
  class UpdateClientTests {

    @Test
    @DisplayName("成功更新客户信息")
    void updateClient_Success() {
      // Given
      Client existingClient =
          Client.builder()
              .id(1L)
              .name("原客户名称")
              .clientType("ENTERPRISE")
              .creditCode("91110000123456789X")
              .contactPhone("13800138000")
              .build();

      UpdateClientCommand command = new UpdateClientCommand();
      command.setId(1L);
      command.setName("更新后的客户名称");
      command.setContactPhone("13900139000");
      command.setLevel("A");

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(existingClient);
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Client> wrapper6 = any(LambdaQueryWrapper.class);
      when(clientRepository.count(wrapper6)).thenReturn(0L);
      when(clientRepository.updateById(any(Client.class))).thenReturn(true);

      // When
      ClientDTO result = clientAppService.updateClient(command);

      // Then
      assertNotNull(result);
      assertEquals("更新后的客户名称", existingClient.getName());
      assertEquals("13900139000", existingClient.getContactPhone());
      assertEquals("A", existingClient.getLevel());
      verify(clientRepository).updateById(existingClient);
    }

    @Test
    @DisplayName("更新时客户名称重复失败")
    void updateClient_DuplicateName() {
      // Given
      Client existingClient = Client.builder().id(1L).name("原客户名称").build();

      UpdateClientCommand command = new UpdateClientCommand();
      command.setId(1L);
      command.setName("重复的客户名称");

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(existingClient);
      @SuppressWarnings("unchecked")
      LambdaQueryWrapper<Client> wrapper7 = any(LambdaQueryWrapper.class);
      when(clientRepository.count(wrapper7)).thenReturn(1L);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> clientAppService.updateClient(command));
      assertEquals("客户名称已存在", exception.getMessage());
    }
  }

  // ==================== 删除客户测试 ====================

  @Nested
  @DisplayName("删除客户测试")
  class DeleteClientTests {

    @Test
    @DisplayName("成功删除客户")
    void deleteClient_Success() {
      // Given
      Client client = Client.builder().id(1L).name("待删除客户").build();

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
      when(matterMapper.countByClientId(1L)).thenReturn(0);
      when(clientMapper.deleteById(1L)).thenReturn(1);

      // When
      clientAppService.deleteClient(1L);

      // Then
      verify(clientMapper).deleteById(1L);
    }

    @Test
    @DisplayName("有关联案件时删除失败")
    void deleteClient_HasRelatedMatters() {
      // Given
      Client client = Client.builder().id(1L).name("有关联案件的客户").build();

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
      when(matterMapper.countByClientId(1L)).thenReturn(2);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> clientAppService.deleteClient(1L));
      assertEquals("该客户存在关联案件，无法删除", exception.getMessage());
    }
  }

  // ==================== 获取客户详情测试 ====================

  @Nested
  @DisplayName("获取客户详情测试")
  class GetClientTests {

    @Test
    @DisplayName("成功获取客户详情")
    void getClientById_Success() {
      // Given
      Client client =
          Client.builder()
              .id(1L)
              .clientNo("C240112ABCD")
              .name("测试客户")
              .clientType("ENTERPRISE")
              .status("ACTIVE")
              .originatorId(2L)
              .responsibleLawyerId(3L)
              .build();

      User originator = new User();
      originator.setId(2L);
      originator.setRealName("案源人");

      User lawyer = new User();
      lawyer.setId(3L);
      lawyer.setRealName("负责律师");

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
      when(userRepository.findById(2L)).thenReturn(originator);
      when(userRepository.findById(3L)).thenReturn(lawyer);

      // When
      ClientDTO result = clientAppService.getClientById(1L);

      // Then
      assertNotNull(result);
      assertEquals("C240112ABCD", result.getClientNo());
      assertEquals("测试客户", result.getName());
      assertEquals("案源人", result.getOriginatorName());
      assertEquals("负责律师", result.getResponsibleLawyerName());
    }
  }

  // ==================== 客户状态管理测试 ====================

  @Nested
  @DisplayName("客户状态管理测试")
  class ClientStatusTests {

    @Test
    @DisplayName("成功修改客户状态")
    void changeStatus_Success() {
      // Given
      Client client = Client.builder().id(1L).name("测试客户").status("POTENTIAL").build();

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
      when(clientRepository.updateById(any(Client.class))).thenReturn(true);

      // When
      clientAppService.changeStatus(1L, "ACTIVE");

      // Then
      assertEquals("ACTIVE", client.getStatus());
      verify(clientRepository).updateById(client);
    }

    @Test
    @DisplayName("成功转正式客户")
    void convertToFormal_Success() {
      // Given
      Client client =
          Client.builder()
              .id(1L)
              .name("潜在客户")
              .status("POTENTIAL")
              .firstCooperationDate(null)
              .build();

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);
      when(clientRepository.updateById(any(Client.class))).thenReturn(true);

      // When
      clientAppService.convertToFormal(1L);

      // Then
      assertEquals("ACTIVE", client.getStatus());
      assertNotNull(client.getFirstCooperationDate());
      verify(clientRepository).updateById(client);
    }

    @Test
    @DisplayName("非潜在客户转正式失败")
    void convertToFormal_NotPotential() {
      // Given
      Client client = Client.builder().id(1L).name("已是正式客户").status("ACTIVE").build();

      when(clientRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(client);

      // When & Then
      BusinessException exception =
          assertThrows(BusinessException.class, () -> clientAppService.convertToFormal(1L));
      assertEquals("只有潜在客户可以转为正式客户", exception.getMessage());
    }
  }

  // ==================== 利冲审查测试 ====================

  @Nested
  @DisplayName("利冲审查测试")
  class ConflictCheckTests {

    @Test
    @DisplayName("成功搜索客户用于利冲审查")
    @SuppressWarnings("unchecked")
    void searchClientsForConflictCheck_Success() {
      // Given
      List<Client> clients =
          Arrays.asList(
              Client.builder().id(1L).name("测试公司A").deleted(false).build(),
              Client.builder().id(2L).name("测试公司B").deleted(false).build());

      // Mock lambdaQuery chain - use doReturn to mock the chain
      when(clientRepository.lambdaQuery())
          .thenAnswer(
              invocation -> {
                // Create a mock chain wrapper that returns clients when list() is called
                LambdaQueryChainWrapper<Client> chain = mock(LambdaQueryChainWrapper.class);
                when(chain.eq(any(), any())).thenReturn(chain);
                when(chain.like(any(), any())).thenReturn(chain);
                when(chain.last(anyString())).thenReturn(chain);
                when(chain.list()).thenReturn(clients);
                return chain;
              });

      // When
      List<ClientDTO> result = clientAppService.searchClientsForConflictCheck("测试", 10);

      // Then
      assertNotNull(result);
      assertEquals(2, result.size());
    }

    @Test
    @DisplayName("关键词太短时返回空列表")
    void searchClientsForConflictCheck_KeywordTooShort() {
      // When
      List<ClientDTO> result = clientAppService.searchClientsForConflictCheck("a", 10);

      // Then
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("关键词为空时返回空列表")
    void searchClientsForConflictCheck_EmptyKeyword() {
      // When
      List<ClientDTO> result = clientAppService.searchClientsForConflictCheck("", 10);

      // Then
      assertTrue(result.isEmpty());
    }
  }

  // ==================== 分页查询测试 ====================

  @Nested
  @DisplayName("分页查询测试")
  class ListClientsTests {

    @Test
    @DisplayName("ALL权限查询所有客户")
    void listClients_AllPermission() {
      // Given
      ClientQueryDTO query = new ClientQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      List<Client> clients =
          Arrays.asList(
              Client.builder().id(1L).name("客户A").originatorId(2L).build(),
              Client.builder().id(2L).name("客户B").responsibleLawyerId(3L).build());

      Page<Client> page = new Page<>(1, 10);
      page.setRecords(clients);
      page.setTotal(2);

      when(clientMapper.selectClientPage(any(), any(), any(), any(), any(), any()))
          .thenReturn(page);
      when(userRepository.listByIds(any()))
          .thenReturn(Arrays.asList(createUser(2L, "案源人"), createUser(3L, "负责律师")));

      // When
      PageResult<ClientDTO> result = clientAppService.listClients(query);

      // Then
      assertNotNull(result);
      assertEquals(2, result.getTotal());
      assertEquals(2, result.getRecords().size());
    }
  }

  // ==================== 辅助方法 ====================

  private User createUser(Long id, String realName) {
    User user = new User();
    user.setId(id);
    user.setRealName(realName);
    return user;
  }
}
