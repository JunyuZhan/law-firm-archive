"""
PaddleOCR服务 - 智慧律所管理系统
基于 PaddleOCR 3.x (PP-OCRv5) 提供通用文字识别、银行回单识别、身份证识别、营业执照识别等功能
优化配置：使用轻量级模型，禁用不必要的预处理，确保快速响应

PaddleOCR 3.x 新特性：
- PP-OCRv5: 单模型支持5种文字类型，准确率提升13%
- 支持简体中文、繁体中文、英文、日文、拼音混合识别

安全特性：
- API Key 鉴权
- 速率限制（IP级别）
- 文件大小限制
- PII 数据脱敏日志
"""
import os
import re
import io
import logging
import time
import hashlib
from typing import Optional, Dict
from collections import defaultdict
from contextlib import asynccontextmanager
from functools import wraps

from fastapi import FastAPI, File, UploadFile, HTTPException, Header, Request, Depends
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from PIL import Image
import numpy as np
from paddleocr import PaddleOCR
import httpx

# ==================== 安全配置 ====================

# API Key（从环境变量获取，生产环境必须设置）
API_KEY = os.getenv("OCR_API_KEY", "")
API_KEY_ENABLED = os.getenv("OCR_API_KEY_ENABLED", "true").lower() == "true"

# 速率限制配置
RATE_LIMIT_REQUESTS = int(os.getenv("OCR_RATE_LIMIT_REQUESTS", "30"))  # 每个时间窗口最大请求数
RATE_LIMIT_WINDOW = int(os.getenv("OCR_RATE_LIMIT_WINDOW", "60"))  # 时间窗口（秒）

# 文件大小限制（字节）
MAX_FILE_SIZE = int(os.getenv("OCR_MAX_FILE_SIZE", str(10 * 1024 * 1024)))  # 默认 10MB

# 允许的图片格式
ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff"}
ALLOWED_CONTENT_TYPES = {
    "image/jpeg", "image/png", "image/gif", "image/bmp", 
    "image/webp", "image/tiff", "application/octet-stream"
}

# ==================== 日志配置（PII 脱敏） ====================

class PIISanitizingFormatter(logging.Formatter):
    """PII 数据脱敏日志格式化器"""
    
    # 敏感数据正则表达式
    PATTERNS = [
        # 身份证号（18位）
        (re.compile(r'\b(\d{6})\d{8}(\d{4})\b'), r'\1********\2'),
        # 手机号（11位）
        (re.compile(r'\b(1[3-9]\d)\d{4}(\d{4})\b'), r'\1****\2'),
        # 银行卡号（16-19位）
        (re.compile(r'\b(\d{4})\d{8,12}(\d{4})\b'), r'\1********\2'),
        # 邮箱
        (re.compile(r'\b(\w{1,3})\w*@(\w+\.\w+)\b'), r'\1***@\2'),
        # 姓名（2-4个中文字符）- 保留姓，隐藏名
        (re.compile(r'姓名[：:]\s*([\u4e00-\u9fa5])([\u4e00-\u9fa5]{1,3})'), r'姓名：\1**'),
    ]
    
    def format(self, record):
        message = super().format(record)
        for pattern, replacement in self.PATTERNS:
            message = pattern.sub(replacement, message)
        return message

# 配置脱敏日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 替换默认处理器为脱敏处理器
for handler in logger.handlers:
    handler.setFormatter(PIISanitizingFormatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    ))

# 如果没有处理器，添加一个
if not logger.handlers:
    handler = logging.StreamHandler()
    handler.setFormatter(PIISanitizingFormatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    ))
    logger.addHandler(handler)

# ==================== 速率限制 ====================

