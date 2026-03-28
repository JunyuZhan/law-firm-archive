package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.entity.User;
import com.archivesystem.entity.UserRole;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.repository.UserRoleMapper;
import com.archivesystem.security.PasswordValidator;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.OperationLogService;
import com.archivesystem.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final OperationLogService operationLogService;

    @Override
    @Transactional
    public User create(User user) {
        // 检查用户名是否存在
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在: " + user.getUsername());
        }

        // 验证密码强度
        PasswordValidator.ValidationResult passwordResult = passwordValidator.validate(user.getPassword());
        if (!passwordResult.isValid()) {
            throw new BusinessException(passwordResult.getFirstError());
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(User.STATUS_ACTIVE);

        userMapper.insert(user);
        log.info("创建用户: id={}, username={}", user.getId(), user.getUsername());

        user.setPassword(null); // 不返回密码哈希
        return user;
    }

    @Override
    @Transactional
    public User update(Long id, User user) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw NotFoundException.of("用户", id);
        }

        // 如果修改了用户名，检查是否重复
        if (!existing.getUsername().equals(user.getUsername())) {
            User byUsername = userMapper.selectByUsername(user.getUsername());
            if (byUsername != null && !byUsername.getId().equals(id)) {
                throw new BusinessException("用户名已存在: " + user.getUsername());
            }
            existing.setUsername(user.getUsername());
        }

        existing.setRealName(user.getRealName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setUserType(user.getUserType());
        existing.setDepartment(user.getDepartment());

        userMapper.updateById(existing);
        existing.setPassword(null); // 不返回密码哈希
        return existing;
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw NotFoundException.of("用户", id);
        }
        user.setPassword(null); // 不返回密码
        return user;
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public PageResult<User> query(String keyword, String userType, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, false);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword)
            );
        }

        if (StringUtils.hasText(userType)) {
            wrapper.eq(User::getUserType, userType);
        }

        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status);
        }

        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> result = userMapper.selectPage(page, wrapper);

        // 隐藏密码
        result.getRecords().forEach(u -> u.setPassword(null));

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return;
        }

        // 删除用户角色关联
        userRoleMapper.deleteByUserId(id);
        
        // 删除用户
        userMapper.deleteById(id);
        log.info("删除用户: id={}", id);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw NotFoundException.of("用户", id);
        }

        // 验证密码强度
        PasswordValidator.ValidationResult passwordResult = passwordValidator.validate(newPassword);
        if (!passwordResult.isValid()) {
            throw new BusinessException(passwordResult.getFirstError());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("重置用户密码: id={}", id);
        
        // 记录安全审计日志
        operationLogService.log(OperationLog.builder()
                .objectType("USER")
                .objectId(String.valueOf(id))
                .operationType("PASSWORD_RESET")
                .operationDesc(String.format("管理员重置用户[%s]的密码", user.getUsername()))
                .build());
    }

    @Override
    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw NotFoundException.of("用户", id);
        }

        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        // 新密码不能与原密码相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        // 验证密码强度
        PasswordValidator.ValidationResult passwordResult = passwordValidator.validate(newPassword);
        if (!passwordResult.isValid()) {
            throw new BusinessException(passwordResult.getFirstError());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        log.info("修改用户密码: id={}", id);
        
        // 记录安全审计日志
        operationLogService.log(OperationLog.builder()
                .objectType("USER")
                .objectId(String.valueOf(id))
                .operationType("PASSWORD_CHANGE")
                .operationDesc(String.format("用户[%s]修改了自己的密码", user.getUsername()))
                .build());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw NotFoundException.of("用户", id);
        }

        user.setStatus(status);
        userMapper.updateById(user);
        log.info("更新用户状态: id={}, status={}", id, status);
    }

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 删除原有角色
        userRoleMapper.deleteByUserId(userId);

        // 添加新角色
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
        log.info("分配用户角色: userId={}, roleIds={}", userId, roleIds);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, userId);
        List<UserRole> userRoles = userRoleMapper.selectList(wrapper);
        return userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
    }
}
