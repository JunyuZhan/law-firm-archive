FROM node:22-slim AS builder

# 构建参数（从 docker-compose 传入）
ARG DOCS_USERNAME=admin
ARG DOCS_PASSWORD=lawfirm@2026

# --max-old-space-size
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
ENV NODE_OPTIONS=--max-old-space-size=8192
ENV TZ=Asia/Shanghai

# 文档站点构建时的环境变量
ENV VITE_DOCS_USERNAME=$DOCS_USERNAME
ENV VITE_DOCS_PASSWORD=$DOCS_PASSWORD

# 安装 git（文档构建需要 git changelog 插件）
RUN apt-get update && apt-get install -y git && rm -rf /var/lib/apt/lists/*

RUN npm i -g corepack

WORKDIR /app

# copy package.json and pnpm-lock.yaml to workspace
COPY . /app

# 初始化 git 仓库并创建初始提交（文档 changelog 插件需要）
RUN git init && \
    git config user.email "build@docker" && \
    git config user.name "Docker Build" && \
    git add -A && \
    git commit -m "Docker build"

# 安装依赖
RUN --mount=type=cache,id=pnpm,target=/pnpm/store pnpm install --no-frozen-lockfile

# 构建主前端应用
RUN pnpm run build:antd

# 构建文档站点
RUN pnpm run build:docs

RUN echo "Builder Success 🎉"

# 使用 nginx unprivileged 镜像（非 root 运行，安全最佳实践）
FROM nginxinc/nginx-unprivileged:alpine AS production

# 切换到 root 用户进行初始化配置
USER root

# 安装必要工具
RUN apk add --no-cache sed

# 配置 nginx
RUN echo "types { application/javascript js mjs; }" > /etc/nginx/conf.d/mjs.conf \
    && rm -rf /etc/nginx/conf.d/default.conf

# 创建 SSL 目录并设置权限（允许 nginx 用户访问）
RUN mkdir -p /etc/nginx/ssl && \
    chown -R nginx:nginx /etc/nginx/ssl && \
    chmod 755 /etc/nginx/ssl

# 复制主前端构建产物
COPY --from=builder /app/apps/web-antd/dist /usr/share/nginx/html

# 复制文档站点构建产物
COPY --from=builder /app/docs/.vitepress/dist /usr/share/nginx/html/docs

# 设置静态文件权限
RUN chown -R nginx:nginx /usr/share/nginx/html

# 复制 nginx 配置（SSL 版本和纯 HTTP 版本）
COPY --from=builder /app/scripts/deploy/nginx-ssl.conf /etc/nginx/nginx-ssl.conf
COPY --from=builder /app/scripts/deploy/nginx.conf /etc/nginx/nginx-http.conf

# 默认使用 SSL 配置（启动脚本会根据证书情况选择）
COPY --from=builder /app/scripts/deploy/nginx-ssl.conf /etc/nginx/nginx.conf

# 设置配置文件权限
RUN chown -R nginx:nginx /etc/nginx/*.conf /etc/nginx/nginx-*.conf

# 复制启动脚本（自动处理 SSL 证书）
COPY --from=builder /app/scripts/deploy/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# 切换回 nginx 用户（非 root 运行）
USER nginx

EXPOSE 8080 8443

# 使用启动脚本（自动生成自签名证书如果不存在）
ENTRYPOINT ["/docker-entrypoint.sh"]
