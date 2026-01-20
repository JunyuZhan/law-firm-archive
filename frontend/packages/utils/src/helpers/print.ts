/**
 * 打印工具函数
 * 提供符合国务院标准公文格式（GB/T 9704-2012）的打印功能
 *
 * 格式说明：
 * - 对外文书（起诉状、代理词、公函、介绍信等）：使用标准公文格式（16pt仿宋体）
 * - 内部文档（合同、卷宗等）：使用4号或小4号字体（14pt或12pt）
 */

/**
 * 对外文书类型列表（需要使用标准公文格式）
 */
export const EXTERNAL_DOCUMENT_TYPES = [
  '起诉状',
  '答辩状',
  '上诉状',
  '法律意见书',
  '律师函',
  '代理词',
  '辩护词',
  '公函',
  '介绍信',
  '函件',
  '申请书',
  '申诉状',
  '再审申请书',
];

/**
 * 判断是否为对外文书
 */
export function isExternalDocument(documentType?: string): boolean {
  if (!documentType) return false;
  return EXTERNAL_DOCUMENT_TYPES.some(
    (type) => documentType.includes(type) || type.includes(documentType),
  );
}

/**
 * 解码 HTML 实体
 * 用于修复 XSS 拦截导致的 HTML 实体转义问题（如 &lt;、&gt;、&quot; 等）
 * @param text 需要解码的文本
 * @returns 解码后的文本
 */
function decodeHtmlEntities(text: string): string {
  if (!text) return text;

  // 常见 HTML 实体映射
  const entities: Record<string, string> = {
    '&quot;': '"',
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&apos;': "'",
    '&#39;': "'",
    '&#x27;': "'",
    '&nbsp;': ' ',
  };

  let result = text;
  for (const [entity, char] of Object.entries(entities)) {
    result = result.split(entity).join(char);
  }

  // 处理数字实体 &#xxx;
  result = result.replace(/&#(\d+);/g, (_, num) =>
    String.fromCharCode(parseInt(num, 10)),
  );
  // 处理十六进制实体 &#xXXX;
  result = result.replace(/&#x([0-9a-fA-F]+);/g, (_, hex) =>
    String.fromCharCode(parseInt(hex, 16)),
  );

  return result;
}

/**
 * 国务院标准公文格式样式（对外文书）
 * 根据《党政机关公文格式》国家标准（GB/T 9704-2012）
 */
export const OFFICIAL_DOCUMENT_STYLES = `
  @page { 
    size: A4;
    margin-top: 3.7cm;
    margin-bottom: 3.5cm;
    margin-left: 2.8cm;
    margin-right: 2.6cm;
  }
  body { 
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
    font-size: 16pt; 
    line-height: 28pt;
    color: #000;
    padding: 0;
    margin: 0;
  }
  * {
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
  }
  h1, h2, h3 { 
    text-align: center; 
    font-family: "FZXiaoBiaoSong-B05S", "方正小标宋", "FZXBS", serif;
    font-size: 22pt;
    font-weight: normal;
    letter-spacing: 2pt;
    margin: 20pt 0 10pt;
  }
  p { 
    text-indent: 2em; 
    margin: 0; 
    padding: 0;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
    font-size: 16pt;
    line-height: 28pt;
  }
  .no-indent {
    text-indent: 0;
  }
  .signature { 
    text-align: right; 
    margin-top: 40pt; 
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
    font-size: 16pt;
    line-height: 28pt;
  }
  .header {
    text-align: center;
    margin-bottom: 30pt;
  }
  .title {
    font-size: 22pt;
    font-weight: normal;
    font-family: "FZXiaoBiaoSong-B05S", "方正小标宋", "FZXBS", serif;
    letter-spacing: 2pt;
    margin-bottom: 10pt;
  }
  .letter-no {
    margin-top: 10pt;
    font-size: 16pt;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
  }
  .recipient {
    margin-bottom: 20pt;
    font-size: 16pt;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
    line-height: 28pt;
  }
  .content {
    text-indent: 2em;
    text-align: justify;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
    font-size: 16pt;
    line-height: 28pt;
  }
  .content p {
    margin: 0;
    padding: 0;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
    font-size: 16pt;
    line-height: 28pt;
    text-indent: 2em;
  }
  .footer {
    margin-top: 40pt;
    text-align: right;
    padding-right: 0;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
  }
  .footer-item {
    margin-bottom: 8pt;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
    font-size: 16pt;
    line-height: 28pt;
  }
  .seal-area {
    margin-top: 20pt;
    font-size: 16pt;
    color: #666;
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif;
  }
  .letter-content {
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif !important;
    font-size: 16pt !important;
    line-height: 28pt !important;
    color: #000;
  }
  .letter-content *:not(style) {
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif !important;
  }
  .letter-content p {
    font-size: 16pt !important;
    line-height: 28pt !important;
    text-indent: 2em !important;
    margin: 0 !important;
    padding: 0 !important;
  }
  .letter-content h2 {
    font-family: "FZXiaoBiaoSong-B05S", "方正小标宋", "FZXBS", serif !important;
    font-size: 22pt !important;
    font-weight: normal !important;
    letter-spacing: 2pt !important;
    text-align: center !important;
    margin: 20pt 0 10pt !important;
  }
  .letter-content div[style*="text-align: center"] {
    text-align: center !important;
  }
  .letter-content div[style*="text-align: right"] {
    text-align: right !important;
  }
  .qr-code-area {
    font-family: "FangSong", "仿宋_GB2312", "仿宋", serif !important;
  }
  @media print {
    body { padding: 0; margin: 0; }
  }
`;

