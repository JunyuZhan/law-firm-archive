/**
 * 合同打印格式化工具
 * 将结构化模板内容转换为排版好的打印 HTML
 */

export interface TemplateBlocks {
  title: {
    contractName: string;
  };
  parties: {
    partyA: string;
    partyB: string;
  };
  clauses: string;
  signature: {
    partyASign: string;
    partyBSign: string;
    signInfo: string;
  };
}

export interface StructuredContent {
  _structured: boolean;
  _version: number;
  blocks: TemplateBlocks;
}

/**
 * 解码 HTML 实体
 * @export 导出以便在其他组件中使用
 */
export function decodeHtmlEntities(text: string): string {
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
 * 检查内容是否为结构化格式
 * 支持处理可能被转义的内容
 */
export function isStructuredContent(content: string): boolean {
  if (!content || typeof content !== 'string') {
    return false;
  }

  // 先解码 HTML 实体
  const decoded = decodeHtmlEntities(content);

  // 尝试直接解析
  try {
    const parsed = JSON.parse(decoded);
    if (parsed && typeof parsed === 'object' && parsed._structured === true) {
      return true;
    }
  } catch {
    // 继续尝试其他方式
  }

  // 如果直接解析失败，尝试处理可能的转义
  try {
    // 移除首尾空白
    const trimmed = decoded.trim();

    // 如果看起来像 JSON（以 { 开头）
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      // 尝试解析
      const parsed = JSON.parse(trimmed);
      if (parsed && typeof parsed === 'object' && parsed._structured === true) {
        return true;
      }
    }

    // 如果内容看起来像被转义的 JSON 字符串（以 " 开头和结尾）
    if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
      try {
        // 先解析外层字符串
        const unescaped = JSON.parse(trimmed);
        if (typeof unescaped === 'string') {
          // 再解析内层 JSON
          const innerParsed = JSON.parse(unescaped);
          if (
            innerParsed &&
            typeof innerParsed === 'object' &&
            innerParsed._structured === true
          ) {
            return true;
          }
        }
      } catch {
        // 忽略
      }
    }
  } catch {
    // 忽略
  }

  return false;
}

/**
 * 解析结构化内容
 * 支持处理可能被转义的内容和 HTML 编码的内容
 */
export function parseStructuredContent(
  content: string,
): StructuredContent | null {
  if (!content || typeof content !== 'string') {
    return null;
  }

  // 先解码 HTML 实体
  const decoded = decodeHtmlEntities(content);

  // 尝试直接解析
  try {
    const parsed = JSON.parse(decoded);
    if (parsed && typeof parsed === 'object' && parsed._structured === true) {
      return parsed as StructuredContent;
    }
  } catch {
    // 继续尝试其他方式
  }

  // 如果直接解析失败，尝试处理可能的转义
  try {
    // 移除首尾空白
    const trimmed = decoded.trim();

    // 如果看起来像 JSON（以 { 开头）
    if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
      try {
        const parsed = JSON.parse(trimmed);
        if (
          parsed &&
          typeof parsed === 'object' &&
          parsed._structured === true
        ) {
          return parsed as StructuredContent;
        }
      } catch {
        // 忽略
      }
    }

    // 如果内容看起来像被转义的 JSON 字符串（以 " 开头和结尾）
    if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
      try {
        // 先解析外层字符串
        const unescaped = JSON.parse(trimmed);
        if (typeof unescaped === 'string') {
          // 再解析内层 JSON
          const innerParsed = JSON.parse(unescaped);
          if (
            innerParsed &&
            typeof innerParsed === 'object' &&
            innerParsed._structured === true
          ) {
            return innerParsed as StructuredContent;
          }
        }
      } catch {
        // 忽略
      }
    }
  } catch {
    // 忽略
  }

  return null;
}

/**
 * 将文本中的换行转为 HTML 段落
 */
