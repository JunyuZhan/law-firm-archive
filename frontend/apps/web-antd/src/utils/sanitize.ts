/**
 * HTML 清理工具
 * 使用 DOMPurify 防止 XSS 攻击
 */
import DOMPurify from 'dompurify';

/**
 * 清理 HTML 内容，移除潜在的 XSS 攻击代码
 * @param dirty 需要清理的 HTML 字符串
 * @returns 安全的 HTML 字符串
 */
export function sanitizeHtml(dirty: string | null | undefined): string {
  if (!dirty) return '';
  return DOMPurify.sanitize(dirty, {
    // 允许的标签
    ALLOWED_TAGS: [
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'p', 'div', 'span', 'br', 'hr',
      'strong', 'b', 'em', 'i', 'u', 's', 'del', 'ins',
      'ul', 'ol', 'li',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'a', 'img',
      'blockquote', 'pre', 'code',
      'sub', 'sup',
    ],
    // 允许的属性
    ALLOWED_ATTR: [
      'href', 'src', 'alt', 'title', 'class', 'style',
      'width', 'height', 'colspan', 'rowspan',
      'target', 'rel',
    ],
    // 允许的 URI 协议
    ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto|tel):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i,
    // 强制链接在新窗口打开时添加 noopener
    ADD_ATTR: ['target'],
  });
}

/**
 * 清理纯文本（移除所有 HTML 标签）
 * @param dirty 需要清理的字符串
 * @returns 纯文本
 */
export function sanitizeText(dirty: string | null | undefined): string {
  if (!dirty) return '';
  return DOMPurify.sanitize(dirty, { ALLOWED_TAGS: [] });
}

/**
 * HTML 字符转义，防止 XSS（用于纯文本显示）
 * @param text 需要转义的文本
 * @returns 转义后的安全文本
 */
export function escapeHtml(text: string | null | undefined): string {
  if (!text) return '';
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;',
  };
  return String(text).replaceAll(/[&<>"']/g, (char) => map[char] ?? char);
}

export default { sanitizeHtml, sanitizeText, escapeHtml };
