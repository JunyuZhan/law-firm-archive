/**
 * 金额转中文大写
 */
export function amountToChinese(num: null | number | undefined): string {
  if (num === undefined || num === null || Number.isNaN(num)) return '';
  if (num === 0) return '零元整';

  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'];
  const units = ['', '拾', '佰', '仟'];
  const bigUnits = ['', '万', '亿', '兆'];

  // 分离整数和小数部分
  const parts = num.toFixed(2).split('.');
  const intPart = parts[0] || '0';
  const decPart = parts[1] || '00';
  let result = '';

  // 处理整数部分
  if (intPart !== '0') {
    let intStr: string = intPart;
    let groupIndex = 0;

    while (intStr.length > 0) {
      const group = intStr.slice(-4);
      intStr = intStr.slice(0, -4);

      let groupResult = '';
      let hasZero = false;

      for (let i = 0; i < group.length; i++) {
        const char = group[i];
        if (char === undefined) continue;
        const digit = Number.parseInt(char, 10);
        const unitIndex = group.length - 1 - i;

        if (digit === 0) {
          hasZero = true;
        } else {
          if (hasZero) {
            groupResult += '零';
            hasZero = false;
          }
          groupResult += (digits[digit] || '') + (units[unitIndex] || '');
        }
      }

      if (groupResult) {
        result = groupResult + (bigUnits[groupIndex] || '') + result;
      } else if (
        result &&
        groupIndex > 0 && // 如果当前组为空但后面有数字，需要添加零
        !result.startsWith('零')
      ) {
        result = `零${result}`;
      }

      groupIndex++;
    }

    result += '元';
  }

  // 处理小数部分
  const jiaoChar = decPart[0];
  const fenChar = decPart[1];
  const jiao = jiaoChar ? Number.parseInt(jiaoChar, 10) : 0;
  const fen = fenChar ? Number.parseInt(fenChar, 10) : 0;

  if (jiao === 0 && fen === 0) {
    result += '整';
  } else {
    if (jiao > 0) {
      result += `${digits[jiao] || ''}角`;
    } else if (result) {
      result += '零';
    }
    if (fen > 0) {
      result += `${digits[fen] || ''}分`;
    }
  }

  return result || '零元整';
}

/**
 * 格式化金额显示
 */
export function formatMoney(value?: number): string {
  if (value === undefined || value === null) return '-';
  return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

/**
 * 获取合同状态颜色
 */
export function getStatusColor(status: string): string {
  const colorMap: Record<string, string> = {
    DRAFT: 'default',
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red',
    EXPIRED: 'default',
    TERMINATED: 'default',
  };
  return colorMap[status] || 'default';
}

/**
 * 获取合同状态文本
 */
export function getStatusText(status: string): string {
  const textMap: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审批',
    APPROVED: '已生效',
    REJECTED: '已拒绝',
    EXPIRED: '已到期',
    TERMINATED: '已终止',
  };
  return textMap[status] || status;
}