function textToHtmlParagraphs(text: string, indent: boolean = true): string {
  if (!text) return '';

  const lines = text.split('\n').filter((line) => line.trim());
  return lines
    .map((line) => {
      const trimmed = line.trim();
      // 检查是否是条款编号开头（一、二、三... 或 1. 2. 3...）
      const isClauseTitle =
        /^[一二三四五六七八九十]+、/.test(trimmed) ||
        /^第[一二三四五六七八九十]+条/.test(trimmed) ||
        /^\d+[\.、]/.test(trimmed);

      if (isClauseTitle) {
        return `<p style="text-indent: 0; margin-top: 1em; font-weight: 600;">${trimmed}</p>`;
      }
      return `<p style="${indent ? 'text-indent: 2em;' : ''}">${trimmed}</p>`;
    })
    .join('\n');
}

/**
 * 格式化结构化模板为打印 HTML
 */
export function formatStructuredForPrint(
  content: string,
  variables: Record<string, string> = {},
): string {
  if (!content || content.trim() === '') {
    return '<p>模板内容为空</p>';
  }

  const structured = parseStructuredContent(content);

  if (!structured) {
    // 如果不是结构化内容，尝试智能格式化纯文本
    console.warn('内容不是结构化格式，尝试作为纯文本处理');
    return formatPlainTextForPrint(content, variables);
  }

  const { blocks } = structured;

  // 验证 blocks 结构
  if (!blocks || typeof blocks !== 'object') {
    console.error('结构化内容格式错误：blocks 不存在或格式不正确');
    return '<p style="color: red;">⚠️ 模板内容格式错误：缺少 blocks 字段</p>';
  }

  // 替换变量
  const replaceVars = (text: string): string => {
    if (!text) return '';
    let result = text;
    Object.entries(variables).forEach(([key, value]) => {
      // 转义特殊字符，避免在正则表达式中出错
      const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      result = result.replace(
        new RegExp(`\\$\\{${escapedKey}\\}`, 'g'),
        value || `[${key}]`,
      );
    });
    return result;
  };

  // 构建打印 HTML
  let html = '';

  // 1. 标题区（合同编号由系统自动生成，通过 variables 传入）
  if (blocks.title && blocks.title.contractName) {
    const contractName = String(blocks.title.contractName || '').trim();
    if (contractName) {
      html += `<h2 style="text-align: center; font-size: 22pt; font-weight: bold; letter-spacing: 0.2em; margin-bottom: 8px;">${replaceVars(contractName)}</h2>\n`;
    }
  }
  // 合同编号（如果 variables 中有 contractNo）- 右对齐
  if (variables.contractNo) {
    html += `<p style="text-align: right; font-size: 12pt; margin-bottom: 20px;">合同编号：${variables.contractNo}</p>\n`;
  }

  // 2. 主体区（甲乙方信息）- 段落格式
  if (blocks.parties && (blocks.parties.partyA || blocks.parties.partyB)) {
    html += `<div style="margin: 20px 0;">\n`;

    // 甲方信息
    if (blocks.parties.partyA) {
      const partyA = String(blocks.parties.partyA || '').trim();
      if (partyA) {
        const partyALines = partyA.split('\n').filter((l) => l.trim());
        partyALines.forEach((line) => {
          html += `<p style="text-indent: 2em; margin: 4px 0;">${replaceVars(line.trim())}</p>\n`;
        });
      }
    }

    // 乙方信息
    if (blocks.parties.partyB) {
      const partyB = String(blocks.parties.partyB || '').trim();
      if (partyB) {
        const partyBLines = partyB.split('\n').filter((l) => l.trim());
        partyBLines.forEach((line) => {
          html += `<p style="text-indent: 2em; margin: 4px 0;">${replaceVars(line.trim())}</p>\n`;
        });
      }
    }

    html += `</div>\n`;
  }

  // 3. 条款区
  if (blocks.clauses) {
    const clauses = String(blocks.clauses || '').trim();
    if (clauses) {
      html += `<div style="margin: 24px 0;">\n`;
      html += textToHtmlParagraphs(replaceVars(clauses), true);
      html += `</div>\n`;
    }
  }

  // 4. 签署区
  if (blocks.signature) {
    html += `<div style="margin-top: 48px;">\n`;
    html += `<table style="width: 100%; border: none;">\n`;
    html += `<tr>\n`;

    // 甲方签署
    html += `<td style="width: 50%; vertical-align: top; border: none; padding-right: 20px;">\n`;
    if (blocks.signature.partyASign) {
      const partyASign = String(blocks.signature.partyASign || '').trim();
      if (partyASign) {
        const lines = partyASign.split('\n').filter((l) => l.trim());
        lines.forEach((line) => {
          html += `<p style="margin: 8px 0;">${replaceVars(line.trim())}</p>\n`;
        });
      } else {
        html += `<p style="margin: 8px 0;"><strong>甲方（签章）：</strong></p>\n`;
        html += `<p style="margin: 8px 0;">&nbsp;</p>\n`;
      }
    } else {
      html += `<p style="margin: 8px 0;"><strong>甲方（签章）：</strong></p>\n`;
      html += `<p style="margin: 8px 0;">&nbsp;</p>\n`;
    }
    html += `</td>\n`;

    // 乙方签署
    html += `<td style="width: 50%; vertical-align: top; border: none; padding-left: 20px;">\n`;
    if (blocks.signature.partyBSign) {
      const partyBSign = String(blocks.signature.partyBSign || '').trim();
      if (partyBSign) {
        const lines = partyBSign.split('\n').filter((l) => l.trim());
        lines.forEach((line) => {
          html += `<p style="margin: 8px 0;">${replaceVars(line.trim())}</p>\n`;
        });
      } else {
        html += `<p style="margin: 8px 0;"><strong>乙方（签章）：</strong></p>\n`;
        html += `<p style="margin: 8px 0;">&nbsp;</p>\n`;
      }
    } else {
      html += `<p style="margin: 8px 0;"><strong>乙方（签章）：</strong></p>\n`;
      html += `<p style="margin: 8px 0;">&nbsp;</p>\n`;
    }
    html += `</td>\n`;

    html += `</tr>\n`;
    html += `</table>\n`;

    // 签订日期/地点
    if (blocks.signature.signInfo) {
      const signInfo = String(blocks.signature.signInfo || '').trim();
      if (signInfo) {
        html += `<p style="text-align: center; margin-top: 32px;">${replaceVars(signInfo)}</p>\n`;
      }
    }
    html += `</div>\n`;
  }

  // 如果 HTML 为空，返回提示
  if (!html || html.trim() === '') {
    return '<p style="color: orange;">⚠️ 模板内容为空，请填写至少一个区块的内容</p>';
  }

  return html;
}

