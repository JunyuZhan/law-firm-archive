FROM python:3.10-slim-bookworm

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 安装Python依赖
COPY requirements.txt .
# 所有包都从官方源安装（确保ARM架构兼容性）
# PaddleOCR 3.x 需要 paddlepaddle>=3.0.0
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir "paddlepaddle>=3.0.0" -i https://pypi.org/simple && \
    pip install --no-cache-dir "paddleocr>=3.0.0" -i https://pypi.org/simple && \
    pip install --no-cache-dir -r requirements.txt -i https://pypi.org/simple

# 复制应用代码
COPY app.py .

# 创建模型和缓存目录
RUN mkdir -p /app/models /app/cache

# 暴露端口
EXPOSE 8000

# 启动服务
CMD ["python", "app.py"]
