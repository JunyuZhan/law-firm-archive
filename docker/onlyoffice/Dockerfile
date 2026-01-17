# OnlyOffice 自定义镜像 - 包含中文字体
FROM onlyoffice/documentserver:latest

LABEL maintainer="Law Firm Management System"
LABEL description="OnlyOffice Document Server with Chinese fonts"

# 安装中文字体包和依赖
RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends \
        fonts-wqy-zenhei \
        fonts-wqy-microhei \
        fonts-noto-cjk \
        fontconfig \
        curl \
        jq && \
    # 清理缓存减少镜像大小
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 创建自定义字体目录
RUN mkdir -p /usr/share/fonts/truetype/custom

# 复制自定义字体文件（仿宋、楷体、小标宋等法律文档常用字体）
COPY fonts/*.TTF /usr/share/fonts/truetype/custom/ 2>/dev/null || true
COPY fonts/*.ttf /usr/share/fonts/truetype/custom/ 2>/dev/null || true

# 设置字体文件权限
RUN chmod 644 /usr/share/fonts/truetype/custom/*.{TTF,ttf} 2>/dev/null || true && \
    # 刷新字体缓存
    fc-cache -fv

# 复制启动脚本
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# 复制字体注册脚本
COPY register-fonts.sh /app/register-fonts.sh
RUN chmod +x /app/register-fonts.sh

# 使用自定义 entrypoint（在后台运行字体注册）
ENTRYPOINT ["/app/entrypoint.sh"]

# 包含的自定义字体:
# - 仿宋_GB2312.TTF  (法律文书常用)
# - 楷体_GB2312.TTF  (法律文书常用)
# - 方正小标宋简.TTF (公文标题常用)
