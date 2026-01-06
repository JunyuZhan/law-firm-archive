package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.CreateUserCommand;
import com.lawfirm.application.system.command.UpdateUserCommand;
import com.lawfirm.application.system.dto.UserDTO;
import com.lawfirm.application.system.dto.UserQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.external.excel.ExcelImportExportService;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ExcelImportExportService excelImportExportService;

    /**
     * 分页查询用户
     */
    public PageResult<UserDTO> listUsers(UserQueryDTO query) {
        IPage<User> page = userMapper.selectUserPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getUsername(),
                query.getRealName(),
                query.getPhone(),
                query.getDepartmentId(),
                query.getStatus(),
                query.getCompensationType()
        );

        List<UserDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建用户
     */
    @Transactional
    public UserDTO createUser(CreateUserCommand command) {
        // 1. 验证用户名唯一性
        if (userRepository.existsByUsername(command.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 创建用户实体
        User user = User.builder()
                .username(command.getUsername())
                .password(passwordEncoder.encode(command.getPassword()))
                .realName(command.getRealName())
                .email(command.getEmail())
                .phone(command.getPhone())
                .departmentId(command.getDepartmentId())
                .position(command.getPosition())
                .employeeNo(command.getEmployeeNo())
                .lawyerLicenseNo(command.getLawyerLicenseNo())
                .joinDate(command.getJoinDate())
                .compensationType(command.getCompensationType() != null ? command.getCompensationType() : "COMMISSION")
                .canBeOriginator(command.getCanBeOriginator() != null ? command.getCanBeOriginator() : true)
                .status("ACTIVE")
                .build();

        // 3. 保存用户
        userRepository.save(user);

        // 4. 保存用户角色关联
        if (command.getRoleIds() != null && !command.getRoleIds().isEmpty()) {
            userMapper.insertUserRoles(user.getId(), command.getRoleIds());
        }

        log.info("用户创建成功: {}", user.getUsername());
        return toDTO(user);
    }

    /**
     * 更新用户
     */
    @Transactional
    public UserDTO updateUser(UpdateUserCommand command) {
        User user = userRepository.getByIdOrThrow(command.getId(), "用户不存在");

        // 更新字段
        if (StringUtils.hasText(command.getRealName())) {
            user.setRealName(command.getRealName());
        }
        if (StringUtils.hasText(command.getEmail())) {
            user.setEmail(command.getEmail());
        }
        if (StringUtils.hasText(command.getPhone())) {
            user.setPhone(command.getPhone());
        }
        if (command.getDepartmentId() != null) {
            user.setDepartmentId(command.getDepartmentId());
        }
        if (StringUtils.hasText(command.getPosition())) {
            user.setPosition(command.getPosition());
        }
        if (StringUtils.hasText(command.getEmployeeNo())) {
            user.setEmployeeNo(command.getEmployeeNo());
        }
        if (StringUtils.hasText(command.getLawyerLicenseNo())) {
            user.setLawyerLicenseNo(command.getLawyerLicenseNo());
        }
        if (command.getJoinDate() != null) {
            user.setJoinDate(command.getJoinDate());
        }
        if (StringUtils.hasText(command.getCompensationType())) {
            user.setCompensationType(command.getCompensationType());
        }
        if (command.getCanBeOriginator() != null) {
            user.setCanBeOriginator(command.getCanBeOriginator());
        }

        userRepository.updateById(user);

        // 更新角色
        if (command.getRoleIds() != null) {
            userMapper.deleteUserRoles(user.getId());
            if (!command.getRoleIds().isEmpty()) {
                userMapper.insertUserRoles(user.getId(), command.getRoleIds());
            }
        }

        log.info("用户更新成功: {}", user.getUsername());
        return toDTO(user);
    }

    /**
     * 删除用户
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.getByIdOrThrow(id, "用户不存在");
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("系统管理员不能删除");
        }
        userMapper.deleteById(id);
        userMapper.deleteUserRoles(id);
        log.info("用户删除成功: {}", user.getUsername());
    }

    /**
     * 批量删除用户
     */
    @Transactional
    public void deleteUsers(List<Long> ids) {
        for (Long id : ids) {
            deleteUser(id);
        }
    }

    /**
     * 重置密码（管理员）
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.getByIdOrThrow(id, "用户不存在");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateById(user);
        log.info("用户密码重置成功: {}", user.getUsername());
    }

    /**
     * 修改自己的密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.getByIdOrThrow(userId, "用户不存在");
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        
        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateById(user);
        log.info("用户修改密码成功: {}", user.getUsername());
    }

    /**
     * 更新个人信息（用户自己）
     * 注意：姓名和用户名不允许修改
     */
    @Transactional
    public UserDTO updateProfile(Long userId, String email, String phone, String introduction) {
        User user = userRepository.getByIdOrThrow(userId, "用户不存在");
        
        if (StringUtils.hasText(email)) {
            user.setEmail(email);
        }
        if (StringUtils.hasText(phone)) {
            user.setPhone(phone);
        }
        // introduction 字段需要在User实体中添加，暂时忽略
        
        userRepository.updateById(user);
        log.info("用户更新个人信息成功: {}", user.getUsername());
        return toDTO(user);
    }

    /**
     * 修改用户状态
     */
    @Transactional
    public void changeStatus(Long id, String status) {
        User user = userRepository.getByIdOrThrow(id, "用户不存在");
        if ("admin".equals(user.getUsername()) && !"ACTIVE".equals(status)) {
            throw new BusinessException("系统管理员不能禁用");
        }
        user.setStatus(status);
        userRepository.updateById(user);
        log.info("用户状态修改成功: {} -> {}", user.getUsername(), status);
    }

    /**
     * 根据ID获取用户
     */
    public UserDTO getUserById(Long id) {
        User user = userRepository.getByIdOrThrow(id, "用户不存在");
        UserDTO dto = toDTO(user);
        dto.setRoleCodes(userRepository.findRoleCodesByUserId(id));
        dto.setRoleIds(userMapper.selectRoleIdsByUserId(id));
        dto.setPermissions(userRepository.findPermissionsByUserId(id));
        return dto;
    }

    /**
     * 根据用户名获取用户
     */
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toDTO(user);
    }

    // ========== 用户导出（M1-016，P2） ==========

    /**
     * 导出用户列表为Excel
     */
    public java.io.InputStream exportUsers(UserQueryDTO query) throws IOException {
        // 查询所有符合条件的用户（不分页）
        IPage<User> page = userMapper.selectUserPage(
                new Page<>(1, 10000), // 最多导出10000条
                query.getUsername(),
                query.getRealName(),
                query.getPhone(),
                query.getDepartmentId(),
                query.getStatus(),
                query.getCompensationType()
        );

        List<User> users = page.getRecords();

        // 构建表头
        List<String> headers = List.of(
                "用户名", "姓名", "邮箱", "手机号", "工号", "职位",
                "执业证号", "部门ID", "入职日期", "薪酬模式", "可发起人", "状态"
        );

        // 构建数据
        List<List<Object>> data = new ArrayList<>();
        for (User user : users) {
            List<Object> row = new ArrayList<>();
            row.add(user.getUsername());
            row.add(user.getRealName());
            row.add(user.getEmail());
            row.add(user.getPhone());
            row.add(user.getEmployeeNo());
            row.add(user.getPosition());
            row.add(user.getLawyerLicenseNo());
            row.add(user.getDepartmentId());
            row.add(user.getJoinDate());
            row.add(getCompensationTypeName(user.getCompensationType()));
            row.add(user.getCanBeOriginator() != null && user.getCanBeOriginator() ? "是" : "否");
            row.add(getStatusName(user.getStatus()));
            data.add(row);
        }

        return excelImportExportService.createExcel(headers, data, "用户信息");
    }

    // ========== 用户导入（M1-016，P2） ==========

    /**
     * 批量导入用户
     */
    @Transactional
    public Map<String, Object> importUsers(MultipartFile file) throws IOException {
        List<Map<String, Object>> excelData = excelImportExportService.readExcel(file);

        int successCount = 0;
        int failCount = 0;
        List<String> errorMessages = new ArrayList<>();

        for (int i = 0; i < excelData.size(); i++) {
            Map<String, Object> row = excelData.get(i);
            int rowNum = i + 2; // Excel行号（从2开始，因为第1行是表头）

            try {
                CreateUserCommand command = parseUserFromExcel(row);
                createUser(command);
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

        log.info("用户批量导入完成: 总数={}, 成功={}, 失败={}", excelData.size(), successCount, failCount);
        return result;
    }

    /**
     * 从Excel行数据解析用户信息
     */
    private CreateUserCommand parseUserFromExcel(Map<String, Object> row) {
        CreateUserCommand.CreateUserCommandBuilder builder = CreateUserCommand.builder();

        // 必填字段
        String username = getStringValue(row, "用户名");
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("用户名不能为空");
        }
        builder.username(username);

        String realName = getStringValue(row, "姓名");
        if (!StringUtils.hasText(realName)) {
            throw new BusinessException("姓名不能为空");
        }
        builder.realName(realName);

        // 密码（必填，默认密码）
        String password = getStringValue(row, "密码");
        if (!StringUtils.hasText(password)) {
            password = "123456"; // 默认密码
        }
        builder.password(password);

        // 其他字段
        builder.email(getStringValue(row, "邮箱"));
        builder.phone(getStringValue(row, "手机号"));
        builder.employeeNo(getStringValue(row, "工号"));
        builder.position(getStringValue(row, "职位"));
        builder.lawyerLicenseNo(getStringValue(row, "执业证号"));

        // 部门ID（需要根据部门名称查找，这里简化处理）
        String departmentIdStr = getStringValue(row, "部门ID");
        if (StringUtils.hasText(departmentIdStr)) {
            try {
                builder.departmentId(Long.parseLong(departmentIdStr));
            } catch (NumberFormatException e) {
                log.warn("部门ID格式错误: {}", departmentIdStr);
            }
        }

        // 入职日期
        String joinDateStr = getStringValue(row, "入职日期");
        if (StringUtils.hasText(joinDateStr)) {
            try {
                builder.joinDate(LocalDate.parse(joinDateStr));
            } catch (Exception e) {
                log.warn("入职日期格式错误: {}", joinDateStr);
            }
        }

        // 薪酬模式
        String compensationType = getStringValue(row, "薪酬模式");
        if (StringUtils.hasText(compensationType)) {
            compensationType = convertCompensationTypeFromChinese(compensationType);
            builder.compensationType(compensationType);
        }

        // 可发起人
        String canBeOriginatorStr = getStringValue(row, "可发起人");
        if (StringUtils.hasText(canBeOriginatorStr)) {
            builder.canBeOriginator("是".equals(canBeOriginatorStr) || "Y".equalsIgnoreCase(canBeOriginatorStr) || "true".equalsIgnoreCase(canBeOriginatorStr));
        }

        return builder.build();
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
     * 转换薪酬模式（中文->代码）
     */
    private String convertCompensationTypeFromChinese(String chinese) {
        if (chinese == null) return null;
        return switch (chinese) {
            case "提成制" -> "COMMISSION";
            case "固定工资" -> "SALARIED";
            case "混合制" -> "HYBRID";
            default -> chinese; // 如果已经是代码，直接返回
        };
    }

    /**
     * 获取薪酬模式名称
     */
    private String getCompensationTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "COMMISSION" -> "提成制";
            case "SALARIED" -> "固定工资";
            case "HYBRID" -> "混合制";
            default -> type;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ACTIVE" -> "启用";
            case "DISABLED" -> "禁用";
            case "LOCKED" -> "锁定";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setDepartmentId(user.getDepartmentId());
        dto.setPosition(user.getPosition());
        dto.setEmployeeNo(user.getEmployeeNo());
        dto.setLawyerLicenseNo(user.getLawyerLicenseNo());
        dto.setJoinDate(user.getJoinDate());
        dto.setCompensationType(user.getCompensationType());
        dto.setCanBeOriginator(user.getCanBeOriginator());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
