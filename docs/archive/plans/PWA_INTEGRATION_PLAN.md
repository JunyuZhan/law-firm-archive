# PWA 集成方案

> 智慧律所管理系统 Progressive Web App 集成实施方案

## 一、PWA 简介

### 什么是 PWA

**PWA (Progressive Web App)** 是一种渐进式 Web 应用，它结合了传统 Web 应用和原生应用的优点。

| 特性 | 说明 |
|------|------|
| **离线可用** | 通过 Service Worker 缓存资源，断网也能访问 |
| **可安装** | 可以添加到手机/电脑主屏幕，像原生 App 一样启动 |
| **推送通知** | 支持消息推送（可选） |
| **响应式** | 适配各种设备屏幕 |
| **安全** | 必须通过 HTTPS 访问 |
| **快速启动** | 二次打开速度接近原生应用 |

### 为什么律所系统需要 PWA

| 场景 | 痛点 | PWA 解决方案 |
|------|------|-------------|
| **律师外出办案** | 法院、看守所等地网络信号差，无法查看案件资料 | 离线缓存案件、客户信息 |
| **突发情况** | 庭上需要快速查阅资料，刷新半天加载不出 | Service Worker 缓存静态资源 |
| **移动办公** | 客户突然来电问进度，掏手机就能快速查看 | 添加到主屏幕，像 App 一样启动 |
| **文档预览** | 大文件 PDF 每次都重新下载，浪费时间 | 缓存策略加速二次访问 |

---

## 二、项目现状分析

### 当前 PWA 配置状态

项目使用 **Vite + Vue 3** 技术栈，已内置 `vite-plugin-pwa` 插件支持：

```49:50:frontend/internal/vite-config/src/config/application.ts
      pwa: true,
      pwaOptions: getDefaultPwaOptions(appTitle),
```

当前插件配置（`frontend/internal/vite-config/src/plugins/index.ts:164-167`）：
- `injectRegister: false` - Service Worker 未自动注册
- `globPatterns: []` - 不缓存任何文件

### 存在的问题

1. **缓存策略未配置** - `globPatterns: []` 空数组，不缓存任何文件
2. **图标配置不匹配** - 配置中使用外部 unpkg 链接，但项目中已有本地图标文件（`pwa-192x192.png`、`pwa-512x512.png`）未使用
3. **应用信息未定制** - 描述还是 Vben Admin 默认值
4. **Service Worker 未注册** - `injectRegister: false`，需要手动注册

---

## 三、实施方案

### 步骤 1: 定制应用信息

**重要说明**：
- 修改后的配置会通过 `pwaOptions` 传递给插件，插件会合并配置（用户配置会覆盖默认值）
- `name` 参数来自环境变量 `VITE_APP_TITLE`（通过 `loadAndConvertEnv()` 加载），无需硬编码
- 如果需要在 `.env` 文件中配置应用名称，设置 `VITE_APP_TITLE=智慧律所管理系统`

修改 `frontend/internal/vite-config/src/options.ts`:

