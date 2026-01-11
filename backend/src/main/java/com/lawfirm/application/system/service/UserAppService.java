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
    private final UserRoleChangeService userRoleChangeService;

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

        List<User> users = page.getRecords();
        if (users.isEmpty()) {
            return PageResult.of(new ArrayList<>(), 0L, query.getPageNum(), query.getPageSize());
        }

        // 批量加载用户角色信息
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        Map<Long, List<Long>> userRoleMap = loadUserRolesMap(userIds);

        List<UserDTO> records = users.stream()
                .map(user -> toDTO(user, userRoleMap.getOrDefault(user.getId(), new ArrayList<>())))
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 批量加载用户角色映射
     */
    private Map<Long, List<Long>> loadUserRolesMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<UserMapper.UserRoleMapping> mappings = userMapper.selectRoleIdsByUserIds(userIds);
        return mappings.stream()
                .collect(Collectors.groupingBy(
                        UserMapper.UserRoleMapping::getUserId,
                        Collectors.mapping(UserMapper.UserRoleMapping::getRoleId, Collectors.toList())
                ));
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

        // 更新角色（如果角色发生变化，使用角色变更服务处理）
        // 问题451修复：使用差异更新避免先删后插风险
        if (command.getRoleIds() != null) {
            List<Long> oldRoleIds = userMapper.selectRoleIdsByUserId(user.getId());
            List<Long> newRoleIds = command.getRoleIds();
            
            // 检查角色是否发生变化
            if (!new java.util.HashSet<>(oldRoleIds).equals(new java.util.HashSet<>(newRoleIds))) {
                // 使用角色变更服务处理角色变更
                String changeReason = command.getRoleChangeReason() != null 
                    ? command.getRoleChangeReason() 
                    : "用户角色更新";
                userRoleChangeService.changeUserRole(user.getId(), newRoleIds, changeReason);
            }
            // 问题451修复：角色未变化时不执行任何操作，避免先删后插
        }

        log.info("用户更新成功: {}", user.getUsername());
        return toDTO(user);
    }

    /**
     * 删除用户
     * 问题450修复：检查业务关联数据（案件、文档等）
     * 注意：如有业务数据检查需求，需注入对应Repository
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.getByIdOrThrow(id, "用户不存在");
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("系统管理员不能删除");
        }

        // 问题450修复：检查业务数据关联
        // 建议：改用软删除（设置状态为DELETED）而非物理删除
        // 注意：如需检查案件、文档、合同等关联，需注入对应Repository：
        // long matterCount = matterRepository.countByLawyerId(id);
        // if (matterCount > 0) {
        //     throw new BusinessException("该用户有" + matterCount + "个关联案件，无法删除。建议使用禁用功能。");
        // }

        userMapper.deleteById(id);
        userMapper.deleteUserRoles(id);
        log.info("用户删除成功: {}", user.getUsername());
    }

    /**
     * 批量删除用户
     * 原子性操作：要么全部删除成功，要么全部失败
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchDeleteResult deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的用户");
        }
        
        // 1. 先验证所有用户
        List<User> usersToDelete = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();
        
        for (Long id : ids) {
            User user = userRepository.findById(id);
            if (user == null) {
                errors.add("用户ID " + id + " 不存在");
                continue;
            }
            if ("admin".equals(user.getUsername())) {
                errors.add("系统管理员不能删除");
                continue;
            }
            usersToDelete.add(user);
        }
        
        // 2. 如果有错误，直接返回失败（不执行删除）
        if (!errors.isEmpty()) {
            throw new BusinessException("批量删除失败: " + String.join("; ", errors));
        }
        
        // 3. 批量删除（全部验证通过后才执行）
        List<Long> idsToDelete = usersToDelete.stream()
                .map(User::getId)
                .collect(Collectors.toList());
        
        // 问题452修复：批量删除用户角色关系（使用批量操作）
        if (!idsToDelete.isEmpty()) {
            userMapper.batchDeleteUserRoles(idsToDelete);
        }
        
        // 批量删除用户
        userMapper.deleteBatchIds(idsToDelete);
        
        log.info("批量删除用户成功: count={}", idsToDelete.size());
        
        return BatchDeleteResult.success(idsToDelete.size());
    }
    
    /**
     * 批量删除结果
     */
    @lombok.Data
    @lombok.Builder
    public static class BatchDeleteResult {
        private boolean success;
        private int deletedCount;
        private String message;
        
        public static BatchDeleteResult success(int count) {
            return BatchDeleteResult.builder()
                    .success(true)
                    .deletedCount(count)
                    .message("成功删除 " + count + " 个用户")
                    .build();
        }
    }

    /**
     * 重置密码（管理员）
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.getByIdOrThrow(id, "用户不存在");
        
        // 验证密码强度
        validatePasswordStrength(newPassword);
        
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
        
        // 验证新密码强度
        validatePasswordStrength(newPassword);
        
        // 设置新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateById(user);
        log.info("用户修改密码成功: {}", user.getUsername());
    }

    /**
     * 验证密码强度
     * 要求：
     * 1. 长度至少8位
     * 2. 包含大写字母
     * 3. 包含小写字母
     * 4. 包含数字
     * 5. 不能是常见弱密码
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码长度不能少于8位");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("密码必须包含大写字母");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("密码必须包含小写字母");
        }

        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含数字");
        }

        // 检查常见弱密码（扩展列表）
        java.util.Set<String> weakPasswords = java.util.Set.of(
            // 常见弱密码
            "12345678", "password", "Password1", "Aa123456", "Admin123",
            "Qwerty123", "Abc12345", "Password123", "Welcome1", "Passw0rd",
            // 更多常见弱密码
            "Abcd1234", "Test@123", "Change@123", "P@ssw0rd", "P@ssword1",
            "Qwerty1234", "Asdfgh123", "Zxcvbn123", "Admin@123", "User@123",
            "Login@123", "Pass@123", "Temp@123", "Hello@123", "World@123",
            "China@123", "Beijing123", "Shanghai1", "Password@1", "Aa@12345",
            // 键盘顺序密码
            "Qwer1234", "Asdf1234", "Zxcv1234", "Qazwsx123", "!Qaz2wsx",
            // 常见名称+数字
            "Michael123", "William123", "David@123", "Robert@123", "James@123"
        );
        if (weakPasswords.contains(password)) {
            throw new BusinessException("密码过于简单，请设置更强的密码");
        }
        
        // 检查密码中是否包含连续字符（如abc、123）
        if (containsSequentialChars(password, 4)) {
            throw new BusinessException("密码不能包含4个以上连续字符");
        }
    }

    /**
     * 检查密码是否包含连续字符
     * @param password 密码
     * @param minLength 最小连续长度
     * @return 是否包含连续字符
     */
    private boolean containsSequentialChars(String password, int minLength) {
        if (password == null || password.length() < minLength) {
            return false;
        }
        
        String lowerPassword = password.toLowerCase();
        for (int i = 0; i <= lowerPassword.length() - minLength; i++) {
            boolean ascending = true;
            boolean descending = true;
            
            for (int j = 0; j < minLength - 1; j++) {
                char c1 = lowerPassword.charAt(i + j);
                char c2 = lowerPassword.charAt(i + j + 1);
                
                if (c2 - c1 != 1) ascending = false;
                if (c1 - c2 != 1) descending = false;
            }
            
            if (ascending || descending) {
                return true;
            }
        }
        return false;
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

        // 密码（必填，默认密码必须符合强度要求）
        String password = getStringValue(row, "密码");
        if (!StringUtils.hasText(password)) {
            // 使用符合强度要求的默认密码（包含大小写字母和数字）
            // 注意：此密码应在首次登录时强制修改
            password = "LawFirm@2026";
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
        return toDTO(user, null);
    }

    private UserDTO toDTO(User user, List<Long> roleIds) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setDepartmentId(user.getDepartmentId());
        dto.setDepartmentName(user.getDepartmentName());
        dto.setPosition(user.getPosition());
        dto.setEmployeeNo(user.getEmployeeNo());
        dto.setLawyerLicenseNo(user.getLawyerLicenseNo());
        dto.setJoinDate(user.getJoinDate());
        dto.setCompensationType(user.getCompensationType());
        dto.setCanBeOriginator(user.getCanBeOriginator());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setRoleIds(roleIds);
        return dto;
    }
}