/**
 * 内部文档格式样式（4号字体，14pt）
 * 适用于合同、卷宗等内部文档
 */
export const INTERNAL_DOCUMENT_STYLES_SIZE_4 = `
  @page { 
    size: A4;
    margin: 2cm;
  }
  body { 
    font-family: "SimSun", "宋体", serif; 
    font-size: 14pt; 
    line-height: 1.8;
    color: #000;
    padding: 20px;
    margin: 0;
  }
  * {
    font-family: "SimSun", "宋体", serif;
  }
  h1, h2, h3 { 
    text-align: center; 
    font-family: "SimHei", "黑体", sans-serif;
    font-size: 18pt;
    font-weight: bold;
    margin: 20pt 0 10pt;
  }
  p { 
    margin: 0; 
    padding: 0;
    font-family: "SimSun", "宋体", serif; 
    font-size: 14pt;
    line-height: 1.8;
  }
  .content {
    text-align: justify;
    font-family: "SimSun", "宋体", serif;
    font-size: 14pt;
    line-height: 1.8;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 15px 0;
  }
  table th,
  table td {
    border: 1px solid #333;
    padding: 8px;
    font-size: 14pt;
  }
  table th {
    background-color: #f5f5f5;
    font-weight: bold;
    text-align: center;
  }
  @media print {
    body { padding: 20px; margin: 0; }
  }
`;

/**
 * 内部文档格式样式（小4号字体，12pt）
 * 适用于表格、清单等需要更多内容的内部文档
 */
export const INTERNAL_DOCUMENT_STYLES_SIZE_SMALL_4 = `
  @page { 
    size: A4;
    margin: 1.5cm;
  }
  body { 
    font-family: "SimSun", "宋体", serif; 
    font-size: 12pt; 
    line-height: 1.6;
    color: #000;
    padding: 15px;
    margin: 0;
  }
  * {
    font-family: "SimSun", "宋体", serif;
  }
  h1, h2, h3 { 
    text-align: center; 
    font-family: "SimHei", "黑体", sans-serif;
    font-size: 16pt;
    font-weight: bold;
    margin: 15pt 0 8pt;
  }
  p { 
    margin: 0; 
    padding: 0;
    font-family: "SimSun", "宋体", serif; 
    font-size: 12pt;
    line-height: 1.6;
  }
  .content {
    text-align: justify;
    font-family: "SimSun", "宋体", serif;
    font-size: 12pt;
    line-height: 1.6;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    margin: 10px 0;
  }
  table th,
  table td {
    border: 1px solid #333;
    padding: 6px;
    font-size: 12pt;
  }
  table th {
    background-color: #f5f5f5;
    font-weight: bold;
    text-align: center;
  }
  @media print {
    body { padding: 15px; margin: 0; }
  }
`;

/**
 * 打印选项
 */
export interface PrintOptions {
  /** 文档标题 */
  title?: string;
  /** HTML 内容 */
  content: string;
  /** 是否使用标准公文格式（默认 true） */
  useOfficialFormat?: boolean;
  /** 内部文档字体大小：'size4' (14pt) 或 'small4' (12pt)，默认 'size4' */
  internalFontSize?: 'size4' | 'small4';
  /** 自定义样式（会与标准样式合并） */
  customStyles?: string;
}

/**
 * 使用标准公文格式打印文档
 * @param options 打印选项
 */
