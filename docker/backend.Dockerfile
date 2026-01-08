# 智慧律所管理系统 - 后端 Dockerfile
# 多阶段构建：构建 + 运行

# ============================================
# 阶段1：构建
# ============================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制 pom.xml 先下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -Pprod -DskipTests -B

RUN echo "Build Success 🎉"

# ============================================
# 阶段2：运行
# ============================================
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="lawfirm"
LABEL description="智慧律所管理系统后端服务"

# 设置时区
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 创建非 root 用户运行应用
RUN addgroup -S lawfirm && adduser -S lawfirm -G lawfirm
USER lawfirm

WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 优化参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