/**
 * 格式化纯文本/旧格式内容为打印 HTML
 * 智能识别并排版
 */
export function formatPlainTextForPrint(
  content: string,
  variables: Record<string, string> = {},
): string {
  if (!content) return '';

  // 检测是否为JSON字符串（可能是转义的结构化内容）
  const trimmed = content.trim();
  if (
    (trimmed.startsWith('{') && trimmed.endsWith('}')) ||
    (trimmed.startsWith('[') && trimmed.endsWith(']'))
  ) {
    try {
      // 先尝试直接解析
      let parsed = JSON.parse(content);

      // 如果内容是被转义的字符串，需要再次解析
      if (typeof parsed === 'string') {
        try {
          parsed = JSON.parse(parsed);
        } catch {
          // 忽略
        }
      }

      // 如果是结构化内容但未被识别，尝试重新格式化
      if (
        parsed &&
        typeof parsed === 'object' &&
        parsed._structured === true &&
        parsed.blocks
      ) {
        console.warn(
          'formatPlainTextForPrint: 检测到结构化内容但未被 isStructuredContent 识别，尝试重新格式化',
        );
        // 使用原始内容或解析后的内容
        const contentToFormat = typeof parsed === 'string' ? parsed : content;
        return formatStructuredForPrint(contentToFormat, variables);
      }
    } catch (e) {
      // 不是有效的JSON，继续处理
      console.debug(
        'formatPlainTextForPrint: JSON 解析失败，作为纯文本处理',
        e,
      );
    }
  }

  // 替换变量
  let text = content;
  Object.entries(variables).forEach(([key, value]) => {
    // 转义特殊字符，避免在正则表达式中出错
    const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    text = text.replace(
      new RegExp(`\\$\\{${escapedKey}\\}`, 'g'),
      value || `[${key}]`,
    );
  });

  // 如果已经是 HTML 格式，直接返回
  if (/<[^>]+>/.test(text)) {
    return text;
  }

  // 纯文本智能格式化
  const lines = text.split('\n');
  let html = '';

  lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) {
      html += '<p>&nbsp;</p>\n';
      return;
    }

    // 检测标题（第一行或包含"合同"的短行）
    if (
      index === 0 ||
      (trimmed.includes('合同') &&
        trimmed.length < 30 &&
        !trimmed.includes('：'))
    ) {
      html += `<h2 style="text-align: center; font-size: 22pt; font-weight: bold; letter-spacing: 0.2em; margin: 16px 0;">${trimmed}</h2>\n`;
      return;
    }

    // 检测合同编号行 - 右对齐
    if (
      trimmed.includes('合同编号') ||
      (trimmed.includes('编号') && trimmed.includes('字第'))
    ) {
      html += `<p style="text-align: right; margin: 8px 0 16px;">${trimmed}</p>\n`;
      return;
    }

    // 检测条款标题（一、二、三... 或 第X条）
    if (
      /^[一二三四五六七八九十]+[、．.]/.test(trimmed) ||
      /^第[一二三四五六七八九十]+条/.test(trimmed)
    ) {
      html += `<h3 style="font-size: 14pt; font-weight: 600; margin: 1.5em 0 0.5em; text-indent: 0;">${trimmed}</h3>\n`;
      return;
    }

    // 检测签章区域
    if (
      trimmed.includes('签章') ||
      trimmed.includes('盖章') ||
      trimmed.includes('签字')
    ) {
      html += `<p style="margin: 8px 0; font-weight: 500;">${trimmed}</p>\n`;
      return;
    }

    // 检测日期行
    if (
      /^\d{4}年\d{1,2}月\d{1,2}日/.test(trimmed) ||
      trimmed.includes('签订日期')
    ) {
      html += `<p style="text-align: center; margin-top: 24px;">${trimmed}</p>\n`;
      return;
    }

    // 普通段落，添加首行缩进
    html += `<p style="text-indent: 2em; margin: 4px 0;">${trimmed}</p>\n`;
  });

  return html;
}

/**
 * 获取打印用的完整 HTML 文档
 */
export function getPrintDocument(
  content: string,
  title: string,
  variables: Record<string, string> = {},
): string {
  const formattedContent = isStructuredContent(content)
    ? formatStructuredForPrint(content, variables)
    : formatPlainTextForPrint(content, variables);

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>${title}</title>
  <style>
    @page { 
      margin: 2.5cm; 
      size: A4; 
    }
    body { 
      font-family: "SimSun", "宋体", "FangSong", serif; 
      font-size: 12pt; 
      line-height: 1.8;
      color: #000;
    }
    h2 {
      font-family: "SimHei", "黑体", sans-serif;
      font-weight: bold;
    }
    h3 {
      font-family: "SimHei", "黑体", sans-serif;
    }
    table {
      page-break-inside: avoid;
    }
    p {
      margin: 0.5em 0;
      text-align: justify;
    }
    @media print {
      body { 
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
    }
  </style>
</head>
<body>
  ${formattedContent}
</body>
</html>`;
}
