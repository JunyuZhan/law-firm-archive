# OnlyOffice 欢迎页面密码说明

## 访问地址

OnlyOffice Document Server 的欢迎页面：
- 直接访问：`http://localhost:8088/welcome/`
- 通过代理：`http://localhost:5666/onlyoffice/welcome/`

## Bootstrap Code（临时密码）

OnlyOffice 的欢迎页面在首次访问时需要输入 **Bootstrap Code**（引导代码）来完成初始化设置。

### 如何获取 Bootstrap Code

1. **查看 Admin Panel 日志**（推荐方法）：
   ```bash
   docker exec law-firm-onlyoffice cat /var/log/onlyoffice/documentserver/adminpanel/out.log | grep -i bootstrap
   ```

2. **查看容器启动日志**：
   ```bash
   docker logs law-firm-onlyoffice | grep -i bootstrap
   ```

3. **实时监控日志**（访问 welcome 页面时）：
   ```bash
   # 终端1：监控日志
   docker exec law-firm-onlyoffice tail -f /var/log/onlyoffice/documentserver/adminpanel/out.log
   
   # 终端2：访问 welcome 页面
   curl http://localhost:8088/welcome/
   ```

### Bootstrap Code 格式

Bootstrap Code 通常是：
- 11位字符（大写字母和数字）
- 格式类似：`ELJB4VIF8YCM`
- 有效期：1小时
- 示例日志输出：
  ```
  AdminPanel SETUP REQUIRED | Bootstrap code: ELJB4VIF8YCM | Expires: 2026-01-18T17:00:00.000Z | Open: http://host/admin
  ```

## 使用步骤

1. **访问 welcome 页面**：
   ```
   http://localhost:8088/welcome/
   ```

2. **获取 Bootstrap Code**：
   - 查看日志文件：`/var/log/onlyoffice/documentserver/adminpanel/out.log`
   - 或查看容器日志：`docker logs law-firm-onlyoffice`

3. **输入 Bootstrap Code**：
   - 在 welcome 页面输入获取到的代码
   - 设置新的管理员密码

4. **完成设置**：
   - 设置完成后，可以使用新密码登录 Admin Panel

## 重置密码

如果忘记了管理员密码，可以重置：

1. **删除密码配置**：
   ```bash
   docker exec law-firm-onlyoffice rm -f /var/www/onlyoffice/Data/.private/admin.json
   ```

2. **重启容器**：
   ```bash
   docker restart law-firm-onlyoffice
   ```

3. **重新获取 Bootstrap Code**：
   - 访问 welcome 页面
   - 查看日志获取新的 bootstrap code

## Admin Panel 访问

设置完成后，可以通过以下地址访问 Admin Panel：

- Admin Panel 端口：9000（默认，但可能未暴露）
- 通过 welcome 页面完成设置后，可以使用设置的管理员密码登录

## 注意事项

1. **Bootstrap Code 有效期**：只有1小时，过期后需要重新生成
2. **首次访问**：只有在首次访问 welcome 页面时才会生成 bootstrap code
3. **日志位置**：bootstrap code 会出现在 Admin Panel 的日志中
4. **生产环境**：建议在生产环境中禁用 welcome 页面，直接配置管理员密码

## 快速查找命令

```bash
# 方法1：查看 Admin Panel 日志
docker exec law-firm-onlyoffice tail -50 /var/log/onlyoffice/documentserver/adminpanel/out.log | grep -i bootstrap

# 方法2：查看容器日志
docker logs law-firm-onlyoffice 2>&1 | grep -i bootstrap

# 方法3：实时监控（访问 welcome 页面时）
docker exec law-firm-onlyoffice tail -f /var/log/onlyoffice/documentserver/adminpanel/out.log
```

## 如果找不到 Bootstrap Code

1. **确认 Admin Panel 服务正在运行**：
   ```bash
   docker exec law-firm-onlyoffice ps aux | grep admin
   ```

2. **检查日志文件权限**：
   ```bash
   docker exec law-firm-onlyoffice ls -la /var/log/onlyoffice/documentserver/adminpanel/
   ```

3. **手动触发生成**：
   - 访问 `http://localhost:8088/welcome/`
   - 同时监控日志：`docker exec law-firm-onlyoffice tail -f /var/log/onlyoffice/documentserver/adminpanel/out.log`
