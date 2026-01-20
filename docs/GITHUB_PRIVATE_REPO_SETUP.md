# GitHub 私有仓库部署配置指南

## 📋 概述

当你的 GitHub 仓库是私有仓库时，服务器需要通过认证才能执行 `git pull` 操作。本指南介绍三种常用的配置方法。

---

## 🔐 方法一：SSH 密钥认证（推荐）

这是最安全和推荐的方式，适合长期部署。

### 步骤 1: 在服务器上生成 SSH 密钥

```bash
# 登录到服务器
ssh user@your-server

# 生成 SSH 密钥（如果还没有）
ssh-keygen -t ed25519 -C "deploy@law-firm" -f ~/.ssh/id_ed25519_deploy

# 如果系统不支持 ed25519，使用 RSA
# ssh-keygen -t rsa -b 4096 -C "deploy@law-firm" -f ~/.ssh/id_rsa_deploy

# 查看公钥内容
cat ~/.ssh/id_ed25519_deploy.pub
```

### 步骤 2: 将公钥添加到 GitHub

**方式 A: 添加到个人账户（推荐用于个人项目）**

1. 复制公钥内容（`~/.ssh/id_ed25519_deploy.pub`）
2. 访问 GitHub: https://github.com/settings/keys
3. 点击 "New SSH key"
4. 粘贴公钥，添加描述（如 "Law Firm Server"）
5. 点击 "Add SSH key"

**方式 B: 添加为 Deploy Key（推荐用于服务器部署）**

1. 复制公钥内容（`~/.ssh/id_ed25519_deploy.pub`）
2. 访问你的仓库: `https://github.com/your-username/law-firm/settings/keys`
3. 点击 "Add deploy key"
4. 粘贴公钥，添加标题（如 "Production Server"）
5. ✅ **取消勾选 "Allow write access"**（只读即可）
6. 点击 "Add key"

### 步骤 3: 配置 SSH 使用该密钥

```bash
# 编辑 SSH 配置文件
nano ~/.ssh/config

# 添加以下内容：
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_deploy
    IdentitiesOnly yes

# 保存并退出（Ctrl+X, Y, Enter）
```

### 步骤 4: 测试 SSH 连接

```bash
# 测试连接
ssh -T git@github.com

# 如果成功，会看到：
# Hi your-username! You've successfully authenticated, but GitHub does not provide shell access.
```

### 步骤 5: 修改仓库远程 URL（如果已克隆）

```bash
# 进入项目目录
cd /opt/law-firm

# 检查当前远程 URL
git remote -v

# 如果使用的是 HTTPS，改为 SSH
git remote set-url origin git@github.com:your-username/law-firm.git

# 验证
git remote -v
```

### 步骤 6: 测试 git pull

```bash
cd /opt/law-firm
git pull origin main
```

---

## 🔑 方法二：Personal Access Token (PAT)

适合临时使用或不想配置 SSH 的场景。

### 步骤 1: 创建 Personal Access Token

1. 访问 GitHub: https://github.com/settings/tokens
2. 点击 "Generate new token" → "Generate new token (classic)"
3. 填写信息：
   - **Note**: `Law Firm Deployment`
   - **Expiration**: 选择合适的时间（建议 90 天或 1 年）
   - **Scopes**: 至少勾选 `repo`（私有仓库需要）
4. 点击 "Generate token"
5. **⚠️ 重要：立即复制 token，之后无法再查看！**

### 步骤 2: 使用 Token 配置 Git

**方式 A: 在 URL 中包含 Token（不推荐，token 会出现在命令历史）**

```bash
cd /opt/law-firm
git remote set-url origin https://YOUR_TOKEN@github.com/your-username/law-firm.git
```

**方式 B: 使用 Git Credential Helper（推荐）**

```bash
# 配置 Git 凭据存储
git config --global credential.helper store

# 执行一次 git pull，输入用户名和 token
cd /opt/law-firm
git pull origin main
# Username: your-username
# Password: YOUR_TOKEN (注意：这里输入的是 token，不是密码)

# 之后就可以直接 git pull，无需再输入
```

**方式 C: 使用环境变量（适合脚本）**

```bash
# 在 ~/.bashrc 或 ~/.zshrc 中添加
export GIT_ASKPASS=echo
export GIT_USERNAME=your-username
export GIT_TOKEN=your_token_here

# 然后配置远程 URL
git remote set-url origin https://${GIT_USERNAME}:${GIT_TOKEN}@github.com/your-username/law-firm.git
```

---

## 🚀 方法三：使用 GitHub CLI (gh)

适合需要更多 GitHub 功能的场景。

### 步骤 1: 安装 GitHub CLI

```bash
# Ubuntu/Debian
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
sudo apt update
sudo apt install gh

# CentOS/RHEL
sudo dnf install gh

# macOS
brew install gh
```

### 步骤 2: 登录 GitHub

```bash
gh auth login
# 选择 GitHub.com
# 选择 HTTPS
# 选择 "Login with a web browser" 或 "Paste an authentication token"
```

### 步骤 3: 配置 Git 使用 gh

```bash
gh auth setup-git
```

之后就可以正常使用 `git pull` 了。

---

## 📝 快速配置脚本

我们提供了两个自动化配置脚本：

### 场景一：服务器上还没有代码（首次配置）

如果你的服务器上还没有克隆代码，需要先配置 SSH 才能克隆。

⚠️ **注意**：由于是私有仓库，服务器无法直接从 GitHub 下载脚本，需要先上传脚本到服务器。

