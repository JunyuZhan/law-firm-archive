<script setup lang="ts">
import type {
  IssuePayrollCommand,
  PayrollItemDTO,
  PayrollSheetDTO,
  PayrollSheetQuery,
  UpdatePayrollItemCommand,
} from '#/api/hr/payroll';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  AutoComplete,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Divider,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  exportPayrollSheet,
  financeConfirmPayrollSheet,
  getPayrollItemsByYearMonth,
  getPayrollSheetDetail,
  getPayrollSheetList,
  issuePayroll,
  submitPayrollSheet,
  updatePayrollItem,
  updatePayrollItemByEmployee,
} from '#/api/hr/payroll';

defineOptions({ name: 'PayrollManagement' });

// 状态
const loading = ref(false);
const selectedSheetId = ref<number | undefined>(undefined); // 当前选中的工资表ID
const dataSource = ref<PayrollItemDTO[]>([]); // 员工工资明细列表
const itemEditModalVisible = ref(false);
const issueModalVisible = ref(false);
const currentSheet = ref<null | PayrollSheetDTO>(null);
const currentItem = ref<null | PayrollItemDTO>(null);
const currentItemConfirmDeadline = ref<any>(null); // 用于DatePicker的确认截止时间（dayjs对象）

// 查询参数
const queryParams = reactive<PayrollSheetQuery>({
  pageNum: 1,
  pageSize: 100,
  payrollYear: new Date().getFullYear(),
  payrollMonth: new Date().getMonth() + 1,
  status: undefined,
  payrollNo: undefined,
});

// 筛选参数
const filterParams = reactive({
  employeeName: '',
  employeeNo: '',
});

// 计算收入总额（提成总额）- 只从收入项计算，不从扣减项计算
function calculateIncomeTotal(record: PayrollItemDTO): number {
  if (!record.incomes || record.incomes.length === 0) {
    // 如果没有收入项，收入为0（收入应该来自提成管理模块的数据）
    return 0;
  }
  return record.incomes.reduce((sum, income) => {
    const amount =
      typeof income.amount === 'string'
        ? Number.parseFloat(income.amount)
        : income.amount || 0;
    return sum + amount;
  }, 0);
}