```typescript
const getDefaultPwaOptions = (name: string): Partial<PwaPluginOptions> => ({
  manifest: {
    // 使用从环境变量 VITE_APP_TITLE 加载的应用名称
    name: `${name}${isDevelopment ? ' dev' : ''}`,
    // short_name 建议使用简短版本（PWA 标准建议不超过 12 个字符）
    // 可以根据实际应用名称自定义，例如：
    // - "智慧律所管理系统" -> "律所系统"
    // - "Law Firm Management" -> "Law Firm"
    short_name: name.length > 12 
      ? (name.includes('律所') ? '律所系统' : name.substring(0, 12))
      : name,
    description: '专业的律所业务管理平台，支持案件管理、客户管理、文档管理等功能',
    display: 'standalone',
    theme_color: '#1890ff',
    background_color: '#ffffff',
    start_url: '/',
    icons: [
      {
        src: '/pwa-192x192.png',
        sizes: '192x192',
        type: 'image/png',
        purpose: 'any'
      },
      {
        src: '/pwa-512x512.png',
        sizes: '512x512',
        type: 'image/png',
        purpose: 'any'
      },
      // 可选：Android 自适应图标（maskable）
      // 如果提供了 maskable 版本，取消下面的注释并添加文件
      // {
      //   src: '/pwa-maskable-512x512.png',
      //   sizes: '512x512',
      //   type: 'image/png',
      //   purpose: 'maskable'
      // }
    ]
  },
  // 注册方式：'auto' 会自动注入基础注册代码，但仍可通过 registerSW 自定义更新逻辑
  // 如果只需要基础功能，可以设置为 'auto' 并跳过步骤3
  // 如果需要自定义更新提示，设置为 'inline' 并在步骤3中手动注册
  injectRegister: 'inline', // 改为 'inline' 以便手动控制更新逻辑
  // 更新策略
  registerType: 'autoUpdate',
  workbox: {
    // 缓存策略
    globPatterns: [
      '**/*.{js,css,html,woff2,png,svg,jpg,jpeg,gif,webp,ico}'
    ],
    // 预缓存版本控制
    navigateFallback: '/index.html',
    // 最大缓存大小（建议根据实际调整）
    maximumFileSizeToCacheInBytes: 5 * 1024 * 1024, // 5MB
    // 注意：如果 plugins/index.ts 中有默认的 workbox 配置，这里的配置会合并覆盖
    // 运行时缓存策略
    runtimeCaching: [
      {
        // API 请求缓存 - 网络优先
        urlPattern: /^https?:\/\/.*\/api\/.*/i,
        handler: 'NetworkFirst',
        options: {
          cacheName: 'api-cache',
          expiration: {
            maxEntries: 100,
            maxAgeSeconds: 60 * 60 * 24 // 24 小时
          },
          networkTimeoutSeconds: 10
        }
      },
      {
        // 静态资源缓存 - 缓存优先
        urlPattern: /\.(?:png|jpg|jpeg|svg|gif|webp|woff2?)$/i,
        handler: 'CacheFirst',
        options: {
          cacheName: 'static-assets',
          expiration: {
            maxEntries: 200,
            maxAgeSeconds: 60 * 60 * 24 * 30 // 30 天
          }
        }
      }
    ]
  },
  // 开发环境禁用
  devOptions: {
    enabled: false
  }
});
```

### 步骤 1.5: 配置环境变量（可选）

如果需要自定义应用名称，在 `frontend/apps/web-antd/.env` 或 `.env.production` 文件中设置：

```bash
# 应用标题（会用于 PWA manifest 的 name 字段）
VITE_APP_TITLE=智慧律所管理系统
```

如果不设置，将使用默认值（在 `loadAndConvertEnv()` 中定义，当前为 `'Vben Admin'`）。

### 步骤 2: 准备应用图标

**当前状态**：项目中已存在 PWA 图标文件：
- ✅ `public/pwa-192x192.png` - 192×192 像素图标
- ✅ `public/pwa-512x512.png` - 512×512 像素图标

**路径说明**：
- 图标文件直接放在 `public/` 目录下（不是 `public/pwa/` 子目录）
- 构建后，这些文件会被复制到输出目录的根路径
- 配置中的路径 `/pwa-192x192.png` 对应 `public/pwa-192x192.png` 文件

**如果图标文件不存在或需要更新**：

| 文件名 | 尺寸（像素） | 用途 | 是否必需 | 当前状态 |
|--------|------------|------|---------|---------|
| `pwa-192x192.png` | 192×192 | Android 主屏幕图标、iOS 主屏幕图标 | ✅ 必需 | ✅ 已存在 |
| `pwa-512x512.png` | 512×512 | iOS/Android 高分辨率图标、安装提示图标 | ✅ 必需 | ✅ 已存在 |
| `pwa-maskable-512x512.png` | 512×512 | 适配 Android 自适应图标（圆角、裁剪） | ⚠️ 可选 | ❌ 不存在 |

**注意**：如果将来需要创建 `pwa` 子目录来组织图标文件，可以：
1. 创建 `public/pwa/` 目录
2. 将图标文件移动到该目录并重命名为 `icon-192.png` 和 `icon-512.png`
3. 更新配置中的路径为 `/pwa/icon-192.png` 和 `/pwa/icon-512.png`

