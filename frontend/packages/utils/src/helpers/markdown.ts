/**
 * Markdown 工具函数
 * 用于将 Markdown 格式转换为纯文本，方便复制到 Word 或打印
 */

/**
 * 将 Markdown 格式转换为纯文本
 * 移除所有 Markdown 语法标记，保留文本内容
 * 
 * @param markdown Markdown 格式的文本
 * @returns 纯文本内容
 */
export function markdownToPlainText(markdown: string): string {
  if (!markdown) return '';

  let text = markdown;

  // 1. 移除代码块（```code``` 或 ```language\ncode\n```）
  text = text.replace(/```[\s\S]*?```/g, (match) => {
    // 保留代码块内的内容，但移除标记
    const lines = match.split('\n');
    if (lines.length > 2 && lines[0]?.trim().startsWith('```')) {
      // 移除第一行和最后一行
      return lines.slice(1, -1).join('\n');
    }
    return '';
  });

  // 2. 移除行内代码（`code`）
  text = text.replace(/`([^`]+)`/g, '$1');

  // 3. 移除粗体（**text** 或 __text__）
  text = text.replace(/\*\*([^*]+)\*\*/g, '$1');
  text = text.replace(/__([^_]+)__/g, '$1');

  // 4. 移除斜体（*text* 或 _text_）
  text = text.replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '$1');
  text = text.replace(/(?<!_)_([^_]+)_(?!_)/g, '$1');

  // 5. 移除删除线（~~text~~）
  text = text.replace(/~~([^~]+)~~/g, '$1');

  // 6. 移除链接（[text](url) 或 [text][ref]）
  text = text.replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1');
  text = text.replace(/\[([^\]]+)\]\[[^\]]+\]/g, '$1');

  // 7. 移除图片（![alt](url)）
  text = text.replace(/!\[([^\]]*)\]\([^\)]+\)/g, '$1');

  // 8. 移除标题标记（# ## ### 等）
  text = text.replace(/^#{1,6}\s+(.+)$/gm, '$1');

  // 9. 移除引用标记（> text）
  text = text.replace(/^>\s+(.+)$/gm, '$1');

  // 10. 移除列表标记（- * + 或 1. 2. 等）
  text = text.replace(/^[\s]*[-*+]\s+(.+)$/gm, '$1');
  text = text.replace(/^[\s]*\d+\.\s+(.+)$/gm, '$1');

  // 11. 移除水平线（--- 或 ***）
  text = text.replace(/^[-*]{3,}$/gm, '');

  // 12. 移除表格标记（| col1 | col2 |）
  text = text.replace(/\|/g, ' '); // 将 | 替换为空格
  text = text.replace(/^[\s]*[-:]+[\s]*$/gm, ''); // 移除表格分隔行

  // 13. 清理多余的空行（保留最多两个连续空行）
  text = text.replace(/\n{3,}/g, '\n\n');

  // 14. 清理行首行尾空格
  text = text.split('\n').map(line => line.trimEnd()).join('\n');

  // 15. 移除行首多余空格（但保留缩进）
  text = text.replace(/^[ \t]+/gm, '');

  return text.trim();
}

/**
 * 将 Markdown 格式转换为 HTML（用于打印）
 * 保留基本格式，转换为 HTML 标签
 * 
 * @param markdown Markdown 格式的文本
 * @returns HTML 格式的文本
 */
export function markdownToHtml(markdown: string): string {
  if (!markdown) return '';

  let html = markdown;

  // 转义 HTML 特殊字符
  html = html
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  // 代码块
  html = html.replace(/```[\s\S]*?```/g, (match) => {
    const code = match.replace(/```[\w]*\n?/g, '').replace(/```/g, '');
    return `<pre><code>${code}</code></pre>`;
  });

  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

  // 粗体
  html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/__([^_]+)__/g, '<strong>$1</strong>');

  // 斜体
  html = html.replace(/(?<!\*)\*([^*]+)\*(?!\*)/g, '<em>$1</em>');
  html = html.replace(/(?<!_)_([^_]+)_(?!_)/g, '<em>$1</em>');

  // 标题
  html = html.replace(/^######\s+(.+)$/gm, '<h6>$1</h6>');
  html = html.replace(/^#####\s+(.+)$/gm, '<h5>$1</h5>');
  html = html.replace(/^####\s+(.+)$/gm, '<h4>$1</h4>');
  html = html.replace(/^###\s+(.+)$/gm, '<h3>$1</h3>');
  html = html.replace(/^##\s+(.+)$/gm, '<h2>$1</h2>');
  html = html.replace(/^#\s+(.+)$/gm, '<h1>$1</h1>');

  // 链接
  html = html.replace(/\[([^\]]+)\]\(([^\)]+)\)/g, '<a href="$2">$1</a>');

  // 换行转段落
  html = html.split('\n\n').map(para => {
    if (para.trim()) {
      // 如果已经是 HTML 标签，不包装
      if (para.match(/^<[h|p|d|u|o|l|t]/)) {
        return para;
      }
      return `<p>${para.replace(/\n/g, '<br>')}</p>`;
    }
    return '';
  }).join('');

  return html;
}
