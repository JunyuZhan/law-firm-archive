/**
 * 函件打印格式化工具
 * 将结构化模板内容转换为排版好的打印 HTML
 */

export interface LetterTemplateBlocks {
  title: {
    letterNo: string;
    letterTitle: string;
  };
  recipient: string;
  body: string;
  signature: {
    contactInfo: string;
    date: string;
    firmName: string;
    lawyerNames: string;
  };
}

export interface StructuredLetterContent {
  _structured: boolean;
  _version: number;
  blocks: LetterTemplateBlocks;
}

/**
 * 解码 HTML 实体
 */
export function decodeHtmlEntities(text: string): string {
  if (!text) return text;

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
  result = result.replaceAll(/&#(\d+);/g, (_, num) =>
    String.fromCodePoint(Number.parseInt(num, 10)),
  );
  // 处理十六进制实体 &#xXXX;
  result = result.replaceAll(/&#x([0-9a-fA-F]+);/g, (_, hex) =>
    String.fromCodePoint(Number.parseInt(hex, 16)),
  );

  return result;
}

/**
 * 检查内容是否为结构化格式
 */
export function isStructuredLetterContent(content: string): boolean {
  if (!content || typeof content !== 'string') {
    return false;
  }

  const decoded = decodeHtmlEntities(content);

  try {
    const parsed = JSON.parse(decoded);
    if (parsed && typeof parsed === 'object' && parsed._structured === true) {
      return true;
    }
  } catch {
    // 继续尝试其他方式
  }

  try {
    const trimmed = decoded.trim();
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      const parsed = JSON.parse(trimmed);
      if (parsed && typeof parsed === 'object' && parsed._structured === true) {
        return true;
      }
    }
  } catch {
    // 忽略
  }

  return false;
}

/**
 * 解析结构化内容
 */
export function parseStructuredLetterContent(
  content: string,
): null | StructuredLetterContent {
  if (!content || typeof content !== 'string') {
    return null;
  }

  const decoded = decodeHtmlEntities(content);

  try {
    const parsed = JSON.parse(decoded);
    if (parsed && typeof parsed === 'object' && parsed._structured === true) {
      return parsed as StructuredLetterContent;
    }
  } catch {
    // 继续尝试其他方式
  }

  try {
    const trimmed = decoded.trim();
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      const parsed = JSON.parse(trimmed);
      if (parsed && typeof parsed === 'object' && parsed._structured === true) {
        return parsed as StructuredLetterContent;
      }
    }
  } catch {
    // 忽略
  }

  return null;
}

/**
 * 将文本中的换行转为 HTML 段落（公文格式：三号仿宋GB2312）
 */
function textToHtmlParagraphs(text: string, indent: boolean = true): string {
  if (!text) return '';

  const lines = text.split('\n').filter((line) => line.trim());
  return lines
    .map((line) => {
      const trimmed = line.trim();
      // 公文格式：三号仿宋GB2312，行距28pt
      return `<p style="font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt; ${indent ? 'text-indent: 2em;' : ''} margin: 4px 0;">${trimmed}</p>`;
    })
    .join('\n');
}

/**
 * 格式化结构化函件模板为打印 HTML
 */
