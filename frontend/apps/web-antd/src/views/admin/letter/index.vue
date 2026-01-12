<script setup lang="ts">
import type { LetterPrintData } from '@vben/utils';

import type { LetterApplicationDTO } from '#/api/admin';

import { computed, h, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { printLetter } from '@vben/utils';

import {
  Badge,
  Button,
  Card,
  Col,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  TabPane,
  Tabs,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  approveApplication,
  confirmPrint,
  confirmReceive,
  getAllApplications,
  rejectApplication,
  returnApplication,
  updateLetterContent,
} from '#/api/admin';
import { getLetterQrCode } from '#/api/admin/letter';
import { getConfigValue } from '#/api/system';
import RichTextEditor from '#/components/RichTextEditor/index.vue';

defineOptions({ name: 'AdminLetter' });

// 状态
const loading = ref(false);
const activeTab = ref('all');
const allApplications = ref<LetterApplicationDTO[]>([]);
const pendingList = ref<LetterApplicationDTO[]>([]);
const completedList = ref<LetterApplicationDTO[]>([]);
const firmName = ref('');

// 详情弹框
const detailVisible = ref(false);
const detailRecord = ref<LetterApplicationDTO | null>(null);
const editingContent = ref('');
const saving = ref(false);

// 查询参数
const queryParams = reactive({
  applicationNo: '',
  matterName: '',
  status: undefined as string | undefined,
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
});

// 统计数据
const stats = computed(() => {
  const pending = allApplications.value.filter(
    (a) => a.status === 'PENDING',
  ).length;
  const approved = allApplications.value.filter(
    (a) => a.status === 'APPROVED',
  ).length;
  const printed = allApplications.value.filter(
    (a) => a.status === 'PRINTED',
  ).length;
  const received = allApplications.value.filter(
    (a) => a.status === 'RECEIVED',
  ).length;
  return {
    pending,
    approved,
    printed,
    received,
    total: allApplications.value.length,
  };
});

// 表格列
const columns = [
  {
    title: '申请编号',
    dataIndex: 'applicationNo',
    key: 'applicationNo',
    width: 140,
  },
  {
    title: '函件类型',
    dataIndex: 'letterTypeName',
    key: 'letterTypeName',
    width: 100,
  },
  {
    title: '项目',
    dataIndex: 'matterName',
    key: 'matterName',
    width: 180,
    ellipsis: true,
  },
  {
    title: '接收单位',
    dataIndex: 'targetUnit',
    key: 'targetUnit',
    width: 180,
    ellipsis: true,
  },
  {
    title: '出函律师',
    dataIndex: 'lawyerNames',
    key: 'lawyerNames',
    width: 120,
  },
  { title: '份数', dataIndex: 'copies', key: 'copies', width: 60 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  {
    title: '申请人',
    dataIndex: 'applicantName',
    key: 'applicantName',
    width: 80,
  },
  { title: '申请时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 220, fixed: 'right' as const },
];

// 加载律所名称
async function loadFirmName() {
  try {
    const config = await getConfigValue('firm.name');
    if (config && config.configValue) {
      firmName.value = config.configValue;
    }
  } catch (error) {
    console.warn('获取律所名称失败，使用默认值', error);
  }
}

// 加载全部数据
async function fetchAllData() {
  loading.value = true;
  try {
    const res = await getAllApplications(queryParams);
    allApplications.value = res || [];
    // 分类
    pendingList.value = allApplications.value.filter(
      (a) =>
        a.status === 'PENDING' ||
        a.status === 'APPROVED' ||
        a.status === 'PRINTED',
    );
    completedList.value = allApplications.value.filter(
      (a) =>
        a.status === 'RECEIVED' ||
        a.status === 'REJECTED' ||
        a.status === 'CANCELLED',
    );
  } catch (error: any) {
    message.error(error.message || '加载数据失败');
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleSearch() {
  fetchAllData();
}

// 重置
function handleReset() {
  queryParams.applicationNo = '';
  queryParams.matterName = '';
  queryParams.status = undefined;
  queryParams.startDate = undefined;
  queryParams.endDate = undefined;
  fetchAllData();
}

// Tab切换
function handleTabChange() {
  // 数据已经在fetchAllData中分类好了
}

// 获取当前数据源
function getCurrentDataSource() {
  if (activeTab.value === 'all') return allApplications.value;
  if (activeTab.value === 'pending') return pendingList.value;
  if (activeTab.value === 'completed') return completedList.value;
  return [];
}

// 审批通过
function handleApprove(record: LetterApplicationDTO) {
  Modal.confirm({
    title: '确认审批',
    content: `确定要审批通过 "${record.applicationNo}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await approveApplication(record.id);
        message.success('审批通过');
        fetchAllData();
      } catch (error: any) {
        message.error(error.message || '审批失败');
      }
    },
  });
}

// 审批拒绝
function handleReject(record: LetterApplicationDTO) {
  const rejectReasonRef = ref<string>('');

  Modal.confirm({
    title: '拒绝审批',
    width: 500,
    content: () => {
      return h('div', [
        h(
          'p',
          { style: 'margin-bottom: 12px' },
          `确定要拒绝申请 "${record.applicationNo}" 吗？`,
        ),
        h(Textarea, {
          value: rejectReasonRef.value,
          placeholder: '请输入拒绝原因（必填）',
          rows: 4,
          'onUpdate:value': (value: string) => {
            rejectReasonRef.value = value;
          },
        }),
      ]);
    },
    okText: '确认拒绝',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      if (!rejectReasonRef.value?.trim()) {
        message.error('请输入拒绝原因');
        throw undefined;
      }
      try {
        await rejectApplication(record.id, rejectReasonRef.value.trim());
        message.success('已拒绝');
        fetchAllData();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

// 退回修改
function handleReturn(record: LetterApplicationDTO) {
  const returnReasonRef = ref<string>('');

  Modal.confirm({
    title: '退回修改',
    width: 500,
    content: () => {
      return h('div', [
        h(
          'p',
          { style: 'margin-bottom: 12px' },
          `确定要退回申请 "${record.applicationNo}" 让申请人修改吗？`,
        ),
        h(Textarea, {
          value: returnReasonRef.value,
          placeholder: '请输入退回原因（必填）',
          rows: 4,
          'onUpdate:value': (value: string) => {
            returnReasonRef.value = value;
          },
        }),
      ]);
    },
    okText: '确认退回',
    cancelText: '取消',
    okButtonProps: { danger: false },
    onOk: async () => {
      if (!returnReasonRef.value?.trim()) {
        message.error('请输入退回原因');
        throw undefined;
      }
      try {
        await returnApplication(record.id, returnReasonRef.value.trim());
        message.success('已退回');
        fetchAllData();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

// 确认打印
function handlePrint(record: LetterApplicationDTO) {
  Modal.confirm({
    title: '确认已打印',
    content: `确认已打印 "${record.applicationNo}" 的函件（${record.copies}份）吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await confirmPrint(record.id);
        message.success('已确认打印');
        fetchAllData();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

// 确认领取
function handleReceive(record: LetterApplicationDTO) {
  Modal.confirm({
    title: '确认领取',
    content: `确认律师已领取 "${record.applicationNo}" 的函件吗？`,
    okText: '确认领取',
    cancelText: '取消',
    onOk: async () => {
      try {
        await confirmReceive(record.id);
        message.success('已确认领取');
        fetchAllData();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

// 查看详情
function handleView(record: LetterApplicationDTO) {
  detailRecord.value = record;
  editingContent.value = record.content || '';
  detailVisible.value = true;
}

// 保存函件内容
async function handleSaveContent() {
  if (!detailRecord.value) return;

  saving.value = true;
  try {
    await updateLetterContent(detailRecord.value.id, editingContent.value);
    message.success('函件内容已保存');
    // 更新本地数据
    if (detailRecord.value) {
      detailRecord.value.content = editingContent.value;
    }
    // 刷新列表
    fetchAllData();
  } catch (error: any) {
    message.error(error.message || '保存失败');
  } finally {
    saving.value = false;
  }
}

// 打印函件（从详情弹框）
function handlePrintFromDetail() {
  if (detailRecord.value) {
    // 使用当前编辑的内容创建临时记录
    const tempRecord = { ...detailRecord.value, content: editingContent.value };
    showPrintOptions(tempRecord);
  }
}

// 打印选项状态
const printWithQrCode = ref(true); // 默认打印二维码

// 显示打印选项弹窗
function showPrintOptions(record: LetterApplicationDTO) {
  printWithQrCode.value = true; // 重置为默认值
  
  Modal.confirm({
    title: '打印选项',
    width: 400,
    content: () => {
      return h('div', { style: 'padding: 16px 0' }, [
        h('div', { style: 'margin-bottom: 16px; color: #666' }, 
          `即将打印函件：${record.applicationNo}`
        ),
        h('div', { style: 'display: flex; align-items: center; gap: 8px' }, [
          h('input', {
            type: 'checkbox',
            checked: printWithQrCode.value,
            id: 'printQrCodeCheckbox',
            style: 'width: 16px; height: 16px; cursor: pointer',
            onChange: (e: Event) => {
              printWithQrCode.value = (e.target as HTMLInputElement).checked;
            },
          }),
          h('label', { 
            for: 'printQrCodeCheckbox',
            style: 'cursor: pointer; user-select: none' 
          }, '打印验证二维码'),
        ]),
        h('div', { style: 'margin-top: 8px; font-size: 12px; color: #999' }, 
          '二维码用于扫码验证函件真伪，推荐启用'
        ),
      ]);
    },
    okText: '打印',
    cancelText: '取消',
    onOk: async () => {
      await executePrint(record, printWithQrCode.value);
    },
  });
}

// 执行打印（内部函数）
async function executePrint(record: LetterApplicationDTO, withQrCode: boolean) {
  try {
    let qrCodeBase64: string | undefined;
    
    if (withQrCode) {
      // 获取验证二维码
      try {
        const qrCodeRes = await getLetterQrCode(record.id, 120); // 120px适合打印（约70pt）
        qrCodeBase64 = qrCodeRes.qrCodeBase64;
      } catch (qrError: any) {
        console.warn('获取二维码失败，将不显示二维码:', qrError.message);
        message.warning('获取二维码失败，将不打印二维码');
      }
    }

    const printData: LetterPrintData = {
      letterTypeName: record.letterTypeName || '介绍信',
      applicationNo: record.applicationNo,
      targetUnit: record.targetUnit,
      targetContact: record.targetContact,
      targetPhone: record.targetPhone,
      targetAddress: record.targetAddress,
      content: record.content || record.purpose || '',
      lawyerNames: record.lawyerNames,
      firmName: firmName.value,
      date: dayjs().format('YYYY年MM月DD日'),
      qrCodeBase64, // 根据选择添加或不添加二维码
    };
    printLetter(printData);
  } catch (error: any) {
    message.error(error.message || '打印失败');
  }
}

// 打印函件内容（直接调用，带选项弹窗）
function handlePrintContent(record: LetterApplicationDTO) {
  showPrintOptions(record);
}

// 下载为Word文档
function handleDownloadWord(record: LetterApplicationDTO) {
  // 创建Word文档的HTML格式
  const wordContent = `
    <html xmlns:o="urn:schemas-microsoft-com:office:office" 
          xmlns:w="urn:schemas-microsoft-com:office:word" 
          xmlns="http://www.w3.org/TR/REC-html40">
    <head>
      <meta charset="UTF-8">
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
      <!--[if gte mso 9]>
      <xml>
        <w:WordDocument>
          <w:View>Print</w:View>
          <w:Zoom>100</w:Zoom>
        </w:WordDocument>
      </xml>
      <![endif]-->
      <style>
        body { 
          font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
          line-height: 28pt; 
          font-size: 16pt; 
        }
        * { font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; }
        .header { 
          text-align: center; 
          margin-bottom: 30pt; 
          font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
        }
        .title { 
          font-size: 18pt; 
          font-weight: bold; 
          font-family: "SimHei", "黑体", sans-serif;
          letter-spacing: 2pt; 
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
          white-space: pre-wrap; 
          text-indent: 2em; 
          text-align: justify; 
          font-family: "FangSong", "仿宋_GB2312", "仿宋", serif; 
          font-size: 16pt; 
          line-height: 28pt;
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
      </style>
    </head>
    <body>
      <div class="header">
        <div class="title">${record.letterTypeName || '介绍信'}</div>
        <div class="letter-no">编号：${record.applicationNo}</div>
      </div>
      <div class="recipient">${record.targetUnit}：</div>
      <div class="content">${record.content || record.purpose}</div>
      <div class="footer">
        <div class="footer-item">出函律师：${record.lawyerNames || '-'}</div>
        <div class="footer-item" style="margin-top: 20pt;">${firmName.value}</div>
        <div class="footer-item">${dayjs().format('YYYY年MM月DD日')}</div>
        <div class="seal-area">（盖章处）</div>
      </div>
    </body>
    </html>
  `;

  // 创建Blob并下载
  const blob = new Blob([`\uFEFF${wordContent}`], {
    type: 'application/msword',
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${record.letterTypeName}_${record.applicationNo}.doc`;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
  message.success('文档已下载');
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'blue',
    REJECTED: 'red',
    RETURNED: 'gold',
    PRINTED: 'cyan',
    RECEIVED: 'green',
    CANCELLED: 'default',
  };
  return colorMap[status] || 'default';
}

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待审批', value: 'PENDING' },
  { label: '已审批', value: 'APPROVED' },
  { label: '已打印', value: 'PRINTED' },
  { label: '已领取', value: 'RECEIVED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已退回', value: 'RETURNED' },
  { label: '已取消', value: 'CANCELLED' },
];

onMounted(() => {
  loadFirmName();
  fetchAllData();
});
</script>

<template>
  <Page title="出函管理" description="行政人员管理出函申请的审批、打印和领取">
    <!-- 统计卡片 -->
    <Row :gutter="16" style="margin-bottom: 16px">
      <Col :span="4">
        <Card size="small">
          <Statistic
            title="待审批"
            :value="stats.pending"
            :value-style="{ color: '#faad14' }"
          />
        </Card>
      </Col>
      <Col :span="4">
        <Card size="small">
          <Statistic
            title="待打印"
            :value="stats.approved"
            :value-style="{ color: '#1890ff' }"
          />
        </Card>
      </Col>
      <Col :span="4">
        <Card size="small">
          <Statistic
            title="待领取"
            :value="stats.printed"
            :value-style="{ color: '#13c2c2' }"
          />
        </Card>
      </Col>
      <Col :span="4">
        <Card size="small">
          <Statistic
            title="已完成"
            :value="stats.received"
            :value-style="{ color: '#52c41a' }"
          />
        </Card>
      </Col>
      <Col :span="4">
        <Card size="small">
          <Statistic title="总申请" :value="stats.total" />
        </Card>
      </Col>
    </Row>

    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :span="5">
            <Input
              v-model:value="queryParams.applicationNo"
              placeholder="申请编号"
              allow-clear
            />
          </Col>
          <Col :span="5">
            <Input
              v-model:value="queryParams.matterName"
              placeholder="项目名称"
              allow-clear
            />
          </Col>
          <Col :span="4">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allow-clear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :span="6">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Tabs v-model:active-key="activeTab" @change="handleTabChange">
        <TabPane key="all">
          <template #tab>
            <span
              >全部申请
              <Badge
                :count="stats.total"
                :number-style="{ backgroundColor: '#999' }"
            /></span>
          </template>
        </TabPane>
        <TabPane key="pending">
          <template #tab>
            <span
              >待办理
              <Badge
                :count="pendingList.length"
                :number-style="{ backgroundColor: '#faad14' }"
            /></span>
          </template>
        </TabPane>
        <TabPane key="completed">
          <template #tab>
            <span
              >已完成
              <Badge
                :count="completedList.length"
                :number-style="{ backgroundColor: '#52c41a' }"
            /></span>
          </template>
        </TabPane>
      </Tabs>

      <Table
        :columns="columns"
        :data-source="getCurrentDataSource()"
        :loading="loading"
        :pagination="{
          pageSize: 15,
          showTotal: (t: number) => `共 ${t} 条`,
          showSizeChanger: true,
        }"
        row-key="id"
        :scroll="{ x: 1500 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'statusName'">
            <Tag
              :color="getStatusColor((record as LetterApplicationDTO).status)"
            >
              {{ (record as LetterApplicationDTO).statusName }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record as LetterApplicationDTO)">查看</a>

              <!-- 待审批状态：审批操作 -->
              <template
                v-if="(record as LetterApplicationDTO).status === 'PENDING'"
              >
                <a
                  style="color: #52c41a"
                  @click="handleApprove(record as LetterApplicationDTO)"
                  >通过</a
                >
                <a
                  style="color: #faad14"
                  @click="handleReturn(record as LetterApplicationDTO)"
                  >退回</a
                >
                <a
                  style="color: red"
                  @click="handleReject(record as LetterApplicationDTO)"
                  >拒绝</a
                >
              </template>

              <!-- 已审批状态：打印操作 -->
              <template
                v-if="(record as LetterApplicationDTO).status === 'APPROVED'"
              >
                <a
                  style="color: #1890ff"
                  @click="handlePrintContent(record as LetterApplicationDTO)"
                  >打印</a
                >
                <a
                  style="color: #722ed1"
                  @click="handleDownloadWord(record as LetterApplicationDTO)"
                  >下载</a
                >
                <a @click="handlePrint(record as LetterApplicationDTO)"
                  >确认已打印</a
                >
              </template>

              <!-- 已打印状态：领取操作 + 补打 -->
              <template
                v-if="(record as LetterApplicationDTO).status === 'PRINTED'"
              >
                <a @click="handleReceive(record as LetterApplicationDTO)"
                  >确认领取</a
                >
                <a
                  style="color: #1890ff"
                  @click="handlePrintContent(record as LetterApplicationDTO)"
                  >补打</a
                >
                <a
                  style="color: #722ed1"
                  @click="handleDownloadWord(record as LetterApplicationDTO)"
                  >下载</a
                >
              </template>

              <!-- 已领取状态：补打 -->
              <template
                v-if="(record as LetterApplicationDTO).status === 'RECEIVED'"
              >
                <a
                  style="color: #1890ff"
                  @click="handlePrintContent(record as LetterApplicationDTO)"
                  >补打</a
                >
                <a
                  style="color: #722ed1"
                  @click="handleDownloadWord(record as LetterApplicationDTO)"
                  >下载</a
                >
              </template>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 详情弹框（可编辑） -->
    <Modal
      v-model:open="detailVisible"
      title="出函申请详情"
      width="900px"
      :footer="null"
      :mask-closable="false"
    >
      <div v-if="detailRecord" style="max-height: 80vh; overflow-y: auto">
        <!-- 基本信息 -->
        <Card size="small" style="margin-bottom: 16px">
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px">
            <div>
              <strong>申请编号：</strong>{{ detailRecord.applicationNo }}
            </div>
            <div>
              <strong>函件类型：</strong>{{ detailRecord.letterTypeName }}
            </div>
            <div>
              <strong>项目：</strong>{{ detailRecord.matterName || '-' }}
            </div>
            <div>
              <strong>项目编号：</strong>{{ detailRecord.matterNo || '-' }}
            </div>
            <div><strong>接收单位：</strong>{{ detailRecord.targetUnit }}</div>
            <div>
              <strong>联系人：</strong>{{ detailRecord.targetContact || '-' }}
            </div>
            <div>
              <strong>联系电话：</strong>{{ detailRecord.targetPhone || '-' }}
            </div>
            <div><strong>份数：</strong>{{ detailRecord.copies }}</div>
            <div>
              <strong>出函律师：</strong>{{ detailRecord.lawyerNames || '-' }}
            </div>
            <div>
              <strong>期望日期：</strong>{{ detailRecord.expectedDate || '-' }}
            </div>
            <div><strong>申请人：</strong>{{ detailRecord.applicantName }}</div>
            <div>
              <strong>状态：</strong
              ><Tag :color="getStatusColor(detailRecord.status)">
                {{ detailRecord.statusName }}
              </Tag>
            </div>
          </div>
          <div style="margin-top: 12px">
            <strong>出函事由：</strong>{{ detailRecord.purpose }}
          </div>
          <div v-if="detailRecord.approvalComment" style="margin-top: 12px">
            <strong>审批意见：</strong>{{ detailRecord.approvalComment }}
          </div>
          <div v-if="detailRecord.remark" style="margin-top: 12px">
            <strong>备注：</strong>{{ detailRecord.remark }}
          </div>
          <div
            style="
              padding-top: 12px;
              margin-top: 12px;
              font-size: 12px;
              color: #999;
              border-top: 1px solid #e8e8e8;
            "
          >
            <div>申请时间：{{ detailRecord.createdAt }}</div>
            <div v-if="detailRecord.approvedAt">
              审批时间：{{ detailRecord.approvedAt }}（{{
                detailRecord.approverName || '-'
              }}）
            </div>
            <div v-if="detailRecord.printedAt">
              打印时间：{{ detailRecord.printedAt }}（{{
                detailRecord.printerName || '-'
              }}）
            </div>
            <div v-if="detailRecord.receivedAt">
              领取时间：{{ detailRecord.receivedAt }}
            </div>
          </div>
        </Card>

        <!-- 函件内容编辑 -->
        <Card size="small" title="函件内容">
          <RichTextEditor
            v-model="editingContent"
            height="500px"
            placeholder="请输入函件内容..."
            :show-variables="false"
          />
        </Card>

        <!-- 操作按钮 -->
        <div
          style="
            padding-top: 16px;
            margin-top: 16px;
            text-align: right;
            border-top: 1px solid #e8e8e8;
          "
        >
          <Space>
            <Button @click="detailVisible = false">关闭</Button>
            <Button type="primary" :loading="saving" @click="handleSaveContent">
              保存内容
            </Button>
            <Button type="default" @click="handlePrintFromDetail">打印</Button>
          </Space>
        </div>
      </div>
    </Modal>
  </Page>
</template>
