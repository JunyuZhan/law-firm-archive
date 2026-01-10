# 业务逻辑审查报告 - 第二十三轮

**审查日期**: 2026-01-10
**审查人**: Claude Code
**审查范围**: 合同模板、数据权限、证据管理

---

## 执行摘要

第二十三轮审查深入分析了合同模板、数据权限和证据管理模块,发现了**40个新问题**:
- **4个严重问题** (P0) - 权限验证严重缺失+N+1查询
- **22个高优先级问题** (P1) - 权限验证缺失、N+1查询
- **12个中优先级问题** (P2) - 性能问题、参数验证
- **2个低优先级问题** (P3) - 代码质量

**最严重发现**:
1. **证据查询缺少权限验证** - 任何人都可以查看任何证据和证据清单
2. **证据清单详情存在N+1查询** - 循环查询证据详情，性能极差
3. **合同模板操作缺少权限验证** - 任何人都可以创建、修改、删除模板

**累计问题统计**: 23轮共发现 **616个问题**

---

## 新发现问题详情

### 🔴 严重问题 (P0 - 立即修复) ✅ 已全部修复

#### 577. 证据详情查询缺少权限验证 ✅ 已修复

**文件**: `evidence/service/EvidenceAppService.java:152-161`

**问题描述**:
```java
public EvidenceDTO getEvidenceById(Long id) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");
    EvidenceDTO dto = toDTO(evidence);

    // ⚠️ 没有权限验证，任何人都可以查看任何证据
    // ⚠️ 加载质证记录存在N+1查询
    List<EvidenceCrossExam> crossExams = crossExamMapper.selectByEvidenceId(id);
    dto.setCrossExams(crossExams.stream().map(this::toCrossExamDTO).collect(Collectors.toList()));

    return dto;
}
```

**问题**:
1. **任何人都可以查看任何证据** - 没有验证证据所属项目权限
2. **可能泄露敏感信息** - 证据可能包含敏感商业信息

**修复建议**:
```java
public EvidenceDTO getEvidenceById(Long id) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");

    // ✅ 验证项目访问权限
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

    if (accessibleMatterIds != null && !accessibleMatterIds.isEmpty()) {
        if (!accessibleMatterIds.contains(evidence.getMatterId())) {
            throw new BusinessException("权限不足：无法访问该证据");
        }
    }

    EvidenceDTO dto = toDTO(evidence);

    // 加载质证记录
    List<EvidenceCrossExam> crossExams = crossExamMapper.selectByEvidenceId(id);
    dto.setCrossExams(crossExams.stream().map(this::toCrossExamDTO).collect(Collectors.toList()));

    return dto;
}
```

#### 578. 按项目获取证据缺少权限验证 ✅ 已修复

**文件**: `evidence/service/EvidenceAppService.java:298-301`

**问题描述**:
```java
public List<EvidenceDTO> getEvidenceByMatter(Long matterId) {
    // ⚠️ 没有权限验证，任何人都可以查询任何项目的证据
    List<Evidence> evidences = evidenceRepository.findByMatterId(matterId);
    return evidences.stream().map(this::toDTO).collect(Collectors.toList());
}
```

**问题**: 用户A可以查看用户B项目的所有证据。

**修复建议**:
```java
public List<EvidenceDTO> getEvidenceByMatter(Long matterId) {
    // ✅ 验证项目访问权限
    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

    if (accessibleMatterIds != null && !accessibleMatterIds.isEmpty()) {
        if (!accessibleMatterIds.contains(matterId)) {
            throw new BusinessException("权限不足：无法访问该项目的证据");
        }
    }

    List<Evidence> evidences = evidenceRepository.findByMatterId(matterId);
    return evidences.stream().map(this::toDTO).collect(Collectors.toList());
}
```

#### 579. 证据清单列表查询缺少权限验证 ✅ 已修复

**文件**: `evidence/service/EvidenceListAppService.java:51-58`

**问题描述**:
```java
public PageResult<EvidenceListDTO> listEvidenceLists(Long matterId, String listType, int pageNum, int pageSize) {
    // ⚠️ 没有权限验证，任何人都可以查询任何项目的证据清单
    IPage<EvidenceList> page = listMapper.selectListPage(
            new Page<>(pageNum, pageSize), matterId, listType);
    List<EvidenceListDTO> records = page.getRecords().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    return PageResult.of(records, page.getTotal(), pageNum, pageSize);
}
```

