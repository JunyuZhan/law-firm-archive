<script setup lang="ts">
import { reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Button,
  Col,
  Divider,
  Form,
  FormItem,
  Input,
  message,
  Row,
  Select,
  Space,
  Textarea,
} from 'ant-design-vue';

import { requestClient } from '#/api/request';

import { feeTypeOptions, templateTypeOptions } from './contract-templates';
import StructuredTemplateEditor from './StructuredTemplateEditor.vue';

const emit = defineEmits<{
  success: [];
}>();

// 合同模板专用变量
const contractVariables = [
  // 合同基本信息
  {
    label: '合同编号',
    value: 'contractNo',
    description: '系统自动生成的合同编号',
  },
  { label: '合同名称', value: 'contractName', description: '合同名称' },
  { label: '签订日期', value: 'signDate', description: '合同签订日期' },
  { label: '生效日期', value: 'effectiveDate', description: '合同生效日期' },
  { label: '到期日期', value: 'expiryDate', description: '合同到期日期' },

  // 项目/案件信息
  { label: '项目名称', value: 'matterName', description: '委托项目/案件名称' },
  { label: '项目编号', value: 'matterNo', description: '案件编号' },
  {
    label: '案件描述',
    value: 'matterDescription',
    description: '案件/事项描述',
  },
  { label: '案由', value: 'causeOfAction', description: '案件案由' },
  { label: '案件类型', value: 'caseType', description: '民事/刑事/行政等' },
  { label: '审理阶段', value: 'trialStage', description: '一审/二审/再审等' },
  {
    label: '对方当事人',
    value: 'opposingParty',
    description: '对方当事人姓名',
  },
  {
    label: '管辖法院',
    value: 'jurisdictionCourt',
    description: '管辖法院名称',
  },
  { label: '案情摘要', value: 'caseSummary', description: '案情简要描述' },

  // 委托人/客户信息
  {
    label: '委托人姓名',
    value: 'clientName',
    description: '委托人/当事人姓名',
  },
  {
    label: '委托人身份证号',
    value: 'clientIdNumber',
    description: '委托人身份证号码',
  },
  {
    label: '委托人地址',
    value: 'clientAddress',
    description: '委托人联系地址',
  },
  { label: '委托人电话', value: 'clientPhone', description: '委托人联系电话' },
  { label: '委托人邮箱', value: 'clientEmail', description: '委托人邮箱' },
  {
    label: '法定代表人',
    value: 'legalRepresentative',
    description: '企业法定代表人',
  },
  {
    label: '统一社会信用代码',
    value: 'creditCode',
    description: '企业统一社会信用代码',
  },

  // 律师/律所信息
  { label: '承办律师', value: 'lawyerName', description: '承办律师姓名' },
  {
    label: '律师执业证号',
    value: 'lawyerLicenseNo',
    description: '承办律师执业证号',
  },
  { label: '律所名称', value: 'firmName', description: '律师事务所全称' },
  { label: '律所地址', value: 'firmAddress', description: '律师事务所地址' },
  { label: '律所电话', value: 'firmPhone', description: '律师事务所电话' },
  {
    label: '律所负责人',
    value: 'firmLegalPerson',
    description: '律师事务所负责人',
  },

  // 收费信息
  {
    label: '合同金额',
    value: 'totalAmount',
    description: '合同总金额（数字）',
  },
  {
    label: '大写金额',
    value: 'totalAmountChinese',
    description: '合同金额大写（如：壹万元整）',
  },
  {
    label: '格式化金额',
    value: 'totalAmountFormatted',
    description: '带千分位的金额（如：10,000.00）',
  },
  { label: '标的金额', value: 'claimAmount', description: '诉讼标的金额' },
  {
    label: '标的大写',
    value: 'claimAmountChinese',
    description: '标的金额大写',
  },
  { label: '收费方式', value: 'feeType', description: '固定/计时/风险代理等' },
  { label: '付款方式', value: 'paymentTerms', description: '付款方式说明' },
  {
    label: '风险代理比例',
    value: 'riskRatio',
    description: '风险代理比例（%）',
  },
  {
    label: '预支差旅费',
    value: 'advanceTravelFee',
    description: '预支差旅费金额',
  },

  // 代理权限
  { label: '代理程序', value: 'procedureStage', description: '代理程序阶段' },
  { label: '代理权限', value: 'authorityScope', description: '代理权限范围' },

  // 日期变量
  { label: '当前年份', value: 'currentYear', description: '当前年份' },
  { label: '当前日期', value: 'currentDate', description: '当前完整日期' },
];