class RateLimiter:
    """基于内存的 IP 速率限制器"""
    
    def __init__(self, max_requests: int, window_seconds: int):
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self.requests: Dict[str, list] = defaultdict(list)
    
    def is_allowed(self, client_ip: str) -> bool:
        """检查是否允许请求"""
        now = time.time()
        window_start = now - self.window_seconds
        
        # 清理过期记录
        self.requests[client_ip] = [
            req_time for req_time in self.requests[client_ip]
            if req_time > window_start
        ]
        
        # 检查是否超限
        if len(self.requests[client_ip]) >= self.max_requests:
            return False
        
        # 记录本次请求
        self.requests[client_ip].append(now)
        return True
    
    def get_remaining(self, client_ip: str) -> int:
        """获取剩余请求数"""
        now = time.time()
        window_start = now - self.window_seconds
        current_requests = len([
            req_time for req_time in self.requests[client_ip]
            if req_time > window_start
        ])
        return max(0, self.max_requests - current_requests)

rate_limiter = RateLimiter(RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW)

# ==================== 安全依赖 ====================

def get_client_ip(request: Request) -> str:
    """获取客户端 IP"""
    # 优先使用 X-Forwarded-For（如果通过代理）
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        return forwarded.split(",")[0].strip()
    # 否则使用直接连接 IP
    return request.client.host if request.client else "unknown"

async def verify_api_key(
    request: Request,
    x_api_key: Optional[str] = Header(None, alias="X-API-Key")
):
    """验证 API Key"""
    if not API_KEY_ENABLED:
        return True
    
    if not API_KEY:
        logger.warning("⚠️ OCR_API_KEY 未配置，API Key 验证已禁用")
        return True
    
    if not x_api_key:
        logger.warning(f"缺少 API Key，IP: {get_client_ip(request)}")
        raise HTTPException(
            status_code=401,
            detail="缺少 API Key，请在请求头中添加 X-API-Key"
        )
    
    # 使用常量时间比较防止时序攻击
    if not hmac_compare(x_api_key, API_KEY):
        logger.warning(f"无效的 API Key，IP: {get_client_ip(request)}")
        raise HTTPException(
            status_code=401,
            detail="无效的 API Key"
        )
    
    return True

def hmac_compare(a: str, b: str) -> bool:
    """常量时间字符串比较，防止时序攻击"""
    if len(a) != len(b):
        return False
    result = 0
    for x, y in zip(a.encode(), b.encode()):
        result |= x ^ y
    return result == 0

async def check_rate_limit(request: Request):
    """检查速率限制"""
    client_ip = get_client_ip(request)
    
    if not rate_limiter.is_allowed(client_ip):
        remaining = rate_limiter.get_remaining(client_ip)
        logger.warning(f"速率限制触发，IP: {client_ip}")
        raise HTTPException(
            status_code=429,
            detail=f"请求过于频繁，请在 {RATE_LIMIT_WINDOW} 秒后重试",
            headers={
                "X-RateLimit-Limit": str(RATE_LIMIT_REQUESTS),
                "X-RateLimit-Remaining": str(remaining),
                "X-RateLimit-Reset": str(RATE_LIMIT_WINDOW),
                "Retry-After": str(RATE_LIMIT_WINDOW)
            }
        )

async def validate_file(file: UploadFile):
    """验证上传文件"""
    # 检查文件名
    if not file.filename:
        raise HTTPException(status_code=400, detail="文件名不能为空")
    
    # 检查文件扩展名
    ext = file.filename.rsplit(".", 1)[-1].lower() if "." in file.filename else ""
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的文件格式: {ext}，允许的格式: {', '.join(ALLOWED_EXTENSIONS)}"
        )
    
    # 检查 Content-Type
    if file.content_type and file.content_type not in ALLOWED_CONTENT_TYPES:
        logger.warning(f"可疑的 Content-Type: {file.content_type}")
    
    # 检查文件大小
    contents = await file.read()
    if len(contents) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=413,
            detail=f"文件过大，最大允许 {MAX_FILE_SIZE // (1024*1024)}MB"
        )
    
    # 重置文件指针
    await file.seek(0)
    
    return contents

# ==================== OCR 初始化 ====================

ocr = None

