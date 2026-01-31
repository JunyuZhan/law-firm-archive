# 开发指南

本文档面向开发人员，介绍前端组件使用和后端工具类使用规范。

---

## 📚 前端组件使用指南

### 概述

本项目基于 [vue-vben-admin](https://github.com/vbenjs/vue-vben-admin) 框架，使用以下技术栈：

- **Vue 3** + **TypeScript**
- **Vite** 构建工具
- **Ant Design Vue** UI 组件库
- **Shadcn UI** 基础组件（vben-core）
- **vxe-table** 高级表格

### 组件优先级原则

1. **优先使用** vben 封装的组件（`@vben/common-ui`、`useVbenForm`、`useVbenVxeGrid`）
2. **其次使用** ant-design-vue 组件（保持与现有代码一致）
3. **避免** 直接使用原生 HTML 元素实现复杂交互

---

### vben 封装组件（推荐）

| 组件 | 导入方式 | 用途 |
|------|----------|------|
| `Page` | `import { Page } from '@vben/common-ui'` | 页面容器，统一标题和描述 |
| `useVbenForm` | `import { useVbenForm } from '#/adapter/form'` | 表单（支持 schema 配置） |
| `useVbenVxeGrid` | `import { useVbenVxeGrid } from '#/adapter/vxe-table'` | 高级表格（支持分页、筛选） |
| `useVbenModal` | `import { useVbenModal } from '@vben/common-ui'` | 弹窗 |
| `useVbenDrawer` | `import { useVbenDrawer } from '@vben/common-ui'` | 抽屉 |
| `ColPage` | `import { ColPage } from '@vben/common-ui'` | 分栏页面 |
| `EllipsisText` | `import { EllipsisText } from '@vben/common-ui'` | 文本省略 |
| `JsonViewer` | `import { JsonViewer } from '@vben/common-ui'` | JSON 查看器 |
| `IconPicker` | `import { IconPicker } from '@vben/common-ui'` | 图标选择器 |

### ant-design-vue 组件（现有风格）

```typescript
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  Modal,
  Textarea,
  Tag,
  Tabs,
  Tooltip,
  Alert,
  Popconfirm,
  Descriptions,
  DescriptionsItem,
  Row,
  Col,
  // ... 其他组件
} from 'ant-design-vue';
```

### 图标使用

```typescript
// vben 图标（推荐）
import { Plus, Copy, Eye } from '@vben/icons';

// 使用方式
<Plus class="size-4" />
```

---

### 推荐组件用法

#### 页面容器 - Page

**必须使用**，所有页面都应该用 `Page` 组件包裹。

```vue
<script setup lang="ts">
import { Page } from '@vben/common-ui';
</script>

<template>
  <Page title="页面标题" description="页面描述">
    <!-- 页面内容 -->
  </Page>
</template>
```

#### 表单 - useVbenForm

推荐使用 `useVbenForm` 进行表单管理，支持 schema 配置。

```vue
<script setup lang="ts">
import { useVbenForm } from '#/adapter/form';

const { formRef, formSchema, handleSubmit } = useVbenForm({
  schema: [
    {
      fieldName: 'name',
      label: '名称',
      component: 'Input',
      required: true,
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: {
        options: [
          { label: '启用', value: '1' },
          { label: '禁用', value: '0' },
        ],
      },
    },
  ],
  submitApi: async (values) => {
    return await api.create(values);
  },
});
</script>

<template>
  <Page title="新增">
    <VbenForm ref="formRef" :schema="formSchema" @submit="handleSubmit" />
  </Page>
</template>
```

#### 表格 - useVbenVxeGrid

推荐使用 `useVbenVxeGrid` 进行表格管理，支持分页、筛选、排序。

```vue
<script setup lang="ts">
import { useVbenVxeGrid } from '#/adapter/vxe-table';

const { gridRef, gridOptions, reload } = useVbenVxeGrid({
  columns: [
    { field: 'name', title: '名称' },
    { field: 'status', title: '状态' },
  ],
  api: {
    list: api.getList,
  },
});
</script>

<template>
  <Page title="列表">
    <VbenVxeGrid ref="gridRef" v-bind="gridOptions" />
  </Page>
</template>
```

---

### 代码规范

1. **统一使用 TypeScript**：所有 `.vue` 文件使用 `<script setup lang="ts">`
2. **组件命名**：使用 PascalCase，如 `UserManagement.vue`
3. **文件命名**：使用 kebab-case，如 `user-management.vue`
4. **导入顺序**：先导入第三方库，再导入项目内部模块
5. **样式**：优先使用 Tailwind CSS，必要时使用 scoped CSS

---

## 🔧 后端工具类使用指南

### 工具清单速查

| 工具 | 用途 | 使用场景 | 是否必须使用 |
|------|------|----------|-------------|
| `FileValidator` | 文件上传验证 | 所有文件上传接口 | ✅ 必须 |
| `IpUtils` | 获取真实IP | 登录、操作日志 | ✅ 必须 |
| `XssFilter` | XSS过滤 | 自动生效（Filter） | ✅ 自动 |
| `TraceIdFilter` | 请求追踪 | 自动生效（Filter） | ✅ 自动 |
| `DeviceFingerprintUtils` | 设备识别 | 登录日志 | 🟡 推荐 |
| `FieldChangeUtils` | 字段变更对比 | 审计日志 | 🟡 推荐 |
| `Assert` | 参数校验 | Service层入参 | 🟡 推荐 |
| `SensitiveUtils` | 数据脱敏 | 日志输出、API返回 | ✅ 必须 |
| `@RateLimiter` | 接口限流 | 敏感接口 | 🟡 推荐 |
| `@RepeatSubmit` | 防重复提交 | 表单提交接口 | 🟡 推荐 |
| `CompressUtils` | 文件压缩 | 批量下载打包 | 🟡 推荐 |

---

### FileValidator - 文件上传验证

**为什么要用？**
防止恶意文件上传，包括：
- 可执行文件（.exe, .sh, .php）
- 伪装文件（把 .exe 改成 .jpg）
- 超大文件攻击

**在哪里使用？**
**所有文件上传接口**都必须调用！

**使用示例**：

```java
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.common.util.FileValidator.ValidationResult;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @PostMapping("/upload")
    public Result<FileDTO> upload(@RequestParam MultipartFile file) {
        // ⚠️ 必须：在处理文件前先验证
        ValidationResult result = FileValidator.validate(file);
        if (!result.isValid()) {
            throw new BusinessException(result.getErrorMessage());
        }
        
        // 验证通过，继续处理上传...
        return fileService.upload(file);
    }
}
```

---

### IpUtils - IP地址工具

**为什么要用？**
- `request.getRemoteAddr()` 在有代理时获取的是代理IP
- 需要正确获取用户真实IP用于：登录日志、操作审计、限流

**使用示例**：

```java
import com.lawfirm.common.util.IpUtils;

@Service
public class LoginService {

    public LoginResult login(LoginCommand cmd, HttpServletRequest request) {
        // ⚠️ 必须：使用 IpUtils 获取真实IP
        String clientIp = IpUtils.getClientIp(request);
        
        // 记录登录日志
        loginLogService.recordLogin(cmd.getUsername(), clientIp);
        
        // ... 登录逻辑
    }
}
```

---

### SensitiveUtils - 数据脱敏

**为什么要用？**
防止敏感信息泄露到日志或API返回中，包括：
- 手机号、身份证号、银行卡号
- 密码、Token

**使用示例**：

```java
import com.lawfirm.common.util.SensitiveUtils;

@Service
public class UserService {

    public UserDTO getUser(Long id) {
        User user = userRepository.findById(id);
        UserDTO dto = convertToDTO(user);
        
        // ⚠️ 必须：脱敏敏感信息
        dto.setPhone(SensitiveUtils.maskPhone(user.getPhone()));
        dto.setIdCard(SensitiveUtils.maskIdCard(user.getIdCard()));
        
        return dto;
    }
    
    public void logUserInfo(User user) {
        // ⚠️ 必须：日志中也要脱敏
        log.info("用户信息: phone={}, idCard={}", 
            SensitiveUtils.maskPhone(user.getPhone()),
            SensitiveUtils.maskIdCard(user.getIdCard()));
    }
}
```

---

### Assert - 参数校验

**为什么要用？**
在 Service 层进行参数校验，避免空指针异常和业务逻辑错误。

**使用示例**：

```java
import com.lawfirm.common.util.Assert;

@Service
public class ContractService {

    public void createContract(ContractCommand cmd) {
        // ⚠️ 推荐：使用 Assert 进行参数校验
        Assert.notNull(cmd, "合同信息不能为空");
        Assert.notNull(cmd.getClientId(), "客户ID不能为空");
        Assert.notBlank(cmd.getTitle(), "合同标题不能为空");
        
        // ... 业务逻辑
    }
}
```

---

### @RateLimiter - 接口限流

**为什么要用？**
防止接口被恶意调用，保护系统资源。

**使用示例**：

```java
import com.lawfirm.common.annotation.RateLimiter;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    @RateLimiter(key = "login", count = 5, time = 60) // 60秒内最多5次
    public Result<LoginResult> login(@RequestBody LoginCommand cmd) {
        // ... 登录逻辑
    }
}
```

---

### @RepeatSubmit - 防重复提交

**为什么要用？**
防止用户重复点击提交按钮，导致重复操作。

**使用示例**：

```java
import com.lawfirm.common.annotation.RepeatSubmit;

@RestController
@RequestMapping("/api/contract")
public class ContractController {

    @PostMapping("/submit")
    @RepeatSubmit(interval = 3000) // 3秒内不允许重复提交
    public Result<Void> submit(@RequestBody ContractCommand cmd) {
        // ... 提交逻辑
    }
}
```

---

### FieldChangeUtils - 字段变更对比

**为什么要用？**
记录字段变更历史，用于审计日志。

**使用示例**：

```java
import com.lawfirm.common.util.FieldChangeUtils;

@Service
public class UserService {

    public void updateUser(UserUpdateCommand cmd) {
        User oldUser = userRepository.findById(cmd.getId());
        User newUser = convertToEntity(cmd);
        
        // ⚠️ 推荐：记录字段变更
        Map<String, FieldChangeUtils.FieldChange> changes = 
            FieldChangeUtils.compare(oldUser, newUser);
        
        if (!changes.isEmpty()) {
            auditLogService.recordFieldChange("User", cmd.getId(), changes);
        }
        
        userRepository.save(newUser);
    }
}
```

---

### DeviceFingerprintUtils - 设备识别

**为什么要用？**
识别用户设备，用于登录日志和安全审计。

**使用示例**：

```java
import com.lawfirm.common.util.DeviceFingerprintUtils;

@Service
public class LoginService {

    public LoginResult login(LoginCommand cmd, HttpServletRequest request) {
        // ⚠️ 推荐：记录设备指纹
        String deviceFingerprint = DeviceFingerprintUtils.generate(request);
        
        // 记录登录日志
        loginLogService.recordLogin(
            cmd.getUsername(), 
            IpUtils.getClientIp(request),
            deviceFingerprint
        );
        
        // ... 登录逻辑
    }
}
```

---

### CompressUtils - 文件压缩

**为什么要用？**
批量下载时打包多个文件，提高下载效率。

**使用示例**：

```java
import com.lawfirm.common.util.CompressUtils;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @GetMapping("/batch-download")
    public void batchDownload(@RequestParam List<Long> fileIds, 
                              HttpServletResponse response) {
        // ⚠️ 推荐：使用 CompressUtils 打包
        List<File> files = fileService.getFiles(fileIds);
        
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=files.zip");
        
        CompressUtils.compressToZip(files, response.getOutputStream());
    }
}
```

---

## 📚 相关文档

- [API 接口文档](./introduction.md)
- [后端实现指南](../../../docs/BACKEND_IMPLEMENTATION_GUIDE.md)
- [版本管理](../../../docs/VERSION_MANAGEMENT.md)

---

**最后更新**: 2026-01-31