export function formatStructuredLetterForPrint(
  content: string,
  variables: Record<string, string> = {},
): string {
  if (!content || content.trim() === '') {
    return '<p>模板内容为空</p>';
  }

  const structured = parseStructuredLetterContent(content);

  if (!structured) {
    // 如果不是结构化内容，直接返回（可能是HTML格式）
    return content;
  }

  const { blocks } = structured;

  if (!blocks || typeof blocks !== 'object') {
    return '<p style="color: red;">⚠️ 模板内容格式错误：缺少 blocks 字段</p>';
  }

  // 替换变量
  const replaceVars = (text: string): string => {
    if (!text) return '';
    let result = text;
    Object.entries(variables).forEach(([key, value]) => {
      const escapedKey = key.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
      result = result.replaceAll(
        new RegExp(String.raw`\$\{${escapedKey}\}`, 'g'),
        value || `[${key}]`,
      );
    });
    return result;
  };

  // 构建打印 HTML
  let html = '';

  // 1. 标题区（居中显示）- 公文格式：二号方正小标宋/仿宋
  if (blocks.title && blocks.title.letterTitle) {
    const letterTitle = String(blocks.title.letterTitle || '').trim();
    if (letterTitle) {
      html += `<div style="text-align: center; margin-bottom: 30px;">
  <h2 style="text-align: center; font-family: 'FZXiaoBiaoSong-B05S', '方正小标宋简体', '方正小标宋', 'FZXBS', 'FangSong', '仿宋', serif; font-size: 22pt; font-weight: normal; letter-spacing: 2pt; margin: 0 0 10px 0;">${replaceVars(letterTitle)}</h2>\n`;
      html += `</div>\n`;
    }
  }

  // 编号（如果有）- 公文格式：四号仿宋GB2312，右对齐
  if (blocks.title && blocks.title.letterNo) {
    const letterNo = String(blocks.title.letterNo || '').trim();
    if (letterNo) {
      html += `<p style="text-align: right; font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 14pt; line-height: 24pt; margin: 0 0 20px 0;">${replaceVars(letterNo)}</p>\n`;
    }
  }

  // 2. 收件单位（左对齐）- 公文格式：三号仿宋GB2312
  if (blocks.recipient) {
    const recipient = String(blocks.recipient || '').trim();
    if (recipient) {
      html += `<p style="text-indent: 0; margin-bottom: 20px; font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt;"><strong>${replaceVars(recipient)}</strong></p>\n`;
    }
  }

  // 3. 正文内容（段落缩进）
  if (blocks.body) {
    const body = String(blocks.body || '').trim();
    if (body) {
      html += `<div style="margin: 20px 0;">\n`;
      html += textToHtmlParagraphs(replaceVars(body), true);
      html += `</div>\n`;
    }
  }

  // 4. "此致"（如果有）
  // 注意：如果正文末尾已经有"此致"，这里不需要重复添加
  // 可以在模板正文中自行包含"此致"

  // 5. 落款区（右对齐）- 公文格式：三号仿宋GB2312
  if (blocks.signature) {
    html += `<div style="text-align: right; margin-top: 40px; font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt;">\n`;

    if (blocks.signature.lawyerNames) {
      const lawyerNames = String(blocks.signature.lawyerNames || '').trim();
      if (lawyerNames) {
        html += `  <p style="text-indent: 0; margin: 0 0 8px 0;">${replaceVars(lawyerNames)}</p>\n`;
      }
    }

    if (blocks.signature.contactInfo) {
      const contactInfo = String(blocks.signature.contactInfo || '').trim();
      if (contactInfo) {
        html += `  <p style="text-indent: 0; margin: 0 0 8px 0;">${replaceVars(contactInfo)}</p>\n`;
      }
    }

    if (blocks.signature.firmName) {
      const firmName = String(blocks.signature.firmName || '').trim();
      if (firmName) {
        html += `  <p style="text-indent: 0; margin: 0 0 8px 0;">${replaceVars(firmName)}</p>\n`;
      }
    }

    if (blocks.signature.date) {
      const date = String(blocks.signature.date || '').trim();
      if (date) {
        html += `  <p style="text-indent: 0; margin: 0;">${replaceVars(date)}</p>\n`;
      }
    }

    html += `</div>\n`;
  }

  if (!html || html.trim() === '') {
    return '<p style="color: orange;">⚠️ 模板内容为空，请填写至少一个区块的内容</p>';
  }

  return html;
}

/**
 * 格式化结构化函件模板为预览 HTML（带变量高亮样式）
 */