def init_ocr():
    """初始化 PaddleOCR 3.x (PP-OCRv5) - 优化速度"""
    global ocr
    ocr_lang = os.getenv("OCR_LANG", "ch")
    
    logger.info("🚀 初始化 PaddleOCR 3.x (PP-OCRv5)...")
    start = time.time()
    
    # PaddleOCR 3.x 配置
    # 参考: https://paddlepaddle.github.io/PaddleOCR/main/en/quick_start.html
    # PP-OCRv5 支持：简体中文、繁体中文、英文、日文、拼音混合识别
    ocr = PaddleOCR(
        lang=ocr_lang,
        use_doc_orientation_classify=False,  # 禁用文档方向分类（加速）
        use_doc_unwarping=False,             # 禁用文档矫正（加速）
        use_textline_orientation=False,      # 禁用文本行方向检测（加速）
    )
    
    # 预热模型（使用一个小图片进行预热）
    logger.info("🔥 预热 PP-OCRv5 模型...")
    dummy_img = np.zeros((100, 100, 3), dtype=np.uint8)
    dummy_img.fill(255)
    try:
        list(ocr.predict(dummy_img))  # 消费迭代器以完成预热
    except Exception as e:
        logger.warning(f"预热失败（可忽略）: {e}")
    
    elapsed = time.time() - start
    logger.info(f"✅ PaddleOCR 3.x (PP-OCRv5) 初始化完成，耗时: {elapsed:.2f}秒")
    
    # 安全配置日志
    logger.info(f"🔐 安全配置:")
    logger.info(f"   - API Key 验证: {'启用' if API_KEY_ENABLED and API_KEY else '禁用'}")
    logger.info(f"   - 速率限制: {RATE_LIMIT_REQUESTS} 请求/{RATE_LIMIT_WINDOW}秒")
    logger.info(f"   - 最大文件大小: {MAX_FILE_SIZE // (1024*1024)}MB")
    
    return ocr

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    init_ocr()
    yield
    logger.info("OCR服务关闭")

# ==================== FastAPI 应用 ====================

app = FastAPI(
    title="PaddleOCR Service",
    version="3.0.0",
    description="基于 PaddleOCR 3.x (PP-OCRv5) 的安全加固版 OCR 服务",
    lifespan=lifespan
)

# CORS 配置（生产环境应限制允许的源）
ALLOWED_ORIGINS = os.getenv("OCR_ALLOWED_ORIGINS", "*").split(",")
app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

class UrlRequest(BaseModel):
    image_url: str

# ==================== API 端点 ====================

@app.get("/health")
async def health_check():
    """健康检查（不需要认证）"""
    return {
        "status": "healthy",
        "service": "paddle-ocr",
        "version": "3.0.0",
        "engine": "PaddleOCR 3.x (PP-OCRv5)",
        "security": {
            "api_key_enabled": API_KEY_ENABLED and bool(API_KEY),
            "rate_limit": f"{RATE_LIMIT_REQUESTS}/{RATE_LIMIT_WINDOW}s"
        }
    }


@app.post("/ocr/general", dependencies=[Depends(verify_api_key), Depends(check_rate_limit)])
async def recognize_general(request: Request, file: UploadFile = File(...)):
    """通用文字识别"""
    try:
        await validate_file(file)
        image = await load_image(file)
        # PaddleOCR 3.x 使用 predict() 方法
        result = ocr.predict(image)
        _, detailed = extract_ocr_text(result)
        
        logger.info(f"通用OCR完成，识别 {len(detailed)} 行文本，IP: {get_client_ip(request)}")
        return {"success": True, "result": detailed}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"通用文字识别失败: {e}")
        raise HTTPException(status_code=500, detail="OCR 识别失败")


@app.post("/ocr/bank_receipt", dependencies=[Depends(verify_api_key), Depends(check_rate_limit)])
async def recognize_bank_receipt(request: Request, file: UploadFile = File(...)):
    """银行回单识别"""
    try:
        await validate_file(file)
        image = await load_image(file)
        result = ocr.predict(image)
        
        all_text, _ = extract_ocr_text(result)
        full_text = "\n".join(all_text)
        parsed = parse_bank_receipt(full_text)
        parsed["raw_text"] = full_text
        parsed["confidence"] = 0.9
        
        logger.info(f"银行回单识别完成，IP: {get_client_ip(request)}")
        return {"success": True, "result": parsed}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"银行回单识别失败: {e}")
        raise HTTPException(status_code=500, detail="银行回单识别失败")


