package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.User;

import java.util.List;

/**
 * 用户服务接口.
 * @author junyuzhan
 */
public interface UserService {

    /**
     * 创建用户.
     */
    User create(User user);

    /**
     * 更新用户.
     */
    User update(Long id, User user);

    /**
     * 获取用户详情.
     */
    User getById(Long id);

    /**
     * 根据用户名获取.
     */
    User getByUsername(String username);

    /**
     * 获取启用中的用户.
     */
    User getActiveById(Long id);

    /**
     * 记录登录成功后的用户状态.
     */
    void recordLoginSuccess(Long id, String clientIp);

    /**
     * 分页查询用户.
     */
    PageResult<User> query(String keyword, String userType, String status, Integer pageNum, Integer pageSize);

    /**
     * 删除用户.
     */
    void delete(Long id);

    /**
     * 重置密码.
     */
    void resetPassword(Long id, String newPassword);

    /**
     * 修改密码.
     */
    void changePassword(Long id, String oldPassword, String newPassword);

    /**
     * 启用/禁用用户.
     */
    void updateStatus(Long id, String status);

    /**
     * 分配角色.
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 获取用户角色.
     */
    List<Long> getUserRoleIds(Long userId);
}