// 表格列（员工工资明细）
const columns = [
  { title: '工号', dataIndex: 'employeeNo', key: 'employeeNo', width: 100 },
  { title: '姓名', dataIndex: 'employeeName', key: 'employeeName', width: 100 },
  {
    title: '收入',
    key: 'income',
    width: 120,
    customRender: ({ record }: { record: PayrollItemDTO }) =>
      formatCurrency(calculateIncomeTotal(record)),
  },
  {
    title: '应发工资',
    dataIndex: 'grossAmount',
    key: 'grossAmount',
    width: 120,
    customRender: ({ record }: { record: PayrollItemDTO }) =>
      formatCurrency(record.grossAmount),
  },
  {
    title: '扣减总额',
    dataIndex: 'deductionAmount',
    key: 'deductionAmount',
    width: 120,
    customRender: ({ record }: { record: PayrollItemDTO }) =>
      formatCurrency(record.deductionAmount),
  },
  {
    title: '实发工资',
    dataIndex: 'netAmount',
    key: 'netAmount',
    width: 120,
    customRender: ({ record }: { record: PayrollItemDTO }) =>
      formatCurrency(record.netAmount),
  },
  {
    title: '确认状态',
    dataIndex: 'confirmStatusName',
    key: 'confirmStatusName',
    width: 100,
  },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 月份选项
const monthOptions = Array.from({ length: 12 }, (_, i) => ({
  label: `${i + 1}月`,
  value: i + 1,
}));

// 年份选项（最近5年）
const currentYear = new Date().getFullYear();
const yearOptions = Array.from({ length: 5 }, (_, i) => ({
  label: `${currentYear - i}年`,
  value: currentYear - i,
}));

// 发放表单
const issueForm = reactive<IssuePayrollCommand>({
  payrollSheetId: 0,
  paymentMethod: 'BANK_TRANSFER',
  paymentVoucherUrl: '',
  remark: '',
});

// 格式化货币
function formatCurrency(amount: number | string | undefined): string {
  if (amount === undefined || amount === null) return '¥0.00';
  const num = typeof amount === 'string' ? Number.parseFloat(amount) : amount;
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

// 加载员工工资明细列表（根据年月）
async function fetchPayrollItems() {
  if (!queryParams.payrollYear || !queryParams.payrollMonth) {
    dataSource.value = [];
    return;
  }

  loading.value = true;
  try {
    // 直接根据年月查询员工工资明细
    const items = await getPayrollItemsByYearMonth(
      queryParams.payrollYear,
      queryParams.payrollMonth,
    );
    dataSource.value = items;

    // 查找该年月的工资表，用于显示汇总信息
    const res = await getPayrollSheetList({
      payrollYear: queryParams.payrollYear,
      payrollMonth: queryParams.payrollMonth,
      pageSize: 1,
    });
    if (res.list.length > 0 && res.list[0]) {
      const sheet = res.list[0];
      if (sheet.id) {
        selectedSheetId.value = sheet.id;
        currentSheet.value = await getPayrollSheetDetail(sheet.id);
      }
    } else {
      selectedSheetId.value = undefined;
      currentSheet.value = null;
    }
  } catch (error: any) {
    message.error(error.message || '加载员工工资明细失败');
    dataSource.value = [];
  } finally {
    loading.value = false;
  }
}

// 搜索
async function handleSearch() {
  queryParams.pageNum = 1;
  await fetchPayrollItems();
}

// 重置筛选
function handleReset() {
  filterParams.employeeName = '';
  filterParams.employeeNo = '';
  handleSearch();
}

// 过滤后的数据源
const filteredDataSource = computed(() => {
  let result = dataSource.value;

  if (filterParams.employeeName) {
    result = result.filter(
      (item) =>
        item.employeeName &&
        item.employeeName.includes(filterParams.employeeName),
    );
  }

  if (filterParams.employeeNo) {
    result = result.filter(
      (item) =>
        item.employeeNo && item.employeeNo.includes(filterParams.employeeNo),
    );
  }

  return result;
});

// 编辑工资明细（可编辑扣减项）
function handleEditItem(item: PayrollItemDTO) {
  currentItem.value = { ...item };
  // 设置确认截止时间（用于DatePicker）
  if (currentItem.value.confirmDeadline) {
    currentItemConfirmDeadline.value = dayjs(currentItem.value.confirmDeadline);
  } else if (queryParams.payrollYear && queryParams.payrollMonth) {
    const year = queryParams.payrollYear;
    const month = queryParams.payrollMonth;
    // 设置默认确认截止时间为每月27日23:59:59
    currentItemConfirmDeadline.value = dayjs(
      `${year}-${month.toString().padStart(2, '0')}-27 23:59:59`,
    );
  } else {
    currentItemConfirmDeadline.value = null;
  }
  // 重新计算应发工资和实发工资
  updateDeductionAmount(currentItem.value);
  itemEditModalVisible.value = true;
}

// 保存工资明细
async function handleSaveItem() {
  if (!currentItem.value) return;

  // 验证扣减项必须有备注
  if (currentItem.value.deductions) {
    for (const deduction of currentItem.value.deductions) {
      if (!deduction.remark || deduction.remark.trim() === '') {
        message.error('扣减项必须填写说明');
        return;
      }
    }
  }

  try {
    // 确保应发工资和实发工资已更新
    updateDeductionAmount(currentItem.value);

    const command: UpdatePayrollItemCommand = {
      payrollItemId: currentItem.value.id,
      confirmDeadline: currentItemConfirmDeadline.value
        ? dayjs(currentItemConfirmDeadline.value).toISOString()
        : undefined,
      incomes: currentItem.value.incomes?.map((income) => ({
        id: income.id,
        incomeType: income.incomeType!,
        amount:
          typeof income.amount === 'string'
            ? Number.parseFloat(income.amount)
            : income.amount || 0,
        remark: income.remark || '',
        sourceType: income.sourceType,
        sourceId: income.sourceId,
      })),
      deductions: currentItem.value.deductions?.map((deduction) => ({
        id: deduction.id,
        deductionType: deduction.deductionType || 'OTHER_DEDUCTION', // 如果为空则使用默认值
        amount:
          typeof deduction.amount === 'string'
            ? Number.parseFloat(deduction.amount)
            : deduction.amount || 0,
        remark: deduction.remark || '',
        sourceType: deduction.sourceType,
      })),
    };

    // 如果有工资明细ID，使用更新接口；否则使用根据员工ID更新的接口
    if (currentItem.value.id) {
      await updatePayrollItem(currentItem.value.id, command);
    } else if (
      currentItem.value.employeeId &&
      queryParams.payrollYear &&
      queryParams.payrollMonth
    ) {
      await updatePayrollItemByEmployee(
        queryParams.payrollYear,
        queryParams.payrollMonth,
        currentItem.value.employeeId,
        command,
      );
    } else {
      message.error('无法保存：缺少必要信息');
      return;
    }

    message.success('保存工资明细成功');
    itemEditModalVisible.value = false;
    await fetchPayrollItems();
  } catch (error: any) {
    message.error(error.message || '保存工资明细失败');
  }
}

// 获取收入类型名称
function getIncomeTypeName(type: string): string {
  const typeMap: Record<string, string> = {
    BASE_SALARY: '基本工资',
    COMMISSION: '提成',
    PERFORMANCE_BONUS: '绩效奖金',
    OTHER_ALLOWANCE: '其他津贴',
  };
  return typeMap[type] || type;
}

// 提交工资表
function handleSubmit(record: PayrollSheetDTO) {
  Modal.confirm({
    title: '确认提交',
    content: `确定要提交工资表 ${record.payrollNo} 吗？提交后将进入待确认状态。`,
    onOk: async () => {
      try {
        await submitPayrollSheet(record.id!);
        message.success('提交成功');
        await fetchPayrollItems();
      } catch (error: any) {
        message.error(error.message || '提交失败');
      }
    },
  });
}

// 财务确认
function handleFinanceConfirm(record: PayrollSheetDTO) {
  Modal.confirm({
    title: '确认财务确认',
    content: `确定要财务确认工资表 ${record.payrollNo} 吗？确认后可以发放工资。`,
    onOk: async () => {
      try {
        await financeConfirmPayrollSheet(record.id!);
        message.success('财务确认成功');
        await fetchPayrollItems();
      } catch (error: any) {
        message.error(error.message || '财务确认失败');
      }
    },
  });
}

// 发放工资
function handleIssue(record: PayrollSheetDTO) {
  issueForm.payrollSheetId = record.id!;
  issueForm.paymentMethod = 'BANK_TRANSFER';
  issueForm.paymentVoucherUrl = '';
  issueForm.remark = '';
  issueModalVisible.value = true;
}

// 确认发放
async function handleConfirmIssue() {
  try {
    await issuePayroll(issueForm.payrollSheetId, issueForm);
    message.success('发放成功');
    issueModalVisible.value = false;
    await fetchPayrollItems();
  } catch (error: any) {
    message.error(error.message || '发放失败');
  }
}

// 判断是否为税费类扣减项
function isTaxDeduction(deductionType: string): boolean {
  if (!deductionType) return false;
  return (
    deductionType === 'INCOME_TAX' ||
    deductionType === 'SOCIAL_INSURANCE' ||
    deductionType === 'HOUSING_FUND' ||
    deductionType.includes('税') ||
    deductionType.includes('社保') ||
    deductionType.includes('公积金')
  );
}

// 计算税费扣减项总额
function calculateTaxDeductionTotal(item: PayrollItemDTO): number {
  if (!item.deductions) return 0;
  return item.deductions
    .filter((d) => isTaxDeduction(d.deductionType || ''))
    .reduce((sum, deduction) => {
      const amount =
        typeof deduction.amount === 'string'
          ? Number.parseFloat(deduction.amount)
          : deduction.amount || 0;
      return sum + amount;
    }, 0);
}

// 计算其他扣减项总额
function calculateOtherDeductionTotal(item: PayrollItemDTO): number {
  if (!item.deductions) return 0;
  return item.deductions
    .filter((d) => !isTaxDeduction(d.deductionType || ''))
    .reduce((sum, deduction) => {
      const amount =
        typeof deduction.amount === 'string'
          ? Number.parseFloat(deduction.amount)
          : deduction.amount || 0;
      return sum + amount;
    }, 0);
}

// 更新扣减项金额并重新计算应发工资和实发工资
function updateDeductionAmount(item: PayrollItemDTO) {
  const taxDeductionTotal = calculateTaxDeductionTotal(item);
  const otherDeductionTotal = calculateOtherDeductionTotal(item);
  const totalDeduction = taxDeductionTotal + otherDeductionTotal;

  item.deductionAmount = totalDeduction;

  // 收入（提成总额）
  const income = calculateIncomeTotal(item);

  // 应发工资 = 收入 - 税费扣减项
  item.grossAmount = income - taxDeductionTotal;

  // 实发工资 = 应发工资 - 其他扣减项
  item.netAmount = item.grossAmount - otherDeductionTotal;
}

// 扣减类型预设选项（AutoComplete组件使用）
const deductionTypeOptions = [
  { value: '个人所得税' },
  { value: '社保个人部分' },
  { value: '公积金个人部分' },
  { value: '办公室租金' },
  { value: '其他欠费' },
  { value: '其他扣款' },
];

// 添加扣减项
function addDeduction() {
  if (!currentItem.value) return;
  if (!currentItem.value.deductions) {
    currentItem.value.deductions = [];
  }
  currentItem.value.deductions.push({
    payrollItemId: currentItem.value.id,
    deductionType: '', // 默认为空，用户可以从预设选项选择或自定义输入
    amount: 0,
    remark: '',
    sourceType: 'MANUAL',
  } as any);
}

// 删除扣减项
function removeDeduction(index: number) {
  if (currentItem.value?.deductions) {
    currentItem.value.deductions.splice(index, 1);
    updateDeductionAmount(currentItem.value);
  }
}

// 发放方式选项
const paymentMethodOptions = [
  { label: '银行转账', value: 'BANK_TRANSFER' },
  { label: '现金', value: 'CASH' },
  { label: '其他', value: 'OTHER' },
];

const canSubmit = (record: PayrollSheetDTO) => {
  return record.status === 'DRAFT';
};

const canFinanceConfirm = (record: PayrollSheetDTO) => {
  return record.status === 'PENDING_CONFIRM' || record.status === 'CONFIRMED';
};

const canIssue = (record: PayrollSheetDTO) => {
  return record.status === 'FINANCE_CONFIRMED';
};

const canExport = (record: PayrollSheetDTO) => {
  return record.status === 'APPROVED' || record.status === 'ISSUED';
};

// 导出工资表
async function handleExport() {
  if (!currentSheet.value || !currentSheet.value.id) {
    message.error('请先选择工资表');
    return;
  }

  if (!canExport(currentSheet.value)) {
    message.error('只有已审批通过的工资表才能导出');
    return;
  }

  try {
    loading.value = true;
    const blob = await exportPayrollSheet(currentSheet.value.id);

    // 创建下载链接
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const filename = `${currentSheet.value.payrollYear}年${currentSheet.value.payrollMonth}月工资表.xlsx`;
    link.download = filename;
    document.body.append(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);

    message.success('导出成功');
  } catch (error: any) {
    message.error(error.message || '导出失败');
  } finally {
    loading.value = false;
  }
}

// 打印工资表
function handlePrint() {
  if (!currentSheet.value) {
    message.error('请先选择工资表');
    return;
  }

  const printWindow = window.open('', '_blank');
  if (!printWindow) {
    message.error('无法打开打印窗口，请检查浏览器弹窗设置');
    return;
  }

  // 生成打印HTML
  const html = generatePrintHtml();

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>${currentSheet.value.payrollYear}年${currentSheet.value.payrollMonth}月工资表</title>
      <style>
        @page { margin: 2cm; size: A4; }
        body { 
          font-family: "SimSun", "宋体", serif; 
          font-size: 14pt; 
          line-height: 1.6;
          color: #000;
          padding: 20px;
        }
        .header { 
          text-align: center; 
          margin-bottom: 30px; 
        }
        .title { 
          font-size: 24pt; 
          font-weight: bold; 
          margin-bottom: 10px;
          letter-spacing: 4pt;
        }
        .subtitle {
          font-size: 12pt;
          color: #666;
        }
        table { 
          width: 100%; 
          border-collapse: collapse; 
          margin: 20px 0; 
        }
        th, td { 
          border: 1px solid #333; 
          padding: 8px; 
          text-align: center;
        }
        th { 
          background: #f5f5f5; 
          font-weight: bold; 
        }
        .summary {
          margin-top: 20px;
          padding: 15px;
          background: #f9f9f9;
          border: 1px solid #ddd;
        }
        .summary-item {
          margin: 5px 0;
          font-size: 13pt;
        }
        .no-print {
          text-align: center;
          margin-top: 20px;
          padding: 20px;
        }
        .no-print button {
          padding: 10px 20px;
          margin: 0 10px;
          font-size: 14pt;
          cursor: pointer;
        }
        @media print {
          .no-print { display: none; }
          body { padding: 0; }
        }
      </style>
    </head>
    <body>
      ${html}
      <div class="no-print">
        <button onclick="window.print()">打印</button>
        <button onclick="window.close()">关闭</button>
      </div>
    </body>
    </html>
  `);
  printWindow.document.close();
}

// 生成打印HTML
function generatePrintHtml(): string {
  if (!currentSheet.value) return '';

  const sheet = currentSheet.value;
  const year = sheet.payrollYear || new Date().getFullYear();
  const month = sheet.payrollMonth || new Date().getMonth() + 1;

  // 表头
  let html = `
    <div class="header">
      <div class="title">工资表</div>
      <div class="subtitle">${year}年${month}月</div>
    </div>
  `;

  // 汇总信息
  html += `
    <div class="summary">
      <div class="summary-item"><strong>工资表编号：</strong>${sheet.payrollNo || '-'}</div>
      <div class="summary-item"><strong>状态：</strong>${sheet.statusName || '-'}</div>
      <div class="summary-item"><strong>总人数：</strong>${sheet.totalEmployees || 0}人</div>
      <div class="summary-item"><strong>应发总额：</strong>${formatCurrency(sheet.totalGrossAmount)}</div>
      <div class="summary-item"><strong>扣减总额：</strong>${formatCurrency(sheet.totalDeductionAmount)}</div>
      <div class="summary-item"><strong>实发总额：</strong>${formatCurrency(sheet.totalNetAmount)}</div>
      <div class="summary-item"><strong>已确认人数：</strong>${sheet.confirmedCount || 0}/${sheet.totalEmployees || 0}</div>
    </div>
  `;

  // 表格
  html += `
    <table>
      <thead>
        <tr>
          <th style="width: 8%;">序号</th>
          <th style="width: 10%;">工号</th>
          <th style="width: 10%;">姓名</th>
          <th style="width: 12%;">收入（提成总额）</th>
          <th style="width: 12%;">税费扣减</th>
          <th style="width: 12%;">应发工资（税后）</th>
          <th style="width: 12%;">其他扣减</th>
          <th style="width: 12%;">实发工资</th>
          <th style="width: 10%;">确认状态</th>
        </tr>
      </thead>
      <tbody>
  `;

  filteredDataSource.value.forEach((item, index) => {
    const income = calculateIncomeTotal(item);
    const taxDeduction = calculateTaxDeductionTotal(item);
    const otherDeduction = calculateOtherDeductionTotal(item);
    const grossAmount =
      typeof item.grossAmount === 'string'
        ? Number.parseFloat(item.grossAmount)
        : item.grossAmount || 0;
    const netAmount =
      typeof item.netAmount === 'string'
        ? Number.parseFloat(item.netAmount)
        : item.netAmount || 0;

    html += `
      <tr>
        <td>${index + 1}</td>
        <td>${item.employeeNo || '-'}</td>
        <td>${item.employeeName || '-'}</td>
        <td>${formatCurrency(income)}</td>
        <td>${formatCurrency(taxDeduction)}</td>
        <td>${formatCurrency(grossAmount)}</td>
        <td>${formatCurrency(otherDeduction)}</td>
        <td>${formatCurrency(netAmount)}</td>
        <td>${item.confirmStatusName || '-'}</td>
      </tr>
    `;
  });

  html += `
      </tbody>
    </table>
  `;

  return html;
}

onMounted(() => {
  fetchPayrollItems();
});
</script>

<template>
  <Page>
    <Card>
      <!-- 搜索表单 -->
      <div class="mb-4">
        <Row :gutter="[16, 16]">
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="queryParams.payrollYear"
              placeholder="年份"
              style="width: 100%"
              :options="yearOptions"
              @change="handleSearch"
            />
          </Col>
          <Col :xs="12" :sm="8" :md="4" :lg="3">
            <Select
              v-model:value="queryParams.payrollMonth"
              placeholder="月份"
              style="width: 100%"
              :options="monthOptions"
              @change="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="8" :md="5" :lg="4">
            <Input
              v-model:value="filterParams.employeeName"
              placeholder="员工姓名"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="8" :md="5" :lg="4">
            <Input
              v-model:value="filterParams.employeeNo"
              placeholder="工号"
              allow-clear
              @press-enter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="16" :md="6" :lg="10">
            <Space wrap>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button
                v-if="currentSheet && canSubmit(currentSheet)"
                type="primary"
                @click="handleSubmit(currentSheet)"
              >
                提交工资表
              </Button>
              <Button
                v-if="currentSheet && canFinanceConfirm(currentSheet)"
                type="primary"
                @click="handleFinanceConfirm(currentSheet)"
              >
                财务确认
              </Button>
              <Button
                v-if="currentSheet && canIssue(currentSheet)"
                type="primary"
                @click="handleIssue(currentSheet)"
              >
                发放工资
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <!-- 工资表汇总信息 -->
      <div v-if="currentSheet" class="mb-4">
        <Space>
          <span><strong>工资表编号：</strong>{{ currentSheet.payrollNo }}</span>
          <span><strong>状态：</strong>{{ currentSheet.statusName }}</span>
          <span
            ><strong>总人数：</strong>{{ currentSheet.totalEmployees }}</span
          >
          <span
            ><strong>应发总额：</strong
            >{{ formatCurrency(currentSheet.totalGrossAmount) }}</span
          >
          <span
            ><strong>扣减总额：</strong
            >{{ formatCurrency(currentSheet.totalDeductionAmount) }}</span
          >
          <span
            ><strong>实发总额：</strong
            >{{ formatCurrency(currentSheet.totalNetAmount) }}</span
          >
          <span
            ><strong>已确认：</strong>{{ currentSheet.confirmedCount }}/{{
              currentSheet.totalEmployees
            }}</span
          >
          <Space v-if="canExport(currentSheet)" style="margin-left: 20px">
            <Button type="default" @click="handleExport">导出Excel</Button>
            <Button type="default" @click="handlePrint">打印</Button>
          </Space>
        </Space>
      </div>

      <!-- 提示信息 -->
      <div
        v-if="
          !currentSheet && queryParams.payrollYear && queryParams.payrollMonth
        "
        class="mb-4"
      >
        <div
          style="
            padding: 16px;
            background: #e6f7ff;
            border: 1px solid #91d5ff;
            border-radius: 4px;
          "
        >
          <span style="color: #1890ff">
            当前显示所有符合条件的员工工资信息。员工属性确认后自动显示在工资列表中，离职后次月起不再显示。
          </span>
        </div>
      </div>

      <!-- 员工工资明细表格 -->
      <Table
        :columns="columns"
        :data-source="filteredDataSource"
        :loading="loading"
        :pagination="false"
        row-key="employeeId"
        size="small"
        :virtual="filteredDataSource.length > 50"
        :scroll="{ y: 600 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'income'">
            <span
              :style="{
                color:
                  calculateIncomeTotal(record as PayrollItemDTO) < 0
                    ? '#ff4d4f'
                    : '#1890ff',
                fontWeight: '500',
              }"
            >
              {{
                formatCurrency(calculateIncomeTotal(record as PayrollItemDTO))
              }}
            </span>
          </template>
          <template v-if="column.key === 'grossAmount'">
            <span
              :style="{
                color:
                  (typeof record.grossAmount === 'string'
                    ? parseFloat(record.grossAmount)
                    : record.grossAmount || 0) < 0
                    ? '#ff4d4f'
                    : '',
              }"
            >
              {{ formatCurrency(record.grossAmount) }}
            </span>
          </template>
          <template v-if="column.key === 'deductionAmount'">
            {{ formatCurrency(record.deductionAmount) }}
          </template>
          <template v-if="column.key === 'netAmount'">
            <span
              :style="{
                color:
                  (typeof record.netAmount === 'string'
                    ? parseFloat(record.netAmount)
                    : record.netAmount || 0) < 0
                    ? '#ff4d4f'
                    : '',
              }"
            >
              {{ formatCurrency(record.netAmount) }}
            </span>
          </template>
          <template v-if="column.key === 'confirmStatusName'">
            <Tag
              :color="
                record.confirmStatus === 'PENDING'
                  ? 'orange'
                  : record.confirmStatus === 'CONFIRMED'
                    ? 'green'
                    : 'red'
              "
            >
              {{ record.confirmStatusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Button
              type="link"
              size="small"
              @click="handleEditItem(record as unknown as PayrollItemDTO)"
            >
              编辑扣减项
            </Button>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 编辑工资明细弹窗 -->
    <Modal
      v-model:open="itemEditModalVisible"
      title="编辑工资明细"
      width="800px"
      @ok="handleSaveItem"
    >
      <div v-if="currentItem">
        <Descriptions :column="2" bordered>
          <DescriptionsItem label="工号">
            {{ currentItem.employeeNo }}
          </DescriptionsItem>
          <DescriptionsItem label="姓名">
            {{ currentItem.employeeName }}
          </DescriptionsItem>
        </Descriptions>

        <Divider>收入项明细（自动从提成数据导入，不可编辑）</Divider>
        <Table
          :columns="[
            {
              title: '收入类型',
              dataIndex: 'incomeType',
              key: 'incomeType',
              width: 150,
            },
            { title: '金额', dataIndex: 'amount', key: 'amount', width: 150 },
            { title: '说明', dataIndex: 'remark', key: 'remark' },
          ]"
          :data-source="currentItem.incomes"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'incomeType'">
              {{ getIncomeTypeName(record.incomeType) }}
            </template>
            <template v-if="column.key === 'amount'">
              {{ formatCurrency(record.amount) }}
            </template>
            <template v-if="column.key === 'remark'">
              {{ record.remark || '-' }}
            </template>
          </template>
        </Table>

        <Divider>扣减项明细</Divider>
        <Table
          :columns="[
            {
              title: '扣减类型',
              dataIndex: 'deductionType',
              key: 'deductionType',
              width: 150,
            },
            { title: '类别', key: 'category', width: 100 },
            { title: '金额', dataIndex: 'amount', key: 'amount', width: 150 },
            { title: '说明（必填）', dataIndex: 'remark', key: 'remark' },
            { title: '操作', key: 'action', width: 80 },
          ]"
          :data-source="currentItem.deductions"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'deductionType'">
              <AutoComplete
                v-model:value="record.deductionType"
                :options="deductionTypeOptions"
                placeholder="请选择或输入扣减类型"
                style="width: 100%"
                allow-clear
                @change="() => updateDeductionAmount(currentItem!)"
              />
            </template>
            <template v-if="column.key === 'category'">
              <Tag
                :color="
                  isTaxDeduction(record.deductionType || '') ? 'blue' : 'orange'
                "
              >
                {{
                  isTaxDeduction(record.deductionType || '')
                    ? '税费类'
                    : '其他类'
                }}
              </Tag>
            </template>
            <template v-if="column.key === 'amount'">
              <InputNumber
                v-model:value="record.amount"
                :precision="2"
                style="width: 100%"
                @change="() => updateDeductionAmount(currentItem!)"
              />
            </template>
            <template v-if="column.key === 'remark'">
              <Input
                v-model:value="record.remark"
                placeholder="请输入扣减说明（必填）"
                style="width: 100%"
                :required="true"
              />
            </template>
            <template v-if="column.key === 'action'">
              <Button
                type="link"
                size="small"
                danger
                @click="removeDeduction(index)"
              >
                删除
              </Button>
            </template>
          </template>
        </Table>
        <Button type="dashed" block class="mt-2" @click="addDeduction">
          添加扣减项
        </Button>
        <div style="margin-top: 8px; font-size: 12px; color: #999">
          <div>
            • 税费类扣减项（个人所得税、社保、公积金等）用于计算应发工资
          </div>
          <div>• 其他扣减项（预支、欠款、办公室租金等）用于计算实发工资</div>
          <div>• 工资可以为负数（当扣减项大于收入时）</div>
        </div>

        <Divider>确认截止时间设置</Divider>
        <Form layout="inline" class="mb-4">
          <FormItem label="确认截止时间">
            <DatePicker
              v-model:value="currentItemConfirmDeadline"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择确认截止时间（默认每月27日24时）"
              style="width: 300px"
            />
            <div style="margin-top: 4px; font-size: 12px; color: #999">
              超过此时间未确认，系统将自动确认。默认：每月27日24时
            </div>
          </FormItem>
        </Form>

        <Divider>汇总</Divider>
        <Descriptions :column="4" bordered>
          <DescriptionsItem label="收入（提成总额）">
            <span
              :style="{
                color: calculateIncomeTotal(currentItem) < 0 ? '#ff4d4f' : '',
              }"
            >
              {{ formatCurrency(calculateIncomeTotal(currentItem)) }}
            </span>
          </DescriptionsItem>
          <DescriptionsItem label="税费扣减">
            {{ formatCurrency(calculateTaxDeductionTotal(currentItem)) }}
          </DescriptionsItem>
          <DescriptionsItem label="应发工资（税后）">
            <span
              :style="{
                color:
                  (typeof currentItem.grossAmount === 'string'
                    ? parseFloat(currentItem.grossAmount)
                    : currentItem.grossAmount || 0) < 0
                    ? '#ff4d4f'
                    : '',
              }"
            >
              {{ formatCurrency(currentItem.grossAmount) }}
            </span>
          </DescriptionsItem>
          <DescriptionsItem label="其他扣减">
            {{ formatCurrency(calculateOtherDeductionTotal(currentItem)) }}
          </DescriptionsItem>
          <DescriptionsItem label="实发工资" :span="2">
            <span
              :style="{
                color:
                  (typeof currentItem.netAmount === 'string'
                    ? parseFloat(currentItem.netAmount)
                    : currentItem.netAmount || 0) < 0
                    ? '#ff4d4f'
                    : '',
              }"
            >
              {{ formatCurrency(currentItem.netAmount) }}
            </span>
          </DescriptionsItem>
        </Descriptions>
      </div>
    </Modal>

    <!-- 发放工资弹窗 -->
    <Modal
      v-model:open="issueModalVisible"
      title="发放工资"
      width="500px"
      @ok="handleConfirmIssue"
    >
      <Form :model="issueForm" layout="vertical">
        <FormItem label="发放方式" required>
          <Select
            v-model:value="issueForm.paymentMethod"
            :options="paymentMethodOptions"
          />
        </FormItem>
        <FormItem label="发放凭证URL">
          <Input
            v-model:value="issueForm.paymentVoucherUrl"
            placeholder="请输入凭证URL"
          />
        </FormItem>
        <FormItem label="备注">
          <Input.TextArea
            v-model:value="issueForm.remark"
            :rows="3"
            placeholder="请输入备注"
          />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
