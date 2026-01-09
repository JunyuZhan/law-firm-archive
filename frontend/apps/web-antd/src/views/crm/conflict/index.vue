<script setup lang="ts">
import { ref, reactive, h } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Button,
  Space,
  Input,
  Select,
  Form,
  FormItem,
  Textarea,
  Row,
  Col,
  Tag,
  Upload,
  Spin,
  Alert,
  Tooltip,
} from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { Plus } from '@vben/icons';
import type { VxeGridProps } from '#/adapter/vxe-table';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getConflictCheckList,
  applyConflictCheck,
  approveConflictCheck,
  rejectConflictCheck,
  applyExemption,
  approveExemption,
  rejectExemption,
} from '#/api/client';
import { recognizeIdCard, recognizeBusinessLicense, type OcrResultDTO } from '#/api/ocr';
import type { ConflictCheckDTO, ConflictCheckQuery, ApplyConflictCheckCommand } from '#/api/client/types';

defineOptions({ name: 'CrmConflict' });

// ==================== 状态定义 ====================

const modalVisible = ref(false);
const formRef = ref();
const ocrLoading = ref(false);
const queryParams = ref<ConflictCheckQuery>({
  pageNum: 1,
  pageSize: 10,
  checkNo: undefined,
  clientName: undefined,
  status: undefined,
  result: undefined,
});

const formData = reactive<ApplyConflictCheckCommand>({
  clientName: '',
  opposingParty: '',
  matterName: '',
  checkType: 'NEW_CLIENT',
  remark: '',
});

// ==================== 常量选项 ====================

const checkTypeOptions = [
  { label: '新客户', value: 'NEW_CLIENT' },
  { label: '新项目', value: 'NEW_MATTER' },
];

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '待检查', value: 'PENDING' },
  { label: '已通过', value: 'PASSED' },
  { label: '存在冲突', value: 'CONFLICT' },
  { label: '豁免待审批', value: 'EXEMPTION_PENDING' },
  { label: '已豁免', value: 'WAIVED' },
  { label: '已拒绝', value: 'REJECTED' },
];

// ==================== 表格配置 ====================

const gridColumns: VxeGridProps['gridOptions']['columns'] = [
  { title: '审查编号', field: 'checkNo', width: 130 },
  { title: '客户名称', field: 'clientName', minWidth: 150 },
  { title: '对方当事人', field: 'opposingParty', width: 150 },
  { title: '项目名称', field: 'matterName', minWidth: 180, showOverflow: true },
  { title: '审查类型', field: 'checkTypeName', width: 100 },
  { title: '状态', field: 'statusName', width: 100, slots: { default: 'status' } },
  { title: '结果', field: 'resultName', width: 100, slots: { default: 'result' } },
  { title: '申请人', field: 'applicantName', width: 100 },
  { title: '申请时间', field: 'applyTime', width: 160 },
  { title: '操作', field: 'action', width: 180, fixed: 'right', slots: { default: 'action' } },
];

async function loadData({ page }: { page: { currentPage: number; pageSize: number } }) {
  const params = { ...queryParams.value, pageNum: page.currentPage, pageSize: page.pageSize };
  const res = await getConflictCheckList(params);
  return { items: res.list || [], total: res.total || 0 };
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    pagerConfig: {},
    proxyConfig: { ajax: { query: loadData } },
  },
});

// ==================== 搜索操作 ====================

function handleSearch() {
  gridApi.reload();
}

function handleReset() {
  queryParams.value = { pageNum: 1, pageSize: 10, checkNo: undefined, clientName: undefined, status: undefined, result: undefined };
  gridApi.reload();
}

// ==================== 申请操作 ====================

function handleAdd() {
  Object.assign(formData, {
    clientName: '',
    opposingParty: '',
    matterName: '',
    checkType: 'NEW_CLIENT',
    remark: '',
  });
  modalVisible.value = true;
}

async function handleSave() {
  try {
    await formRef.value?.validate();
    await applyConflictCheck(formData);
    message.success('申请成功');
    modalVisible.value = false;
    gridApi.reload();
  } catch (error: any) {
    if (error?.errorFields) return;
    message.error(error.message || '申请失败');
  }
}