**图标设计建议**：
- 使用律所 LOGO 或法律相关元素（天平、法槌等）
- 背景色与主题色 `#1890ff` 协调
- 简洁易识别，小尺寸下清晰可见
- **重要**：确保 PNG 图片的实际尺寸与配置中的 `sizes` 完全一致
  - `icon-192.png` 必须是 192×192 像素
  - `icon-512.png` 必须是 512×512 像素
  - `maskable-icon.png`（如果使用）必须是 512×512 像素，且内容在安全区域内（中心 80% 区域）

**图标配置说明**：
- `purpose: 'any'` - 用于标准图标显示（所有平台）
- `purpose: 'maskable'` - 用于 Android 自适应图标（可选，需要单独提供）
- 不要使用 `purpose: 'any maskable'`（这是无效的配置）

### 步骤 3: 注册 Service Worker（自定义更新逻辑）

**注意**：由于步骤1中设置了 `injectRegister: 'inline'`，需要手动注册 Service Worker 以自定义更新逻辑。

在 `frontend/apps/web-antd/src/main.ts` 中添加：

```typescript
import { registerSW } from 'virtual:pwa-register'

const updateSW = registerSW({
  onNeedRefresh() {
    // 显示更新提示
    if (confirm('发现新版本，是否立即更新？')) {
      updateSW(true)
    }
  },
  onOfflineReady() {
    // 离线就绪提示（可选）
    console.log('应用已准备离线使用')
  },
  onRegistered(registration) {
    // 每小时检查一次更新
    registration && setInterval(() => {
      registration.update()
    }, 60 * 60 * 1000)
  }
})
```

**替代方案**：如果不需要自定义更新提示，可以在步骤1中设置 `injectRegister: 'auto'`，然后跳过此步骤。插件会自动处理 Service Worker 注册和更新。

### 步骤 4: 添加安装提示（可选）

创建 `frontend/apps/web-antd/src/composables/usePwaInstall.ts`:

```typescript
import { ref, onMounted, onUnmounted } from 'vue'

export function usePwaInstall() {
  const deferredPrompt = ref<Event | null>(null)
  const showInstallPrompt = ref(false)
  const isInstallable = ref(false)

  const handler = (e: Event) => {
    e.preventDefault()
    deferredPrompt.value = e
    isInstallable.value = true
  }

  onMounted(() => {
    window.addEventListener('beforeinstallprompt', handler)
  })

  onUnmounted(() => {
    window.removeEventListener('beforeinstallprompt', handler)
  })

  const install = async () => {
    if (!deferredPrompt.value) return

    ;(deferredPrompt.value as any).prompt()
    const { outcome } = await (deferredPrompt.value as any).userChoice
    if (outcome === 'accepted') {
      isInstallable.value = false
    }
    deferredPrompt.value = null
  }

  return {
    isInstallable,
    showInstallPrompt,
    install
  }
}
```

在设置页面或工作台添加安装按钮：

```vue
<script setup lang="ts">
import { usePwaInstall } from '#/composables/usePwaInstall'

const { isInstallable, install } = usePwaInstall()
</script>

<template>
  <a-button
    v-if="isInstallable"
    type="primary"
    @click="install"
  >
    安装应用到桌面
  </a-button>
</template>
```

### 步骤 5: 离线提示页面