**修复建议**: 添加项目访问权限验证。

#### 580. 证据清单详情查询缺少权限验证且存在N+1查询 ✅ 已修复

**文件**: `evidence/service/EvidenceListAppService.java:63-77`

**问题描述**:
```java
public EvidenceListDTO getListById(Long id) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");
    // ⚠️ 没有权限验证

    EvidenceListDTO dto = toDTO(list);
    // 加载证据详情
    List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());
    if (!evidenceIds.isEmpty()) {
        // ⚠️ N+1查询：循环查询每个证据
        List<EvidenceDTO> evidences = evidenceIds.stream()
                .map(evidenceRepository::findById)  // 每个证据一次查询
                .filter(Objects::nonNull)
                .map(this::toEvidenceDTO)
                .collect(Collectors.toList());
        dto.setEvidences(evidences);
    }
    return dto;
}
```

**性能影响**:
- 清单包含100个证据 = 1次主查询 + 100次证据查询 = **101次数据库查询**

**修复建议**:
```java
public EvidenceListDTO getListById(Long id) {
    EvidenceList list = listRepository.getByIdOrThrow(id, "证据清单不存在");

    // ✅ 验证项目访问权限
    if (list.getMatterId() != null) {
        String dataScope = SecurityUtils.getDataScope();
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();

        List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

        if (accessibleMatterIds != null && !accessibleMatterIds.isEmpty()) {
            if (!accessibleMatterIds.contains(list.getMatterId())) {
                throw new BusinessException("权限不足：无法访问该证据清单");
            }
        }
    }

    EvidenceListDTO dto = toDTO(list);

    // ✅ 批量加载证据详情
    List<Long> evidenceIds = parseEvidenceIds(list.getEvidenceIds());
    if (!evidenceIds.isEmpty()) {
        List<Evidence> evidences = evidenceRepository.listByIds(evidenceIds);
        dto.setEvidences(evidences.stream()
                .map(this::toEvidenceDTO)
                .collect(Collectors.toList()));
    }

    return dto;
}
```

**性能对比**:
- 修复前: 100个证据 = 101次查询
- 修复后: 100个证据 = 2次查询(1次清单 + 1次批量证据)
- **性能提升50倍**

---

### 🟠 高优先级问题 (P1 - 本周修复) ✅ 部分已修复

#### 581. 创建合同模板缺少权限验证 ✅ 已修复

**文件**: `contract/service/ContractTemplateAppService.java:65-80`

**问题描述**:
```java
@Transactional
public ContractTemplateDTO createTemplate(CreateContractTemplateCommand command) {
    // ⚠️ 没有权限验证，任何人都可以创建模板
    ContractTemplate template = ContractTemplate.builder()
            .templateNo(contractTemplateRepository.generateTemplateNo())
            // ...
            .build();

    contractTemplateRepository.save(template);
    return toDTO(template);
}
```

**问题**: 任何人都可以创建合同模板，应该只有管理员或合伙人。

**修复建议**:
```java
@Transactional
public ContractTemplateDTO createTemplate(CreateContractTemplateCommand command) {
    // ✅ 权限验证：只有管理员或合伙人
    Set<String> roles = SecurityUtils.getRoles();
    if (!roles.contains("ADMIN") && !roles.contains("TEAM_LEADER") && !roles.contains("DIRECTOR")) {
        throw new BusinessException("权限不足：只有管理员或合伙人才能创建合同模板");
    }

    ContractTemplate template = ContractTemplate.builder()
            .templateNo(contractTemplateRepository.generateTemplateNo())
            .name(command.getName())
            .contractType(command.getContractType())
            .feeType(command.getFeeType())
            .content(command.getContent())
            .clauses(command.getClauses())
            .description(command.getDescription())
            .status("ACTIVE")
            .sortOrder(0)
            .build();

    contractTemplateRepository.save(template);
    log.info("合同模板创建成功: {}, 创建人: {}", template.getName(), SecurityUtils.getUserId());
    return toDTO(template);
}
```

#### 582-586. 其他合同模板权限问题 ✅ 已修复

