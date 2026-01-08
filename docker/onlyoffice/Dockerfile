# OnlyOffice 自定义镜像 - 包含中文字体
FROM onlyoffice/documentserver:latest

LABEL maintainer="Law Firm Management System"
LABEL description="OnlyOffice Document Server with Chinese fonts"

# 复制自定义字体文件（如仿宋、楷体等）
COPY fonts/ /usr/share/fonts/truetype/custom/

# 安装中文字体
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends \
        fonts-wqy-zenhei \
        fonts-wqy-microhei \
        fonts-noto-cjk \
        fontconfig && \
    # 清理缓存减少镜像大小
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    # 刷新字体缓存
    fc-cache -fv

# 生成 OnlyOffice 字体列表（在容器启动时会自动执行，这里预生成加速启动）
# 注意：这一步在构建时可能会失败，因为服务还没启动，所以用 || true 忽略错误
RUN cd /var/www/onlyoffice/documentserver/server/tools && \
    ./allfontsgen || true