@app.post("/ocr/idcard_front", dependencies=[Depends(verify_api_key), Depends(check_rate_limit)])
async def recognize_idcard_front(request: Request, file: UploadFile = File(...)):
    """身份证正面识别"""
    try:
        await validate_file(file)
        image = await load_image(file)
        logger.info(f"处理身份证识别请求，图片尺寸: {image.shape}, IP: {get_client_ip(request)}")
        
        result = ocr.predict(image)
        all_text, detailed = extract_ocr_text(result)
        
        if not all_text:
            logger.warning("OCR识别结果为空")
            return {
                "success": True,
                "result": {
                    "name": "",
                    "gender": "",
                    "ethnicity": "",
                    "birth_date": "",
                    "address": "",
                    "id_number": "",
                    "raw_text": "",
                    "confidence": 0.0
                }
            }
        
        full_text = "\n".join(all_text)
        logger.info(f"身份证OCR识别行数: {len(all_text)}")
        
        parsed = parse_idcard_front(full_text)
        parsed["raw_text"] = full_text
        parsed["confidence"] = 0.95
        
        # 脱敏日志
        logger.info(f"身份证识别完成，姓名: {parsed.get('name', '')[:1]}**, IP: {get_client_ip(request)}")
        return {"success": True, "result": parsed}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"身份证正面识别失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="身份证识别失败")


@app.post("/ocr/idcard_back", dependencies=[Depends(verify_api_key), Depends(check_rate_limit)])
async def recognize_idcard_back(request: Request, file: UploadFile = File(...)):
    """身份证背面识别"""
    try:
        await validate_file(file)
        image = await load_image(file)
        result = ocr.predict(image)
        
        all_text, _ = extract_ocr_text(result)
        full_text = "\n".join(all_text)
        parsed = parse_idcard_back(full_text)
        parsed["confidence"] = 0.95
        
        logger.info(f"身份证背面识别完成，IP: {get_client_ip(request)}")
        return {"success": True, "result": parsed}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"身份证背面识别失败: {e}")
        raise HTTPException(status_code=500, detail="身份证背面识别失败")


@app.post("/ocr/business_license", dependencies=[Depends(verify_api_key), Depends(check_rate_limit)])
async def recognize_business_license(request: Request, file: UploadFile = File(...)):
    """营业执照识别"""
    try:
        await validate_file(file)
        image = await load_image(file)
        result = ocr.predict(image)
        
        all_text, _ = extract_ocr_text(result)
        full_text = "\n".join(all_text)
        parsed = parse_business_license(full_text)
        parsed["confidence"] = 0.9
        
        logger.info(f"营业执照识别完成，IP: {get_client_ip(request)}")
        return {"success": True, "result": parsed}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"营业执照识别失败: {e}")
        raise HTTPException(status_code=500, detail="营业执照识别失败")


@app.post("/ocr/business_card", dependencies=[Depends(verify_api_key), Depends(check_rate_limit)])
async def recognize_business_card(request: Request, file: UploadFile = File(...)):
    """名片识别"""
    try:
        await validate_file(file)
        image = await load_image(file)
        result = ocr.predict(image)
        
        all_text, _ = extract_ocr_text(result)
        full_text = "\n".join(all_text)
        parsed = parse_business_card(full_text)
        parsed["raw_text"] = full_text
        parsed["confidence"] = 0.9
        
        logger.info(f"名片识别完成，IP: {get_client_ip(request)}")
        return {"success": True, "result": parsed}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"名片识别失败: {e}")
        raise HTTPException(status_code=500, detail="名片识别失败")


@app.post("/ocr/invoice", dependencies=[Depends(verify_api_key), Depends(check_rate_limit)])
async def recognize_invoice(request: Request, file: UploadFile = File(...)):
    """发票/票据识别"""
    try:
        await validate_file(file)
        image = await load_image(file)
        result = ocr.predict(image)
        
        all_text, _ = extract_ocr_text(result)
        full_text = "\n".join(all_text)
        parsed = parse_invoice(full_text)
        parsed["raw_text"] = full_text
        parsed["confidence"] = 0.9
        
        logger.info(f"发票识别完成，IP: {get_client_ip(request)}")
        return {"success": True, "result": parsed}
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"发票识别失败: {e}")
        raise HTTPException(status_code=500, detail="发票识别失败")


