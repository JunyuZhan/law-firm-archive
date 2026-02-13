# JMeter 压力测试脚本

## 概述

本目录包含律师事务所管理系统的 JMeter 压力测试脚本，用于验证系统在高并发场景下的性能表现。

## 测试场景

| 脚本文件 | 测试场景 | 并发数 | 持续时间 |
|----------|----------|--------|----------|
| `login-stress-test.jmx` | 并发登录测试 | 100 | 5分钟 |
| `matter-stress-test.jmx` | 项目管理接口压测 | 50 | 5分钟 |
| `client-stress-test.jmx` | 客户管理接口压测 | 50 | 5分钟 |
| `file-upload-stress-test.jmx` | 文件上传压测 | 10 | 5分钟 |
| `full-stress-test.jmx` | 综合压力测试 | 100 | 10分钟 |

## 性能指标要求

| 指标 | 目标值 |
|------|--------|
| 响应时间 P50 | < 100ms |
| 响应时间 P95 | < 500ms |
| 响应时间 P99 | < 1000ms |
| 错误率 | < 0.1% |
| TPS（登录） | > 100 |
| TPS（查询） | > 200 |
| TPS（写入） | > 50 |

## 使用方法

### 1. 安装 JMeter

```bash
# macOS
brew install jmeter

# 或下载官方版本
# https://jmeter.apache.org/download_jmeter.cgi
```

### 2. 配置测试参数

编辑 `config.properties` 文件：

```properties
# 服务器配置
server.host=localhost
server.port=8080
server.protocol=http

# 测试账号
test.username=admin
test.password=admin123

# 并发配置
threads.login=100
threads.query=50
threads.write=20
```

### 3. 运行测试

```bash
# 使用批量执行脚本（推荐）
./run-all-tests.sh login              # 运行登录测试
./run-all-tests.sh matter             # 运行项目管理测试
./run-all-tests.sh client             # 运行客户管理测试
./run-all-tests.sh upload             # 运行文件上传测试
./run-all-tests.sh full               # 运行综合测试
./run-all-tests.sh all                # 运行所有测试

# 指定服务器地址
./run-all-tests.sh -h 192.168.1.100 -p 8080 all

# GUI 模式（调试用）
./run-all-tests.sh -g login
jmeter -t login-stress-test.jmx

# 命令行模式（正式压测）
jmeter -n -t login-stress-test.jmx -l results/login-result.jtl -e -o results/login-report
```

### 4. 查看报告

测试完成后，在 `results/` 目录下查看 HTML 报告。

## 测试前准备

1. 确保数据库有足够的测试数据
2. 确保 Redis 服务正常运行
3. 关闭不必要的日志输出（生产模式）
4. 确保测试账号有足够的权限

## 注意事项

- 压测前请备份数据库
- 建议在独立的测试环境进行
- 监控服务器 CPU、内存、网络使用情况
- 注意限流配置可能影响测试结果

## 作者

junyuzhan @ 2026-01-10

## 文件清单

```
scripts/jmeter/
├── README.md                      # 本文档
├── config.properties              # 测试配置文件
├── run-all-tests.sh               # 批量执行脚本
├── login-stress-test.jmx          # 登录压力测试
├── matter-stress-test.jmx         # 项目管理压力测试
├── client-stress-test.jmx         # 客户管理压力测试
├── file-upload-stress-test.jmx    # 文件上传压力测试
├── full-stress-test.jmx           # 综合压力测试
└── results/                       # 测试结果目录（自动生成）
```
