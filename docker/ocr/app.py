"""
PaddleOCR服务 - 智慧律所管理系统
提供通用文字识别、银行回单识别、身份证识别、营业执照识别等功能
"""
import os
import re
import io
import logging
from typing import Optional
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

# 初始化FastAPI
app = FastAPI(title="PaddleOCR Service", version="1.0.0")

# 初始化PaddleOCR
ocr_lang = os.getenv("OCR_LANG", "ch")
use_gpu = os.getenv("OCR_USE_GPU", "false").lower() == "true"

logger.info(f"初始化PaddleOCR: lang={ocr_lang}, use_gpu={use_gpu}")
ocr = PaddleOCR(use_angle_cls=True, lang=ocr_lang, use_gpu=use_gpu, show_log=False)


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
        result = ocr.ocr(image, cls=True)
        
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
        result = ocr.ocr(image, cls=True)
        
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
        result = ocr.ocr(image, cls=True)
        
        all_text = []
        for line in result[0] if result[0] else []:
            all_text.append(line[1][0])
        
        full_text = "\n".join(all_text)
        parsed = parse_idcard_front(full_text)
        parsed["confidence"] = 0.95
        
        return {"success": True, "result": parsed}
    except Exception as e:
        logger.error(f"身份证正面识别失败: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/ocr/idcard_back")
async def recognize_idcard_back(file: UploadFile = File(...)):
    """身份证背面识别"""
    try:
        image = await load_image(file)
        result = ocr.ocr(image, cls=True)
        
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
        result = ocr.ocr(image, cls=True)
        
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
    return np.array(image)


async def load_image_from_url(url: str) -> np.ndarray:
    """从URL加载图片"""
    async with httpx.AsyncClient() as client:
        response = await client.get(url)
        response.raise_for_status()
        image = Image.open(io.BytesIO(response.content))
        return np.array(image)



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
    name_match = re.search(r"姓名\s*(.+?)(?:\n|性别)", text)
    if name_match:
        result["name"] = name_match.group(1).strip()
    
    # 性别
    if "男" in text:
        result["gender"] = "男"
    elif "女" in text:
        result["gender"] = "女"
    
    # 民族
    ethnicity_match = re.search(r"民族\s*(\S+)", text)
    if ethnicity_match:
        result["ethnicity"] = ethnicity_match.group(1).strip()
    
    # 出生日期
    birth_match = re.search(r"(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日", text)
    if birth_match:
        result["birth_date"] = f"{birth_match.group(1)}-{birth_match.group(2).zfill(2)}-{birth_match.group(3).zfill(2)}"
    
    # 住址
    addr_match = re.search(r"住址\s*(.+?)(?:公民身份号码|$)", text, re.DOTALL)
    if addr_match:
        result["address"] = addr_match.group(1).replace("\n", "").strip()
    
    # 身份证号
    id_match = re.search(r"(\d{17}[\dXx])", text)
    if id_match:
        result["id_number"] = id_match.group(1).upper()
    
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


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