# ==================== 辅助函数 ====================

def extract_ocr_text(result) -> tuple[list[str], list[dict]]:
    """
    从 PaddleOCR 3.x 结果中提取文本
    参考: https://paddlepaddle.github.io/PaddleOCR/v3.0.3/en/version3.x/pipeline_usage/OCR.html
    
    PaddleOCR 3.x 结果对象是字典类型，包含:
    - rec_texts: 识别的文本列表
    - rec_scores: 置信度分数列表
    - dt_polys: 检测框坐标
    
    返回: (文本列表, 详细结果列表)
    """
    all_text = []
    detailed_results = []
    
    for res in result:
        # PaddleOCR 3.x 结果对象是字典类型
        texts = res.get('rec_texts', []) or []
        scores = res.get('rec_scores', []) or []
        
        for i, text in enumerate(texts):
            if text:  # 跳过空文本
                score = float(scores[i]) if i < len(scores) else 0.9
                all_text.append(str(text))
                detailed_results.append({"text": str(text), "confidence": score})
    
    return all_text, detailed_results


async def load_image(file: UploadFile) -> np.ndarray:
    """加载图片"""
    contents = await file.read()
    image = Image.open(io.BytesIO(contents))
    if image.mode != 'RGB':
        image = image.convert('RGB')
    img_array = np.array(image)
    if len(img_array.shape) == 2:
        img_array = np.stack([img_array] * 3, axis=-1)
    return img_array


async def load_image_from_url(url: str) -> np.ndarray:
    """从URL加载图片"""
    async with httpx.AsyncClient(timeout=30.0) as client:
        response = await client.get(url)
        response.raise_for_status()
        image = Image.open(io.BytesIO(response.content))
        if image.mode != 'RGB':
            image = image.convert('RGB')
        img_array = np.array(image)
        if len(img_array.shape) == 2:
            img_array = np.stack([img_array] * 3, axis=-1)
        return img_array


# ==================== 解析函数 ====================

def parse_bank_receipt(text: str) -> dict:
    """解析银行回单"""
    result = {
        "bank_name": "",
        "amount": "",
        "transaction_date": "",
        "payer_name": "",
        "payer_account": "",
        "payee_name": "",
        "payee_account": "",
        "transaction_no": "",
        "remark": ""
    }
    
    # 银行名称
    bank_patterns = [
        r"(中国工商银行|中国建设银行|中国农业银行|中国银行|交通银行|招商银行|中信银行|浦发银行|民生银行|兴业银行|光大银行|华夏银行|平安银行|广发银行|北京银行|上海银行)",
    ]
    for pattern in bank_patterns:
        match = re.search(pattern, text)
        if match:
            result["bank_name"] = match.group(1)
            break
    
    # 金额
    amount_patterns = [
        r"金额[：:]\s*([\d,]+\.?\d*)",
        r"人民币[：:]?\s*([\d,]+\.?\d*)",
        r"([\d,]+\.?\d*)\s*元",
    ]
    for pattern in amount_patterns:
        match = re.search(pattern, text)
        if match:
            result["amount"] = match.group(1).replace(",", "")
            break
    
    # 交易日期
    date_patterns = [
        r"(\d{4}[-/年]\d{1,2}[-/月]\d{1,2}日?)",
        r"交易日期[：:]\s*(\d{4}\d{2}\d{2})",
    ]
    for pattern in date_patterns:
        match = re.search(pattern, text)
        if match:
            result["transaction_date"] = match.group(1)
            break
    
    # 付款人
    payer_patterns = [
        r"付款人[：:]\s*(.+?)(?:\n|$)",
        r"付款单位[：:]\s*(.+?)(?:\n|$)",
    ]
    for pattern in payer_patterns:
        match = re.search(pattern, text)
        if match:
            result["payer_name"] = match.group(1).strip()
            break
    
    # 收款人
    payee_patterns = [
        r"收款人[：:]\s*(.+?)(?:\n|$)",
        r"收款单位[：:]\s*(.+?)(?:\n|$)",
    ]
    for pattern in payee_patterns:
        match = re.search(pattern, text)
        if match:
            result["payee_name"] = match.group(1).strip()
            break
    
    # 账号
    account_pattern = r"(\d{16,19})"
    accounts = re.findall(account_pattern, text)
    if len(accounts) >= 2:
        result["payer_account"] = accounts[0]
        result["payee_account"] = accounts[1]
    elif len(accounts) == 1:
        result["payee_account"] = accounts[0]
    
    # 流水号
    trans_patterns = [
        r"流水号[：:]\s*(\w+)",
        r"交易流水[：:]\s*(\w+)",
        r"凭证号[：:]\s*(\w+)",
    ]
    for pattern in trans_patterns:
        match = re.search(pattern, text)
        if match:
            result["transaction_no"] = match.group(1)
            break
    
    # 摘要/备注
    remark_patterns = [
        r"摘要[：:]\s*(.+?)(?:\n|$)",
        r"备注[：:]\s*(.+?)(?:\n|$)",
        r"用途[：:]\s*(.+?)(?:\n|$)",
    ]
    for pattern in remark_patterns:
        match = re.search(pattern, text)
        if match:
            result["remark"] = match.group(1).strip()
            break
    
    return result