export function formatStructuredLetterForPreview(
  content: string,
  variables: Record<string, string> = {},
): string {
  if (!content || content.trim() === '') {
    return '<p>模板内容为空</p>';
  }

  const structured = parseStructuredLetterContent(content);

  if (!structured) {
    // 如果不是结构化内容，直接返回（可能是HTML格式）
    return content;
  }

  const { blocks } = structured;

  if (!blocks || typeof blocks !== 'object') {
    return '<p style="color: red;">⚠️ 模板内容格式错误：缺少 blocks 字段</p>';
  }

  // 替换变量（预览模式下添加高亮样式）
  const replaceVars = (text: string): string => {
    if (!text) return '';
    let result = text;
    Object.entries(variables).forEach(([key, value]) => {
      const escapedKey = key.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
      const displayValue = value || `[${key}]`;
      // 添加高亮样式
      result = result.replaceAll(
        new RegExp(String.raw`\$\{${escapedKey}\}`, 'g'),
        `<span class="preview-var">${displayValue}</span>`,
      );
    });
    return result;
  };

  // 构建打印 HTML（与 formatStructuredLetterForPrint 相同，但使用带样式的替换）
  let html = '';

  // 1. 标题区（居中显示）
  if (blocks.title && blocks.title.letterTitle) {
    const letterTitle = String(blocks.title.letterTitle || '').trim();
    if (letterTitle) {
      html += `<div style="text-align: center; margin-bottom: 30px;">
  <h2 style="text-align: center; font-family: 'FZXiaoBiaoSong-B05S', '方正小标宋简体', '方正小标宋', 'FZXBS', 'FangSong', '仿宋', serif; font-size: 22pt; font-weight: normal; letter-spacing: 2pt; margin: 0 0 10px 0;">${replaceVars(letterTitle)}</h2>\n`;
      html += `</div>\n`;
    }
  }

  // 编号（如果有）- 公文格式：四号仿宋GB2312，右对齐
  if (blocks.title && blocks.title.letterNo) {
    const letterNo = String(blocks.title.letterNo || '').trim();
    if (letterNo) {
      html += `<p style="text-align: right; font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 14pt; line-height: 24pt; margin: 0 0 20px 0;">${replaceVars(letterNo)}</p>\n`;
    }
  }

  // 2. 收件单位（左对齐）
  if (blocks.recipient) {
    const recipient = String(blocks.recipient || '').trim();
    if (recipient) {
      html += `<p style="text-indent: 0; margin-bottom: 20px; font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt;"><strong>${replaceVars(recipient)}</strong></p>\n`;
    }
  }

  // 3. 正文内容（段落缩进）
  if (blocks.body) {
    const body = String(blocks.body || '').trim();
    if (body) {
      html += `<div style="margin: 20px 0;">\n`;
      html += textToHtmlParagraphs(replaceVars(body), true);
      html += `</div>\n`;
    }
  }

  // 4. 落款区（右对齐）
  if (blocks.signature) {
    html += `<div style="text-align: right; margin-top: 40px; font-family: 'FangSong', '仿宋_GB2312', '仿宋', serif; font-size: 16pt; line-height: 28pt;">\n`;

    if (blocks.signature.lawyerNames) {
      const lawyerNames = String(blocks.signature.lawyerNames || '').trim();
      if (lawyerNames) {
        html += `  <p style="text-indent: 0; margin: 0 0 8px 0;">${replaceVars(lawyerNames)}</p>\n`;
      }
    }

    if (blocks.signature.contactInfo) {
      const contactInfo = String(blocks.signature.contactInfo || '').trim();
      if (contactInfo) {
        html += `  <p style="text-indent: 0; margin: 0 0 8px 0;">${replaceVars(contactInfo)}</p>\n`;
      }
    }

    if (blocks.signature.firmName) {
      const firmName = String(blocks.signature.firmName || '').trim();
      if (firmName) {
        html += `  <p style="text-indent: 0; margin: 0 0 8px 0;">${replaceVars(firmName)}</p>\n`;
      }
    }

    if (blocks.signature.date) {
      const date = String(blocks.signature.date || '').trim();
      if (date) {
        html += `  <p style="text-indent: 0; margin: 0;">${replaceVars(date)}</p>\n`;
      }
    }

    html += `</div>\n`;
  }

  if (!html || html.trim() === '') {
    return '<p style="color: orange;">⚠️ 模板内容为空，请填写至少一个区块的内容</p>';
  }

  return html;
}

