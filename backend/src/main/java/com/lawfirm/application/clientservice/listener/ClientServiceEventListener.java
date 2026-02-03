package com.lawfirm.application.clientservice.listener;

import com.lawfirm.application.clientservice.dto.PushRequest;
import com.lawfirm.application.clientservice.service.DataPushService;
import com.lawfirm.domain.clientservice.entity.PushConfig;
import com.lawfirm.domain.clientservice.entity.PushRecord;
import com.lawfirm.domain.matter.event.MatterUpdatedEvent;
import com.lawfirm.infrastructure.persistence.mapper.clientservice.PushConfigMapper;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 客户服务事件监听器
 * 监听项目更新等事件，触发自动推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientServiceEventListener {

  private final DataPushService dataPushService;
  private final PushConfigMapper pushConfigMapper;

  /**
   * 监听项目更新事件，检查是否需要自动推送
   *
   * @param event 项目更新事件
   */
  @Async
  @EventListener
  public void onMatterUpdated(final MatterUpdatedEvent event) {
    Long matterId = event.getMatterId();
    Long clientId = event.getClientId();
    Long operatorId = event.getOperatorId();

    log.debug("收到项目更新事件: matterId={}, clientId={}, operatorId={}", 
        matterId, clientId, operatorId);

    // 客户ID为空，无法推送
    if (clientId == null) {
      log.debug("项目 {} 未关联客户，跳过自动推送", matterId);
      return;
    }

    // 查询推送配置
    PushConfig config = pushConfigMapper.selectByMatterId(matterId);
    if (config == null) {
      log.debug("项目 {} 无推送配置，跳过自动推送", matterId);
      return;
    }

    // 检查是否启用自动推送
    if (!Boolean.TRUE.equals(config.getAutoPushOnUpdate())) {
      log.debug("项目 {} 未启用自动推送，跳过", matterId);
      return;
    }

    // 检查是否启用推送功能
    if (!Boolean.TRUE.equals(config.getEnabled())) {
      log.debug("项目 {} 推送功能未启用，跳过自动推送", matterId);
      return;
    }

    // 执行自动推送
    try {
      log.info("触发自动推送: matterId={}, clientId={}", matterId, clientId);

      // 构建推送请求
      List<String> scopes = config.getScopes() != null 
          ? Arrays.asList(config.getScopes().split(","))
          : List.of("MATTER_INFO", "MATTER_PROGRESS");

      PushRequest request = new PushRequest();
      request.setMatterId(matterId);
      request.setClientId(clientId);
      request.setScopes(scopes);
      request.setValidDays(config.getValidDays() != null ? config.getValidDays() : 30);
      request.setPushType(PushRecord.TYPE_AUTO);

      dataPushService.pushMatterData(request, operatorId);

      log.info("自动推送成功: matterId={}", matterId);
    } catch (Exception e) {
      log.error("自动推送失败: matterId={}, error={}", matterId, e.getMessage(), e);
      // 自动推送失败不影响主流程，仅记录日志
    }
  }
}
