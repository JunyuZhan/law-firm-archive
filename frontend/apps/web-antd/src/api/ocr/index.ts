/**
 * OCR识别模块 API
 * 提供通用文字识别、银行回单识别、身份证识别、营业执照识别等功能
 */
import { requestClient } from '#/api/request';

// 全局开关：关闭 OCR 功能
const OCR_DISABLED = true;
const ocrDisabledPromise = <T>() =>
  Promise.reject(new Error('OCR 功能已下线，已禁用调用。'));

// ========== 类型定义 ==========

/** OCR识别结果基础类型 */
export interface OcrResultDTO {
  success: boolean;
  errorMessage?: string;
  type?: string;
  typeName?: string;
  rawText?: string;
  data?: Record<string, any>;
  confidence?: number;
  
  // 银行回单字段
  bankName?: string;
  amount?: number;
  transactionDate?: string;
  payerName?: string;
  payerAccount?: string;
  payeeName?: string;
  payeeAccount?: string;
  transactionNo?: string;
  remark?: string;
  
  // 身份证字段
  name?: string;
  idNumber?: string;
  gender?: string;
  ethnicity?: string;
  birthDate?: string;
  address?: string;
  issuingAuthority?: string;
  validFrom?: string;
  validTo?: string;
  
  // 营业执照字段
  companyName?: string;
  creditCode?: string;
  companyType?: string;
  legalRepresentative?: string;
  registeredCapital?: string;
  establishDate?: string;
  businessTerm?: string;
  businessScope?: string;
  registeredAddress?: string;

  // 名片字段
  cardCompany?: string;
  title?: string;
  mobile?: string;
  phone?: string;
  email?: string;
  website?: string;

  // 发票字段
  invoiceType?: string;
  invoiceCode?: string;
  invoiceNo?: string;
  invoiceDate?: string;
  sellerName?: string;
  sellerTaxNo?: string;
  buyerName?: string;
  buyerTaxNo?: string;
  invoiceAmount?: number;
  taxAmount?: number;
  totalAmount?: number;
}

/** 收款匹配候选项 */
export interface MatchCandidateDTO {
  feeId: number;
  feeNo: string;
  feeName: string;
  matterId?: number;
  matterName?: string;
  matterNo?: string;
  clientId?: number;
  clientName?: string;
  expectedAmount: number;
  unpaidAmount: number;
  plannedDate?: string;
  score: number;
  matchReasons: string[];
}

/** 智能对账结果 */
export interface ReconciliationResultDTO {
  candidates: MatchCandidateDTO[];
  recommended?: MatchCandidateDTO;
  hasRecommended: boolean;
  canAutoReconcile: boolean;
}

// ========== OCR识别 API ==========

/**
 * 通用文字识别
 * @param file 图片文件
 */
export function recognizeText(file: File) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<OcrResultDTO>('/ocr/text', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 通用文字识别（通过URL）
 * @param imageUrl 图片URL
 */
export function recognizeTextByUrl(imageUrl: string) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  return requestClient.post<OcrResultDTO>('/ocr/text/url', null, {
    params: { imageUrl },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 银行回单识别
 * @param file 银行回单图片
 */
export function recognizeBankReceipt(file: File) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<OcrResultDTO>('/ocr/bank-receipt', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 银行回单识别（通过URL）
 * @param imageUrl 图片URL
 */
export function recognizeBankReceiptByUrl(imageUrl: string) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  return requestClient.post<OcrResultDTO>('/ocr/bank-receipt/url', null, {
    params: { imageUrl },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 身份证识别
 * @param file 身份证图片
 * @param isFront 是否正面（true=正面，false=背面）
 */
export function recognizeIdCard(file: File, isFront: boolean = true) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<OcrResultDTO>('/ocr/id-card', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    params: { isFront },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 身份证识别（通过URL）
 * @param imageUrl 图片URL
 * @param isFront 是否正面
 */
export function recognizeIdCardByUrl(imageUrl: string, isFront: boolean = true) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  return requestClient.post<OcrResultDTO>('/ocr/id-card/url', null, {
    params: { imageUrl, isFront },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 营业执照识别
 * @param file 营业执照图片
 */
export function recognizeBusinessLicense(file: File) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<OcrResultDTO>('/ocr/business-license', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 营业执照识别（通过URL）
 * @param imageUrl 图片URL
 */
export function recognizeBusinessLicenseByUrl(imageUrl: string) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  return requestClient.post<OcrResultDTO>('/ocr/business-license/url', null, {
    params: { imageUrl },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 名片识别
 * @param file 名片图片
 */
export function recognizeBusinessCard(file: File) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<OcrResultDTO>('/ocr/business-card', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 名片识别（通过URL）
 * @param imageUrl 图片URL
 */
export function recognizeBusinessCardByUrl(imageUrl: string) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  return requestClient.post<OcrResultDTO>('/ocr/business-card/url', null, {
    params: { imageUrl },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 发票/票据识别
 * @param file 发票图片
 */
export function recognizeInvoice(file: File) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  const formData = new FormData();
  formData.append('file', file);
  return requestClient.post<OcrResultDTO>('/ocr/invoice', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

/**
 * 发票/票据识别（通过URL）
 * @param imageUrl 图片URL
 */
export function recognizeInvoiceByUrl(imageUrl: string) {
  if (OCR_DISABLED) return ocrDisabledPromise<OcrResultDTO>();
  return requestClient.post<OcrResultDTO>('/ocr/invoice/url', null, {
    params: { imageUrl },
    timeout: 120000, // OCR识别超时时间120秒
  });
}

// ========== 智能对账 API ==========

/**
 * 根据OCR结果智能匹配待收款
 * @param ocrResult OCR识别结果（银行回单）
 */
export function matchPaymentFromOcr(ocrResult: OcrResultDTO) {
  return requestClient.post<ReconciliationResultDTO>('/finance/payment/match-ocr', {
    amount: ocrResult.amount,
    payerName: ocrResult.payerName,
    transactionDate: ocrResult.transactionDate,
    transactionNo: ocrResult.transactionNo,
  });
}

/**
 * 手动智能匹配待收款
 */
export function matchPayment(params: {
  amount: number;
  payerName?: string;
  transactionDate?: string;
  transactionNo?: string;
}) {
  return requestClient.get<ReconciliationResultDTO>('/finance/fee/reconciliation/match', {
    params: {
      amount: params.amount,
      payerName: params.payerName,
      transactionNo: params.transactionNo,
      paymentDate: params.transactionDate,
    },
  });
}

