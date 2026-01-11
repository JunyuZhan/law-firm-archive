package com.lawfirm.application.openapi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.openapi.dto.ClientAccessTokenDTO;
import com.lawfirm.application.openapi.dto.CreateTokenCommand;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.openapi.entity.ClientAccessToken;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import com.lawfirm.infrastructure.persistence.mapper.openapi.ClientAccessTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI 令牌管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiTokenService {

    private final ClientAccessTokenMapper tokenMapper;
    private final ClientMapper clientMapper;
    private final MatterMapper matterMapper;
    private final UserMapper userMapper;

    @Value("${law-firm.openapi.portal-base-url:}")
    private String portalBaseUrl;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /**
     * 创建客户访问令牌
     */
    @Transactional
    public ClientAccessTokenDTO createToken(CreateTokenCommand command) {
        // 验证客户存在
        Client client = clientMapper.selectById(command.getClientId());
        if (client == null || client.getDeleted()) {
            throw new BusinessException("客户不存在");
        }

        // 验证项目存在（如果指定）
        Matter matter = null;
        if (command.getMatterId() != null) {
            matter = matterMapper.selectById(command.getMatterId());
            if (matter == null || matter.getDeleted()) {
                throw new BusinessException("项目不存在");
            }
        }

        // 验证授权范围
        validateScopes(command.getScopes());

        // 生成安全令牌
        String token = generateSecureToken();

        // 创建令牌实体
        ClientAccessToken accessToken = ClientAccessToken.builder()
                .token(token)
                .tokenType("BEARER")
                .clientId(command.getClientId())
                .matterId(command.getMatterId())
                .scope(String.join(",", command.getScopes()))
                .expiresAt(LocalDateTime.now().plusDays(command.getValidDays()))
                .maxAccessCount(command.getMaxAccessCount())
                .accessCount(0)
                .ipWhitelist(command.getIpWhitelist())
                .status(ClientAccessToken.STATUS_ACTIVE)
                .remark(command.getRemark())
                .build();

        // 设置创建信息
        accessToken.setCreatedBy(SecurityUtils.getUserId());
        accessToken.setCreatedAt(LocalDateTime.now());
        accessToken.setUpdatedAt(LocalDateTime.now());

        tokenMapper.insert(accessToken);

        log.info("【OpenAPI】创建客户访问令牌: clientId={}, matterId={}, tokenId={}, operator={}",
                command.getClientId(), command.getMatterId(), accessToken.getId(), SecurityUtils.getUsername());

        // 返回完整令牌（仅创建时返回）
        ClientAccessTokenDTO dto = toDTO(accessToken, client, matter);
        dto.setToken(token); // 创建时返回完整令牌
        dto.setPortalUrl(generatePortalUrl(token));
        return dto;
    }

    /**
     * 分页查询令牌列表
     */
    public PageResult<ClientAccessTokenDTO> listTokens(Long clientId, Long matterId, String status, int pageNum, int pageSize) {
        IPage<ClientAccessToken> page = tokenMapper.selectPage(
                new Page<>(pageNum, pageSize),
                clientId, matterId, status);

        List<ClientAccessTokenDTO> records = page.getRecords().stream()
                .map(this::toDTOWithRelations)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取令牌详情
     */
    public ClientAccessTokenDTO getTokenById(Long id) {
        ClientAccessToken token = tokenMapper.selectById(id);
        if (token == null || token.getDeleted()) {
            throw new BusinessException("令牌不存在");
        }
        return toDTOWithRelations(token);
    }

    /**
     * 撤销令牌
     */
    @Transactional
    public void revokeToken(Long id, String reason) {
        ClientAccessToken token = tokenMapper.selectById(id);
        if (token == null || token.getDeleted()) {
            throw new BusinessException("令牌不存在");
        }

        if (!ClientAccessToken.STATUS_ACTIVE.equals(token.getStatus())) {
            throw new BusinessException("令牌状态无法撤销");
        }

        tokenMapper.revokeToken(id, SecurityUtils.getUserId(), LocalDateTime.now(), reason);

        log.warn("【OpenAPI】撤销客户访问令牌: tokenId={}, reason={}, operator={}",
                id, reason, SecurityUtils.getUsername());
    }

    /**
     * 根据令牌字符串获取令牌信息（供公开接口验证用）
     */
    public ClientAccessToken getByToken(String token) {
        return tokenMapper.selectByToken(token);
    }

    /**
     * 更新令牌访问信息
     */
    @Transactional
    public void updateAccessInfo(Long tokenId, String clientIp) {
        tokenMapper.updateAccessInfo(tokenId, clientIp, LocalDateTime.now());
    }

    /**
     * 批量更新过期令牌
     */
    @Transactional
    public int updateExpiredTokens() {
        return tokenMapper.updateExpiredTokens();
    }

    /**
     * 获取客户的有效令牌列表
     */
    public List<ClientAccessTokenDTO> getActiveTokensByClientId(Long clientId) {
        return tokenMapper.selectActiveByClientId(clientId).stream()
                .map(this::toDTOWithRelations)
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    /**
     * 生成安全令牌（64位随机字符）
     */
    private String generateSecureToken() {
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            sb.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 验证授权范围
     */
    private void validateScopes(List<String> scopes) {
        Set<String> validScopes = Set.of(
                ClientAccessToken.SCOPE_MATTER_INFO,
                ClientAccessToken.SCOPE_MATTER_PROGRESS,
                ClientAccessToken.SCOPE_TASK_LIST,
                ClientAccessToken.SCOPE_DEADLINE_INFO,
                ClientAccessToken.SCOPE_DOCUMENT_LIST,
                ClientAccessToken.SCOPE_LAWYER_INFO,
                ClientAccessToken.SCOPE_FEE_INFO
        );

        for (String scope : scopes) {
            if (!validScopes.contains(scope)) {
                throw new BusinessException("无效的授权范围: " + scope);
            }
        }
    }

    /**
     * 生成门户访问链接
     */
    private String generatePortalUrl(String token) {
        if (portalBaseUrl == null || portalBaseUrl.isEmpty()) {
            return null;
        }
        return portalBaseUrl + "/portal?token=" + token;
    }

    /**
     * 转换为 DTO（带关联信息）
     */
    private ClientAccessTokenDTO toDTOWithRelations(ClientAccessToken token) {
        Client client = clientMapper.selectById(token.getClientId());
        Matter matter = token.getMatterId() != null ? matterMapper.selectById(token.getMatterId()) : null;
        return toDTO(token, client, matter);
    }

    /**
     * 转换为 DTO
     */
    private ClientAccessTokenDTO toDTO(ClientAccessToken token, Client client, Matter matter) {
        ClientAccessTokenDTO dto = new ClientAccessTokenDTO();
        dto.setId(token.getId());
        // 令牌脱敏显示
        if (token.getToken() != null && token.getToken().length() > 12) {
            dto.setToken(token.getToken().substring(0, 8) + "****" + token.getToken().substring(token.getToken().length() - 4));
        }
        dto.setClientId(token.getClientId());
        dto.setClientName(client != null ? client.getName() : null);
        dto.setMatterId(token.getMatterId());
        dto.setMatterName(matter != null ? matter.getName() : null);
        dto.setScopes(token.getScope() != null ? Arrays.asList(token.getScope().split(",")) : Collections.emptyList());
        dto.setExpiresAt(token.getExpiresAt());
        dto.setMaxAccessCount(token.getMaxAccessCount());
        dto.setAccessCount(token.getAccessCount());
        dto.setIpWhitelist(token.getIpWhitelist());
        dto.setLastAccessIp(token.getLastAccessIp());
        dto.setLastAccessAt(token.getLastAccessAt());
        dto.setStatus(token.getStatus());
        dto.setCreatedAt(token.getCreatedAt());
        dto.setCreatedBy(token.getCreatedBy());
        dto.setRemark(token.getRemark());

        // 获取创建人姓名
        if (token.getCreatedBy() != null) {
            User creator = userMapper.selectById(token.getCreatedBy());
            dto.setCreatorName(creator != null ? creator.getRealName() : null);
        }

        return dto;
    }
}