**方式 A: 上传脚本到服务器后运行（推荐）**

```bash
# 1. 在本地电脑上，使用 scp 上传脚本到服务器
#    替换 root 为你的实际用户名（如 ubuntu, admin 等）
scp scripts/init-github-ssh.sh root@192.168.50.10:/tmp/

# 2. SSH 登录到服务器
ssh root@192.168.50.10

# 3. 在服务器上运行脚本
bash /tmp/init-github-ssh.sh
```

**方式 B: 手动复制脚本内容到服务器**

```bash
# 1. 在本地查看脚本内容
cat scripts/init-github-ssh.sh

# 2. SSH 登录到服务器（替换 root 为你的实际用户名）
ssh root@192.168.50.10

# 3. 在服务器上创建脚本文件
nano /tmp/init-github-ssh.sh
# 粘贴脚本内容，保存退出（Ctrl+X, Y, Enter）

# 4. 添加执行权限并运行
chmod +x /tmp/init-github-ssh.sh
bash /tmp/init-github-ssh.sh
```

**方式 C: 手动配置（最简单，无需脚本）**

```bash
# 1. SSH 登录到服务器（替换 root 为你的实际用户名）
ssh root@192.168.50.10

# 2. 生成 SSH 密钥
ssh-keygen -t ed25519 -C "deploy@law-firm" -f ~/.ssh/id_ed25519_deploy -N ""

# 3. 查看公钥（复制这个内容）
cat ~/.ssh/id_ed25519_deploy.pub

# 4. 将公钥添加到 GitHub
#    访问: https://github.com/junyuzhan/law-firm/settings/keys
#    点击 "Add deploy key"，粘贴公钥，取消勾选 "Allow write access"

# 5. 配置 SSH config
cat >> ~/.ssh/config << EOF
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_deploy
    IdentitiesOnly yes
    StrictHostKeyChecking accept-new
EOF
chmod 600 ~/.ssh/config

# 6. 测试连接
ssh -T git@github.com

# 7. 克隆代码
git clone git@github.com:junyuzhan/law-firm.git /opt/law-firm
```

### 场景二：服务器上已有代码

如果代码已经克隆到服务器，可以使用完整配置脚本：

```bash
# 进入项目目录
cd /opt/law-firm

# 运行配置脚本
./scripts/setup-github-ssh.sh
```

脚本会：
1. ✅ 检查是否已有 SSH 密钥
2. ✅ 如果没有，自动生成新的 SSH 密钥
3. ✅ 显示公钥内容，提示你添加到 GitHub
4. ✅ 配置 SSH config
5. ✅ 测试连接
6. ✅ 更新仓库远程 URL

---

## 🔍 验证配置

配置完成后，验证是否正常工作：

```bash
# 1. 测试 SSH 连接
ssh -T git@github.com

# 2. 测试 git pull
cd /opt/law-firm
git pull origin main

# 3. 检查远程 URL
git remote -v
# 应该显示: git@github.com:your-username/law-firm.git (fetch)
#          git@github.com:your-username/law-firm.git (push)
```

---

## ⚠️ 常见问题

### 问题 1: Permission denied (publickey)

**原因**: SSH 密钥未正确配置或未添加到 GitHub

**解决**:
```bash
# 检查 SSH 密钥是否存在
ls -la ~/.ssh/

# 测试 SSH 连接并查看详细信息
ssh -vT git@github.com

# 确保公钥已添加到 GitHub
cat ~/.ssh/id_ed25519_deploy.pub
```

### 问题 2: fatal: could not read Username

**原因**: 使用 HTTPS 但未配置认证

**解决**:
```bash
# 方法 1: 改用 SSH（推荐）
git remote set-url origin git@github.com:your-username/law-firm.git

# 方法 2: 配置 Git 凭据
git config --global credential.helper store
git pull origin main  # 输入用户名和 token
```

### 问题 3: 多个服务器使用同一个密钥

**建议**: 每个服务器使用不同的密钥，便于管理和撤销访问。

### 问题 4: Token 过期

**解决**: 
- 创建新的 token
- 更新 Git 凭据或环境变量
- 或改用 SSH 密钥（不会过期）

---

## 🔄 更新部署脚本

配置完成后，现有的部署脚本就可以正常工作了：

```bash
# 升级代码（保留数据）
cd /opt/law-firm
git pull origin main
./scripts/deploy.sh --quick

# 或一行命令
cd /opt/law-firm && git pull origin main && ./scripts/deploy.sh --quick
```

---

## 📚 相关文档

- [服务器端：升级代码（保留数据）完整指南](./SERVER_UPGRADE_GUIDE.md)
- [服务器端：拉取代码、重置数据、重新部署完整指南](./SERVER_RESET_AND_REDEPLOY.md)
- [GitHub Deploy Keys 文档](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys)
- [GitHub Personal Access Tokens 文档](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token)

---

## 💡 最佳实践

1. **使用 SSH 密钥 + Deploy Key**（只读权限）
   - ✅ 最安全
   - ✅ 不会过期
   - ✅ 可以针对单个仓库
   - ✅ 可以随时撤销

2. **为每个服务器使用不同的密钥**
   - ✅ 便于管理
   - ✅ 出问题时可以单独撤销

3. **定期轮换 Token**（如果使用 PAT）
   - ✅ 提高安全性
   - ⚠️ 需要更新配置

4. **不要在代码中硬编码凭据**
   - ❌ 不要提交 token 到仓库
   - ✅ 使用环境变量或配置文件（已加入 .gitignore）
