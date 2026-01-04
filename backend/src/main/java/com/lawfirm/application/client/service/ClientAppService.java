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
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.external.excel.ExcelImportExportService;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 客户应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAppService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final ExcelImportExportService excelImportExportService;
    private final UserRepository userRepository;
    private final com.lawfirm.infrastructure.persistence.mapper.MatterMapper matterMapper;

    /**
     * 分页查询客户
     */
    public PageResult<ClientDTO> listClients(ClientQueryDTO query) {
        IPage<Client> page = clientMapper.selectClientPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getName(),
                query.getClientType(),
                query.getStatus(),
                query.getResponsibleLawyerId()
        );

        List<ClientDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建客户
     */
    @Transactional
    public ClientDTO createClient(CreateClientCommand command) {
        // 1. 验证客户名称唯一性
        if (clientRepository.count(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Client>()
                        .eq(Client::getName, command.getName())) > 0) {
            throw new BusinessException("客户名称已存在");
        }

        // 2. 企业客户验证信用代码
        if ("ENTERPRISE".equals(command.getClientType()) && !StringUtils.hasText(command.getCreditCode())) {
            throw new BusinessException("企业客户必须填写统一社会信用代码");
        }

        // 3. 个人客户验证身份证
        if ("INDIVIDUAL".equals(command.getClientType()) && !StringUtils.hasText(command.getIdCard())) {
            throw new BusinessException("个人客户必须填写身份证号");
        }

        // 4. 生成客户编号
        String clientNo = generateClientNo();

        // 5. 创建客户实体
        Client client = Client.builder()
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
                .originatorId(command.getOriginatorId() != null ? command.getOriginatorId() : SecurityUtils.getUserId())
                .responsibleLawyerId(command.getResponsibleLawyerId())
                .firstCooperationDate(command.getFirstCooperationDate())
                .remark(command.getRemark())
                .build();

        // 6. 保存客户
        clientRepository.save(client);

        log.info("客户创建成功: {} ({})", client.getName(), client.getClientNo());
        return toDTO(client);
    }

    /**
     * 更新客户
     */
    @Transactional
    public ClientDTO updateClient(UpdateClientCommand command) {
        Client client = clientRepository.getByIdOrThrow(command.getId(), "客户不存在");

        // 更新字段
        if (StringUtils.hasText(command.getName())) {
            // 检查名称唯一性（排除自己）
            if (clientRepository.count(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Client>()
                            .eq(Client::getName, command.getName())
                            .ne(Client::getId, command.getId())) > 0) {
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
     */
    @Transactional
    public void deleteClient(Long id) {
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
     */
    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.getByIdOrThrow(id, "客户不存在");
        return toDTO(client);
    }

    /**
     * 修改客户状态
     */
    @Transactional
    public void changeStatus(Long id, String status) {
        Client client = clientRepository.getByIdOrThrow(id, "客户不存在");
        client.setStatus(status);
        clientRepository.updateById(client);
        log.info("客户状态修改成功: {} -> {}", client.getName(), status);
    }

    /**
     * 转正式客户
     */
    @Transactional
    public void convertToFormal(Long id) {
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
     */
    private String generateClientNo() {
        String prefix = "C" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 获取客户类型名称
     */
    private String getClientTypeName(String type) {
        if (type == null) return null;
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
     */
    private String getLevelName(String level) {
        if (level == null) return null;
        return switch (level) {
            case "A" -> "重要客户";
            case "B" -> "普通客户";
            case "C" -> "一般客户";
            default -> level;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "POTENTIAL" -> "潜在";
            case "ACTIVE" -> "正式";
            case "INACTIVE" -> "休眠";
            case "BLACKLIST" -> "黑名单";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private ClientDTO toDTO(Client client) {
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
        
        // 查询关联数据
        if (client.getOriginatorId() != null) {
            var user = userRepository.findById(client.getOriginatorId());
            if (user != null) {
                dto.setOriginatorName(user.getRealName());
            }
        }
        if (client.getResponsibleLawyerId() != null) {
            var user = userRepository.findById(client.getResponsibleLawyerId());
            if (user != null) {
                dto.setResponsibleLawyerName(user.getRealName());
            }
        }
        
        return dto;
    }

    /**
     * 获取客户分类名称
     */
    private String getCategoryName(String category) {
        if (category == null) return null;
        return switch (category) {
            case "VIP" -> "重要客户";
            case "NORMAL" -> "普通客户";
            case "POTENTIAL" -> "潜在客户";
            default -> category;
        };
    }

    // ========== 客户导出（M2-008，P2） ==========

    /**
     * 导出客户信息为Excel
     */
    public ByteArrayInputStream exportClients(ClientQueryDTO query) throws IOException {
        // 查询所有符合条件的客户（不分页）
        IPage<Client> page = clientMapper.selectClientPage(
                new Page<>(1, 10000), // 最大导出10000条
                query.getName(),
                query.getClientType(),
                query.getStatus(),
                query.getResponsibleLawyerId()
        );

        List<Client> clients = page.getRecords();
        
        // 准备表头
        List<String> headers = Arrays.asList(
                "客户编号", "客户名称", "客户类型", "统一社会信用代码", "身份证号",
                "法定代表人", "注册地址", "联系人", "联系电话", "联系邮箱",
                "所属行业", "客户来源", "客户级别", "客户分类", "状态",
                "案源人", "负责律师", "首次合作日期", "备注", "创建时间"
        );

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
            
            // 案源人和负责律师名称需要查询，这里简化处理
            row.add(client.getOriginatorId() != null ? client.getOriginatorId().toString() : "");
            row.add(client.getResponsibleLawyerId() != null ? client.getResponsibleLawyerId().toString() : "");
            
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
     */
    @Transactional
    public Map<String, Object> importClients(MultipartFile file) throws IOException {
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
     */
    private CreateClientCommand parseClientFromExcel(Map<String, Object> row) {
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
     */
    private String getStringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    /**
     * 转换客户类型（中文->代码）
     */
    private String convertClientTypeFromChinese(String chinese) {
        if (chinese == null) return null;
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
     */
    private String convertLevelFromChinese(String chinese) {
        if (chinese == null) return null;
        return switch (chinese) {
            case "重要" -> "A";
            case "普通" -> "B";
            case "一般" -> "C";
            default -> chinese;
        };
    }

    /**
     * 转换客户分类（中文->代码）
     */
    private String convertCategoryFromChinese(String chinese) {
        if (chinese == null) return null;
        return switch (chinese) {
            case "重要客户" -> "VIP";
            case "普通客户" -> "NORMAL";
            case "潜在客户" -> "POTENTIAL";
            default -> chinese;
        };
    }
}