/**
 * 检测内容是否为 HTML 格式
 */
export function isHtmlContent(content: string): boolean {
  if (!content) return false;
  // 检测常见的 HTML 标签
  return /<(div|p|h[1-6]|span|strong|em|br|table|ul|ol|li)[^>]*>/i.test(
    content,
  );
}

/**
 * 格式化 HTML 格式的函件模板为预览 HTML
 * 只替换变量，保持 HTML 结构不变
 */
export function formatHtmlLetterForPreview(
  content: string,
  variables: Record<string, string> = {},
): string {
  if (!content || content.trim() === '') {
    return '<p>模板内容为空</p>';
  }

  // 替换变量（带高亮样式）
  let result = content;
  Object.entries(variables).forEach(([key, value]) => {
    const escapedKey = key.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
    const displayValue = value || `[${key}]`;
    result = result.replaceAll(
      new RegExp(String.raw`\$\{${escapedKey}\}`, 'g'),
      `<span class="preview-var">${displayValue}</span>`,
    );
  });

  return result;
}

/**
 * 格式化纯文本函件模板为预览 HTML
 * 用于传统格式的函件模板（非结构化 JSON、非 HTML）
 */
export function formatPlainTextLetterForPreview(
  content: string,
  variables: Record<string, string> = {},
): string {
  if (!content || content.trim() === '') {
    return '<p>模板内容为空</p>';
  }

  // 替换变量（带高亮样式）
  let result = content;
  Object.entries(variables).forEach(([key, value]) => {
    const escapedKey = key.replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
    const displayValue = value || `[${key}]`;
    result = result.replaceAll(
      new RegExp(String.raw`\$\{${escapedKey}\}`, 'g'),
      `<span class="preview-var">${displayValue}</span>`,
    );
  });

  // 将纯文本转换为公文格式的 HTML
  const lines = result.split('\n');
  let html = '';
  let inBody = false;

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed) {
      html += '<p style="margin: 8px 0;">&nbsp;</p>\n';
      continue;
    }

    // 检测是否是标题行
    if (trimmed.startsWith('致：') || trimmed.startsWith('致:')) {
      html += `<p style="font-family: FangSong, 仿宋_GB2312, 仿宋, serif; font-size: 16pt; line-height: 28pt; text-indent: 0; margin: 0 0 20px 0;"><strong>${trimmed}</strong></p>\n`;
      inBody = true;
      continue;
    }

    // 检测落款区
    if (
      trimmed.includes('律师事务所') ||
      /^\d{4}年\d{1,2}月\d{1,2}日$/.test(trimmed) ||
      trimmed.includes('联系电话：') ||
      trimmed.includes('地址：') ||
      trimmed.startsWith('承办律师：')
    ) {
      html += `<p style="font-family: FangSong, 仿宋_GB2312, 仿宋, serif; font-size: 16pt; line-height: 28pt; text-align: right; text-indent: 0; margin: 4px 0;">${trimmed}</p>\n`;
      continue;
    }

    // 正文段落
    const indent = inBody ? 'text-indent: 2em;' : '';
    html += `<p style="font-family: FangSong, 仿宋_GB2312, 仿宋, serif; font-size: 16pt; line-height: 28pt; ${indent} margin: 4px 0; text-align: justify;">${trimmed}</p>\n`;
    inBody = true;
  }

  return html;
}
