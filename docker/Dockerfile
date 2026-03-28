# 档案管理系统 Docker构建文件
# 多阶段构建

# ===== 第一阶段：构建 =====
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 复制pom.xml并下载依赖（利用Docker缓存层）
COPY backend/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY backend/src ./src
RUN mvn package -DskipTests -B

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
