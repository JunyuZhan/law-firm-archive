# 档案管理系统 Docker构建文件
# 多阶段构建

# ===== 第一阶段：构建 =====
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 产品版本：优先构建参数 APP_VERSION，否则使用仓库根目录 VERSION（与 frontend、脚本约定一致）
COPY VERSION /tmp/PRODUCT_VERSION
ARG APP_VERSION=
ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public
# 为 CI / 内网环境显式配置 Maven 镜像，避免直接访问 Maven Central 握手失败
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>build-mirror</id>
      <mirrorOf>central</mirrorOf>
      <name>Build Maven Mirror</name>
      <url>${MAVEN_MIRROR_URL}</url>
    </mirror>
  </mirrors>
</settings>
EOF
# 复制源码并直接构建，避免额外的 go-offline 远程握手失败
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN VER="${APP_VERSION:-$(tr -d ' \r\n' < /tmp/PRODUCT_VERSION)}" && \
    mvn -B org.codehaus.mojo:versions-maven-plugin:2.17.1:set -DnewVersion="$VER" -DgenerateBackupPoms=false && \
    mvn package -DskipTests -B

# ===== 第二阶段：运行 =====
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="archive-system"
LABEL description="档案管理系统"

WORKDIR /app

# 安装curl用于健康检查
RUN apk add --no-cache curl

# 创建非root用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制jar包
COPY --from=builder /app/target/*.jar app.jar

# 设置文件权限
RUN chown -R appuser:appgroup /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 8090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/api/actuator/health || exit 1

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
