package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.dto.LoginLogDTO;
import com.lawfirm.application.system.dto.LoginLogQueryDTO;
import com.lawfirm.application.system.service.LoginLogAppService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 登录日志控制器 */
@RestController
@RequestMapping("/system/login-log")
@RequiredArgsConstructor
@Tag(name = "登录日志", description = "登录日志查询和管理")
public class LoginLogController {

  /** 登录日志应用服务 */
  private final LoginLogAppService loginLogAppService;

  /**
   * 查询登录日志列表
   *
   * @param query 查询条件
   * @return 登录日志分页结果
   */
  @GetMapping
  @RequirePermission("sys:loginlog:list")
  @Operation(summary = "查询登录日志列表")
  public Result<PageResult<LoginLogDTO>> listLoginLogs(final LoginLogQueryDTO query) {
    PageResult<LoginLogDTO> result = loginLogAppService.listLoginLogs(query);
    return Result.success(result);
  }

  /**
   * 获取登录日志详情
   *
   * @param id 登录日志ID
   * @return 登录日志详情
   */
  @GetMapping("/{id}")
  @RequirePermission("sys:loginlog:view")
  @Operation(summary = "获取登录日志详情")
  public Result<LoginLogDTO> getLoginLog(@PathVariable final Long id) {
    LoginLogDTO log = loginLogAppService.getLoginLog(id);
    if (log == null) {
      return Result.notFound("登录日志不存在");
    }
    return Result.success(log);
  }

  /**
   * 查询用户最近登录记录
   *
   * @param userId 用户ID
   * @param limit 返回记录数限制
   * @return 最近登录记录列表
   */
  @GetMapping("/users/{userId}/recent")
  @RequirePermission("sys:loginlog:view")
  @Operation(summary = "查询用户最近登录记录")
  public Result<List<LoginLogDTO>> getRecentLogs(
      @PathVariable final Long userId, @RequestParam(defaultValue = "10") final int limit) {
    List<LoginLogDTO> logs = loginLogAppService.getRecentLogsByUserId(userId, limit);
    return Result.success(logs);
  }

  /**
   * 统计登录失败次数
   *
   * @param username 用户名
   * @return 失败次数
   */
  @GetMapping("/failure-count")
  @RequirePermission("sys:loginlog:view")
  @Operation(summary = "统计登录失败次数")
  public Result<Integer> countFailure(@RequestParam final String username) {
    int count = loginLogAppService.countFailureByUsername(username);
    return Result.success(count);
  }
}
