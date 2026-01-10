# JMeter 压力测试计划

> **版本**：1.0.0  
> **日期**：2026-01-10  
> **作者**：Kiro-1

---

## 📋 测试概述

### 测试目标

1. 验证系统在高并发下的稳定性
2. 找出系统性能瓶颈
3. 确定系统的最大承载能力
4. 验证响应时间是否满足 SLA 要求

### 性能指标要求

| 指标 | 要求 |
|------|------|
| 响应时间 P50 | < 200ms |
| 响应时间 P95 | < 500ms |
| 响应时间 P99 | < 1000ms |
| 错误率 | < 0.1% |
| TPS（核心接口） | > 100 |

---

## 🔧 环境准备

### JMeter 安装

```bash
# macOS
brew install jmeter

# 或下载安装
# https://jmeter.apache.org/download_jmeter.cgi
```

### 必要插件

1. **JMeter Plugins Manager**
2. **Custom Thread Groups**
3. **3 Basic Graphs**
4. **PerfMon Servers Agent**

### 配置调优

```bash
# 编辑 jmeter.properties 或 user.properties
# 增加堆内存
HEAP="-Xms2g -Xmx4g"

# 关闭不必要的监听器（在命令行模式下）
jmeter.save.saveservice.output_format=csv
```

---

## 📝 测试场景

### 场景1：登录接口压测

**接口**：`POST /api/auth/login`

**参数**：
- 并发用户：100
- 循环次数：100
- Ramp-Up：30秒

**请求示例**：
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**JMeter 配置**：
```xml
<ThreadGroup>
  <stringProp name="ThreadGroup.num_threads">100</stringProp>
  <stringProp name="ThreadGroup.ramp_time">30</stringProp>
  <boolProp name="ThreadGroup.scheduler">true</boolProp>
  <stringProp name="ThreadGroup.duration">300</stringProp>
</ThreadGroup>
```

**断言**：
- 响应码：200
- 响应时间 < 500ms
- 响应包含 "token"

---

### 场景2：项目列表查询

**接口**：`GET /api/matters?page=1&size=20`

**参数**：
- 并发用户：50
- 循环次数：200
- Ramp-Up：20秒

**需要 Token**：是（从登录接口提取）

**JMeter 配置**：
```xml
<!-- 正则表达式提取 Token -->
<RegexExtractor>
  <stringProp name="RegexExtractor.regex">"token":"(.+?)"</stringProp>
  <stringProp name="RegexExtractor.template">$1$</stringProp>
  <stringProp name="RegexExtractor.match_number">1</stringProp>
  <stringProp name="RegexExtractor.refname">token</stringProp>
</RegexExtractor>

<!-- HTTP Header Manager -->
<HeaderManager>
  <collectionProp name="HeaderManager.headers">
    <elementProp name="" elementType="Header">
      <stringProp name="Header.name">Authorization</stringProp>
      <stringProp name="Header.value">Bearer ${token}</stringProp>
    </elementProp>
  </collectionProp>
</HeaderManager>
```

---

### 场景3：创建项目

**接口**：`POST /api/matters`

**参数**：
- 并发用户：20
- 循环次数：50
- Ramp-Up：10秒

**请求示例**：
```json
{
  "name": "测试案件-${__threadNum}-${__time(yyyyMMddHHmmss)}",
  "matterType": "LITIGATION",
  "caseType": "CIVIL",
  "clientId": 1,
  "description": "压力测试创建的案件"
}
```

**断言**：
- 响应码：200 或 201
- 响应时间 < 1000ms

---

### 场景4：文件上传

**接口**：`POST /api/documents/upload`

**参数**：
- 并发用户：10
- 循环次数：20
- Ramp-Up：10秒
- 文件大小：10MB

