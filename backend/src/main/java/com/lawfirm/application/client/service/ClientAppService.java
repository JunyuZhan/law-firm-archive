package com.lawfirm.application.client.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.lawfirm.infrastructure.external.excel.ExcelImportExportService;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** 客户应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAppService {

  /** 客户仓储 */
  private final ClientRepository clientRepository;

  /** 客户Mapper */
  private final ClientMapper clientMapper;

  /** Excel导入导出服务 */
  private final ExcelImportExportService excelImportExportService;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /** 案件Mapper */
  private final com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterMapper;

  /** 导出最大记录数 */
  private static final int EXPORT_MAX_RECORDS = 10000;

  /**
   * 分页查询客户 数据权限： - ALL: 可查看全部客户 - DEPT/DEPT_AND_CHILD: 可查看部门内用户负责的客户 - SELF: 只能查看自己的客户（我是负责律师 OR
   * 我是案源人） 优化：使用批量查询避免N+1问题
   *
   * @param query 客户查询条件对象
   * @return 分页结果对象，包含客户DTO列表和分页信息
   */
  public PageResult<ClientDTO> listClients(final ClientQueryDTO query) {
    Long currentUserId = SecurityUtils.getUserId();
    String dataScope = SecurityUtils.getDataScope();

    // 根据数据权限范围决定过滤逻辑
    List<Long> myClientIds = null;
    if (!"ALL".equals(dataScope)) {
      // 非 ALL 权限：获取自己的客户ID列表（负责律师或案源人）
      myClientIds = getMyClientIds(currentUserId);

      // 如果没有自己的客户，返回空结果
      if (myClientIds.isEmpty()) {
        return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
      }
    }
    // ALL 权限：myClientIds 为 null，表示不过滤

    IPage<Client> page =
        clientMapper.selectClientPage(
            new Page<>(query.getPageNum(), query.getPageSize()),
            query.getName(),
            query.getClientType(),
            query.getStatus(),
            query.getResponsibleLawyerId(),
            myClientIds);

    List<Client> clients = page.getRecords();
    if (clients.isEmpty()) {
      return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
    }

    // 批量加载用户信息（案源人和负责律师），避免N+1查询
    Map<Long, User> userMap = batchLoadUsers(clients);

    List<ClientDTO> records =
        clients.stream().map(client -> toDTO(client, userMap)).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 批量加载用户信息（案源人和负责律师） 避免N+1查询问题
   *
   * @param clients 客户列表
   * @return 用户ID到用户实体的映射
   */
  private Map<Long, User> batchLoadUsers(final List<Client> clients) {
    Set<Long> userIds = new HashSet<>();
    for (Client c : clients) {
      if (c.getOriginatorId() != null) {
        userIds.add(c.getOriginatorId());
      }
      if (c.getResponsibleLawyerId() != null) {
        userIds.add(c.getResponsibleLawyerId());
      }
    }

    if (userIds.isEmpty()) {
      return Collections.emptyMap();
    }

    return userRepository.listByIds(new ArrayList<>(userIds)).stream()
        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
  }

  /**
   * 获取自己的客户ID列表（我是负责律师 OR 我是案源人）
   *
   * @param userId 用户ID
   * @return 客户ID列表
   */
  private List<Long> getMyClientIds(final Long userId) {
    return clientRepository
        .lambdaQuery()
        .select(Client::getId)
        .eq(Client::getDeleted, false)
        .and(
            wrapper ->
                wrapper
                    .eq(Client::getResponsibleLawyerId, userId)
                    .or()
                    .eq(Client::getOriginatorId, userId))
        .list()
        .stream()
        .map(Client::getId)
        .collect(Collectors.toList());
  }

  /**
   * 利冲审查 - 搜索全所客户（用于对方当事人字段） 所有人都可以搜索全所客户用于利冲审查
   *
   * @param keyword 搜索关键词
   * @param limit 最大返回数量
   * @return 客户DTO列表
   */
  public List<ClientDTO> searchClientsForConflictCheck(final String keyword, final int limit) {
    if (!StringUtils.hasText(keyword) || keyword.length() < 2) {
      return Collections.emptyList();
    }

    // 限制最大查询数量，防止参数被恶意利用
    int safeLimit = Math.min(Math.max(1, limit), 100);

    // 全所客户搜索，不限制权限
    List<Client> clients =
        clientRepository
            .lambdaQuery()
            .eq(Client::getDeleted, false)
            .like(Client::getName, keyword)
            .last("LIMIT " + safeLimit)
            .list();

    // 批量加载用户信息
    Map<Long, User> userMap = batchLoadUsers(clients);

    return clients.stream().map(c -> toDTO(c, userMap)).collect(Collectors.toList());
  }

  /**
   * 创建客户
   *
   * @param command 创建客户命令对象
   * @return 创建成功的客户DTO
   * @throws BusinessException 当客户名称已存在或必要信息缺失时抛出异常
   */
  @Transactional
  public ClientDTO createClient(final CreateClientCommand command) {
    // 1. 验证客户名称唯一性
    if (clientRepository.count(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Client>()
                .eq(Client::getName, command.getName()))
        > 0) {
      throw new BusinessException("客户名称已存在");
    }

    // 2. 企业客户验证信用代码
    if ("ENTERPRISE".equals(command.getClientType())
        && !StringUtils.hasText(command.getCreditCode())) {
      throw new BusinessException("企业客户必须填写统一社会信用代码");
    }

    // 3. 个人客户验证身份证
    if ("INDIVIDUAL".equals(command.getClientType()) && !StringUtils.hasText(command.getIdCard())) {
      throw new BusinessException("个人客户必须填写身份证号");
    }

    // 4. 生成客户编号
    String clientNo = generateClientNo();

    // 5. 创建客户实体
    Client client =
        Client.builder()
            .clientNo(clientNo)
            .name(command.getName())
            .clientType(command.getClientType())
            .creditCode(command.getCreditCode())
            .idCard(command.getIdCard())
            .legalRepresentative(command.getLegalRepresentative())
            .registeredAddress(command.getRegisteredAddress())
            .contactPerson(command.getContactPerson())
            .contactPhone(command.getContactPhone())
            .contactEmail(command.getContactEmail())
            .industry(command.getIndustry())
            .source(command.getSource())
            .level(command.getLevel() != null ? command.getLevel() : "B")
            .category(command.getCategory() != null ? command.getCategory() : "NORMAL")
            .status("POTENTIAL")
            .originatorId(
                command.getOriginatorId() != null
                    ? command.getOriginatorId()
                    : SecurityUtils.getUserId())
            .responsibleLawyerId(
                command.getResponsibleLawyerId() != null
                    ? command.getResponsibleLawyerId()
                    : SecurityUtils.getUserId())
            .firstCooperationDate(command.getFirstCooperationDate())
            .remark(command.getRemark())
            .build();

    // 6. 保存客户
    clientRepository.save(client);

    log.info("客户创建成功: {} ({})", client.getName(), client.getClientNo());
    return toDTO(client);
  }

  /**
   * 更新客户信息
   *
   * @param command 更新客户命令对象
   * @return 更新后的客户DTO
   * @throws BusinessException 当客户不存在或名称重复时抛出异常
   */
  @Transactional
  public ClientDTO updateClient(final UpdateClientCommand command) {
    Client client = clientRepository.getByIdOrThrow(command.getId(), "客户不存在");

    // 更新字段
    if (StringUtils.hasText(command.getName())) {
      // 检查名称唯一性（排除自己）
      if (clientRepository.count(
              new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Client>()
                  .eq(Client::getName, command.getName())
                  .ne(Client::getId, command.getId()))
          > 0) {
        throw new BusinessException("客户名称已存在");
      }
      client.setName(command.getName());
    }
    if (StringUtils.hasText(command.getClientType())) {
      client.setClientType(command.getClientType());
    }
    if (command.getCreditCode() != null) {
      client.setCreditCode(command.getCreditCode());
    }
    if (command.getIdCard() != null) {
      client.setIdCard(command.getIdCard());
    }
    if (command.getLegalRepresentative() != null) {
      client.setLegalRepresentative(command.getLegalRepresentative());
    }
    if (command.getRegisteredAddress() != null) {
      client.setRegisteredAddress(command.getRegisteredAddress());
    }
    if (command.getContactPerson() != null) {
      client.setContactPerson(command.getContactPerson());
    }
    if (command.getContactPhone() != null) {
      client.setContactPhone(command.getContactPhone());
    }
    if (command.getContactEmail() != null) {
      client.setContactEmail(command.getContactEmail());
    }
    if (command.getIndustry() != null) {
      client.setIndustry(command.getIndustry());
    }
    if (command.getSource() != null) {
      client.setSource(command.getSource());
    }
    if (command.getLevel() != null) {
      client.setLevel(command.getLevel());
    }
    if (command.getCategory() != null) {
      client.setCategory(command.getCategory());
    }
    if (command.getStatus() != null) {
      client.setStatus(command.getStatus());
    }
    if (command.getOriginatorId() != null) {
      client.setOriginatorId(command.getOriginatorId());
    }
    if (command.getResponsibleLawyerId() != null) {
      client.setResponsibleLawyerId(command.getResponsibleLawyerId());
    }
    if (command.getFirstCooperationDate() != null) {
      client.setFirstCooperationDate(command.getFirstCooperationDate());
    }
    if (command.getRemark() != null) {
      client.setRemark(command.getRemark());
    }

    clientRepository.updateById(client);
    log.info("客户更新成功: {}", client.getName());
    return toDTO(client);
  }

  /**
   * 删除客户
   *
   * @param id 客户ID
   * @throws BusinessException 当客户不存在或存在关联案件时抛出异常
   */
  @Transactional
  public void deleteClient(final Long id) {
    Client client = clientRepository.getByIdOrThrow(id, "客户不存在");

    // 检查是否有关联案件
    int matterCount = matterMapper.countByClientId(id);
    if (matterCount > 0) {
      throw new BusinessException("该客户存在关联案件，无法删除");
    }

    clientMapper.deleteById(id);
    log.info("客户删除成功: {}", client.getName());
  }

  /**
   * 获取客户详情
   *
   * @param id 客户ID
   * @return 客户DTO
   */
  public ClientDTO getClientById(final Long id) {
    Client client = clientRepository.getByIdOrThrow(id, "客户不存在");
    return toDTO(client);
  }

  /**
   * 修改客户状态
   *
   * @param id 客户ID
   * @param status 新状态
   */
  @Transactional
  public void changeStatus(final Long id, final String status) {
    Client client = clientRepository.getByIdOrThrow(id, "客户不存在");
    client.setStatus(status);
    clientRepository.updateById(client);
    log.info("客户状态修改成功: {} -> {}", client.getName(), status);
  }

  /**
   * 转正式客户
   *
   * @param id 客户ID
   * @throws BusinessException 当客户状态不是潜在客户时抛出异常
   */
  @Transactional
  public void convertToFormal(final Long id) {
    Client client = clientRepository.getByIdOrThrow(id, "客户不存在");
    if (!"POTENTIAL".equals(client.getStatus())) {
      throw new BusinessException("只有潜在客户可以转为正式客户");
    }
    client.setStatus("ACTIVE");
    if (client.getFirstCooperationDate() == null) {
      client.setFirstCooperationDate(LocalDate.now());
    }
    clientRepository.updateById(client);
    log.info("客户转正式成功: {}", client.getName());
  }

  /**
   * 生成客户编号
   *
   * @return 客户编号
   */
  private String generateClientNo() {
    String prefix = "C" + LocalDate.now().toString().replace("-", "").substring(2);
    String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    return prefix + random;
  }

  /**
   * 获取客户类型名称
   *
   * @param type 客户类型代码
   * @return 客户类型名称
   */
  private String getClientTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "INDIVIDUAL" -> "个人";
      case "ENTERPRISE" -> "企业";
      case "GOVERNMENT" -> "政府机关";
      case "OTHER" -> "其他";
      default -> type;
    };
  }

  /**
   * 获取客户级别名称
   *
   * @param level 客户级别代码
   * @return 客户级别名称
   */
  private String getLevelName(final String level) {
    if (level == null) {
      return null;
    }
    return switch (level) {
      case "A" -> "重要客户";
      case "B" -> "普通客户";
      case "C" -> "一般客户";
      default -> level;
    };
  }

  /**
   * 获取状态名称
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    if (status == null) {
      return null;
    }
    return switch (status) {
      case "POTENTIAL" -> "潜在";
      case "ACTIVE" -> "正式";
      case "INACTIVE" -> "休眠";
      case "BLACKLIST" -> "黑名单";
      default -> status;
    };
  }

  /**
   * Entity 转 DTO（单个查询时使用，会产生N+1查询）
   *
   * @param client 客户实体
   * @return 客户DTO
   */
  private ClientDTO toDTO(final Client client) {
    // 单个客户查询时，直接查询用户信息
    Map<Long, User> userMap = new HashMap<>();
    if (client.getOriginatorId() != null) {
      User user = userRepository.findById(client.getOriginatorId());
      if (user != null) {
        userMap.put(user.getId(), user);
      }
    }
    if (client.getResponsibleLawyerId() != null
        && !userMap.containsKey(client.getResponsibleLawyerId())) {
      User user = userRepository.findById(client.getResponsibleLawyerId());
      if (user != null) {
        userMap.put(user.getId(), user);
      }
    }
    return toDTO(client, userMap);
  }

  /**
   * Entity 转 DTO（批量查询优化版本，从预加载的Map获取用户信息）
   *
   * @param client 客户实体
   * @param userMap 用户映射
   * @return 客户DTO
   */
  private ClientDTO toDTO(final Client client, final Map<Long, User> userMap) {
    ClientDTO dto = new ClientDTO();
    dto.setId(client.getId());
    dto.setClientNo(client.getClientNo());
    dto.setName(client.getName());
    dto.setClientType(client.getClientType());
    dto.setClientTypeName(getClientTypeName(client.getClientType()));
    dto.setCreditCode(client.getCreditCode());
    dto.setIdCard(client.getIdCard());
    dto.setLegalRepresentative(client.getLegalRepresentative());
    dto.setRegisteredAddress(client.getRegisteredAddress());
    dto.setContactPerson(client.getContactPerson());
    dto.setContactPhone(client.getContactPhone());
    dto.setContactEmail(client.getContactEmail());
    dto.setIndustry(client.getIndustry());
    dto.setSource(client.getSource());
    dto.setLevel(client.getLevel());
    dto.setLevelName(getLevelName(client.getLevel()));
    dto.setCategory(client.getCategory());
    dto.setCategoryName(getCategoryName(client.getCategory()));
    dto.setStatus(client.getStatus());
    dto.setStatusName(getStatusName(client.getStatus()));
    dto.setOriginatorId(client.getOriginatorId());
    dto.setResponsibleLawyerId(client.getResponsibleLawyerId());
    dto.setFirstCooperationDate(client.getFirstCooperationDate());
    dto.setRemark(client.getRemark());
    dto.setCreatedAt(client.getCreatedAt());
    dto.setUpdatedAt(client.getUpdatedAt());

    // 从Map获取用户名称，避免N+1查询
    if (client.getOriginatorId() != null) {
      User user = userMap.get(client.getOriginatorId());
      if (user != null) {
        dto.setOriginatorName(user.getRealName());
      }
    }
    if (client.getResponsibleLawyerId() != null) {
      User user = userMap.get(client.getResponsibleLawyerId());
      if (user != null) {
        dto.setResponsibleLawyerName(user.getRealName());
      }
    }

    return dto;
  }

  /**
   * 获取客户分类名称
   *
   * @param category 客户分类
   * @return 分类名称
   */
  private String getCategoryName(final String category) {
    if (category == null) {
      return null;
    }
    return switch (category) {
      case "VIP" -> "重要客户";
      case "NORMAL" -> "普通客户";
      case "POTENTIAL" -> "潜在客户";
      default -> category;
    };
  }

  // ========== 客户导出（M2-008，P2） ==========

  /**
   * 导出客户信息为Excel 数据权限：根据角色数据范围过滤 优化：批量加载用户信息，显示姓名而非ID
   *
   * @param query 查询条件
   * @return Excel文件流
   * @throws IOException IO异常
   */
  public ByteArrayInputStream exportClients(final ClientQueryDTO query) throws IOException {
    Long currentUserId = SecurityUtils.getUserId();
    String dataScope = SecurityUtils.getDataScope();

    // 根据数据权限范围决定过滤逻辑
    List<Long> myClientIds = null;
    if (!"ALL".equals(dataScope)) {
      myClientIds = getMyClientIds(currentUserId);
    }

    // 查询所有符合条件的客户（不分页）
    IPage<Client> page =
        clientMapper.selectClientPage(
            new Page<>(1, EXPORT_MAX_RECORDS), // 最大导出10000条
            query.getName(),
            query.getClientType(),
            query.getStatus(),
            query.getResponsibleLawyerId(),
            myClientIds);

    List<Client> clients = page.getRecords();

    // 批量加载用户信息（案源人和负责律师）
    Map<Long, User> userMap = batchLoadUsers(clients);

    // 准备表头
    List<String> headers =
        Arrays.asList(
            "客户编号",
            "客户名称",
            "客户类型",
            "统一社会信用代码",
            "身份证号",
            "法定代表人",
            "注册地址",
            "联系人",
            "联系电话",
            "联系邮箱",
            "所属行业",
            "客户来源",
            "客户级别",
            "客户分类",
            "状态",
            "案源人",
            "负责律师",
            "首次合作日期",
            "备注",
            "创建时间");

    // 准备数据
    List<List<Object>> data = new ArrayList<>();
    for (Client client : clients) {
      List<Object> row = new ArrayList<>();
      row.add(client.getClientNo());
      row.add(client.getName());
      row.add(getClientTypeName(client.getClientType()));
      row.add(client.getCreditCode());
      row.add(client.getIdCard());
      row.add(client.getLegalRepresentative());
      row.add(client.getRegisteredAddress());
      row.add(client.getContactPerson());
      row.add(client.getContactPhone());
      row.add(client.getContactEmail());
      row.add(client.getIndustry());
      row.add(client.getSource());
      row.add(getLevelName(client.getLevel()));
      row.add(getCategoryName(client.getCategory()));
      row.add(getStatusName(client.getStatus()));

      // 从Map获取用户姓名
      String originatorName = "";
      if (client.getOriginatorId() != null && userMap.containsKey(client.getOriginatorId())) {
        originatorName = userMap.get(client.getOriginatorId()).getRealName();
      }
      row.add(originatorName);

      String responsibleLawyerName = "";
      if (client.getResponsibleLawyerId() != null
          && userMap.containsKey(client.getResponsibleLawyerId())) {
        responsibleLawyerName = userMap.get(client.getResponsibleLawyerId()).getRealName();
      }
      row.add(responsibleLawyerName);

      row.add(client.getFirstCooperationDate());
      row.add(client.getRemark());
      row.add(client.getCreatedAt());
      data.add(row);
    }

    return excelImportExportService.createExcel(headers, data, "客户信息");
  }

  // ========== 客户导入（M2-007，P2） ==========

  /**
   * 批量导入客户
   *
   * @param file Excel文件
   * @return 导入结果
   * @throws IOException IO异常
   */
  @Transactional
  public Map<String, Object> importClients(final MultipartFile file) throws IOException {
    List<Map<String, Object>> excelData = excelImportExportService.readExcel(file);

    int successCount = 0;
    int failCount = 0;
    List<String> errorMessages = new ArrayList<>();

    for (int i = 0; i < excelData.size(); i++) {
      Map<String, Object> row = excelData.get(i);
      int rowNum = i + 2; // Excel行号（从2开始，因为第1行是表头）

      try {
        CreateClientCommand command = parseClientFromExcel(row);
        createClient(command);
        successCount++;
      } catch (Exception e) {
        failCount++;
        String errorMsg = String.format("第%d行导入失败: %s", rowNum, e.getMessage());
        errorMessages.add(errorMsg);
        log.error(errorMsg, e);
      }
    }

    Map<String, Object> result = new HashMap<>();
    result.put("total", excelData.size());
    result.put("successCount", successCount);
    result.put("failCount", failCount);
    result.put("errorMessages", errorMessages);

    log.info("客户批量导入完成: 总数={}, 成功={}, 失败={}", excelData.size(), successCount, failCount);
    return result;
  }

  /**
   * 从Excel行数据解析客户信息
   *
   * @param row Excel行数据
   * @return 创建客户命令
   */
  private CreateClientCommand parseClientFromExcel(final Map<String, Object> row) {
    CreateClientCommand command = new CreateClientCommand();

    // 必填字段
    String name = getStringValue(row, "客户名称");
    if (!StringUtils.hasText(name)) {
      throw new BusinessException("客户名称不能为空");
    }
    command.setName(name);

    // 客户类型（必填）
    String clientType = getStringValue(row, "客户类型");
    if (!StringUtils.hasText(clientType)) {
      throw new BusinessException("客户类型不能为空");
    }
    // 转换中文为代码
    clientType = convertClientTypeFromChinese(clientType);
    command.setClientType(clientType);

    // 统一社会信用代码（企业客户必填）
    String creditCode = getStringValue(row, "统一社会信用代码");
    if ("ENTERPRISE".equals(clientType) && !StringUtils.hasText(creditCode)) {
      throw new BusinessException("企业客户必须填写统一社会信用代码");
    }
    command.setCreditCode(creditCode);

    // 身份证号（个人客户必填）
    String idCard = getStringValue(row, "身份证号");
    if ("INDIVIDUAL".equals(clientType) && !StringUtils.hasText(idCard)) {
      throw new BusinessException("个人客户必须填写身份证号");
    }
    command.setIdCard(idCard);

    // 其他字段
    command.setLegalRepresentative(getStringValue(row, "法定代表人"));
    command.setRegisteredAddress(getStringValue(row, "注册地址"));
    command.setContactPerson(getStringValue(row, "联系人"));
    command.setContactPhone(getStringValue(row, "联系电话"));
    command.setContactEmail(getStringValue(row, "联系邮箱"));
    command.setIndustry(getStringValue(row, "所属行业"));
    command.setSource(getStringValue(row, "客户来源"));
    command.setLevel(convertLevelFromChinese(getStringValue(row, "客户级别")));
    command.setCategory(convertCategoryFromChinese(getStringValue(row, "客户分类")));
    command.setRemark(getStringValue(row, "备注"));

    // 日期字段
    String firstCooperationDateStr = getStringValue(row, "首次合作日期");
    if (StringUtils.hasText(firstCooperationDateStr)) {
      try {
        command.setFirstCooperationDate(LocalDate.parse(firstCooperationDateStr));
      } catch (Exception e) {
        log.warn("日期格式错误: {}", firstCooperationDateStr);
      }
    }

    return command;
  }

  /**
   * 从Map中获取字符串值
   *
   * @param row 数据行
   * @param key 键
   * @return 字符串值
   */
  private String getStringValue(final Map<String, Object> row, final String key) {
    Object value = row.get(key);
    if (value == null) {
      return null;
    }
    return value.toString().trim();
  }

  /**
   * 转换客户类型（中文->代码）
   *
   * @param chinese 中文名称
   * @return 类型代码
   */
  private String convertClientTypeFromChinese(final String chinese) {
    if (chinese == null) {
      return null;
    }
    return switch (chinese) {
      case "个人" -> "INDIVIDUAL";
      case "企业" -> "ENTERPRISE";
      case "政府机关" -> "GOVERNMENT";
      case "其他" -> "OTHER";
      default -> chinese; // 如果已经是代码，直接返回
    };
  }

  /**
   * 转换客户级别（中文->代码）
   *
   * @param chinese 中文名称
   * @return 级别代码
   */
  private String convertLevelFromChinese(final String chinese) {
    if (chinese == null) {
      return null;
    }
    return switch (chinese) {
      case "重要" -> "A";
      case "普通" -> "B";
      case "一般" -> "C";
      default -> chinese;
    };
  }

  /**
   * 转换客户分类（中文->代码）
   *
   * @param chinese 中文名称
   * @return 分类代码
   */
  private String convertCategoryFromChinese(final String chinese) {
    if (chinese == null) {
      return null;
    }
    return switch (chinese) {
      case "重要客户" -> "VIP";
      case "普通客户" -> "NORMAL";
      case "潜在客户" -> "POTENTIAL";
      default -> chinese;
    };
  }
}
