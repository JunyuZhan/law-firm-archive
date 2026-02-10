package com.lawfirm.application.system.service;

import com.lawfirm.infrastructure.notification.AlertService;
import com.lawfirm.infrastructure.notification.EmailService;
import com.lawfirm.infrastructure.security.IpLocationService;
import com.lawfirm.infrastructure.security.IpLocationService.IpLocation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 登录位置服务
 *
 * <p>功能： 1. 记录用户登录位置历史 2. 检测异地登录 3. 管理用户常用登录地点 4. 异地登录需要管理员许可码
 *
 * <p>异地登录处理： - 用户首次登录：记录位置，不视为异地 - 从常用位置登录：正常登录 - 从新位置登录：拒绝登录，需要管理员许可码 -
 * 许可码模式：固定码（管理员设置）或随机码（邮件发送给管理员）
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLocationService {

  /** IpLocation Service. */
  private final IpLocationService ipLocationService;

  /** RedisTemplate. */
  private final RedisTemplate<String, Object> redisTemplate;

  /** ConfigApp Service. */
  private final SysConfigAppService configAppService;

  /** Email Service. */
  private final EmailService emailService;

  /** Alert Service. */
  private final AlertService alertService;

  /** 随机许可码最大值（6位数字） */
  private static final int RANDOM_PERMIT_CODE_MAX = 1000000;

  // Redis Key 前缀
  /** LOGIN_LOCATION_PREFIX. */
  private static final String LOGIN_LOCATION_PREFIX = "login:location:";

  /** TRUSTED_LOCATION_PREFIX. */
  private static final String TRUSTED_LOCATION_PREFIX = "login:trusted:";

  /** PERMIT_CODE_PREFIX. */
  private static final String PERMIT_CODE_PREFIX = "login:permit_code:"; // 许可码

  // 配置
  /** MAX_LOCATION_HISTORY. */
  private static final int MAX_LOCATION_HISTORY = 20; // 最多保存20条历史记录

  /** TRUSTED_THRESHOLD. */
  private static final int TRUSTED_THRESHOLD = 3; // 登录3次以上视为常用位置

  /** LOCATION_EXPIRE_DAYS. */
  private static final int LOCATION_EXPIRE_DAYS = 90; // 位置记录保存90天

  /** PERMIT_CODE_EXPIRE_MINUTES. */
  private static final int PERMIT_CODE_EXPIRE_MINUTES = 30; // 许可码30分钟过期

  /**
   * 检查是否为异地登录
   *
   * @param userId 用户ID
   * @param ip 登录IP
   * @return 检查结果
   */
  public LocationCheckResult checkLocation(final Long userId, final String ip) {
    // 检查是否启用异地登录检测
    String enabled = configAppService.getConfigValue("security.location.enabled");
    if (!"true".equalsIgnoreCase(enabled)) {
      return LocationCheckResult.normal(ipLocationService.getLocation(ip));
    }

    IpLocation currentLocation = ipLocationService.getLocation(ip);

    // 本地地址不检测
    if (currentLocation.isLocal()) {
      return LocationCheckResult.normal(currentLocation);
    }

    // 获取判断级别：province（省级）或 city（市级）
    String level = configAppService.getConfigValue("security.location.level");
    boolean isCityLevel = "city".equalsIgnoreCase(level);

    // 获取用户常用位置
    Set<String> trustedLocations = getTrustedLocations(userId, isCityLevel);

    // 首次登录
    if (trustedLocations.isEmpty()) {
      log.info(
          "用户首次登录，记录位置: userId={}, location={}", userId, currentLocation.getShortDescription());
      return LocationCheckResult.firstLogin(currentLocation);
    }

    // 检查是否为常用位置
    String currentLocationKey =
        isCityLevel ? currentLocation.getCity() : currentLocation.getProvince();
    if (currentLocationKey != null && trustedLocations.contains(currentLocationKey)) {
      return LocationCheckResult.normal(currentLocation);
    }

    // 异地登录
    log.warn(
        "检测到异地登录: userId={}, currentLocation={}, trustedLocations={}, level={}",
        userId,
        currentLocation.getShortDescription(),
        trustedLocations,
        isCityLevel ? "city" : "province");
    return LocationCheckResult.newLocation(currentLocation, trustedLocations);
  }

  /**
   * 获取用户常用登录位置（省份或城市）
   *
   * @param userId 用户ID
   * @param isCityLevel 是否市级
   * @return 常用位置集合
   */
  @SuppressWarnings("unchecked")
  private Set<String> getTrustedLocations(final Long userId, final boolean isCityLevel) {
    String suffix = isCityLevel ? ":city" : "";
    String key = TRUSTED_LOCATION_PREFIX + userId + suffix;
    // 使用 opsForSet().members() 读取 Redis Set（与 updateTrustedLocation 的 add 操作匹配）
    Set<Object> members = redisTemplate.opsForSet().members(key);
    if (members == null || members.isEmpty()) {
      return new HashSet<>();
    }
    Set<String> result = new HashSet<>();
    for (Object member : members) {
      if (member instanceof String) {
        result.add((String) member);
      }
    }
    return result;
  }

  /**
   * 记录登录位置
   *
   * @param userId 用户ID
   * @param ip 登录IP
   * @param success 是否登录成功
   */
  public void recordLogin(final Long userId, final String ip, final boolean success) {
    if (!success) {
      return;
    }

    IpLocation location = ipLocationService.getLocation(ip);

    // 本地地址不记录
    if (location.isLocal() || location.isUnknown()) {
      return;
    }

    String province = location.getProvince();
    String city = location.getCity();
    if (province == null) {
      return;
    }

    // 更新登录位置历史
    updateLocationHistory(userId, ip, location);

    // 获取判断级别
    String level = configAppService.getConfigValue("security.location.level");
    boolean isCityLevel = "city".equalsIgnoreCase(level);

    // 更新常用位置计数（同时更新省级和市级）
    updateTrustedLocation(userId, province, false);
    if (city != null) {
      updateTrustedLocation(userId, city, true);
    }

    log.debug(
        "记录登录位置: userId={}, ip={}, location={}, level={}",
        userId,
        ip,
        location.getShortDescription(),
        isCityLevel ? "city" : "province");
  }

  /**
   * 更新登录位置历史
   *
   * @param userId 用户ID
   * @param ip IP地址
   * @param location 位置信息
   */
  private void updateLocationHistory(
      final Long userId, final String ip, final IpLocation location) {
    String key = LOGIN_LOCATION_PREFIX + userId;

    LoginRecord record = new LoginRecord();
    record.setIp(ip);
    record.setProvince(location.getProvince());
    record.setCity(location.getCity());
    record.setLoginTime(LocalDateTime.now());

    // 添加到列表头部
    redisTemplate.opsForList().leftPush(key, record);
    // 保持列表长度
    redisTemplate.opsForList().trim(key, 0, MAX_LOCATION_HISTORY - 1);
    // 设置过期时间
    redisTemplate.expire(key, LOCATION_EXPIRE_DAYS, TimeUnit.DAYS);
  }

  /**
   * 更新常用位置
   *
   * @param userId 用户ID
   * @param locationKey 位置标识（省份或城市）
   * @param isCityLevel 是否市级
   */
  private void updateTrustedLocation(
      final Long userId, final String locationKey, final boolean isCityLevel) {
    String suffix = isCityLevel ? ":city" : "";
    String countKey = TRUSTED_LOCATION_PREFIX + "count:" + userId + suffix + ":" + locationKey;
    String trustedKey = TRUSTED_LOCATION_PREFIX + userId + suffix;

    // 增加该位置的登录次数
    Long count = redisTemplate.opsForValue().increment(countKey, 1);
    redisTemplate.expire(countKey, LOCATION_EXPIRE_DAYS, TimeUnit.DAYS);

    // 如果达到阈值，添加到常用位置
    if (count != null && count >= TRUSTED_THRESHOLD) {
      redisTemplate.opsForSet().add(trustedKey, locationKey);
      redisTemplate.expire(trustedKey, LOCATION_EXPIRE_DAYS, TimeUnit.DAYS);
    }
  }

  /**
   * 生成异地登录许可码
   *
   * <p>根据系统配置，许可码可以是： 1. 固定码：管理员在系统配置中设置 2. 随机码：系统生成6位数字码，通过邮件发送给管理员
   *
   * @param userId 用户ID
   * @param username 用户名
   * @param ip 登录IP
   * @param location 登录位置
   * @return 许可码请求ID（用于后续验证）
   */
  public String requestPermitCode(
      final Long userId, final String username, final String ip, final IpLocation location) {
    // 生成请求ID
    String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    String key = PERMIT_CODE_PREFIX + requestId;

    // 获取许可码模式
    String mode = configAppService.getConfigValue("security.location.permit-code.mode");
    String permitCode;

    if ("fixed".equalsIgnoreCase(mode)) {
      // 固定码模式：从配置读取
      permitCode = configAppService.getConfigValue("security.location.permit-code.value");
      if (permitCode == null || permitCode.isEmpty()) {
        permitCode = "888888"; // 默认固定码
      }
    } else {
      // 随机码模式：生成6位随机数字
      permitCode =
          String.format("%06d", ThreadLocalRandom.current().nextInt(RANDOM_PERMIT_CODE_MAX));

      // 发送邮件通知管理员
      sendPermitCodeToAdmin(username, ip, location, permitCode);
    }

    // 存储许可码信息
    Map<String, Object> data = new HashMap<>();
    data.put("userId", userId);
    data.put("username", username);
    data.put("ip", ip);
    data.put("location", location.getShortDescription());
    data.put("permitCode", permitCode);
    data.put("timestamp", System.currentTimeMillis());

    redisTemplate.opsForValue().set(key, data, PERMIT_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

    // 发送异地登录告警邮件（不管是固定码还是随机码模式都发送）
    try {
      alertService.sendNewLocationLoginAlert(username, ip, location.getShortDescription());
    } catch (Exception e) {
      log.error("发送异地登录告警邮件失败", e);
    }

    log.info(
        "生成异地登录许可码请求: requestId={}, username={}, location={}, mode={}",
        requestId,
        username,
        location.getShortDescription(),
        mode);

    return requestId;
  }

  /**
   * 发送许可码邮件给管理员
   *
   * @param username 用户名
   * @param ip IP地址
   * @param location 位置信息
   * @param permitCode 许可码
   */
  private void sendPermitCodeToAdmin(
      final String username, final String ip, final IpLocation location, final String permitCode) {
    String subject = "[智慧律所] 异地登录许可请求";
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    String content =
        String.format(
            """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6;">
                <h2 style="color: #e74c3c;">⚠️ 异地登录许可请求</h2>
                <p>有用户尝试从异地登录系统，请核实后决定是否提供许可码：</p>
                <table style="border-collapse: collapse; margin: 20px 0;">
                    <tr><td style="padding: 8px; border: 1px solid #ddd; "
                        + "background: #f5f5f5;"><strong>用户名</strong></td>"
                        + "<td style=\"padding: 8px; border: 1px solid #ddd;\">%s</td></tr>
                    <tr><td style="padding: 8px; border: 1px solid #ddd; "
                        + "background: #f5f5f5;"><strong>登录IP</strong></td>"
                        + "<td style=\"padding: 8px; border: 1px solid #ddd;\">%s</td></tr>
                    <tr><td style="padding: 8px; border: 1px solid #ddd; "
                        + "background: #f5f5f5;"><strong>登录位置</strong></td>"
                        + "<td style=\"padding: 8px; border: 1px solid #ddd;\">%s</td></tr>
                    <tr><td style="padding: 8px; border: 1px solid #ddd; "
                        + "background: #f5f5f5;"><strong>请求时间</strong></td>"
                        + "<td style=\"padding: 8px; border: 1px solid #ddd;\">%s</td></tr>
                </table>
                <p style="font-size: 24px; color: #3498db; background: #ecf0f1; "
                    + "padding: 15px; text-align: center; letter-spacing: 8px;">
                    许可码：<strong>%s</strong>
                </p>
                <p style="color: #7f8c8d; font-size: 12px;">
                    ⏰ 此许可码 %d 分钟内有效<br>
                    🔒 如非本人操作，请忽略此邮件并检查账户安全
                </p>
            </body>
            </html>
            """,
            username,
            ip,
            location.getShortDescription(),
            time,
            permitCode,
            PERMIT_CODE_EXPIRE_MINUTES);

    try {
      emailService.sendAlertToAdmins(subject, content);
      log.info("异地登录许可码邮件已发送: username={}", username);
    } catch (Exception e) {
      log.error("发送异地登录许可码邮件失败: username={}", username, e);
    }
  }

  /**
   * 验证许可码
   *
   * @param requestId 许可码请求ID
   * @param inputCode 用户输入的许可码
   * @param userId 用户ID
   * @return 验证结果
   */
  @SuppressWarnings("unchecked")
  public PermitCodeVerifyResult verifyPermitCode(
      final String requestId, final String inputCode, final Long userId) {
    if (requestId == null || requestId.isEmpty() || inputCode == null || inputCode.isEmpty()) {
      return PermitCodeVerifyResult.fail("许可码不能为空");
    }

    String key = PERMIT_CODE_PREFIX + requestId;
    Object data = redisTemplate.opsForValue().get(key);

    if (data == null) {
      return PermitCodeVerifyResult.fail("许可码已过期或无效，请重新登录");
    }

    if (!(data instanceof Map)) {
      return PermitCodeVerifyResult.fail("许可码数据异常");
    }

    Map<String, Object> map = (Map<String, Object>) data;
    Object storedUserId = map.get("userId");
    String storedCode = (String) map.get("permitCode");
    String ip = (String) map.get("ip");

    // 验证用户ID
    if (!userId.equals(storedUserId) && !userId.toString().equals(storedUserId.toString())) {
      return PermitCodeVerifyResult.fail("许可码与当前用户不匹配");
    }

    // 验证许可码
    if (!inputCode.equals(storedCode)) {
      log.warn("许可码验证失败: requestId={}, userId={}", requestId, userId);
      return PermitCodeVerifyResult.fail("许可码错误");
    }

    // 验证成功，删除许可码
    redisTemplate.delete(key);

    log.info("许可码验证成功: requestId={}, userId={}", requestId, userId);
    return PermitCodeVerifyResult.success(ip);
  }

  /**
   * 将新位置添加到常用位置（许可码验证通过后）
   *
   * @param userId 用户ID
   * @param username 用户名
   * @param ip IP地址
   */
  public void trustCurrentLocation(final Long userId, final String username, final String ip) {
    IpLocation location = ipLocationService.getLocation(ip);
    String province = location.getProvince();
    String city = location.getCity();

    // 获取判断级别
    String level = configAppService.getConfigValue("security.location.level");
    boolean isCityLevel = "city".equalsIgnoreCase(level);

    // 添加到对应级别的常用位置
    String locationKey = isCityLevel ? city : province;
    if (locationKey != null) {
      String suffix = isCityLevel ? ":city" : "";
      String trustedKey = TRUSTED_LOCATION_PREFIX + userId + suffix;
      redisTemplate.opsForSet().add(trustedKey, locationKey);
      redisTemplate.expire(trustedKey, LOCATION_EXPIRE_DAYS, TimeUnit.DAYS);

      log.info(
          "用户添加新的常用位置: userId={}, location={}, level={}",
          userId,
          locationKey,
          isCityLevel ? "city" : "province");

      // 发送异地登录成功告警邮件
      try {
        alertService.sendNewLocationLoginSuccessAlert(username, ip, location.getShortDescription());
      } catch (Exception e) {
        log.error("发送异地登录成功告警邮件失败", e);
      }
    }
  }

  /** 许可码验证结果. */
  @lombok.Data
  public static class PermitCodeVerifyResult {
    /** 是否成功. */
    private boolean success;

    /** 消息. */
    private String message;

    /** IP地址. */
    private String ip;

    /**
     * 创建成功结果
     *
     * @param ip IP地址
     * @return 验证结果
     */
    public static PermitCodeVerifyResult success(final String ip) {
      PermitCodeVerifyResult result = new PermitCodeVerifyResult();
      result.setSuccess(true);
      result.setIp(ip);
      return result;
    }

    /**
     * 创建失败结果
     *
     * @param message 错误消息
     * @return 验证结果
     */
    public static PermitCodeVerifyResult fail(final String message) {
      PermitCodeVerifyResult result = new PermitCodeVerifyResult();
      result.setSuccess(false);
      result.setMessage(message);
      return result;
    }
  }

  /**
   * 获取用户登录位置历史
   *
   * @param userId 用户ID
   * @param limit 限制数量
   * @return 登录记录列表
   */
  @SuppressWarnings("unchecked")
  public List<LoginRecord> getLoginHistory(final Long userId, final int limit) {
    String key = LOGIN_LOCATION_PREFIX + userId;
    List<Object> records = redisTemplate.opsForList().range(key, 0, limit - 1);

    if (records == null) {
      return new ArrayList<>();
    }

    List<LoginRecord> result = new ArrayList<>();
    for (Object obj : records) {
      if (obj instanceof LoginRecord) {
        result.add((LoginRecord) obj);
      } else if (obj instanceof Map) {
        // 处理从 Redis 反序列化的 LinkedHashMap
        Map<String, Object> map = (Map<String, Object>) obj;
        LoginRecord record = new LoginRecord();
        record.setIp((String) map.get("ip"));
        record.setProvince((String) map.get("province"));
        record.setCity((String) map.get("city"));
        // 处理时间字段
        result.add(record);
      }
    }

    return result;
  }

  // ========== 数据类 ==========

  /** 位置检查结果. */
  @lombok.Data
  public static class LocationCheckResult {
    /** 是否为新位置（异地）. */
    private boolean newLocation;

    /** 是否首次登录. */
    private boolean firstLogin;

    /** 当前位置信息. */
    private IpLocation currentLocation;

    /** 信任的省份集合. */
    private Set<String> trustedProvinces;

    /** 消息. */
    private String message;

    /**
     * 创建正常结果
     *
     * @param location 位置信息
     * @return 检查结果
     */
    public static LocationCheckResult normal(final IpLocation location) {
      LocationCheckResult result = new LocationCheckResult();
      result.setNewLocation(false);
      result.setFirstLogin(false);
      result.setCurrentLocation(location);
      return result;
    }

    /**
     * 创建首次登录结果
     *
     * @param location 位置信息
     * @return 检查结果
     */
    public static LocationCheckResult firstLogin(final IpLocation location) {
      LocationCheckResult result = new LocationCheckResult();
      result.setNewLocation(false);
      result.setFirstLogin(true);
      result.setCurrentLocation(location);
      return result;
    }

    /**
     * 创建新位置结果
     *
     * @param location 位置信息
     * @param trustedProvinces 信任的省份集合
     * @return 检查结果
     */
    public static LocationCheckResult newLocation(
        final IpLocation location, final Set<String> trustedProvinces) {
      LocationCheckResult result = new LocationCheckResult();
      result.setNewLocation(true);
      result.setFirstLogin(false);
      result.setCurrentLocation(location);
      result.setTrustedProvinces(trustedProvinces);
      result.setMessage(String.format("检测到您从新的位置（%s）登录，请完成安全验证", location.getShortDescription()));
      return result;
    }
  }

  /** 登录记录. */
  @lombok.Data
  public static class LoginRecord implements java.io.Serializable {
    /** SerialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** IP地址. */
    private String ip;

    /** 省份. */
    private String province;

    /** 城市. */
    private String city;

    /** 登录时间. */
    private LocalDateTime loginTime;
  }
}