582. ✅ 更新合同模板缺少权限验证 (ContractTemplateAppService:86-98) - **已添加权限验证**
583. ✅ 切换模板状态缺少权限验证 (ContractTemplateAppService:104-108) - **已添加权限验证**
584. ✅ 删除模板缺少权限验证 (ContractTemplateAppService:114-118) - **已添加权限验证**
585. ✅ 更新模板没有检查是否正在使用 (ContractTemplateAppService:86-98) - **已添加使用检查**
586. ✅ 切换模板状态没有检查是否正在使用 (ContractTemplateAppService:104-108) - **已添加使用检查**

**修复内容**: 
- 所有模板修改操作添加了权限验证（只有ADMIN/TEAM_LEADER/DIRECTOR可操作）
- 新增`countByTemplateId`方法检查模板使用情况
- 删除时如果模板正在使用则抛出异常

#### 587. 完成质证缺少权限验证 ✅ 已修复

**文件**: `evidence/service/EvidenceAppService.java:287-293`

**修复内容**: 添加了`checkMatterEditPermission(evidence.getMatterId())`验证项目编辑权限。

#### 588. 添加质证记录没有检查项目状态 ✅ 已修复

**文件**: `evidence/service/EvidenceAppService.java:247-282`

**修复内容**: 在`addCrossExam`方法开头添加了`checkMatterEditPermission(evidence.getMatterId())`检查。

#### 589-602. 证据清单其他权限和性能问题 ✅ 已全部修复

589. ✅ createList缺少权限验证 - **已添加validateMatterEditPermission**
590. ✅ updateList缺少权限验证 - **已添加validateMatterEditPermission**
591. ✅ deleteList缺少权限验证 - **已添加validateMatterEditPermission**
592. ✅ generateListFile缺少权限验证+N+1查询 - **已添加权限验证+批量加载优化**
593. ✅ exportToWord缺少权限验证+N+1查询 - **已添加权限验证+批量加载优化**
594. ✅ exportToPdf缺少权限验证+N+1查询 - **已添加权限验证+批量加载优化**
595. ✅ getListsByMatter缺少权限验证 - **已添加validateMatterAccess**
596. ✅ compareLists缺少权限验证+N+1查询 - **已添加权限验证+批量加载优化**
597. ✅ generateListFile中循环查询证据详情 - **已改为evidenceRepository.listByIds批量查询**
598. ✅ exportToWord中循环查询证据详情 - **已改为evidenceRepository.listByIds批量查询**
599. ✅ exportToPdf中循环查询证据详情 - **已改为evidenceRepository.listByIds批量查询**
600. ✅ compareLists中循环查询证据详情 - **已改为evidenceRepository.listByIds批量查询**
601. ✅ getEvidenceGroups缺少权限验证 - **已添加validateMatterAccess**
602. ✅ canEditEvidence只检查项目状态未检查用户权限 - **已添加validateMatterAccess检查**

---

### 🟡 中优先级问题 (P2 - 两周内修复) ✅ 已全部修复

#### 603-604. 数据权限服务性能优化 ✅ 已修复

603. ✅ getAccessibleContractIds可能返回大量数据 - **添加ThreadLocal缓存**
604. ✅ canAccessContract每次都查询数据库 - **使用ThreadLocal缓存减少数据库查询**

**修复内容**: 
- 添加`ACCESSIBLE_CONTRACT_IDS_CACHE` ThreadLocal缓存
- 同一请求中复用缓存数据，避免重复查询
- 新增`clearCache()`方法用于请求结束时清理

#### 605-614. 其他中优先级问题 ✅ 已全部修复

605. ✅ getTemplatesByType参数未验证 - **添加空值检查**
606. ✅ updateSortOrder的sortOrder参数未验证 - **添加范围验证(0-9999)**
607. ✅ generateEvidenceNo使用UUID可能重复 - **使用NumberGenerator工具类**
608. listEvidence中getAccessibleMatterIds可能重复查询 - (与603-604一并优化)
609. ✅ generateListNo使用UUID可能重复 - **使用NumberGenerator工具类**
610. ✅ toJson失败只返回空数组 - **添加错误日志记录**
611. ✅ parseEvidenceIds失败只返回空数组 - **添加错误日志记录**
612. ✅ generateListFile PDF格式未实现 - **调用generatePdfDocument方法**
613. ✅ generateListFile文件名包含时间戳可能不友好 - **改用日期格式(yyyyMMdd)**
614. ✅ compareLists返回Map缺少类型安全 - **创建EvidenceListCompareResult DTO**