// OCR识别对方当事人身份证
async function handleOcrIdCard(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeIdCard(file);
    if (result.success && result.name) {
      formData.opposingParty = result.name;
      message.success(`身份证识别成功！对方当事人: ${result.name}`);
    } else {
      message.error(result.errorMessage || '身份证识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || '身份证识别失败');
  } finally {
    ocrLoading.value = false;
  }
  return false;
}

// OCR识别对方当事人营业执照
async function handleOcrLicense(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeBusinessLicense(file);
    if (result.success && result.companyName) {
      formData.opposingParty = result.companyName;
      message.success(`营业执照识别成功！对方当事人: ${result.companyName}`);
    } else {
      message.error(result.errorMessage || '营业执照识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || '营业执照识别失败');
  } finally {
    ocrLoading.value = false;
  }
  return false;
}

// ==================== 审核操作 ====================

function handleApprove(row: ConflictCheckDTO) {
  const isExemption = row.status === 'EXEMPTION_PENDING';
  Modal.confirm({
    title: isExemption ? '批准豁免' : '确认通过',
    content: isExemption 
      ? `确定要批准利冲豁免申请 "${row.checkNo}" 吗？`
      : `确定要通过利冲审查 "${row.checkNo}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        if (isExemption) {
          await approveExemption(row.id, '批准豁免');
        } else {
          await approveConflictCheck(row.id);
        }
        message.success(isExemption ? '已批准豁免' : '审核通过');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

function handleReject(row: ConflictCheckDTO) {
  const isExemption = row.status === 'EXEMPTION_PENDING';
  Modal.confirm({
    title: isExemption ? '拒绝豁免' : '确认驳回',
    content: isExemption 
      ? `确定要拒绝利冲豁免申请 "${row.checkNo}" 吗？`
      : `确定要驳回利冲审查 "${row.checkNo}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const comment = prompt(isExemption ? '请输入拒绝原因:' : '请输入驳回原因:');
      if (comment !== null) {
        try {
          if (isExemption) {
            await rejectExemption(row.id, comment);
          } else {
            await rejectConflictCheck(row.id, comment);
          }
          message.success(isExemption ? '已拒绝豁免' : '已驳回');
          gridApi.reload();
        } catch (error: any) {
          message.error(error.message || '操作失败');
        }
      }
    },
  });
}

// ==================== 详情与豁免 ====================

function handleViewDetail(row: ConflictCheckDTO) {
  Modal.info({
    title: `利冲检查详情 - ${row.checkNo}`,
    width: 600,
    content: h('div', { style: 'padding: 16px 0' }, [
      h('p', {}, [h('strong', {}, '客户名称: '), row.clientName || '-']),
      h('p', {}, [h('strong', {}, '对方当事人: '), row.opposingParty || '-']),
      h('p', {}, [h('strong', {}, '项目名称: '), row.matterName || '-']),
      h('p', {}, [h('strong', {}, '审查类型: '), row.checkTypeName || '-']),
      h('p', {}, [h('strong', {}, '状态: '), row.statusName || '-']),
      h('p', {}, [h('strong', {}, '申请人: '), row.applicantName || '-']),
      h('p', {}, [h('strong', {}, '申请时间: '), row.applyTime || '-']),
      h('p', {}, [h('strong', {}, '备注: '), row.remark || '-']),
      row.reviewComment ? h('p', {}, [h('strong', {}, '审核意见: '), row.reviewComment]) : null,
    ]),
  });
}

function handleApplyExemption(row: ConflictCheckDTO) {
  let exemptionReason = '';
  Modal.confirm({
    title: '申请利益冲突豁免',
    content: h('div', {}, [
      h('p', { style: 'color: #ff4d4f; margin-bottom: 16px' }, 
        '该利冲检查发现存在冲突，如确需继续代理，请申请豁免审批。'),
      h('p', {}, `客户：${row.clientName}`),
      h('p', {}, `对方：${row.opposingParty || '-'}`),
      h('div', { style: 'margin-top: 16px' }, [
        h('label', {}, '豁免理由（必填）：'),
        h('textarea', { 
          style: 'width: 100%; height: 80px; margin-top: 8px; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px',
          placeholder: '请输入申请豁免的理由...',
          onInput: (e: any) => { exemptionReason = e.target.value; }
        }),
      ]),
    ]),
    okText: '申请豁免',
    cancelText: '取消',
    onOk: async () => {
      if (!exemptionReason.trim()) {
        message.error('请输入豁免理由');
        return Promise.reject();
      }
      try {
        await applyExemption({
          conflictCheckId: row.id,
          exemptionReason: exemptionReason,
        });
        message.success('豁免申请已提交，等待审批');
        gridApi.reload();
      } catch (error: any) {
        message.error(error.message || '申请失败');
      }
    },
  });
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    PENDING: 'orange',
    CHECKING: 'blue',
    PASSED: 'green',
    CONFLICT: 'red',
    EXEMPTION_PENDING: 'purple',
    WAIVED: 'cyan',
    REJECTED: 'volcano',
  };
  return colorMap[status] || 'default';
}