创建 `frontend/apps/web-antd/public/offline.html`:

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>离线提示 - 智慧律所管理系统</title>
  <style>
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100vh;
      margin: 0;
      background: #f5f5f5;
    }
    .offline-card {
      text-align: center;
      padding: 40px;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
    .icon {
      font-size: 64px;
      margin-bottom: 20px;
    }
    h1 { margin: 0 0 10px; color: #333; }
    p { color: #666; margin: 0; }
  </style>
</head>
<body>
  <div class="offline-card">
    <div class="icon">📡</div>
    <h1>网络连接已断开</h1>
    <p>请检查网络连接，部分功能可能无法使用</p>
  </div>
</body>
</html>
```

---

## 四、缓存策略说明

### 静态资源 - CacheFirst

```
图片、字体、样式等 → 优先从缓存读取 → 缓存过期才更新
```

适用场景：案件文档预览、客户上传的附件

### API 请求 - NetworkFirst

```
API 调用 → 优先请求网络 → 超时或失败时使用缓存
```

适用场景：案件列表、客户信息等需要实时性的数据

**注意**：
- 敏感操作（登录、提交文件、修改数据）不应该被缓存，Service Worker 会自动处理 POST/PUT/DELETE 请求（这些请求不会被缓存）
- API 缓存策略使用 `NetworkFirst`，确保数据实时性，只有在网络失败时才使用缓存
- 静态资源使用 `CacheFirst`，提高加载速度，适合图片、字体等不常变化的资源

---

## 五、测试验证

### 本地测试

```bash
# 构建生产版本
cd frontend/apps/web-antd
pnpm build

# 预览（需要 HTTPS 环境）
pnpm preview
```

### Chrome DevTools 验证

1. 打开 DevTools → **Application** → **Service Workers**
2. 检查 Service Worker 状态（激活/运行中）
3. **Cache Storage** 查看缓存内容
4. **Manifest** 查看应用信息

### 离线测试

1. DevTools → **Network** → 勾选 **Offline** 模式
2. 刷新页面，验证是否正常显示
3. 检查缓存的页面是否能正常访问

### 移动端测试

1. 用手机浏览器打开部署好的应用
2. Android: 浏览器菜单会显示"添加到主屏幕"
3. iOS: Safari 分享按钮会显示"添加到主屏幕"
4. 点击主屏幕图标启动，验证全屏显示效果

---

## 六、部署注意事项

### HTTPS 要求

PWA 必须通过 HTTPS 访问，本地开发可用 `localhost` 例外。

### Nginx 配置示例

```nginx
# Service Worker 文件不要缓存
location /sw.js {
    add_header Cache-Control 'no-cache';
}

# 预缓存资源可以设置长期缓存
location ~* \.(js|css|png|jpg|jpeg|gif|webp|svg|woff2?)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

---

## 七、效果预期

| 指标 | 传统 Web | PWA |
|------|---------|-----|
| 首次加载 | ~3-5s | ~3-5s |
| 二次加载 | ~2-3s | < 1s |
| 离线可用 | ❌ | ✅ |
| 可安装 | ❌ | ✅ |
| 类 App 体验 | ❌ | ✅ |

---

## 八、后续优化方向

1. **后台同步** - 离线时编辑的资料，联网后自动同步
2. **推送通知** - 案件状态变更、开庭提醒等
3. **定期更新** - 重要信息自动后台刷新
4. **数据预取** - 根据用户习惯预加载可能访问的页面

---

## 九、方案正确性说明

### ✅ 方案整体正确性评估

**总体评价**：方案整体正确，但需要修正以下问题：

1. **✅ 正确的部分**：
   - PWA 基础概念和需求分析准确
   - 缓存策略设计合理（NetworkFirst 用于 API，CacheFirst 用于静态资源）
   - 图标路径和 manifest 配置正确
   - 安装提示功能实现方式正确
   - 离线页面设计合理

2. **⚠️ 已修正的问题**：
   - **Service Worker 注册方式**：原方案中 `injectRegister: 'auto'` 与手动注册代码冲突，已修正为 `'inline'` 并添加说明
   - **配置合并说明**：添加了配置合并逻辑的说明，避免配置冲突

3. **💡 建议补充**：
   - 生产环境部署前，建议在 Chrome DevTools 的 Lighthouse 中测试 PWA 评分
   - 考虑添加错误处理和降级方案（如果 Service Worker 注册失败）
   - 对于大型文件（如 PDF），可以考虑使用 IndexedDB 而不是 Cache API

### 🔧 实施注意事项

1. **配置优先级**：`pwaOptions` 中的配置会覆盖插件默认配置，注意 `workbox` 对象的合并方式
2. **开发环境**：PWA 功能在开发环境默认禁用（`devOptions.enabled: false`），需要在生产构建中测试
3. **HTTPS 要求**：PWA 必须通过 HTTPS 访问（localhost 除外），部署时确保配置正确
4. **浏览器兼容性**：Service Worker 在较旧的浏览器中不支持，需要做好降级处理

---

## 十、参考资源

- [PWA 官方文档](https://web.dev/progressive-web-apps/)
- [vite-plugin-pwa 文档](https://vite-pwa-plugin.netlify.app/)
- [Service Worker API](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API)
- [Workbox 文档](https://developers.google.com/web/tools/workbox)