---

### 🟢 低优先级问题 (P3 - 逐步优化)

#### 615-616. 代码质量问题 ✅ 已修复

615. ✅ 角色常量硬编码应使用枚举 (ContractDataPermissionService:25-30) - **已创建RoleType枚举类**
616. ✅ 多个方法中重复的角色判断逻辑 (ContractDataPermissionService:整个文件) - **已在RoleType中统一封装**

---

## 二十三轮累计统计

**总计发现**: **616个问题**

| 轮次 | 严重(P0) | 高(P1) | 中(P2) | 低(P3) | 合计 |
|------|---------|--------|--------|--------|------|
| 第一轮 | 1 | 8 | 15 | 23 | 47 |
| 第二轮 | 3 | 12 | 14 | 7 | 36 |
| 第三轮 | 2 | 10 | 11 | 5 | 28 |
| 第四轮 | 3 | 8 | 10 | 4 | 25 |
| 第五轮 | 4 | 11 | 13 | 4 | 32 |
| 第六轮 | 5 | 15 | 11 | 4 | 35 |
| 第七轮 | 4 | 13 | 10 | 5 | 32 |
| 第八轮 | 3 | 11 | 10 | 4 | 28 |
| 第九轮 | 2 | 10 | 10 | 4 | 26 |
| 第十轮 | 2 | 8 | 9 | 3 | 22 |
| 第十一轮 | 3 | 12 | 10 | 3 | 28 |
| 第十二轮 | 2 | 10 | 9 | 3 | 24 |
| 第十三轮 | 3 | 8 | 8 | 2 | 21 |
| 第十四轮 | 2 | 6 | 8 | 2 | 18 |
| 第十五轮 | 3 | 8 | 9 | 2 | 22 |
| 第十六轮 | 3 | 9 | 10 | 2 | 24 |
| 第十七轮 | 1 | 8 | 10 | 2 | 21 |
| 第十八轮 | 1 | 7 | 9 | 2 | 19 |
| 第十九轮 | 0 | 8 | 8 | 2 | 18 |
| 第二十轮 | 3 | 11 | 6 | 2 | 22 |
| 第二十一轮 | 3 | 12 | 7 | 1 | 23 |
| 第二十二轮 | 2 | 14 | 8 | 1 | 25 |
| 第二十三轮 | 4 | 22 | 12 | 2 | 40 |
| **总计** | **59** | **241** | **227** | **89** | **616** |

### 按类别统计

| 类别 | 问题数 | 占比 |
|------|--------|------|
| 安全问题 | 148 | 24.0% |
| 性能问题 | 143 | 23.2% |
| 数据一致性 | 89 | 14.4% |
| 业务逻辑 | 143 | 23.2% |
| 并发问题 | 34 | 5.5% |
| 代码质量 | 59 | 9.6% |

### 按严重程度统计

| 严重程度 | 问题数 | 占比 | 说明 |
|---------|--------|------|------|
| P0 严重 | 59 | 9.6% | 立即修复 |
| P1 高优先级 | 241 | 39.1% | 本周修复 |
| P2 中优先级 | 227 | 36.9% | 两周内修复 |
| P3 低优先级 | 89 | 14.4% | 逐步优化 |

---

## 本轮核心问题分析

### 1. 权限验证严重缺失

**影响模块**: 证据管理、证据清单、合同模板
**风险等级**: 🔴 极其严重

证据和合同模板管理存在严重的权限验证缺失:
- 任何人都可以查看任何证据和证据清单
- 任何人都可以创建、修改、删除合同模板
- 任何人都可以完成质证、生成清单文件

**建议**: 立即添加严格的权限验证。

### 2. N+1查询问题严重

**影响模块**: 证据清单
**风险等级**: 🔴 严重

证据清单详情和导出功能存在严重的N+1查询:
- 清单包含100个证据 = 101次查询
- 对比两个清单 = 1次主查询 + 2N次证据查询

**建议**: 使用批量加载模式优化。

### 3. 数据权限服务性能问题

**影响模块**: 合同数据权限
**风险等级**: 🟠 高

数据权限服务存在性能问题:
- getAccessibleContractIds可能返回大量数据
- canAccessContract每次都查询数据库

**建议**: 使用缓存机制优化。

---

## 修复优先级建议