export function printOfficialDocument(options: PrintOptions): void {
  const {
    title = '文档打印',
    content,
    useOfficialFormat = true,
    internalFontSize = 'size4',
    customStyles = '',
  } = options;

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    throw new Error('无法打开打印窗口，请检查浏览器弹窗设置');
  }

  let styles = '';
  if (useOfficialFormat) {
    styles = OFFICIAL_DOCUMENT_STYLES;
  } else {
    // 内部文档格式
    styles =
      internalFontSize === 'small4'
        ? INTERNAL_DOCUMENT_STYLES_SIZE_SMALL_4
        : INTERNAL_DOCUMENT_STYLES_SIZE_4;
  }

  if (customStyles) {
    styles += customStyles;
  }

  const html = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>${title}</title>
      <style>
        ${styles}
      </style>
    </head>
    <body>
      ${content}
    </body>
    </html>
  `;

  printWindow.document.write(html);
  printWindow.document.close();

  // 使用标志位防止重复打印
  let hasPrinted = false;
  let printTimer: null | ReturnType<typeof setTimeout> = null;
  let retryTimer: null | ReturnType<typeof setTimeout> = null;

  const doPrint = () => {
    if (!hasPrinted && printWindow && !printWindow.closed) {
      hasPrinted = true;
      // 清理所有定时器
      if (printTimer) clearTimeout(printTimer);
      if (retryTimer) clearTimeout(retryTimer);
      printWindow.print();
    }
  };

  // 清理函数
  const cleanup = () => {
    hasPrinted = true;
    if (printTimer) clearTimeout(printTimer);
    if (retryTimer) clearTimeout(retryTimer);
  };

  // 等待内容加载完成后触发打印
  printWindow.addEventListener('load', () => {
    printTimer = setTimeout(doPrint, 250);
  });

  // 兼容处理：如果onload没有触发，使用readyState检查
  printTimer = setTimeout(() => {
    if (!hasPrinted && printWindow && !printWindow.closed) {
      if (printWindow.document.readyState === 'complete') {
        doPrint();
      } else {
        // 如果还没加载完成，再等待一下
        retryTimer = setTimeout(() => {
          if (!hasPrinted && printWindow && !printWindow.closed) {
            doPrint();
          }
        }, 200);
      }
    }
  }, 500);

  // 监听窗口关闭事件，清理资源
  printWindow.addEventListener('beforeunload', cleanup);

  // 监听窗口焦点事件（用户取消打印后窗口会重新获得焦点）
  let focusHandler: (() => void) | null = null;
  focusHandler = () => {
    // 如果已经打印过，用户取消后重新获得焦点，不再打印
    if (hasPrinted) {
      cleanup();
      if (focusHandler) {
        printWindow.removeEventListener('focus', focusHandler);
      }
    }
  };
  printWindow.addEventListener('focus', focusHandler);
}

/**
 * 生成标准公文格式的 HTML 内容（用于出函）
 */
export interface LetterPrintData {
  /** 函件类型名称 */
  letterTypeName: string;
  /** 申请编号 */
  applicationNo: string;
  /** 接收单位 */
  targetUnit: string;
  /** 接收单位联系人 */
  targetContact?: string;
  /** 接收单位联系电话 */
  targetPhone?: string;
  /** 接收单位地址 */
  targetAddress?: string;
  /** 函件内容 */
  content: string;
  /** 出函律师 */
  lawyerNames?: string;
  /** 律所名称 */
  firmName: string;
  /** 日期（格式：YYYY年MM月DD日） */
  date: string;
  /** 验证二维码（Base64编码，可选） */
  qrCodeBase64?: string;
}

/**
 * 生成出函的 HTML 内容
 */
export function generateLetterHtml(data: LetterPrintData): string {
  // 二维码HTML（如果有）
  const qrCodeHtml = data.qrCodeBase64
    ? `
    <div class="qr-code-area" style="text-align: center; margin-top: 30pt; padding-top: 20pt; border-top: 1px dashed #999;">
      <div style="font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 14pt; color: #000; margin-bottom: 8pt; line-height: 20pt;">扫描二维码验证函件真伪</div>
      <img src="${data.qrCodeBase64}" alt="验证二维码" style="width: 80pt; height: 80pt; display: inline-block;" />
      <div style="font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 12pt; color: #000; margin-top: 6pt; line-height: 18pt;">编号：${data.applicationNo}</div>
    </div>
    `
    : '';

  // record.content 是后端已经替换了模板变量的完整HTML内容
  // 模板中已经包含了：标题、编号、接收单位、正文、页脚（律师、律所、日期）
  // 模板内容本身已经包含了内联样式，符合公文格式
  // 我们直接使用 content，只添加二维码即可
  // printOfficialDocument 函数会应用 @page 规则（页边距）和全局样式
  // 注意：模板内容中的内联样式会覆盖部分CSS样式，但页边距等 @page 规则会生效
  if (data.content && data.content.trim()) {
    // 在内容末尾添加二维码
    // 模板内容通常是完整的HTML，二维码应该添加在最后
    // 确保内容被包裹在 body 中，以便应用公文格式样式
    return `<div class="letter-content">${data.content}${qrCodeHtml}</div>`;
  }

  // 备用格式（如果 content 为空，使用标准公文格式结构）
  return `
    <div class="header">
      <div class="title">${data.letterTypeName || '介绍信'}</div>
      <div class="letter-no">编号：${data.applicationNo}</div>
    </div>
    <div class="recipient">${data.targetUnit}：</div>
    <div class="content">${data.content || ''}</div>
    <div class="footer">
      <div class="footer-item">出函律师：${data.lawyerNames || '-'}</div>
      <div class="footer-item" style="margin-top: 20pt;">${data.firmName}</div>
      <div class="footer-item">${data.date}</div>
      <div class="seal-area">（盖章处）</div>
      ${qrCodeHtml}
    </div>
  `;
}

/**
 * 打印出函
 */
export function printLetter(data: LetterPrintData): void {
  const content = generateLetterHtml(data);
  printOfficialDocument({
    title: `${data.letterTypeName} - ${data.applicationNo}`,
    content,
    useOfficialFormat: true,
  });
}

/**
 * 生成文书文档的 HTML 内容
 */
export interface DocumentPrintData {
  /** 文档标题 */
  title?: string;
  /** 文档内容（HTML 或纯文本） */
  content: string;
  /** 文档类型（用于判断是否为对外文书） */
  documentType?: string;
  /** 是否保留原始格式 */
  preserveFormat?: boolean;
  /** 强制使用内部文档格式（忽略文档类型判断） */
  forceInternal?: boolean;
  /** 内部文档字体大小 */
  internalFontSize?: 'size4' | 'small4';
}

/**
 * 简单的 Markdown 转纯文本（用于打印，避免循环依赖）
 */
function simpleMarkdownToPlainText(text: string): string {
  if (!text) return '';
  
  let result = text;
  
  // 移除代码块
  result = result.replace(/```[\s\S]*?```/g, '');
  
  // 移除行内代码
  result = result.replace(/`([^`]+)`/g, '$1');
  
  // 移除粗体
  result = result.replace(/\*\*([^*]+)\*\*/g, '$1');
  result = result.replace(/__([^_]+)__/g, '$1');
  
  // 移除斜体
  result = result.replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '$1');
  result = result.replace(/(?<!_)_([^_]+)_(?!_)/g, '$1');
  
  // 移除链接
  result = result.replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1');
  
  // 移除标题标记
  result = result.replace(/^#{1,6}\s+/gm, '');
  
  // 移除列表标记
  result = result.replace(/^[\s]*[-*+]\s+/gm, '');
  result = result.replace(/^[\s]*\d+\.\s+/gm, '');
  
  // 移除表格标记
  result = result.replace(/\|/g, ' ');
  result = result.replace(/^[\s]*[-:]+[\s]*$/gm, '');
  
  // 清理多余空行
  result = result.replace(/\n{3,}/g, '\n\n');
  
  return result.trim();
}

