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
        jq \
        net-tools && \
    # 清理缓存减少镜像大小
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 修复 sharp 包缓存目录权限问题
# 这是 OnlyOffice 9.x 版本的已知问题
RUN mkdir -p /var/www/onlyoffice/documentserver/.cache && \
    chown -R ds:ds /var/www/onlyoffice/documentserver/.cache && \
    chmod -R 755 /var/www/onlyoffice/documentserver/.cache

# 创建自定义字体目录
RUN mkdir -p /usr/share/fonts/truetype/custom

# 复制自定义字体文件（仿宋、楷体、小标宋等法律文档常用字体）
# 注意：fonts/ 目录下需要有字体文件，否则跳过
COPY fonts/ /tmp/custom-fonts/

# 移动字体文件并设置权限
RUN if ls /tmp/custom-fonts/*.TTF 1>/dev/null 2>&1; then \
        cp /tmp/custom-fonts/*.TTF /usr/share/fonts/truetype/custom/; \
    fi && \
    if ls /tmp/custom-fonts/*.ttf 1>/dev/null 2>&1; then \
        cp /tmp/custom-fonts/*.ttf /usr/share/fonts/truetype/custom/; \
    fi && \
    chmod 644 /usr/share/fonts/truetype/custom/* 2>/dev/null || true && \
    rm -rf /tmp/custom-fonts && \
    # 刷新字体缓存
    fc-cache -fv

# 复制启动脚本
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# 复制并应用 CORS 配置（开发环境跨域支持）
COPY nginx-cors.conf /tmp/nginx-cors.conf

# 创建 CORS 配置的包含脚本，在 nginx 配置中添加 CORS 头
RUN echo '# Enable CORS for development environment' > /etc/onlyoffice/documentserver/nginx/includes/ds-cors.conf && \
    echo 'add_header Access-Control-Allow-Origin * always;' >> /etc/onlyoffice/documentserver/nginx/includes/ds-cors.conf && \
    echo 'add_header Access-Control-Allow-Methods "GET, POST, OPTIONS, PUT, DELETE" always;' >> /etc/onlyoffice/documentserver/nginx/includes/ds-cors.conf && \
    echo 'add_header Access-Control-Allow-Headers "Authorization, Content-Type, X-Requested-With" always;' >> /etc/onlyoffice/documentserver/nginx/includes/ds-cors.conf

# 复制字体注册脚本
COPY register-fonts.sh /app/register-fonts.sh
RUN chmod +x /app/register-fonts.sh

# 使用自定义 entrypoint（在后台运行字体注册）
ENTRYPOINT ["/app/entrypoint.sh"]

# 包含的自定义字体:
# - 仿宋_GB2312.TTF  (法律文书常用)
# - 楷体_GB2312.TTF  (法律文书常用)
# - 方正小标宋简.TTF (公文标题常用)
