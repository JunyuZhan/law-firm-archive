# 清除浏览器缓存指南

## 🔄 清除浏览器缓存方法

如果浏览器显示的是旧容器的内容，需要清除浏览器缓存。

---

## 🌐 各浏览器清除缓存方法

### Chrome / Edge（推荐方法）

#### 方法一：硬刷新（最快）

**Windows/Linux**：
- `Ctrl + Shift + R`
- 或 `Ctrl + F5`

**Mac**：
- `Cmd + Shift + R`
- 或 `Cmd + Option + R`

#### 方法二：清除缓存

1. **打开开发者工具**
   - 按 `F12` 或右键 → "检查"（Inspect）

2. **清除缓存**
   - 右键点击浏览器刷新按钮
   - 选择 **"清空缓存并硬性重新加载"**（Empty Cache and Hard Reload）

3. **或通过设置清除**
   - `Ctrl + Shift + Delete`（Windows/Linux）
   - `Cmd + Shift + Delete`（Mac）
   - 选择 **"缓存的图片和文件"**（Cached images and files）
   - 时间范围选择 **"全部时间"**（All time）
   - 点击 **"清除数据"**（Clear data）

### Firefox

#### 方法一：硬刷新

**Windows/Linux**：
- `Ctrl + Shift + R`
- 或 `Ctrl + F5`

**Mac**：
- `Cmd + Shift + R`

#### 方法二：清除缓存

1. **打开设置**
   - `Ctrl + Shift + Delete`（Windows/Linux）
   - `Cmd + Shift + Delete`（Mac）

2. **清除缓存**
   - 选择 **"缓存"**（Cache）
   - 时间范围选择 **"全部"**（Everything）
   - 点击 **"立即清除"**（Clear Now）

### Safari

#### 方法一：硬刷新

**Mac**：
- `Cmd + Option + R`

#### 方法二：清除缓存

1. **打开设置**
   - Safari → Preferences（偏好设置）
   - 或 `Cmd + ,`

2. **清除缓存**
   - Advanced（高级）→ 勾选 "Show Develop menu"（显示开发菜单）
   - Develop（开发）→ Empty Caches（清空缓存）
   - 或 `Cmd + Option + E`

---

## 🔧 针对特定网站的缓存清除

### Chrome / Edge

1. **打开开发者工具**
   - 按 `F12`

2. **Application 标签**
   - 点击 **"Application"**（应用程序）标签
   - 左侧展开 **"Storage"**（存储）
   - 点击 **"Clear site data"**（清除网站数据）
   - 勾选所有选项
   - 点击 **"Clear site data"**（清除网站数据）

### Firefox

1. **打开开发者工具**
   - 按 `F12`

2. **Storage 标签**
   - 点击 **"Storage"**（存储）标签
   - 右键点击域名
   - 选择 **"Delete All"**（删除全部）

---

## 🚀 快速清除方法（推荐）

### 方法一：无痕模式（最快）

**Chrome/Edge**：
- `Ctrl + Shift + N`（Windows/Linux）
- `Cmd + Shift + N`（Mac）

**Firefox**：
- `Ctrl + Shift + P`（Windows/Linux）
- `Cmd + Shift + P`（Mac）

在无痕模式下访问，不会使用缓存。

### 方法二：硬刷新（推荐）

访问网站时，直接使用硬刷新：
- **Windows/Linux**: `Ctrl + Shift + R` 或 `Ctrl + F5`
- **Mac**: `Cmd + Shift + R`

### 方法三：清除特定网站数据

1. 访问网站
2. 按 `F12` 打开开发者工具
3. 右键点击刷新按钮
4. 选择 **"清空缓存并硬性重新加载"**

---

## 🔍 验证缓存已清除

### 检查方法

1. **查看页面版本**
   - 按 `F12` 打开开发者工具
   - Network（网络）标签
   - 刷新页面
   - 查看资源文件的响应头
   - 检查 `Last-Modified` 或 `ETag` 时间戳

2. **检查文件内容**
   - 查看页面源代码（`Ctrl + U`）
   - 检查是否有最新的内容

---

## 🛠️ 服务器端缓存清除

如果清除浏览器缓存后仍然显示旧内容，可能需要清除服务器端缓存：

### Nginx 缓存（如果配置了）

```bash
# 清除 Nginx 缓存目录（如果配置了）
sudo rm -rf /var/cache/nginx/*

# 重启 Nginx（在容器中）
docker exec law-firm-frontend nginx -s reload
```

### Docker 容器缓存

```bash
# 重启前端容器
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart frontend
```

### 强制重新构建前端

```bash
# 重新构建前端镜像（不使用缓存）
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml build --no-cache frontend
docker compose --env-file ../.env -f docker-compose.prod.yml up -d frontend
```

---

## 📋 完整清除步骤

### 步骤 1: 清除浏览器缓存

**Chrome/Edge**：
1. 按 `Ctrl + Shift + Delete`（Windows）或 `Cmd + Shift + Delete`（Mac）
2. 选择 "缓存的图片和文件"
3. 时间范围选择 "全部时间"
4. 点击 "清除数据"

### 步骤 2: 硬刷新页面

访问网站时：
- **Windows/Linux**: `Ctrl + Shift + R`
- **Mac**: `Cmd + Shift + R`

### 步骤 3: 如果仍然显示旧内容

```bash
# 重启前端容器
cd /opt/law-firm/docker
docker compose --env-file ../.env -f docker-compose.prod.yml restart frontend

# 等待几秒后，再次硬刷新浏览器
```

---

## 🎯 针对不同场景

### 场景 1: 前端页面显示旧内容

**解决方法**：
1. 硬刷新：`Ctrl + Shift + R`（Windows）或 `Cmd + Shift + R`（Mac）
2. 清除浏览器缓存
3. 重启前端容器

### 场景 2: API 返回旧数据

**解决方法**：
1. 检查后端是否重启
2. 检查数据库是否有新数据
3. 清除浏览器缓存后重试

### 场景 3: 静态资源（JS/CSS）未更新

**解决方法**：
1. 硬刷新：`Ctrl + Shift + R`
2. 清除浏览器缓存
3. 检查 Nginx 缓存配置
4. 重启前端容器

---

## 🔗 相关文档

- [清理和重新部署](./CLEANUP_AND_REDEPLOY.md)
- [生产环境容器清单](./PRODUCTION_CONTAINERS.md)
