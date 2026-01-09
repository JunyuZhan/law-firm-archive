# 替换登录页面插画指南

## 📍 当前实现

登录页面左侧的插画位于：
- **组件文件**: `frontend/packages/effects/layouts/src/authentication/icons/slogan.vue`
- **使用位置**: `frontend/packages/effects/layouts/src/authentication/authentication.vue` (第127行)

## 🎨 浮动动画效果

插画使用了 `animate-float` CSS 类，实现上下浮动效果：
- **动画时长**: 5秒
- **动画效果**: 上下浮动（0px → -20px → 0px）
- **动画类型**: 无限循环

动画定义在 `frontend/internal/tailwind-config/src/index.ts`：
```typescript
float: 'float 5s linear 0ms infinite',
keyframes: {
  float: {
    '0%': { transform: 'translateY(0)' },
    '50%': { transform: 'translateY(-20px)' },
    '100%': { transform: 'translateY(0)' },
  },
}
```

## 🎯 推荐制作SVG插画的网站

### 1. **Undraw** (https://undraw.co/)
- ✅ 免费开源SVG插画库
- ✅ 可自定义颜色
- ✅ 提供SVG代码下载
- ✅ 适合法律行业的插画：搜索 "law", "legal", "document", "contract" 等关键词

### 2. **DrawKit** (https://www.drawkit.io/)
- ✅ 免费和付费SVG插画
- ✅ 提供SVG代码
- ✅ 有法律相关的插画集

### 3. **Open Peeps** (https://www.openpeeps.com/)
- ✅ 手绘风格SVG插画
- ✅ 可组合不同元素
- ✅ 适合制作人物场景

### 4. **ManyPixels** (https://www.manypixels.co/gallery)
- ✅ 免费SVG插画库
- ✅ 可自定义颜色
- ✅ 提供SVG代码

### 5. **SVG Repo** (https://www.svgrepo.com/)
- ✅ 大量免费SVG图标和插画
- ✅ 可直接下载SVG代码
- ✅ 搜索 "law", "legal", "office" 等关键词

### 6. **Freepik** (https://www.freepik.com/)
- ⚠️ 部分免费，部分付费
- ✅ 大量法律行业相关插画
- ✅ 提供SVG格式

## 🔧 替换方法

### 方法一：替换SVG组件文件（推荐）

1. **下载SVG代码**
   - 从上述网站选择一个适合法律行业的插画
   - 下载SVG代码（复制SVG代码或下载.svg文件）

2. **替换组件文件**
   ```bash
   # 备份原文件
   cp frontend/packages/effects/layouts/src/authentication/icons/slogan.vue \
      frontend/packages/effects/layouts/src/authentication/icons/slogan.vue.bak
   
   # 编辑文件
   # 将下载的SVG代码替换到 slogan.vue 文件中
   ```

3. **修改文件结构**
   ```vue
   <template>
     <svg
       enable-background="new 0 0 800 800"
       version="1.1"
       viewBox="0 0 800 800"
       xmlns="http://www.w3.org/2000/svg"
     >
       <!-- 粘贴你的SVG代码 here -->
     </svg>
   </template>
   ```

4. **保持动画效果**
   - 动画会自动应用，因为组件使用了 `animate-float` 类
   - 无需额外配置

### 方法二：使用自定义图片（简单但无动画）

1. **准备图片**
   - 将图片放到 `frontend/apps/web-antd/public/` 目录
   - 例如：`frontend/apps/web-antd/public/login-illustration.svg`

2. **修改登录布局文件**
   ```vue
   <!-- frontend/apps/web-antd/src/layouts/auth.vue -->
   <template>
     <AuthPageLayout
       :app-name="appName"
       :logo="logo"
       :logo-dark="logoDark"
       :page-description="$t('authentication.pageDesc')"
       :page-title="$t('authentication.pageTitle')"
       :slogan-image="'/login-illustration.svg'"
     >
     </AuthPageLayout>
   </template>
   ```

   ⚠️ **注意**: 使用图片方式会失去浮动动画效果

### 方法三：创建新的SVG组件（最佳实践）

1. **创建新组件**
   ```bash
   # 创建新的法律行业插画组件
   touch frontend/packages/effects/layouts/src/authentication/icons/law-illustration.vue
   ```

2. **编写组件**
   ```vue
   <!-- frontend/packages/effects/layouts/src/authentication/icons/law-illustration.vue -->
   <template>
     <svg
       viewBox="0 0 800 800"
       xmlns="http://www.w3.org/2000/svg"
       class="law-illustration"
     >
       <!-- 粘贴你的法律行业SVG代码 -->
     </svg>
   </template>
   ```

3. **修改使用位置**
   ```vue
   <!-- frontend/packages/effects/layouts/src/authentication/authentication.vue -->
   <script>
   import LawIllustration from './icons/law-illustration.vue';
   </script>
   
   <template>
     <LawIllustration v-else :alt="appName" class="animate-float h-64 w-2/5" />
   </template>
   ```

## 💡 法律行业插画建议

### 推荐主题元素：
- 📚 法律书籍/法典
- ⚖️ 天平（正义象征）
- 📝 合同/文件
- 🏛️ 法院建筑
- 👨‍⚖️ 律师/法官形象
- 📋 文件夹/卷宗
- 🔍 放大镜（搜索/审查）
- 💼 公文包

### 推荐配色：
- 主色：深蓝色（专业、信任）
- 辅助色：金色（权威、品质）
- 背景：浅蓝/浅灰（简洁、专业）

## 📝 注意事项

1. **SVG尺寸**: 建议使用 `viewBox="0 0 800 800"` 或类似的尺寸，保持比例
2. **文件大小**: SVG代码应尽量精简，避免过大影响加载速度
3. **颜色适配**: 考虑浅色和暗色主题的兼容性
4. **动画保持**: 使用SVG组件方式可以保持浮动动画效果
5. **响应式**: 确保SVG在不同屏幕尺寸下都能正常显示

## ✅ 完成后的检查清单

- [ ] SVG代码已正确粘贴到组件中
- [ ] 动画效果正常（上下浮动）
- [ ] 浅色主题下显示正常
- [ ] 暗色主题下显示正常（如适用）
- [ ] 不同屏幕尺寸下显示正常
- [ ] 文件大小合理（< 100KB）

## 🔗 相关文件

- 组件文件: `frontend/packages/effects/layouts/src/authentication/icons/slogan.vue`
- 使用位置: `frontend/packages/effects/layouts/src/authentication/authentication.vue`
- 动画配置: `frontend/internal/tailwind-config/src/index.ts`
- 登录布局: `frontend/apps/web-antd/src/layouts/auth.vue`


