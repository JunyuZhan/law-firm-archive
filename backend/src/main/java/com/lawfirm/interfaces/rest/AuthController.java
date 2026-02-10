package com.lawfirm.interfaces.rest;

import com.lawfirm.application.system.service.AuthService;
import com.lawfirm.application.system.service.CaptchaService;
import com.lawfirm.application.system.service.LoginLocationService;
import com.lawfirm.application.system.service.LoginLockService;
import com.lawfirm.application.system.service.SliderCaptchaService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RateLimiter;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.IpUtils;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller
 *
 * <p>安全防护机制： 1. 滑块验证（后端校验）- 防止机器人 2. 登录失败锁定 - 防止暴力破解 3. 条件触发图形验证码 - 失败多次后增强验证 4. IP速率限制 - 防止高频攻击 5.
 * 异地登录检测 - 新位置需要额外验证
 */
@Tag(name = "认证管理", description = "用户认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

  /** 认证服务 */
  private final AuthService authService;

  /** 验证码服务 */
  private final CaptchaService captchaService;

  /** 滑块验证码服务 */
  private final SliderCaptchaService sliderCaptchaService;

  /** 登录锁定服务 */
  private final LoginLockService loginLockService;

  /** 登录位置服务 */
  private final LoginLocationService loginLocationService;

  /** 验证码限流速率 */
  private static final int CAPTCHA_RATE_LIMIT = 20;

  /** 验证码限流间隔 */
  private static final int CAPTCHA_RATE_INTERVAL = 60;

  /** 滑块限流速率 */
  private static final int SLIDER_RATE_LIMIT = 30;

  /** 滑块限流间隔 */
  private static final int SLIDER_RATE_INTERVAL = 60;

  /**
   * 获取图形验证码
   *
   * @return 图形验证码结果
   */
  @Operation(summary = "获取图形验证码")
  @GetMapping("/captcha")
  @RateLimiter(
      key = "captcha",
      rate = CAPTCHA_RATE_LIMIT,
      interval = CAPTCHA_RATE_INTERVAL,
      limitType = RateLimiter.LimitType.IP,
      message = "验证码请求过于频繁")
  public Result<CaptchaService.CaptchaResult> getCaptcha() {
    CaptchaService.CaptchaResult result = captchaService.generateCaptcha();
    return Result.success(result);
  }

  /**
   * 获取滑块验证令牌
   *
   * @return 滑块验证令牌结果
   */
  @Operation(summary = "获取滑块验证令牌")
  @GetMapping("/slider/token")
  @RateLimiter(
      key = "slider",
      rate = SLIDER_RATE_LIMIT,
      interval = SLIDER_RATE_INTERVAL,
      limitType = RateLimiter.LimitType.IP,
      message = "请求过于频繁")
  public Result<SliderCaptchaService.SliderTokenResult> getSliderToken() {
    SliderCaptchaService.SliderTokenResult result = sliderCaptchaService.generateToken();
    return Result.success(result);
  }

  /**
   * 验证滑块操作
   *
   * @param request 滑块验证请求
   * @return 滑块验证结果
   */
  @Operation(summary = "验证滑块操作")
  @PostMapping("/slider/verify")
  @RateLimiter(
      key = "'slider_verify'",
      rate = 20,
      interval = 60,
      limitType = RateLimiter.LimitType.IP,
      message = "验证请求过于频繁")
  public Result<SliderCaptchaService.SliderVerifyResult> verifySlider(
      @RequestBody @Valid final SliderVerifyRequest request) {
    SliderCaptchaService.SliderVerifyResult result =
        sliderCaptchaService.verify(
            request.getTokenId(), request.getSlideTime(), request.getSlideTrack());

    if (!result.isSuccess()) {
      return Result.error(result.getMessage());
    }
    return Result.success(result);
  }

  /**
   * 检查登录状态（是否需要验证码、是否被锁定）
   *
   * @param username 用户名
   * @return 登录状态响应
   */
  @Operation(summary = "检查登录状态")
  @GetMapping("/login/status")
  public Result<LoginStatusResponse> checkLoginStatus(@RequestParam final String username) {
    // 检查账户是否被锁定
    LoginLockService.LockStatus lockStatus = loginLockService.checkLockStatus(username);

    // 检查是否需要图形验证码
    boolean captchaRequired = loginLockService.isCaptchaRequired(username);
    int failCount = loginLockService.getFailCount(username);

    LoginStatusResponse response = new LoginStatusResponse();
    response.setLocked(lockStatus.isLocked());
    response.setLockRemainingMinutes(lockStatus.getRemainingMinutes());
    response.setCaptchaRequired(captchaRequired);
    response.setFailCount(failCount);
    response.setMessage(lockStatus.isLocked() ? lockStatus.getMessage() : null);

    return Result.success(response);
  }

  /**
   * 登录
   *
   * <p>安全验证流程： 1. 检查账户是否被锁定 2. 验证滑块验证凭证（必须） 3. 如果失败次数>=3，需要图形验证码 4. 验证用户名密码 5. 登录失败则记录失败次数
   *
   * @param request 登录请求
   * @param httpRequest HTTP请求
   * @return 登录结果
   */
  @PostMapping("/login")
  @OperationLog(module = "认证", action = "用户登录", saveResult = false, saveParams = false)
  @Operation(summary = "用户登录")
  @RateLimiter(
      key = "login",
      rate = 10,
      interval = 60,
      limitType = RateLimiter.LimitType.IP,
      message = "登录尝试过于频繁，请稍后再试")
  public Result<?> login(
      @RequestBody @Valid final LoginRequest request, final HttpServletRequest httpRequest) {
    String ip = IpUtils.getIpAddr(httpRequest);
    String username = request.getUsername();

    // 1. 检查账户是否被锁定
    LoginLockService.LockStatus lockStatus = loginLockService.checkLockStatus(username);
    if (lockStatus.isLocked()) {
      log.warn(
          "账户已锁定，拒绝登录: username={}, ip={}, remainingMinutes={}",
          username,
          ip,
          lockStatus.getRemainingMinutes());
      return Result.error(lockStatus.getMessage());
    }

    // 2. 验证滑块验证凭证（必须）
    if (request.getSliderVerifyToken() == null || request.getSliderVerifyToken().isEmpty()) {
      log.warn("缺少滑块验证凭证: username={}, ip={}", username, ip);
      return Result.error("请先完成滑块验证");
    }

    boolean sliderVerified = sliderCaptchaService.checkVerified(request.getSliderVerifyToken());
    if (!sliderVerified) {
      log.warn("滑块验证凭证无效: username={}, ip={}", username, ip);
      return Result.error("滑块验证已过期，请重新验证");
    }

    // 3. 检查是否需要图形验证码
    boolean captchaRequired = loginLockService.isCaptchaRequired(username);
    if (captchaRequired) {
      if (request.getCaptchaId() == null || request.getCaptchaCode() == null) {
        log.warn("需要图形验证码但未提供: username={}, ip={}", username, ip);
        return Result.error("CAPTCHA_REQUIRED", "请完成图形验证码");
      }

      boolean captchaVerified =
          captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
      if (!captchaVerified) {
        log.warn("图形验证码验证失败: username={}, ip={}", username, ip);
        return Result.error("验证码错误或已过期，请刷新后重试");
      }
    }

    // 4. 验证用户名密码
    try {
      String userAgent = httpRequest.getHeader("User-Agent");
      AuthService.LoginResult result =
          authService.login(username, request.getPassword(), ip, userAgent);

      // 5. 检测异地登录（密码验证成功后）
      LoginLocationService.LocationCheckResult locationCheck =
          loginLocationService.checkLocation(result.getUserId(), ip);

      if (locationCheck.isNewLocation()) {
        // 检查是否提供了许可码
        if (request.getPermitRequestId() == null || request.getPermitRequestId().isEmpty()) {
          // 需要许可码，生成许可码请求
          String requestId =
              loginLocationService.requestPermitCode(
                  result.getUserId(), username, ip, locationCheck.getCurrentLocation());

          log.warn(
              "检测到异地登录，需要管理员许可码: username={}, ip={}, location={}",
              username,
              ip,
              locationCheck.getCurrentLocation().getShortDescription());

          // 返回异地登录错误，需要用户输入许可码
          NewLocationResponse nlResponse = new NewLocationResponse();
          nlResponse.setRequestId(requestId);
          nlResponse.setCurrentLocation(locationCheck.getCurrentLocation().getShortDescription());
          nlResponse.setMessage("检测到异地登录，请联系管理员获取许可码");

          return Result.error("NEW_LOCATION", nlResponse.getMessage(), nlResponse);
        }

        // 验证许可码
        LoginLocationService.PermitCodeVerifyResult verifyResult =
            loginLocationService.verifyPermitCode(
                request.getPermitRequestId(), request.getPermitCode(), result.getUserId());

        if (!verifyResult.isSuccess()) {
          log.warn(
              "许可码验证失败: username={}, ip={}, message={}", username, ip, verifyResult.getMessage());
          return Result.error("PERMIT_CODE_ERROR", verifyResult.getMessage());
        }

        // 验证通过，将当前位置添加为可信位置
        loginLocationService.trustCurrentLocation(result.getUserId(), username, ip);
        log.info("异地登录许可码验证通过，已添加为可信位置: username={}, ip={}", username, ip);
      }

      // 登录成功，清除失败记录并记录登录位置
      loginLockService.clearFailure(username);
      loginLocationService.recordLogin(result.getUserId(), ip, true);

      LoginResponse response = new LoginResponse();
      response.setAccessToken(result.getAccessToken());
      response.setRefreshToken(result.getRefreshToken());
      response.setExpiresIn(result.getExpiresIn());
      response.setUserId(result.getUserId());
      response.setUsername(result.getUsername());
      response.setRealName(result.getRealName());
      response.setRoles(result.getRoles());
      response.setPermissions(result.getPermissions());

      log.info("用户登录成功: username={}, ip={}", username, ip);
      return Result.success(response);

    } catch (Exception e) {
      // 5. 登录失败，记录失败次数（带IP，用于告警）
      LoginLockService.FailResult failResult = loginLockService.recordFailure(username, ip);
      log.warn(
          "登录失败: username={}, ip={}, failCount={}, locked={}",
          username,
          ip,
          failResult.getFailCount(),
          failResult.isLocked());

      // 根据失败结果返回不同的错误信息
      if (failResult.isLocked()) {
        return Result.error("ACCOUNT_LOCKED", failResult.getMessage());
      } else if (failResult.isCaptchaRequired()) {
        return Result.error("CAPTCHA_REQUIRED", failResult.getMessage());
      } else {
        return Result.error(failResult.getMessage());
      }
    }
  }

  /**
   * 刷新Token
   *
   * @param request 刷新Token请求
   * @return 登录响应
   */
  @PostMapping("/refresh")
  @Operation(summary = "刷新Token")
  public Result<LoginResponse> refresh(@RequestBody @Valid final RefreshRequest request) {
    AuthService.LoginResult result = authService.refreshToken(request.getRefreshToken());

    LoginResponse response = new LoginResponse();
    response.setAccessToken(result.getAccessToken());
    response.setRefreshToken(result.getRefreshToken());
    response.setExpiresIn(result.getExpiresIn());
    response.setUserId(result.getUserId());
    response.setUsername(result.getUsername());
    response.setRealName(result.getRealName());
    response.setRoles(result.getRoles());
    response.setPermissions(result.getPermissions());

    return Result.success(response);
  }

  /**
   * 登出 允许未登录用户调用，避免前端循环重试
   *
   * @param httpRequest HTTP请求
   * @return 空结果
   */
  @PostMapping("/logout")
  @OperationLog(module = "认证", action = "用户登出")
  @Operation(summary = "用户登出")
  public Result<Void> logout(final HttpServletRequest httpRequest) {
    // 尝试获取用户ID，如果未登录则返回null（避免抛出异常）
    Long userId = null;
    try {
      userId = SecurityUtils.getUserId();
    } catch (Exception e) {
      // 用户未登录，允许继续执行（可能是token已过期的情况）
    }

    String token = httpRequest.getHeader("Authorization");
    if (token != null && token.startsWith("Bearer ")) {
      token = token.substring(7);
    }

    // 如果用户已登录，执行登出逻辑；如果未登录，直接返回成功（避免前端循环重试）
    if (userId != null) {
      authService.logout(userId, token);
    }
    return Result.success();
  }

  /**
   * 获取当前用户信息
   *
   * @return 用户信息响应
   */
  @GetMapping("/info")
  @Operation(summary = "获取当前用户信息")
  public Result<UserInfoResponse> getCurrentUser() {
    UserInfoResponse response = new UserInfoResponse();
    response.setUserId(SecurityUtils.getUserId());
    response.setUsername(SecurityUtils.getUsername());
    response.setRealName(SecurityUtils.getRealName());
    response.setRoles(SecurityUtils.getRoles());
    response.setPermissions(SecurityUtils.getPermissions());
    response.setDepartmentId(SecurityUtils.getDepartmentId());
    response.setCompensationType(SecurityUtils.getCompensationType());
    return Result.success(response);
  }

  // ========== Request/Response DTOs ==========

  /** 登录请求 */
  @Data
  public static class LoginRequest {
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 滑块验证凭证（必须） */
    private String sliderVerifyToken;

    /** 图形验证码ID（失败多次后需要） */
    private String captchaId;

    /** 图形验证码答案（失败多次后需要） */
    private String captchaCode;

    /** 许可码请求ID（异地登录后需要） */
    private String permitRequestId;

    /** 许可码（异地登录后需要，联系管理员获取） */
    private String permitCode;
  }

  /** 滑块验证请求 */
  @Data
  public static class SliderVerifyRequest {
    /** 令牌ID */
    @NotBlank(message = "令牌ID不能为空")
    private String tokenId;

    /** 滑动耗时（毫秒） */
    private long slideTime;

    /** 滑动轨迹（可选） */
    private int[] slideTrack;
  }

  /** 登录状态响应 */
  @Data
  public static class LoginStatusResponse {
    /** 是否锁定 */
    private boolean locked;

    /** 锁定剩余分钟数 */
    private int lockRemainingMinutes;

    /** 是否需要验证码 */
    private boolean captchaRequired;

    /** 失败次数 */
    private int failCount;

    /** 消息 */
    private String message;
  }

  /** 刷新令牌请求 */
  @Data
  public static class RefreshRequest {
    /** 刷新令牌 */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
  }

  /** 登录响应 */
  @Data
  public static class LoginResponse {
    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色集合 */
    private Set<String> roles;

    /** 权限集合 */
    private Set<String> permissions;
  }

  /** 新位置响应 */
  @Data
  public static class NewLocationResponse {
    /** 许可码请求ID */
    private String requestId;

    /** 当前登录位置 */
    private String currentLocation;

    /** 提示信息 */
    private String message;
  }

  /** 用户信息响应 */
  @Data
  public static class UserInfoResponse {
    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 部门ID */
    private Long departmentId;

    /** 薪酬类型 */
    private String compensationType;

    /** 角色集合 */
    private Set<String> roles;

    /** 权限集合 */
    private Set<String> permissions;
  }
}