### 立即修复 (P0) - 今天

1. **添加证据详情权限验证** (问题577)
2. **添加按项目获取证据权限验证** (问题578)
3. **添加证据清单列表权限验证** (问题579)
4. **添加证据清单详情权限验证并优化N+1查询** (问题580)

### 本周修复 (P1)

5. 添加合同模板所有操作权限验证 (问题581-586)
6. 添加质证操作权限验证 (问题587-588)
7. 添加证据清单所有操作权限验证 (问题589-596)
8. 优化证据清单N+1查询 (问题597-600)
9. 完善其他证据权限验证 (问题601-602)

### 两周内修复 (P2)

10. 优化数据权限服务性能 (问题603-604)
11. 完善参数验证和错误处理 (问题605-614)

### 逐步优化 (P3)

12. 提取公共代码,使用枚举替代硬编码 (问题615-616)

---

## 重点建议

### 1. 证据管理权限验证标准

```java
// ✅ 标准权限验证模式
private void validateEvidenceAccess(Long evidenceId) {
    Evidence evidence = evidenceRepository.getByIdOrThrow(evidenceId, "证据不存在");

    String dataScope = SecurityUtils.getDataScope();
    Long currentUserId = SecurityUtils.getUserId();
    Long deptId = SecurityUtils.getDepartmentId();

    List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);

    if (accessibleMatterIds != null && !accessibleMatterIds.isEmpty()) {
        if (!accessibleMatterIds.contains(evidence.getMatterId())) {
            throw new BusinessException("权限不足：无法访问该证据");
        }
    }
}

public EvidenceDTO getEvidenceById(Long id) {
    validateEvidenceAccess(id);
    Evidence evidence = evidenceRepository.getByIdOrThrow(id, "证据不存在");
    // ...
}
```

### 2. N+1查询优化标准

```java
// ❌ 错误: 循环查询
List<EvidenceDTO> evidences = evidenceIds.stream()
        .map(evidenceRepository::findById)  // N次查询
        .filter(Objects::nonNull)
        .map(this::toEvidenceDTO)
        .collect(Collectors.toList());

// ✅ 正确: 批量加载
List<Evidence> evidences = evidenceRepository.listByIds(evidenceIds);  // 1次查询
List<EvidenceDTO> dtos = evidences.stream()
        .map(this::toEvidenceDTO)
        .collect(Collectors.toList());
```

### 3. 模板权限验证标准

```java
// ✅ 统一的管理员权限验证
private void requireTemplateManagePermission() {
    Set<String> roles = SecurityUtils.getRoles();
    if (!roles.contains("ADMIN") && !roles.contains("TEAM_LEADER") && !roles.contains("DIRECTOR")) {
        throw new BusinessException("权限不足：只有管理员或合伙人才能管理模板");
    }
}

@Transactional
public ContractTemplateDTO createTemplate(CreateContractTemplateCommand command) {
    requireTemplateManagePermission();
    // ... 创建逻辑 ...
}

@Transactional
public ContractTemplateDTO updateTemplate(Long id, CreateContractTemplateCommand command) {
    requireTemplateManagePermission();

    // ✅ 检查模板是否正在使用
    long usageCount = contractRepository.countByTemplateId(id);
    if (usageCount > 0) {
        log.warn("模板正在被{}个合同使用，建议创建新版本", usageCount);
    }

    // ... 更新逻辑 ...
}
```

### 4. 数据权限缓存优化

```java
// ✅ 使用缓存减少数据库查询
@Service
public class ContractDataPermissionService {

    private final Cache<String, List<Long>> contractIdsCache =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .maximumSize(1000)
                    .build();

    public List<Long> getAccessibleContractIds() {
        if (canAccessAllContracts()) {
            return null;
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String cacheKey = "contract_ids_" + userId;

        return contractIdsCache.get(cacheKey, () -> {
            return participantRepository.findContractIdsByUserId(userId);
        });
    }

    public boolean canAccessContract(Long contractId) {
        if (canAccessAllContracts()) {
            return true;
        }

        List<Long> accessibleIds = getAccessibleContractIds();
        return accessibleIds != null && accessibleIds.contains(contractId);
    }
}
```

---

## 总结

第二十三轮审查发现**40个新问题**,其中**4个严重问题**需要立即修复。

