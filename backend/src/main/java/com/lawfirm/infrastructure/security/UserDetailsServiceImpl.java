package com.lawfirm.infrastructure.security;

import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户详情服务实现
 * 
 * 性能优化：使用Redis缓存用户认证信息，避免每次请求都查询数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 用户认证信息缓存前缀
     */
    private static final String USER_AUTH_CACHE_PREFIX = "auth:user:";
    
    /**
     * 缓存过期时间（分钟）- 与JWT token刷新周期协调
     */
    private static final long CACHE_TTL_MINUTES = 10;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String cacheKey = USER_AUTH_CACHE_PREFIX + username;
        
        // 1. 尝试从缓存获取
        try {
            LoginUser cachedUser = (LoginUser) redisTemplate.opsForValue().get(cacheKey);
            if (cachedUser != null) {
                log.debug("用户认证信息缓存命中: {}", username);
                return cachedUser;
            }
        } catch (Exception e) {
            log.warn("读取用户认证缓存失败，将从数据库加载: {}", e.getMessage());
        }
        
        log.debug("用户认证信息缓存未命中，从数据库加载: {}", username);
        
        // 2. 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 3. 检查用户状态
        if ("LOCKED".equals(user.getStatus())) {
            throw new UsernameNotFoundException("账户已被锁定: " + username);
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 4. 查询角色和权限
        List<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = userRepository.findPermissionsByUserId(user.getId());
        
        // 5. 获取最高数据范围权限
        String dataScope = userRepository.findHighestDataScopeByUserId(user.getId());
        if (dataScope == null) {
            dataScope = "SELF";
        }

        // 6. 构建LoginUser
        LoginUser loginUser = LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .departmentId(user.getDepartmentId())
                .compensationType(user.getCompensationType())
                .roles(new HashSet<>(roles))
                .permissions(new HashSet<>(permissions))
                .dataScope(dataScope)
                .enabled(true)
                .build();
        
        // 7. 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, loginUser, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("用户认证信息已缓存: {}", username);
        } catch (Exception e) {
            log.warn("写入用户认证缓存失败，不影响正常功能: {}", e.getMessage());
        }
        
        return loginUser;
    }
    
    /**
     * 清除用户认证缓存
     * 在用户信息变更、权限变更、登出等场景调用
     */
    public void clearUserAuthCache(String username) {
        if (username == null) {
            return;
        }
        try {
            String cacheKey = USER_AUTH_CACHE_PREFIX + username;
            redisTemplate.delete(cacheKey);
            log.info("已清除用户认证缓存: {}", username);
        } catch (Exception e) {
            log.warn("清除用户认证缓存失败: {}", e.getMessage());
        }
    }
    
    /**
     * 通过用户ID清除认证缓存
     */
    public void clearUserAuthCacheByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            User user = userRepository.findById(userId);
            if (user != null) {
                clearUserAuthCache(user.getUsername());
            }
        } catch (Exception e) {
            log.warn("通过用户ID清除认证缓存失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}

