# 卷宗模板菜单检查指南

## 问题描述

如果发现"卷宗模板"菜单没有显示，可能是数据库初始化脚本未完全执行，导致菜单数据缺失。

## 快速检查命令

### 方法1: 使用检查脚本（推荐）

```bash
# 在服务器上执行
cd /opt/law-firm
./scripts/check-dossier-template-menu.sh
```

### 方法2: 手动SQL检查

如果脚本无法运行，可以直接连接数据库检查：

```bash
# 进入PostgreSQL容器
docker exec -it law-firm-postgres bash

# 连接数据库
psql -U law_admin -d law_firm

# 执行检查SQL
SELECT id, name, path, visible, status 
FROM sys_menu 
WHERE id = 715 OR path = '/document/dossier-template';

# 检查权限分配
SELECT r.id, r.name, rm.menu_id
FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.id
WHERE rm.menu_id = 715;
```

## 快速修复命令

### 方法1: 使用修复脚本（推荐）

```bash
# 在服务器上执行
cd /opt/law-firm
./scripts/fix-dossier-template-menu.sh
```

### 方法2: 手动执行SQL修复

```bash
# 进入PostgreSQL容器
docker exec -it law-firm-postgres bash

# 连接数据库
psql -U law_admin -d law_firm

# 执行修复SQL
```

```sql
-- 1. 插入菜单（如果不存在）
INSERT INTO sys_menu VALUES (
    715, 6, '卷宗模板', '/document/dossier-template', 
    'document/dossier-template/index', NULL, 'FileProtectOutlined', 
    'MENU', 'doc:list', 5, true, 'ENABLED', false, true, 
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, false
) ON CONFLICT (id) DO UPDATE SET 
    name = '卷宗模板', 
    path = '/document/dossier-template', 
    component = 'document/dossier-template/index', 
    permission = 'doc:list', 
    updated_at = CURRENT_TIMESTAMP;

-- 2. 分配权限给管理员角色
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 1, 715, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu 
    WHERE role_id = 1 AND menu_id = 715
);

-- 3. 分配权限给律所主任角色
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 2, 715, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu 
    WHERE role_id = 2 AND menu_id = 715
);

-- 4. 分配权限给行政角色
INSERT INTO sys_role_menu (role_id, menu_id, created_at)
SELECT 8, 715, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu 
    WHERE role_id = 8 AND menu_id = 715
);
```

### 方法3: 重新执行初始化脚本

如果菜单和权限都缺失，可以重新执行初始化数据脚本：

```bash
cd /opt/law-firm

# 只执行初始化数据脚本（不会删除现有数据）
docker exec -i law-firm-postgres psql -U law_admin -d law_firm < scripts/init-db/20-init-data.sql
```

## 验证修复结果

修复后，请执行以下步骤验证：

1. **检查菜单是否存在**：
```bash
docker exec law-firm-postgres psql -U law_admin -d law_firm -c \
  "SELECT id, name, path FROM sys_menu WHERE id = 715;"
```

2. **检查权限分配**：
```bash
docker exec law-firm-postgres psql -U law_admin -d law_firm -c \
  "SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = 715;"
```

3. **刷新前端页面**：
   - 退出登录后重新登录
   - 或清除浏览器缓存后刷新页面
   - 菜单应该出现在"卷宗管理"模块下

## 常见问题

### Q1: 菜单存在但看不到？

**可能原因**：
- 当前用户角色没有权限
- 菜单的 `visible` 字段为 `false`
- 前端缓存未刷新

**解决方法**：
```sql
-- 检查菜单是否可见
SELECT id, name, visible, status FROM sys_menu WHERE id = 715;

-- 如果不可见，设置为可见
UPDATE sys_menu SET visible = true WHERE id = 715;

-- 检查当前用户的角色权限
SELECT r.name, rm.menu_id 
FROM sys_user_role ur
JOIN sys_role_menu rm ON ur.role_id = rm.role_id
JOIN sys_role r ON ur.role_id = r.id
WHERE ur.user_id = (SELECT id FROM sys_user WHERE username = 'admin')
AND rm.menu_id = 715;
```

### Q2: 执行脚本报错"容器不存在"？

**解决方法**：
```bash
# 查看PostgreSQL容器名称
docker ps | grep postgres

# 如果容器名称不同，修改脚本中的容器名称
# 或使用环境变量指定
export DOCKER_CONTAINER=你的容器名称
```

### Q3: 权限分配后仍然看不到菜单？

**解决方法**：
1. 确认用户已重新登录
2. 清除浏览器缓存
3. 检查后端日志是否有权限相关错误
4. 确认前端路由配置正确（`frontend/apps/web-antd/src/router/routes/modules/document.ts`）

## 相关文件

- 菜单配置SQL: `scripts/init-db/20-init-data.sql` (第367行)
- 权限分配SQL: `scripts/init-db/20-init-data.sql` (第1164-1166行)
- 前端路由配置: `frontend/apps/web-antd/src/router/routes/modules/document.ts`
- 前端页面组件: `frontend/apps/web-antd/src/views/document/dossier-template/index.vue`