/**
 * 生成文书文档的 HTML 内容
 */
export function generateDocumentHtml(data: DocumentPrintData): string {
  const { title, content, preserveFormat = false } = data;

  // 先解码 HTML 实体（防止 XSS 过滤导致的乱码）
  let decodedContent = decodeHtmlEntities(content);
  let decodedTitle = title ? decodeHtmlEntities(title) : '';

  // 如果是 Markdown 格式，转换为纯文本（移除所有 Markdown 语法）
  // 检查是否包含 Markdown 语法标记
  const hasMarkdownSyntax = /(\*\*|__|`|#|\[.*\]\(|>|\|)/.test(decodedContent);
  if (hasMarkdownSyntax && !preserveFormat) {
    decodedContent = simpleMarkdownToPlainText(decodedContent);
  }

  let htmlContent = '';
  if (decodedTitle) {
    htmlContent += `<div class="header"><div class="title">${decodedTitle}</div></div>`;
  }

  if (preserveFormat) {
    // 保留原始格式（使用 pre 标签）
    htmlContent += `<div class="content" style="white-space: pre-wrap; font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif;">${decodedContent}</div>`;
  } else {
    // 标准格式：将换行转换为段落
    const paragraphs = decodedContent.split('\n').filter((p) => p.trim());
    htmlContent += '<div class="content">';
    paragraphs.forEach((para, index) => {
      // 第一段可能需要特殊处理（如不需要缩进）
      const indentClass = index === 0 ? 'no-indent' : '';
      htmlContent += `<p class="${indentClass}">${para}</p>`;
    });
    htmlContent += '</div>';
  }

  return htmlContent;
}

/**
 * 打印文书文档
 * 根据文档类型自动选择格式：
 * - 对外文书（起诉状、代理词、公函等）：标准公文格式
 * - 内部文档（合同、卷宗等）：4号或小4号字体
 */
export function printDocument(data: DocumentPrintData): void {
  const content = generateDocumentHtml(data);

  // 判断是否为对外文书
  const isExternal =
    !data.forceInternal && isExternalDocument(data.documentType);

  printOfficialDocument({
    title: data.title || '文书打印',
    content,
    useOfficialFormat: isExternal,
    internalFontSize: data.internalFontSize || 'size4',
  });
}
