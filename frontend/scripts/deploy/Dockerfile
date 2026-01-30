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

FROM nginx:stable-alpine AS production

# 安装 openssl（用于生成自签名证书）
RUN apk add --no-cache openssl

# 配置 nginx
RUN echo "types { application/javascript js mjs; }" > /etc/nginx/conf.d/mjs.conf \
    && rm -rf /etc/nginx/conf.d/default.conf

# 创建 SSL 目录
RUN mkdir -p /etc/nginx/ssl

# 复制主前端构建产物
COPY --from=builder /app/apps/web-antd/dist /usr/share/nginx/html

# 复制文档站点构建产物
COPY --from=builder /app/docs/.vitepress/dist /usr/share/nginx/html/docs

# 复制 nginx 配置（使用 SSL 版本）
COPY --from=builder /app/scripts/deploy/nginx-ssl.conf /etc/nginx/nginx.conf

# 复制启动脚本（自动处理 SSL 证书）
COPY --from=builder /app/scripts/deploy/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 8080 8443

# 使用启动脚本（自动生成自签名证书如果不存在）
ENTRYPOINT ["/docker-entrypoint.sh"]
