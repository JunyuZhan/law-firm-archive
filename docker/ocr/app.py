"""
PaddleOCR服务 - 智慧律所管理系统
提供通用文字识别、银行回单识别、身份证识别、营业执照识别等功能
优化配置：使用轻量级模型，禁用不必要的预处理，确保快速响应
"""
import os
import re
import io
import logging
import time
from typing import Optional
from contextlib import asynccontextmanager
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from PIL import Image
import numpy as np
from paddleocr import PaddleOCR
import httpx

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 全局OCR实例
ocr = None

def init_ocr():
    """初始化轻量级PaddleOCR - 优化速度"""
    global ocr
ocr_lang = os.getenv("OCR_LANG", "ch")
    
    logger.info("🚀 初始化轻量级PaddleOCR...")
    start = time.time()
    
    # 使用轻量级配置，大幅提升速度
    ocr = PaddleOCR(
        use_angle_cls=False,      # 禁用方向分类器（省约30%时间）
        lang=ocr_lang,
        use_gpu=False,
        show_log=False,           # 禁用日志输出
        # 使用移动端轻量模型
        det_model_dir=None,       # 使用默认轻量检测模型
        rec_model_dir=None,       # 使用默认轻量识别模型
        det_db_thresh=0.3,        # 检测阈值
        det_db_box_thresh=0.5,    # 框阈值
        det_db_unclip_ratio=1.6,  # 扩展比例
        rec_batch_num=6,          # 批处理数量
        max_text_length=25,       # 最大文本长度
        use_space_char=True,      # 使用空格字符
    )
    
    # 预热模型 - 用一个小图片触发模型加载
    logger.info("🔥 预热OCR模型...")
    dummy_img = np.zeros((100, 100, 3), dtype=np.uint8)
    dummy_img.fill(255)  # 白色背景
    try:
        ocr.ocr(dummy_img, cls=False)
    except:
        pass  # 忽略预热错误
    
    elapsed = time.time() - start
    logger.info(f"✅ PaddleOCR初始化完成，耗时: {elapsed:.2f}秒")
    return ocr

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理 - 启动时初始化OCR"""
    init_ocr()
    yield
    logger.info("OCR服务关闭")

# 初始化FastAPI（带生命周期管理）
app = FastAPI(title="PaddleOCR Service", version="1.0.0", lifespan=lifespan)


class UrlRequest(BaseModel):
    image_url: str


@app.get("/health")
async def health_check():
    """健康检查"""
    return {"status": "healthy", "service": "paddle-ocr"}


@app.post("/ocr/general")
async def recognize_general(file: UploadFile = File(...)):
    """通用文字识别"""
    try:
        image = await load_image(file)
        result = ocr.ocr(image, cls=False)  # cls=False 跳过方向分类，加速识别
        
        texts = []
        for line in result[0] if result[0] else []:
            text = line[1][0]
            confidence = line[1][1]
            texts.append({"text": text, "confidence": confidence})
        
        return {"success": True, "result": texts}
    except Exception as e:
        logger.error(f"通用文字识别失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ocr/bank_receipt")
async def recognize_bank_receipt(file: UploadFile = File(...)):
    """银行回单识别"""
    try:
        image = await load_image(file)
        result = ocr.ocr(image, cls=False)  # cls=False 跳过方向分类，加速识别
        
        # 提取所有文本
        all_text = []
        for line in result[0] if result[0] else []:
            all_text.append(line[1][0])
        
        full_text = "\n".join(all_text)
        
        # 解析银行回单信息
        parsed = parse_bank_receipt(full_text)
        parsed["raw_text"] = full_text
        parsed["confidence"] = 0.9
        
        return {"success": True, "result": parsed}
    except Exception as e:
        logger.error(f"银行回单识别失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ocr/idcard_front")
async def recognize_idcard_front(file: UploadFile = File(...)):
    """身份证正面识别"""
    try:
        image = await load_image(file)
        logger.info(f"图片尺寸: {image.shape if hasattr(image, 'shape') else 'unknown'}")
        
        result = ocr.ocr(image, cls=False)  # cls=False 跳过方向分类，加速识别
        
        # 检查OCR结果
        if not result or not result[0]:
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
        
        all_text = []
        for line in result[0]:
            if line and len(line) > 1:
                text = line[1][0] if isinstance(line[1], (list, tuple)) else str(line[1])
                all_text.append(text)
        
        full_text = "\n".join(all_text)
        logger.info(f"身份证OCR原始文本 (前500字符): {full_text[:500]}")  # 记录前500字符用于调试
        logger.info(f"身份证OCR识别行数: {len(result[0])}")
        
        parsed = parse_idcard_front(full_text)
        parsed["raw_text"] = full_text  # 添加原始文本
        parsed["confidence"] = 0.95
        
        logger.info(f"身份证OCR解析结果: {parsed}")  # 记录解析结果
        return {"success": True, "result": parsed}
    except Exception as e:
        logger.error(f"身份证正面识别失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ocr/idcard_back")
async def recognize_idcard_back(file: UploadFile = File(...)):
    """身份证背面识别"""
    try:
        image = await load_image(file)
        result = ocr.ocr(image, cls=False)  # cls=False 跳过方向分类，加速识别
        
        all_text = []
        for line in result[0] if result[0] else []:
            all_text.append(line[1][0])
        
        full_text = "\n".join(all_text)
        parsed = parse_idcard_back(full_text)
        parsed["confidence"] = 0.95
        
        return {"success": True, "result": parsed}
    except Exception as e:
        logger.error(f"身份证背面识别失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ocr/business_license")
async def recognize_business_license(file: UploadFile = File(...)):
    """营业执照识别"""
    try:
        image = await load_image(file)
        result = ocr.ocr(image, cls=False)  # cls=False 跳过方向分类，加速识别
        
        all_text = []
        for line in result[0] if result[0] else []:
            all_text.append(line[1][0])
        
        full_text = "\n".join(all_text)
        parsed = parse_business_license(full_text)
        parsed["confidence"] = 0.9
        
        return {"success": True, "result": parsed}
    except Exception as e:
        logger.error(f"营业执照识别失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def load_image(file: UploadFile) -> np.ndarray:
    """加载图片"""
    contents = await file.read()
    image = Image.open(io.BytesIO(contents))
    # 转换为RGB格式（PIL默认可能是RGBA或其他格式）
    if image.mode != 'RGB':
        image = image.convert('RGB')
    # 确保图片是3通道的numpy数组
    img_array = np.array(image)
    # 如果是灰度图（2D），转换为3通道
    if len(img_array.shape) == 2:
        img_array = np.stack([img_array] * 3, axis=-1)
    return img_array


async def load_image_from_url(url: str) -> np.ndarray:
    """从URL加载图片"""
    async with httpx.AsyncClient() as client:
        response = await client.get(url)
        response.raise_for_status()
        image = Image.open(io.BytesIO(response.content))
        # 转换为RGB格式
        if image.mode != 'RGB':
            image = image.convert('RGB')
        img_array = np.array(image)
        # 如果是灰度图（2D），转换为3通道
        if len(img_array.shape) == 2:
            img_array = np.stack([img_array] * 3, axis=-1)
        return img_array



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
    
    # 姓名 - 多种模式匹配
    name_patterns = [
        r"姓名\s*[:：]?\s*([^\n性别]+?)(?:\n|性别|$)",
        r"姓名\s+([^\n]+)",
        r"姓\s*名\s*[:：]?\s*([^\n]+)",
    ]
    for pattern in name_patterns:
        name_match = re.search(pattern, text)
    if name_match:
            name = name_match.group(1).strip()
            # 清理可能的标签文字
            name = re.sub(r'[姓名：:]\s*', '', name).strip()
            if name and len(name) <= 10:  # 姓名通常不超过10个字符
                result["name"] = name
                break
    
    # 如果还没找到，尝试从第一行提取（可能是姓名）
    if not result["name"]:
        lines = text.split('\n')
        for line in lines[:3]:  # 检查前3行
            line = line.strip()
            # 如果这一行看起来像姓名（2-4个中文字符，不包含数字和特殊符号）
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
    
    # 出生日期 - 多种格式
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
    
    # 身份证号 - 更严格的匹配
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
    
    # 签发机关
    authority_match = re.search(r"签发机关\s*(.+?)(?:\n|有效期)", text)
    if authority_match:
        result["issuing_authority"] = authority_match.group(1).strip()
    
    # 有效期
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
    
    # 统一社会信用代码
    credit_match = re.search(r"统一社会信用代码[：:]\s*(\w{18})", text)
    if credit_match:
        result["credit_code"] = credit_match.group(1)
    
    # 企业名称
    name_match = re.search(r"名\s*称[：:]\s*(.+?)(?:\n|类型)", text)
    if name_match:
        result["company_name"] = name_match.group(1).strip()
    
    # 类型
    type_match = re.search(r"类\s*型[：:]\s*(.+?)(?:\n|住所)", text)
    if type_match:
        result["company_type"] = type_match.group(1).strip()
    
    # 法定代表人
    legal_match = re.search(r"法定代表人[：:]\s*(.+?)(?:\n|注册资本)", text)
    if legal_match:
        result["legal_representative"] = legal_match.group(1).strip()
    
    # 注册资本
    capital_match = re.search(r"注册资本[：:]\s*(.+?)(?:\n|成立日期)", text)
    if capital_match:
        result["registered_capital"] = capital_match.group(1).strip()
    
    # 成立日期
    date_match = re.search(r"成立日期[：:]\s*(\d{4}年\d{1,2}月\d{1,2}日)", text)
    if date_match:
        result["establish_date"] = date_match.group(1)
    
    # 营业期限
    term_match = re.search(r"营业期限[：:]\s*(.+?)(?:\n|经营范围)", text)
    if term_match:
        result["business_term"] = term_match.group(1).strip()
    
    # 经营范围
    scope_match = re.search(r"经营范围[：:]\s*(.+?)(?:\n|$)", text, re.DOTALL)
    if scope_match:
        result["business_scope"] = scope_match.group(1).replace("\n", "").strip()
    
    # 住所/地址
    addr_match = re.search(r"住\s*所[：:]\s*(.+?)(?:\n|法定代表人)", text)
    if addr_match:
        result["registered_address"] = addr_match.group(1).strip()
    
    return result


@app.post("/ocr/business_card")
async def recognize_business_card(file: UploadFile = File(...)):
    """名片识别"""
    try:
        image = await load_image(file)
        result = ocr.ocr(image, cls=False)  # cls=False 跳过方向分类，加速识别
        
        all_text = []
        for line in result[0] if result[0] else []:
            all_text.append(line[1][0])
        
        full_text = "\n".join(all_text)
        parsed = parse_business_card(full_text)
        parsed["raw_text"] = full_text
        parsed["confidence"] = 0.9
        
        return {"success": True, "result": parsed}
    except Exception as e:
        logger.error(f"名片识别失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ocr/invoice")
async def recognize_invoice(file: UploadFile = File(...)):
    """发票/票据识别"""
    try:
        image = await load_image(file)
        result = ocr.ocr(image, cls=False)  # cls=False 跳过方向分类，加速识别
        
        all_text = []
        for line in result[0] if result[0] else []:
            all_text.append(line[1][0])
        
        full_text = "\n".join(all_text)
        parsed = parse_invoice(full_text)
        parsed["raw_text"] = full_text
        parsed["confidence"] = 0.9
        
        return {"success": True, "result": parsed}
    except Exception as e:
        logger.error(f"发票识别失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


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
    
    # 手机号 (11位)
    mobile_match = re.search(r'1[3-9]\d{9}', text)
    if mobile_match:
        result["mobile"] = mobile_match.group()
    
    # 固定电话
    phone_match = re.search(r'(\d{3,4}[-\s]?\d{7,8})', text)
    if phone_match:
        result["phone"] = phone_match.group(1)
    
    # 邮箱
    email_match = re.search(r'[\w\.-]+@[\w\.-]+\.\w+', text)
    if email_match:
        result["email"] = email_match.group()
    
    # 网站
    website_match = re.search(r'(www\.[\w\.-]+\.\w+|https?://[\w\.-]+)', text, re.IGNORECASE)
    if website_match:
        result["website"] = website_match.group()
    
    # 地址 (包含省市区关键词的行)
    for line in lines:
        if any(kw in line for kw in ['省', '市', '区', '县', '路', '街', '号', '大厦', '广场']):
            result["address"] = line.strip()
            break
    
    # 公司名称 (包含公司/有限/集团等关键词)
    for line in lines:
        if any(kw in line for kw in ['公司', '有限', '集团', '律师事务所', '律所', '股份']):
            result["company"] = line.strip()
            break
    
    # 职位 (包含职位关键词)
    for line in lines:
        if any(kw in line for kw in ['总', '经理', '主任', '律师', '合伙人', '顾问', '助理', '秘书', '总监', 'CEO', 'CTO', 'CFO']):
            if not result["company"] or line != result["company"]:
                result["title"] = line.strip()
                break
    
    # 姓名 (通常是较短的2-4个汉字，不含数字和符号)
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
    
    # 发票类型
    if "增值税专用发票" in text:
        result["invoice_type"] = "增值税专用发票"
    elif "增值税普通发票" in text:
        result["invoice_type"] = "增值税普通发票"
    elif "电子发票" in text:
        result["invoice_type"] = "电子发票"
    elif "收据" in text:
        result["invoice_type"] = "收据"
    
    # 发票代码
    code_match = re.search(r'发票代码[：:]\s*(\d{10,12})', text)
    if code_match:
        result["invoice_code"] = code_match.group(1)
    
    # 发票号码
    no_match = re.search(r'发票号码[：:]\s*(\d{8})', text)
    if no_match:
        result["invoice_no"] = no_match.group(1)
    
    # 开票日期
    date_patterns = [
        r'开票日期[：:]\s*(\d{4}年\d{1,2}月\d{1,2}日)',
        r'(\d{4}[-/]\d{1,2}[-/]\d{1,2})',
    ]
    for pattern in date_patterns:
        match = re.search(pattern, text)
        if match:
            result["invoice_date"] = match.group(1)
            break
    
    # 金额
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
    
    # 税额
    tax_match = re.search(r'税额[：:]*\s*[¥￥]?([\d,]+\.?\d*)', text)
    if tax_match:
        result["tax_amount"] = tax_match.group(1).replace(",", "")
    
    # 销售方名称
    seller_match = re.search(r'销售方[：:]\s*(.+?)(?:\n|$)', text)
    if not seller_match:
        seller_match = re.search(r'名\s*称[：:]\s*(.+?)(?:\n|税号)', text)
    if seller_match:
        result["seller_name"] = seller_match.group(1).strip()
    
    # 购买方名称
    buyer_match = re.search(r'购买方[：:]\s*(.+?)(?:\n|$)', text)
    if buyer_match:
        result["buyer_name"] = buyer_match.group(1).strip()
    
    # 纳税人识别号
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