**JMeter 配置**：
```xml
<HTTPSamplerProxy>
  <boolProp name="HTTPSampler.postBodyRaw">false</boolProp>
  <elementProp name="HTTPsampler.Files" elementType="HTTPFileArgs">
    <collectionProp name="HTTPFileArgs.files">
      <elementProp name="" elementType="HTTPFileArg">
        <stringProp name="File.path">/path/to/test-file-10mb.pdf</stringProp>
        <stringProp name="File.paramname">file</stringProp>
        <stringProp name="File.mimetype">application/pdf</stringProp>
      </elementProp>
    </collectionProp>
  </elementProp>
</HTTPSamplerProxy>
```

---

### 场景5：混合场景

**模拟真实用户行为**：

| 操作 | 权重 | 说明 |
|------|------|------|
| 登录 | 5% | 初始登录 |
| 查看项目列表 | 40% | 最常用操作 |
| 查看项目详情 | 25% | 点击查看 |
| 创建项目 | 10% | 新建案件 |
| 编辑项目 | 10% | 修改信息 |
| 文件上传 | 5% | 上传文档 |
| 搜索 | 5% | 全局搜索 |

**JMeter 配置**：使用 Throughput Controller 或 Random Controller

---

## 🚀 执行测试

### 命令行模式（推荐）

```bash
# 基本执行
jmeter -n -t test-plan.jmx -l results.jtl

# 指定线程数和循环
jmeter -n -t test-plan.jmx -l results.jtl \
  -Jthreads=100 -Jloops=100 -Jrampup=30

# 生成 HTML 报告
jmeter -n -t test-plan.jmx -l results.jtl \
  -e -o ./report

# 分布式测试（多台压测机）
jmeter -n -t test-plan.jmx -l results.jtl \
  -R slave1,slave2,slave3
```

### 报告生成

```bash
# 从 JTL 文件生成 HTML 报告
jmeter -g results.jtl -o ./html-report
```

---

## 📊 测试报告模板

### 测试结果汇总

| 接口 | 并发数 | 总请求 | 成功率 | P50 | P95 | P99 | TPS | 结论 |
|------|--------|--------|--------|-----|-----|-----|-----|------|
| 登录 | 100 | 10000 | - | - | - | - | - | - |
| 项目列表 | 50 | 10000 | - | - | - | - | - | - |
| 创建项目 | 20 | 1000 | - | - | - | - | - | - |
| 文件上传 | 10 | 200 | - | - | - | - | - | - |

### 问题记录

| 问题ID | 描述 | 严重程度 | 原因分析 | 解决方案 |
|--------|------|----------|----------|----------|
| - | - | - | - | - |

### 优化建议

1. **数据库优化**：
   - [ ] 添加缺失索引
   - [ ] 优化慢查询
   - [ ] 调整连接池大小

2. **应用优化**：
   - [ ] 增加缓存
   - [ ] 优化代码逻辑
   - [ ] 调整 JVM 参数

3. **架构优化**：
   - [ ] 负载均衡
   - [ ] 读写分离
   - [ ] 服务拆分

---

## 📁 测试文件结构

```
scripts/performance/
├── jmeter-test-plan.md          # 本文档
├── postgresql-slow-query-config.sql  # 慢查询配置
├── jmx/                         # JMeter 测试计划文件
│   ├── login-test.jmx           # 登录测试
│   ├── matter-list-test.jmx     # 项目列表测试
│   ├── matter-create-test.jmx   # 创建项目测试
│   ├── file-upload-test.jmx     # 文件上传测试
│   └── mixed-scenario-test.jmx  # 混合场景测试
├── data/                        # 测试数据
│   ├── users.csv                # 测试用户
│   └── test-file-10mb.pdf       # 测试文件
└── reports/                     # 测试报告
    └── .gitkeep
```

---

## ⚠️ 注意事项

1. **测试环境隔离**：不要在生产环境执行压力测试
2. **数据清理**：测试后清理测试数据
3. **监控观察**：测试期间观察服务器资源使用情况
4. **逐步加压**：从低并发开始，逐步增加
5. **多次执行**：每个场景至少执行 3 次，取平均值