function getResultColor(result: string) {
  const colorMap: Record<string, string> = {
    NO_CONFLICT: 'green',
    CONFLICT: 'red',
    WAIVER: 'blue',
  };
  return colorMap[result] || 'default';
}
</script>

<template>
  <Page title="利冲检查" description="管理利益冲突审查">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :xs="24" :sm="12" :md="6">
            <Input
              v-model:value="queryParams.clientName"
              placeholder="客户名称"
              allowClear
              @pressEnter="handleSearch"
            />
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Select
              v-model:value="queryParams.status"
              placeholder="状态"
              allowClear
              style="width: 100%"
              :options="statusOptions"
            />
          </Col>
          <Col :xs="24" :sm="24" :md="12">
            <Space>
              <Button type="primary" @click="handleSearch">查询</Button>
              <Button @click="handleReset">重置</Button>
              <Button type="primary" @click="handleAdd">
                <Plus class="size-4" />申请利冲审查
              </Button>
            </Space>
          </Col>
        </Row>
      </div>

      <Grid>
        <!-- 状态列 -->
        <template #status="{ row }">
          <Tag :color="getStatusColor(row.status)">{{ row.statusName }}</Tag>
        </template>

        <!-- 结果列 -->
        <template #result="{ row }">
          <Tag v-if="row.result" :color="getResultColor(row.result)">{{ row.resultName }}</Tag>
          <span v-else>-</span>
        </template>

        <!-- 操作列 -->
        <template #action="{ row }">
          <Space>
            <a @click="handleViewDetail(row)">查看</a>
            <a v-if="row.status === 'CONFLICT'" @click="handleApplyExemption(row)" style="color: #722ed1">申请豁免</a>
            <template v-if="row.status === 'EXEMPTION_PENDING'">
              <a @click="handleApprove(row)">批准</a>
              <a @click="handleReject(row)" style="color: red">拒绝</a>
            </template>
          </Space>
        </template>
      </Grid>
    </Card>

    <!-- 申请弹窗 -->
    <Modal
      v-model:open="modalVisible"
      title="申请利冲审查"
      width="650px"
      @ok="handleSave"
    >
      <Spin :spinning="ocrLoading" tip="正在识别证件...">
        <!-- OCR识别区域 -->
        <Alert type="info" style="margin-bottom: 16px" show-icon>
          <template #message>
            <span class="font-medium text-blue-700">智能识别对方当事人</span>
            <span class="text-gray-500 text-xs ml-2">上传证件自动填充</span>
          </template>
          <template #description>
            <div class="mt-2">
              <Space>
                <Upload
                  :show-upload-list="false"
                  :before-upload="handleOcrIdCard"
                  accept="image/*"
                >
                  <Tooltip title="上传对方身份证，自动识别姓名">
                    <a class="text-blue-600 hover:text-blue-800 font-medium">
                      <IconifyIcon icon="ant-design:scan-outlined" class="mr-1" />识别身份证
                    </a>
                  </Tooltip>
                </Upload>
                <Upload
                  :show-upload-list="false"
                  :before-upload="handleOcrLicense"
                  accept="image/*"
                >
                  <Tooltip title="上传对方营业执照，自动识别企业名称">
                    <a class="text-green-600 hover:text-green-800 font-medium">
                      <IconifyIcon icon="ant-design:scan-outlined" class="mr-1" />识别营业执照
                    </a>
                  </Tooltip>
                </Upload>
              </Space>
            </div>
          </template>
        </Alert>

      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="审查类型" name="checkType" :rules="[{ required: true, message: '请选择审查类型' }]">
          <Select v-model:value="formData.checkType" :options="checkTypeOptions" />
        </FormItem>
        <FormItem label="客户名称" name="clientName" :rules="[{ required: true, message: '请输入客户名称' }]">
          <Input v-model:value="formData.clientName" placeholder="请输入客户名称" />
        </FormItem>
        <FormItem label="对方当事人" name="opposingParty" :rules="[{ required: true, message: '请输入对方当事人' }]">
            <Input v-model:value="formData.opposingParty" placeholder="请输入对方当事人（可OCR识别）" />
        </FormItem>
        <FormItem label="项目名称" name="matterName">
          <Input v-model:value="formData.matterName" placeholder="请输入项目名称（可选）" />
        </FormItem>
        <FormItem label="备注" name="remark">
          <Textarea v-model:value="formData.remark" :rows="3" placeholder="请输入备注" />
        </FormItem>
      </Form>
      </Spin>
    </Modal>
  </Page>
</template>
