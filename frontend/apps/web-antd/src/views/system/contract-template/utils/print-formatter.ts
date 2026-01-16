/**
 * 合同打印格式化工具
 * 将结构化模板内容转换为排版好的打印 HTML
 */

export interface TemplateBlocks {
  title: {
    contractName: string;
    contractNo: string;
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
 * 检查内容是否为结构化格式
 */
export function isStructuredContent(content: string): boolean {
  try {
    const parsed = JSON.parse(content);
    return parsed._structured === true;
  } catch {
    return false;
  }
}

/**
 * 解析结构化内容
 */
export function parseStructuredContent(content: string): StructuredContent | null {
  try {
    const parsed = JSON.parse(content);
    if (parsed._structured) {
      return parsed as StructuredContent;
    }
  } catch {
    // 不是 JSON
  }
  return null;
}

/**
 * 将文本中的换行转为 HTML 段落
 */
function textToHtmlParagraphs(text: string, indent: boolean = true): string {
  if (!text) return '';
  
  const lines = text.split('\n').filter(line => line.trim());
  return lines.map(line => {
    const trimmed = line.trim();
    // 检查是否是条款编号开头（一、二、三... 或 1. 2. 3...）
    const isClauseTitle = /^[一二三四五六七八九十]+、/.test(trimmed) || 
                          /^第[一二三四五六七八九十]+条/.test(trimmed) ||
                          /^\d+[\.、]/.test(trimmed);
    
    if (isClauseTitle) {
      return `<p style="text-indent: 0; margin-top: 1em; font-weight: 600;">${trimmed}</p>`;
    }
    return `<p style="${indent ? 'text-indent: 2em;' : ''}">${trimmed}</p>`;
  }).join('\n');
}

/**
 * 格式化结构化模板为打印 HTML
 */
export function formatStructuredForPrint(content: string, variables: Record<string, string> = {}): string {
  const structured = parseStructuredContent(content);
  
  if (!structured) {
    // 如果不是结构化内容，尝试智能格式化纯文本
    return formatPlainTextForPrint(content, variables);
  }

  const { blocks } = structured;
  
  // 替换变量
  const replaceVars = (text: string): string => {
    if (!text) return '';
    let result = text;
    Object.entries(variables).forEach(([key, value]) => {
      result = result.replace(new RegExp(`\\$\\{${key}\\}`, 'g'), value || `[${key}]`);
    });
    return result;
  };

  // 构建打印 HTML
  let html = '';

  // 1. 标题区
  if (blocks.title.contractName) {
    html += `<h2 style="text-align: center; font-size: 22pt; letter-spacing: 0.3em; margin-bottom: 8px;">${replaceVars(blocks.title.contractName)}</h2>\n`;
  }
  if (blocks.title.contractNo) {
    html += `<p style="text-align: center; font-size: 12pt; color: #333; margin-bottom: 24px;">${replaceVars(blocks.title.contractNo)}</p>\n`;
  }

  // 2. 主体区（甲乙方信息）
  if (blocks.parties.partyA || blocks.parties.partyB) {
    html += `<table style="width: 100%; border-collapse: collapse; margin: 20px 0; border: 1px solid #000;">\n`;
    
    // 甲方信息
    if (blocks.parties.partyA) {
      const partyALines = blocks.parties.partyA.split('\n').filter(l => l.trim());
      partyALines.forEach((line, idx) => {
        const parts = line.split(/[：:]/);
        if (parts.length >= 2) {
          html += `<tr>
            <td style="width: 20%; padding: 8px; border: 1px solid #000; background: ${idx === 0 ? '#f5f5f5' : '#fff'}; font-weight: ${idx === 0 ? '600' : 'normal'};">${parts[0].trim()}</td>
            <td style="padding: 8px; border: 1px solid #000;">${replaceVars(parts.slice(1).join(':').trim())}</td>
          </tr>\n`;
        } else {
          html += `<tr>
            <td colspan="2" style="padding: 8px; border: 1px solid #000;">${replaceVars(line.trim())}</td>
          </tr>\n`;
        }
      });
    }
    
    // 分隔行
    html += `<tr><td colspan="2" style="height: 8px; border: 1px solid #000; background: #f0f0f0;"></td></tr>\n`;
    
    // 乙方信息
    if (blocks.parties.partyB) {
      const partyBLines = blocks.parties.partyB.split('\n').filter(l => l.trim());
      partyBLines.forEach((line, idx) => {
        const parts = line.split(/[：:]/);
        if (parts.length >= 2) {
          html += `<tr>
            <td style="width: 20%; padding: 8px; border: 1px solid #000; background: ${idx === 0 ? '#f5f5f5' : '#fff'}; font-weight: ${idx === 0 ? '600' : 'normal'};">${parts[0].trim()}</td>
            <td style="padding: 8px; border: 1px solid #000;">${replaceVars(parts.slice(1).join(':').trim())}</td>
          </tr>\n`;
        } else {
          html += `<tr>
            <td colspan="2" style="padding: 8px; border: 1px solid #000;">${replaceVars(line.trim())}</td>
          </tr>\n`;
        }
      });
    }
    
    html += `</table>\n`;
  }

  // 3. 条款区
  if (blocks.clauses) {
    html += `<div style="margin: 24px 0;">\n`;
    html += textToHtmlParagraphs(replaceVars(blocks.clauses), true);
    html += `</div>\n`;
  }

  // 4. 签署区
  html += `<div style="margin-top: 48px;">\n`;
  html += `<table style="width: 100%; border: none;">\n`;
  html += `<tr>\n`;
  
  // 甲方签署
  html += `<td style="width: 50%; vertical-align: top; border: none; padding-right: 20px;">\n`;
  if (blocks.signature.partyASign) {
    const lines = blocks.signature.partyASign.split('\n').filter(l => l.trim());
    lines.forEach(line => {
      html += `<p style="margin: 8px 0;">${replaceVars(line.trim())}</p>\n`;
    });
  } else {
    html += `<p style="margin: 8px 0;"><strong>甲方（签章）：</strong></p>\n`;
    html += `<p style="margin: 8px 0;">&nbsp;</p>\n`;
  }
  html += `</td>\n`;
  
  // 乙方签署
  html += `<td style="width: 50%; vertical-align: top; border: none; padding-left: 20px;">\n`;
  if (blocks.signature.partyBSign) {
    const lines = blocks.signature.partyBSign.split('\n').filter(l => l.trim());
    lines.forEach(line => {
      html += `<p style="margin: 8px 0;">${replaceVars(line.trim())}</p>\n`;
    });
  } else {
    html += `<p style="margin: 8px 0;"><strong>乙方（签章）：</strong></p>\n`;
    html += `<p style="margin: 8px 0;">&nbsp;</p>\n`;
  }
  html += `</td>\n`;
  
  html += `</tr>\n`;
  html += `</table>\n`;
  
  // 签订日期/地点
  if (blocks.signature.signInfo) {
    html += `<p style="text-align: center; margin-top: 32px;">${replaceVars(blocks.signature.signInfo)}</p>\n`;
  }
  html += `</div>\n`;

  return html;
}

/**
 * 格式化纯文本/旧格式内容为打印 HTML
 * 智能识别并排版
 */
export function formatPlainTextForPrint(content: string, variables: Record<string, string> = {}): string {
  if (!content) return '';
  
  // 替换变量
  let text = content;
  Object.entries(variables).forEach(([key, value]) => {
    text = text.replace(new RegExp(`\\$\\{${key}\\}`, 'g'), value || `[${key}]`);
  });

  // 如果已经是 HTML 格式，直接返回
  if (/<[^>]+>/.test(text)) {
    return text;
  }

  // 纯文本智能格式化
  const lines = text.split('\n');
  let html = '';
  let inList = false;

  lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) {
      html += '<p>&nbsp;</p>\n';
      return;
    }

    // 检测标题（第一行或包含"合同"的短行）
    if (index === 0 || (trimmed.includes('合同') && trimmed.length < 30 && !trimmed.includes('：'))) {
      html += `<h2 style="text-align: center; font-size: 18pt; letter-spacing: 0.2em; margin: 16px 0;">${trimmed}</h2>\n`;
      return;
    }

    // 检测合同编号行
    if (trimmed.includes('编号') || trimmed.includes('字第') || /^[\（\(].+[\）\)]$/.test(trimmed)) {
      html += `<p style="text-align: center; margin: 8px 0;">${trimmed}</p>\n`;
      return;
    }

    // 检测条款标题（一、二、三... 或 第X条）
    if (/^[一二三四五六七八九十]+[、．.]/.test(trimmed) || /^第[一二三四五六七八九十]+条/.test(trimmed)) {
      html += `<h3 style="font-size: 14pt; font-weight: 600; margin: 1.5em 0 0.5em; text-indent: 0;">${trimmed}</h3>\n`;
      return;
    }

    // 检测签章区域
    if (trimmed.includes('签章') || trimmed.includes('盖章') || trimmed.includes('签字')) {
      html += `<p style="margin: 8px 0; font-weight: 500;">${trimmed}</p>\n`;
      return;
    }

    // 检测日期行
    if (/^\d{4}年\d{1,2}月\d{1,2}日/.test(trimmed) || trimmed.includes('签订日期')) {
      html += `<p style="text-align: center; margin-top: 24px;">${trimmed}</p>\n`;
      return;
    }

    // 普通段落，添加首行缩进
    html += `<p style="text-indent: 2em; margin: 0.5em 0;">${trimmed}</p>\n`;
  });

  return html;
}

/**
 * 获取打印用的完整 HTML 文档
 */
export function getPrintDocument(content: string, title: string, variables: Record<string, string> = {}): string {
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
