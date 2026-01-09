# OnlyOffice 自定义镜像 - 包含中文字体
FROM onlyoffice/documentserver:latest

LABEL maintainer="Law Firm Management System"
LABEL description="OnlyOffice Document Server with Chinese fonts"

# 复制自定义字体文件（仿宋、楷体、小标宋等法律文档常用字体）
COPY fonts/*.TTF /usr/share/fonts/truetype/custom/
COPY fonts/*.ttf /usr/share/fonts/truetype/custom/

# 安装中文字体包
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

# 生成 OnlyOffice 字体列表
# 注意：这一步在构建时可能会失败，因为服务还没启动，用 || true 忽略错误
# 实际字体生成会在容器首次启动时自动完成
RUN cd /var/www/onlyoffice/documentserver/server/tools && \
    ./allfontsgen || true

# 包含的自定义字体:
# - 仿宋_GB2312.TTF  (法律文书常用)
# - 楷体_GB2312.TTF  (法律文书常用)
# - 方正小标宋简.TTF (公文标题常用)