def parse_idcard_front(text: str) -> dict:
    """解析身份证正面"""
    result = {
        "name": "",
        "gender": "",
        "ethnicity": "",
        "birth_date": "",
        "address": "",
        "id_number": ""
    }
    
    # 姓名
    name_patterns = [
        r"姓名\s*[:：]?\s*([^\n性别]+?)(?:\n|性别|$)",
        r"姓名\s+([^\n]+)",
        r"姓\s*名\s*[:：]?\s*([^\n]+)",
    ]
    for pattern in name_patterns:
        name_match = re.search(pattern, text)
        if name_match:
            name = name_match.group(1).strip()
            name = re.sub(r'[姓名：:]\s*', '', name).strip()
            if name and len(name) <= 10:
                result["name"] = name
                break
    
    if not result["name"]:
        lines = text.split('\n')
        for line in lines[:3]:
            line = line.strip()
            if re.match(r'^[\u4e00-\u9fa5]{2,4}$', line):
                result["name"] = line
                break
    
    # 性别
    if "男" in text or "M" in text.upper():
        result["gender"] = "男"
    elif "女" in text or "F" in text.upper():
        result["gender"] = "女"
    
    # 民族
    ethnicity_patterns = [
        r"民族\s*[:：]?\s*([^\n]+)",
        r"民\s*族\s*[:：]?\s*([^\n]+)",
    ]
    for pattern in ethnicity_patterns:
        ethnicity_match = re.search(pattern, text)
        if ethnicity_match:
            result["ethnicity"] = ethnicity_match.group(1).strip()
            break
    
    # 出生日期
    birth_patterns = [
        r"(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日",
        r"(\d{4})[/-](\d{1,2})[/-](\d{1,2})",
        r"(\d{4})(\d{2})(\d{2})",
    ]
    for pattern in birth_patterns:
        birth_match = re.search(pattern, text)
        if birth_match:
            year, month, day = birth_match.groups()
            result["birth_date"] = f"{year}-{month.zfill(2)}-{day.zfill(2)}"
            break
    
    # 住址
    addr_patterns = [
        r"住址\s*[:：]?\s*(.+?)(?:公民身份号码|身份证号|$)",
        r"住\s*址\s*[:：]?\s*(.+?)(?:公民身份号码|身份证号|$)",
    ]
    for pattern in addr_patterns:
        addr_match = re.search(pattern, text, re.DOTALL)
        if addr_match:
            result["address"] = addr_match.group(1).replace("\n", " ").strip()
            break
    
    # 身份证号
    id_patterns = [
        r"(\d{17}[\dXx])",
        r"身份证号\s*[:：]?\s*(\d{17}[\dXx])",
        r"公民身份号码\s*[:：]?\s*(\d{17}[\dXx])",
    ]
    for pattern in id_patterns:
        id_match = re.search(pattern, text)
        if id_match:
            result["id_number"] = id_match.group(1).upper()
            break
    
    return result