interface ContractTemplateDTO {
  id: number;
  name: string;
  templateType: string;
  feeType: string;
  content: string;
  clauses: string;
  description: string;
}

const editingId = ref<number>();

const formData = reactive({
  name: '',
  templateType: 'CIVIL_PROXY',
  feeType: 'FIXED',
  content: '',
  description: '',
});

const [Modal, modalApi] = useVbenModal({
  footer: false,
  onOpenChange(isOpen) {
    if (!isOpen) {
      resetForm();
    }
  },
});

// 重置表单
function resetForm() {
  editingId.value = undefined;
  Object.assign(formData, {
    name: '',
    templateType: 'CIVIL_PROXY',
    feeType: 'FIXED',
    content: '',
    description: '',
  });
}

// 打开新增弹窗
function openCreate() {
  resetForm();
  modalApi.setState({ title: '新增合同模板' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: ContractTemplateDTO) {
  editingId.value = record.id;
  Object.assign(formData, {
    name: record.name,
    templateType: record.templateType,
    feeType: record.feeType || 'FIXED',
    content: record.content || '',
    description: record.description || '',
  });
  modalApi.setState({ title: '编辑合同模板' });
  modalApi.open();
}

// 保存
async function handleSave() {
  if (!formData.name) {
    message.error('请输入模板名称');
    return;
  }
  if (!formData.content) {
    message.error('请填写模板内容（至少填写一个区块）');
    return;
  }

  try {
    if (editingId.value) {
      await requestClient.put(
        `/system/contract-template/${editingId.value}`,
        formData,
      );
      message.success('模板更新成功');
    } else {
      await requestClient.post('/system/contract-template', formData);
      message.success('模板创建成功');
    }
    modalApi.close();
    emit('success');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '保存失败');
  }
}

defineExpose({
  openCreate,
  openEdit,
});
</script>

<template>
  <Modal class="w-[1100px]">
    <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
      <Row :gutter="16">
        <Col :span="12">
          <FormItem
            label="模板名称"
            required
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
          >
            <Input
              v-model:value="formData.name"
              placeholder="如：民事委托代理合同（标准版）"
            />
          </FormItem>
        </Col>
        <Col :span="6">
          <FormItem
            label="模板类型"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <Select
              v-model:value="formData.templateType"
              :options="templateTypeOptions"
            />
          </FormItem>
        </Col>
        <Col :span="6">
          <FormItem
            label="收费方式"
            :label-col="{ span: 10 }"
            :wrapper-col="{ span: 14 }"
          >
            <Select
              v-model:value="formData.feeType"
              :options="feeTypeOptions"
            />
          </FormItem>
        </Col>
      </Row>
      <FormItem
        label="描述"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <Textarea
          v-model:value="formData.description"
          :rows="2"
          placeholder="模板用途说明"
        />
      </FormItem>
    </Form>

    <Divider style="margin: 12px 0" />

    <!-- 结构化模板编辑器：四个区块 -->
    <StructuredTemplateEditor
      v-model="formData.content"
      :variables="contractVariables"
    />

    <div
      style="
        padding-top: 16px;
        margin-top: 16px;
        text-align: right;
        border-top: 1px solid #e8e8e8;
      "
    >
      <Space>
        <Button @click="modalApi.close()">取消</Button>
        <Button type="primary" @click="handleSave">保存模板</Button>
      </Space>
    </div>
  </Modal>
</template>
