package com.lawfirm.application.openapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.application.openapi.dto.PortalMatterDTO;
import com.lawfirm.application.openapi.dto.PushRecordDTO;
import com.lawfirm.application.openapi.dto.PushRequest;
import com.lawfirm.application.openapi.dto.PushConfigDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.openapi.entity.PushConfig;
import com.lawfirm.domain.openapi.entity.PushRecord;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.infrastructure.persistence.mapper.ClientMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import com.lawfirm.infrastructure.persistence.mapper.ExternalIntegrationMapper;
import com.lawfirm.infrastructure.persistence.mapper.openapi.PushConfigMapper;
import com.lawfirm.infrastructure.persistence.mapper.openapi.PushRecordMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据推送服务
 * 负责将项目数据推送到客户服务系统
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPushService {

    private final PushRecordMapper pushRecordMapper;
    private final PushConfigMapper pushConfigMapper;
    private final MatterMapper matterMapper;
    private final MatterParticipantMapper participantMapper;
    private final ClientMapper clientMapper;
    private final ExternalIntegrationMapper integrationMapper;
    private final PortalDataService portalDataService;
    private final ObjectMapper objectMapper;
    
    // 客户服务系统的集成类型标识
    private static final String CLIENT_SERVICE_TYPE = "CLIENT_SERVICE";
    
    // 不允许推送的项目状态
    private static final Set<String> DISABLED_STATUSES = Set.of("ARCHIVED", "CANCELLED");

    /**
     * 推送项目数据到客户服务系统
     */
    @Transactional
    public PushRecordDTO pushMatterData(PushRequest request, Long operatorId) {
        // 1. 验证项目
        Matter matter = matterMapper.selectById(request.getMatterId());
        if (matter == null || matter.getDeleted()) {
            throw new BusinessException("项目不存在");
        }
        
        // 2. 数据权限校验 - 只有项目参与者才能推送
        checkMatterPermission(matter.getId(), operatorId);
        
        // 3. 项目状态校验
        if (DISABLED_STATUSES.contains(matter.getStatus())) {
            throw new BusinessException("该项目状态不允许推送数据");
        }
        
        // 4. 验证客户
        Long clientId = request.getClientId() != null ? request.getClientId() : matter.getClientId();
        if (clientId == null) {
            throw new BusinessException("项目未关联客户");
        }
        
        Client client = clientMapper.selectById(clientId);
        if (client == null) {
            throw new BusinessException("客户不存在");
        }

        // 2. 获取客户服务系统配置
        ExternalIntegration integration = getClientServiceIntegration();
        
        // 3. 组装推送数据（脱敏）
        PortalMatterDTO matterData = portalDataService.buildMatterData(
            matter.getId(), 
            new HashSet<>(request.getScopes())
        );
        
        // 4. 创建推送记录
        PushRecord record = PushRecord.builder()
                .matterId(matter.getId())
                .clientId(clientId)
                .pushType(request.getPushType() != null ? request.getPushType() : PushRecord.TYPE_MANUAL)
                .scopes(String.join(",", request.getScopes()))
                .status(PushRecord.STATUS_PENDING)
                .retryCount(0)
                .expiresAt(LocalDateTime.now().plusDays(request.getValidDays() != null ? request.getValidDays() : 30))
                .createdBy(operatorId)
                .build();
        
        // 保存数据快照
        try {
            record.setDataSnapshot(objectMapper.writeValueAsString(matterData));
        } catch (Exception e) {
            log.warn("序列化数据快照失败", e);
        }
        
        pushRecordMapper.insert(record);

        // 5. 调用客户服务系统API
        if (integration != null && Boolean.TRUE.equals(integration.getEnabled())) {
            try {
                PushResult result = callClientServiceApi(integration, matterData, client, request);
                
                // 更新推送结果
                pushRecordMapper.updatePushResult(
                    record.getId(),
                    PushRecord.STATUS_SUCCESS,
                    result.externalId(),
                    result.externalUrl(),
                    null
                );
                
                record.setStatus(PushRecord.STATUS_SUCCESS);
                record.setExternalId(result.externalId());
                record.setExternalUrl(result.externalUrl());
                
            } catch (Exception e) {
                log.error("推送到客户服务系统失败", e);
                pushRecordMapper.updatePushResult(
                    record.getId(),
                    PushRecord.STATUS_FAILED,
                    null,
                    null,
                    e.getMessage()
                );
                record.setStatus(PushRecord.STATUS_FAILED);
                record.setErrorMessage(e.getMessage());
            }
        } else {
            // 客户服务系统未配置，标记为待推送
            log.info("客户服务系统未配置或未启用，推送记录已保存，待后续处理");
            record.setErrorMessage("客户服务系统未配置或未启用");
        }

        return convertToDTO(record, matter, client);
    }

    /**
     * 获取项目的推送记录列表
     */
    public PageResult<PushRecordDTO> getPushRecords(Long matterId, Long clientId, String status, int pageNum, int pageSize) {
        Page<PushRecord> page = new Page<>(pageNum, pageSize);
        var resultPage = pushRecordMapper.selectPage(page, matterId, clientId, status);
        
        List<PushRecordDTO> list = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(list, resultPage.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取推送记录详情
     */
    public PushRecordDTO getPushRecordById(Long id) {
        PushRecord record = pushRecordMapper.selectById(id);
        if (record == null || record.getDeleted()) {
            throw new BusinessException("推送记录不存在");
        }
        return convertToDTO(record);
    }

    /**
     * 获取项目的最近一次成功推送
     */
    public PushRecordDTO getLatestPush(Long matterId) {
        PushRecord record = pushRecordMapper.selectLatestSuccessByMatterId(matterId);
        if (record == null) {
            return null;
        }
        return convertToDTO(record);
    }

    /**
     * 获取或创建推送配置
     */
    public PushConfigDTO getOrCreateConfig(Long matterId, Long clientId) {
        PushConfig config = pushConfigMapper.selectByMatterId(matterId);
        if (config == null) {
            config = PushConfig.builder()
                    .matterId(matterId)
                    .clientId(clientId)
                    .enabled(false)
                    .scopes("MATTER_INFO,MATTER_PROGRESS,LAWYER_INFO,DEADLINE_INFO")
                    .autoPushOnUpdate(false)
                    .validDays(30)
                    .build();
            pushConfigMapper.insert(config);
        }
        return convertConfigToDTO(config);
    }

    /**
     * 更新推送配置
     */
    @Transactional
    public PushConfigDTO updateConfig(Long matterId, PushConfigDTO dto) {
        PushConfig config = pushConfigMapper.selectByMatterId(matterId);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        
        if (dto.getEnabled() != null) config.setEnabled(dto.getEnabled());
        if (dto.getScopes() != null) config.setScopes(String.join(",", dto.getScopes()));
        if (dto.getAutoPushOnUpdate() != null) config.setAutoPushOnUpdate(dto.getAutoPushOnUpdate());
        if (dto.getValidDays() != null) config.setValidDays(dto.getValidDays());
        
        pushConfigMapper.updateById(config);
        return convertConfigToDTO(config);
    }

    /**
     * 统计推送信息
     */
    public Map<String, Object> getStatistics(Long matterId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPushCount", pushRecordMapper.countSuccessByMatterId(matterId));
        
        PushRecord latest = pushRecordMapper.selectLatestSuccessByMatterId(matterId);
        if (latest != null) {
            stats.put("lastPushTime", latest.getCreatedAt());
            stats.put("lastPushStatus", latest.getStatus());
            stats.put("externalUrl", latest.getExternalUrl());
        }
        
        return stats;
    }

    /**
     * 获取客户服务系统集成配置
     */
    private ExternalIntegration getClientServiceIntegration() {
        return integrationMapper.selectByType(CLIENT_SERVICE_TYPE);
    }

    /**
     * 调用客户服务系统API
     */
    private PushResult callClientServiceApi(ExternalIntegration integration, 
                                             PortalMatterDTO matterData,
                                             Client client,
                                             PushRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (integration.getApiKey() != null) {
            headers.set("Authorization", "Bearer " + integration.getApiKey());
        }
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("clientId", client.getId());
        requestBody.put("clientName", client.getName());
        requestBody.put("matterData", matterData);
        requestBody.put("validDays", request.getValidDays() != null ? request.getValidDays() : 30);
        requestBody.put("scopes", request.getScopes());
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        // 发送请求
        String apiUrl = integration.getApiUrl() + "/matter/receive";
        ResponseEntity<Map> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            entity,
            Map.class
        );
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            return new PushResult(
                (String) body.get("id"),
                (String) body.get("accessUrl")
            );
        } else {
            throw new BusinessException("客户服务系统返回错误");
        }
    }

    /**
     * 推送结果
     */
    private record PushResult(String externalId, String externalUrl) {}

    /**
     * 转换为DTO
     */
    private PushRecordDTO convertToDTO(PushRecord record) {
        Matter matter = matterMapper.selectById(record.getMatterId());
        Client client = clientMapper.selectById(record.getClientId());
        return convertToDTO(record, matter, client);
    }

    private PushRecordDTO convertToDTO(PushRecord record, Matter matter, Client client) {
        return PushRecordDTO.builder()
                .id(record.getId())
                .matterId(record.getMatterId())
                .matterName(matter != null ? matter.getName() : null)
                .clientId(record.getClientId())
                .clientName(client != null ? client.getName() : null)
                .pushType(record.getPushType())
                .scopes(record.getScopes() != null ? Arrays.asList(record.getScopes().split(",")) : List.of())
                .status(record.getStatus())
                .externalId(record.getExternalId())
                .externalUrl(record.getExternalUrl())
                .errorMessage(record.getErrorMessage())
                .expiresAt(record.getExpiresAt())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private PushConfigDTO convertConfigToDTO(PushConfig config) {
        return PushConfigDTO.builder()
                .id(config.getId())
                .matterId(config.getMatterId())
                .clientId(config.getClientId())
                .enabled(config.getEnabled())
                .scopes(config.getScopes() != null ? Arrays.asList(config.getScopes().split(",")) : List.of())
                .autoPushOnUpdate(config.getAutoPushOnUpdate())
                .validDays(config.getValidDays())
                .build();
    }

    /**
     * 校验用户对项目的数据权限
     * 只有项目参与者才能操作
     */
    private void checkMatterPermission(Long matterId, Long userId) {
        if (userId == null) {
            throw new BusinessException("未获取到当前用户信息");
        }
        
        // 检查是否是项目参与者
        boolean isParticipant = participantMapper.existsByMatterIdAndUserId(matterId, userId);
        if (!isParticipant) {
            // 检查是否是管理员（管理员可以操作所有项目）
            // 注：这里简化处理，实际可以通过 SecurityUtils 获取用户角色判断
            log.warn("用户 {} 尝试操作非自己参与的项目 {}", userId, matterId);
            throw new BusinessException("您没有权限操作此项目");
        }
    }
}