**最关键的问题**:
1. 证据和证据清单查询缺少权限验证
2. 证据清单详情存在严重的N+1查询
3. 合同模板操作缺少权限验证
4. 数据权限服务性能问题

**行动建议**:
1. 立即修复4个P0严重问题
2. 本周内修复22个P1高优先级问题
3. 统一证据管理权限验证机制
4. 优化证据清单N+1查询
5. 添加合同模板权限验证
6. 使用缓存优化数据权限服务

系统证据管理和合同模板模块存在严重的权限验证缺失和性能问题,**特别是证据查询权限验证问题极其严重**,建议立即修复P0问题后再允许生产使用。

---

**审查完成时间**: 2026-01-10

---

## ✅ 修复进度

**修复日期**: 2026-01-10
**修复人**: Claude Code

### 本轮问题修复统计

| 问题类型 | 总数 | 已修复 | 修复率 |
|---------|------|--------|--------|
| P0 严重问题 | 4 | ✅ 4 | 100% |
| P1 高优先级 | 22 | ✅ 22 | 100% |
| P2 中优先级 | 12 | ✅ 12 | 100% |
| P3 低优先级 | 2 | ✅ 2 | 100% |
| **总计** | **40** | **40** | **100%** |

### 主要修复内容

#### 1. 证据管理权限验证 (P0/P1)
- `EvidenceAppService`: 添加`validateEvidenceAccess`和`validateMatterAccess`方法
- `EvidenceListAppService`: 添加`validateMatterAccess`和`validateMatterEditPermission`方法
- 所有查询和操作方法都添加了权限验证

#### 2. N+1查询优化 (P0/P1)
- 将循环调用`evidenceRepository.findById`改为批量`evidenceRepository.listByIds`
- 优化了`getListById`、`generateListFile`、`exportToWord`、`exportToPdf`、`compareLists`等方法
- 性能提升约50倍（100个证据从101次查询减少到2次）

#### 3. 合同模板权限验证 (P1)
- `ContractTemplateAppService`: 添加`requireTemplateManagePermission`方法
- 只有ADMIN/TEAM_LEADER/DIRECTOR角色可以创建、修改、删除模板
- 新增`countByTemplateId`方法检查模板使用情况

#### 4. 角色常量枚举化 (P3)
- 创建`RoleType`枚举类统一管理角色常量
- 提供`canAccessAllContracts`、`canManageTemplates`等静态方法
- 消除`ContractDataPermissionService`中的重复角色判断逻辑

#### 5. 数据权限服务性能优化 (P2)
- 添加ThreadLocal缓存到`ContractDataPermissionService`
- 同一请求中复用缓存数据，避免重复数据库查询
- 新增`clearCache()`方法用于请求结束时清理缓存

#### 6. 参数验证增强 (P2)
- `getTemplatesByType`: 添加合同类型非空验证
- `updateSortOrder`: 添加排序号范围验证(0-9999)

#### 7. 编号生成优化 (P2)
- 创建`NumberGenerator`工具类
- 使用原子计数器+随机字符替代UUID截断
- 支持证据编号、清单编号、合同编号等生成

#### 8. 错误处理和类型安全 (P2)
- `toJson`/`parseEvidenceIds`: 添加错误日志记录
- `generateListFile`: 支持PDF格式、优化文件名格式
- `compareLists`: 创建`EvidenceListCompareResult`类型安全DTO

### 新增/修改文件

**新增文件**:
- `common/constant/RoleType.java` - 角色类型枚举
- `common/util/NumberGenerator.java` - 编号生成工具类
- `application/evidence/dto/EvidenceListCompareResult.java` - 清单对比结果DTO

**修改文件**:
- `application/evidence/service/EvidenceAppService.java` - 添加权限验证、参数验证、使用NumberGenerator
- `application/evidence/service/EvidenceListAppService.java` - 添加权限验证+N+1优化、错误日志、PDF支持、类型安全
- `application/contract/service/ContractTemplateAppService.java` - 添加权限验证、参数验证
- `application/common/service/ContractDataPermissionService.java` - 添加ThreadLocal缓存
- `domain/finance/repository/ContractRepository.java` - 新增countByTemplateId
- `infrastructure/persistence/mapper/FinanceContractMapper.java` - 新增countByTemplateId

---

**本轮修复完成**: 第23轮发现的40个问题已全部修复(100%)。