def parse_idcard_back(text: str) -> dict:
    """解析身份证背面"""
    result = {
        "issuing_authority": "",
        "valid_from": "",
        "valid_to": ""
    }
    
    authority_match = re.search(r"签发机关\s*(.+?)(?:\n|有效期)", text)
    if authority_match:
        result["issuing_authority"] = authority_match.group(1).strip()
    
    valid_match = re.search(r"(\d{4})\.(\d{2})\.(\d{2})\s*[-—]\s*(\d{4})\.(\d{2})\.(\d{2}|长期)", text)
    if valid_match:
        result["valid_from"] = f"{valid_match.group(1)}-{valid_match.group(2)}-{valid_match.group(3)}"
        if valid_match.group(6) == "长期":
            result["valid_to"] = "长期"
        else:
            result["valid_to"] = f"{valid_match.group(4)}-{valid_match.group(5)}-{valid_match.group(6)}"
    
    return result


def parse_business_license(text: str) -> dict:
    """解析营业执照"""
    result = {
        "company_name": "",
        "credit_code": "",
        "company_type": "",
        "legal_representative": "",
        "registered_capital": "",
        "establish_date": "",
        "business_term": "",
        "business_scope": "",
        "registered_address": ""
    }
    
    credit_match = re.search(r"统一社会信用代码[：:]\s*(\w{18})", text)
    if credit_match:
        result["credit_code"] = credit_match.group(1)
    
    name_match = re.search(r"名\s*称[：:]\s*(.+?)(?:\n|类型)", text)
    if name_match:
        result["company_name"] = name_match.group(1).strip()
    
    type_match = re.search(r"类\s*型[：:]\s*(.+?)(?:\n|住所)", text)
    if type_match:
        result["company_type"] = type_match.group(1).strip()
    
    legal_match = re.search(r"法定代表人[：:]\s*(.+?)(?:\n|注册资本)", text)
    if legal_match:
        result["legal_representative"] = legal_match.group(1).strip()
    
    capital_match = re.search(r"注册资本[：:]\s*(.+?)(?:\n|成立日期)", text)
    if capital_match:
        result["registered_capital"] = capital_match.group(1).strip()
    
    date_match = re.search(r"成立日期[：:]\s*(\d{4}年\d{1,2}月\d{1,2}日)", text)
    if date_match:
        result["establish_date"] = date_match.group(1)
    
    term_match = re.search(r"营业期限[：:]\s*(.+?)(?:\n|经营范围)", text)
    if term_match:
        result["business_term"] = term_match.group(1).strip()
    
    scope_match = re.search(r"经营范围[：:]\s*(.+?)(?:\n|$)", text, re.DOTALL)
    if scope_match:
        result["business_scope"] = scope_match.group(1).replace("\n", "").strip()
    
    addr_match = re.search(r"住\s*所[：:]\s*(.+?)(?:\n|法定代表人)", text)
    if addr_match:
        result["registered_address"] = addr_match.group(1).strip()
    
    return result


