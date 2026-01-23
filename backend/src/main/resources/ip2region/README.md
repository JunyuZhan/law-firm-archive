# IP 地理位置数据库

异地登录检测功能需要 [ip2region](https://github.com/lionsoul2014/ip2region) 数据库文件。

## 下载方式

请从以下地址下载数据库文件并放置到本目录：

**IPv4 数据库**（推荐）：
- GitHub: https://github.com/lionsoul2014/ip2region/blob/master/data/ip2region_v4.xdb

**下载命令**：

```bash
# 进入项目目录
cd backend/src/main/resources/ip2region

# 方式1：从 GitHub Releases 下载（推荐，更稳定）
# 访问 https://github.com/lionsoul2014/ip2region/releases 下载最新版

# 方式2：使用 wget（需要科学上网）
wget -O ip2region.xdb "https://github.com/lionsoul2014/ip2region/raw/master/data/ip2region_v4.xdb"

# 方式3：从 Gitee 镜像下载（国内推荐）
wget -O ip2region.xdb "https://gitee.com/lionsoul/ip2region/raw/master/data/ip2region_v4.xdb"
```

## 文件说明

| 文件名 | 说明 | 大小 |
|-------|------|------|
| `ip2region.xdb` | 旧版文件名（兼容） | ~11MB |
| `ip2region_v4.xdb` | 新版 IPv4 数据库 | ~11MB |
| `ip2region_v6.xdb` | IPv6 数据库（可选） | 较小 |

系统会自动识别 `ip2region.xdb` 或 `ip2region_v4.xdb`。

## 注意事项

- 如果没有此文件，异地登录检测功能将自动禁用，不影响正常登录
- 建议定期更新此数据库以保持 IP 地址解析的准确性
- 数据格式：`国家|区域|省份|城市|ISP`，例如 `中国|0|北京|北京市|联通`