def parse_business_card(text: str) -> dict:
    """解析名片"""
    result = {
        "name": "",
        "company": "",
        "title": "",
        "phone": "",
        "mobile": "",
        "email": "",
        "address": "",
        "website": ""
    }
    
    lines = text.split('\n')
    
    mobile_match = re.search(r'1[3-9]\d{9}', text)
    if mobile_match:
        result["mobile"] = mobile_match.group()
    
    phone_match = re.search(r'(\d{3,4}[-\s]?\d{7,8})', text)
    if phone_match:
        result["phone"] = phone_match.group(1)
    
    email_match = re.search(r'[\w\.-]+@[\w\.-]+\.\w+', text)
    if email_match:
        result["email"] = email_match.group()
    
    website_match = re.search(r'(www\.[\w\.-]+\.\w+|https?://[\w\.-]+)', text, re.IGNORECASE)
    if website_match:
        result["website"] = website_match.group()
    
    for line in lines:
        if any(kw in line for kw in ['省', '市', '区', '县', '路', '街', '号', '大厦', '广场']):
            result["address"] = line.strip()
            break
    
    for line in lines:
        if any(kw in line for kw in ['公司', '有限', '集团', '律师事务所', '律所', '股份']):
            result["company"] = line.strip()
            break
    
    for line in lines:
        if any(kw in line for kw in ['总', '经理', '主任', '律师', '合伙人', '顾问', '助理', '秘书', '总监', 'CEO', 'CTO', 'CFO']):
            if not result["company"] or line != result["company"]:
                result["title"] = line.strip()
                break
    
    for line in lines:
        line = line.strip()
        if 2 <= len(line) <= 4 and re.match(r'^[\u4e00-\u9fa5]+$', line):
            if line != result.get("company") and line != result.get("title"):
                result["name"] = line
                break
    
    return result


def parse_invoice(text: str) -> dict:
    """解析发票/票据"""
    result = {
        "invoice_type": "",
        "invoice_code": "",
        "invoice_no": "",
        "invoice_date": "",
        "seller_name": "",
        "seller_tax_no": "",
        "buyer_name": "",
        "buyer_tax_no": "",
        "amount": "",
        "tax_amount": "",
        "total_amount": "",
        "items": []
    }
    
    if "增值税专用发票" in text:
        result["invoice_type"] = "增值税专用发票"
    elif "增值税普通发票" in text:
        result["invoice_type"] = "增值税普通发票"
    elif "电子发票" in text:
        result["invoice_type"] = "电子发票"
    elif "收据" in text:
        result["invoice_type"] = "收据"
    
    code_match = re.search(r'发票代码[：:]\s*(\d{10,12})', text)
    if code_match:
        result["invoice_code"] = code_match.group(1)
    
    no_match = re.search(r'发票号码[：:]\s*(\d{8})', text)
    if no_match:
        result["invoice_no"] = no_match.group(1)
    
    date_patterns = [
        r'开票日期[：:]\s*(\d{4}年\d{1,2}月\d{1,2}日)',
        r'(\d{4}[-/]\d{1,2}[-/]\d{1,2})',
    ]
    for pattern in date_patterns:
        match = re.search(pattern, text)
        if match:
            result["invoice_date"] = match.group(1)
            break
    
    amount_patterns = [
        r'合计金额[：:]*\s*[¥￥]?([\d,]+\.?\d*)',
        r'金额合计[：:]*\s*[¥￥]?([\d,]+\.?\d*)',
        r'小写[：:]*\s*[¥￥]?([\d,]+\.?\d*)',
    ]
    for pattern in amount_patterns:
        match = re.search(pattern, text)
        if match:
            result["total_amount"] = match.group(1).replace(",", "")
            break
    
    tax_match = re.search(r'税额[：:]*\s*[¥￥]?([\d,]+\.?\d*)', text)
    if tax_match:
        result["tax_amount"] = tax_match.group(1).replace(",", "")
    
    seller_match = re.search(r'销售方[：:]\s*(.+?)(?:\n|$)', text)
    if not seller_match:
        seller_match = re.search(r'名\s*称[：:]\s*(.+?)(?:\n|税号)', text)
    if seller_match:
        result["seller_name"] = seller_match.group(1).strip()
    
    buyer_match = re.search(r'购买方[：:]\s*(.+?)(?:\n|$)', text)
    if buyer_match:
        result["buyer_name"] = buyer_match.group(1).strip()
    
    tax_no_pattern = r'纳税人识别号[：:]\s*(\w{15,20})'
    tax_nos = re.findall(tax_no_pattern, text)
    if len(tax_nos) >= 2:
        result["buyer_tax_no"] = tax_nos[0]
        result["seller_tax_no"] = tax_nos[1]
    elif len(tax_nos) == 1:
        result["seller_tax_no"] = tax_nos[0]
    
    return result


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
