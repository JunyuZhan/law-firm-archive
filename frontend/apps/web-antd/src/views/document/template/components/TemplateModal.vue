<script setup lang="ts">
import type { DocumentTemplateDTO } from '#/api/document/template-types';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';

import {
  Alert,
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
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  createTemplate,
  getTemplateDetail,
  updateTemplate,
} from '#/api/document/template';
import RichTextEditor from '#/components/RichTextEditor/index.vue';

const emit = defineEmits<{ success: [] }>();

const editingId = ref<number>();
const loading = ref(false);

const formData = reactive({
  name: '',
  templateType: 'HTML',
  businessType: '',
  content: '',
  description: '',
});

const templateTypeOptions = [
  { label: 'Word文档', value: 'WORD' },
  { label: 'Excel表格', value: 'EXCEL' },
  { label: 'PDF文档', value: 'PDF' },
  { label: '富文本', value: 'HTML' },
  { label: '授权委托书(自动归档)', value: 'POWER_OF_ATTORNEY' },
];

// 业务类型选项（项目大类 + 通用）
const businessTypeOptions = [
  { label: '诉讼案件', value: 'LITIGATION' },
  { label: '非诉项目', value: 'NON_LITIGATION' },
  { label: '通用', value: 'GENERAL' },
];

// 系统预设变量（与后端 TemplateVariableService 一致）
const systemVariables = [
  // 项目相关
  { label: '项目名称', value: 'matter.name', description: '委托项目/案件名称' },
  { label: '项目编号', value: 'matter.no', description: '案件编号' },
  { label: '案件类型', value: 'matter.caseTypeName', description: '民事/刑事/行政等' },
  { label: '项目类型', value: 'matter.matterTypeName', description: '诉讼/非诉等' },
  { label: '案由', value: 'matter.causeOfAction', description: '案件案由' },
  { label: '对方当事人', value: 'matter.opposingParty', description: '对方当事人姓名' },
  { label: '案情简介', value: 'matter.description', description: '案情摘要' },
  { label: '立案日期', value: 'matter.filingDate', description: '立案日期' },
  // 客户相关
  { label: '客户名称', value: 'client.name', description: '委托人/当事人姓名' },
  { label: '客户类型', value: 'client.typeName', description: '企业/个人客户' },
  { label: '身份标识', value: 'client.idLabel', description: '身份证号/统一社会信用代码' },
  { label: '身份号码', value: 'client.idNumber', description: '身份证号或信用代码' },
  { label: '客户电话', value: 'client.phone', description: '委托人联系电话' },
  { label: '客户地址', value: 'client.address', description: '委托人联系地址' },
  { label: '法定代表人', value: 'client.legalPerson', description: '企业法定代表人' },
  // 律师相关
  { label: '承办律师', value: 'lawyer.name', description: '承办律师姓名' },
  { label: '律师执业证号', value: 'lawyer.licenseNo', description: '承办律师执业证号' },
  { label: '律师电话', value: 'lawyer.phone', description: '律师联系电话' },
  // 律所相关
  { label: '律所名称', value: 'firm.name', description: '律师事务所全称' },
  { label: '律所地址', value: 'firm.address', description: '律师事务所地址' },
  { label: '律所电话', value: 'firm.phone', description: '律师事务所电话' },
  // 合同相关
  { label: '合同编号', value: 'contract.no', description: '合同编号' },
  { label: '合同名称', value: 'contract.name', description: '合同名称' },
  { label: '合同金额', value: 'contract.totalAmount', description: '合同总金额' },
  { label: '大写金额', value: 'contract.totalAmountCN', description: '合同金额大写' },
  { label: '收费方式', value: 'contract.feeTypeName', description: '固定/计时/风险代理等' },
  { label: '付款条款', value: 'contract.paymentTerms', description: '付款约定' },
  { label: '合同签署日期', value: 'contract.signDate', description: '合同签署日期' },
  // 审批相关
  { label: '审批状态', value: 'approval.statusName', description: '待审批/已通过/已驳回' },
  { label: '审批人', value: 'approval.approverName', description: '审批人姓名' },
  { label: '审批时间', value: 'approval.approvedAt', description: '审批完成时间' },
  { label: '审批意见', value: 'approval.comment', description: '审批备注意见' },
  // 代理相关
  { label: '代理权限类型', value: 'authorizationType', description: '一般代理/特别代理' },
  { label: '代理权限范围', value: 'authorizationScope', description: '代理权限详细描述' },
  { label: '审理阶段', value: 'trialStage', description: '一审/二审/再审等' },
  // 日期相关
  { label: '当前日期', value: 'date.today', description: '当前完整日期' },
  { label: '当前年份', value: 'date.year', description: '当前年份' },
  { label: '当前月份', value: 'date.month', description: '当前月份' },
  { label: '当前日', value: 'date.day', description: '当前日' },
];

// 预设模板选项
const presetTemplates = [
  { label: '委托代理合同', value: 'contract' },
  { label: '授权委托书', value: 'authorization' },
  { label: '法律意见书', value: 'legalOpinion' },
  { label: '起诉状', value: 'complaint' },
  { label: '授权委托书(自动归档)', value: 'powerOfAttorney' },
];

// 是否使用富文本编辑器
const useRichEditor = computed(() => formData.templateType === 'HTML');

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    if (!formData.name) {
      message.error('请输入模板名称');
      return;
    }
    if (!formData.content) {
      message.error('请输入模板内容');
      return;
    }
    loading.value = true;
    try {
      if (editingId.value) {
        await updateTemplate(editingId.value, {
          name: formData.name,
          templateType: formData.templateType,
          content: formData.content,
          description: formData.description,
        });
        message.success('更新成功');
      } else {
        await createTemplate({
          name: formData.name,
          templateType: formData.templateType,
          businessType: formData.businessType || undefined,
          content: formData.content,
          description: formData.description || undefined,
        });
        message.success('创建成功');
      }
      emit('success');
      modalApi.close();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    } finally {
      loading.value = false;
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      resetForm();
    }
  },
});

function resetForm() {
  editingId.value = undefined;
  Object.assign(formData, {
    name: '',
    templateType: 'HTML',
    businessType: '',
    content: '',
    description: '',
  });
}

// 获取预设模板内容
function getPresetContent(type: string): string {
  const contractTpl = [
    '<h2 style="text-align: center;">委托代理合同</h2>',
    '<p style="text-align: center;">合同编号：${contractNo}</p>',
    '<p><strong>甲方（委托人）：</strong>${clientName}</p>',
    '<p><strong>身份证号：</strong>${clientIdNumber}</p>',
    '<p><strong>地址：</strong>${clientAddress}</p>',
    '<p><strong>联系电话：</strong>${clientPhone}</p>',
    '<p><strong>乙方（受托人）：</strong>${firmName}</p>',
    '<p><strong>地址：</strong>${firmAddress}</p>',
    '<p><strong>联系电话：</strong>${firmPhone}</p>',
    '<p>甲方因<strong>${causeOfAction}</strong>一案，委托乙方指派律师担任诉讼代理人。经双方协商，达成如下协议：</p>',
    '<p><strong>一、委托事项</strong></p>',
    '<p>甲方委托乙方指派律师担任${matterName}案件的诉讼代理人。</p>',
    '<p><strong>二、代理权限</strong></p>',
    '<p>□ 一般代理 □ 特别授权代理</p>',
    '<p><strong>三、代理费用</strong></p>',
    '<p>代理费用为人民币${totalAmount}元（大写：${totalAmountChinese}）。</p>',
    '<div style="text-align: right; margin-top: 40px;">',
    '<p>甲方（签章）：________________</p>',
    '<p>乙方（签章）：________________</p>',
    '<p>日期：${currentDate}</p>',
    '</div>',
  ].join('\n');

  const authTpl = [
    '<h2 style="text-align: center;">授权委托书</h2>',
    '<p>委托人：${clientName}</p>',
    '<p>身份证号码：${clientIdNumber}</p>',
    '<p>地址：${clientAddress}</p>',
    '<p>联系电话：${clientPhone}</p>',
    '<p>受委托人：${lawyerNames}</p>',
    '<p>执业证号：${lawyerLicenseNo}</p>',
    '<p>工作单位：${firmName}</p>',
    '<p>现委托上列受委托人在本人与${opposingParty}${causeOfAction}一案中，作为本人的诉讼代理人。</p>',
    '<p><strong>代理权限：</strong></p>',
    '<p>□ 一般代理 □ 特别授权</p>',
    '<div style="text-align: right; margin-top: 60px;">',
    '<p>委托人（签名）：________________</p>',
    '<p>${currentDate}</p>',
    '</div>',
  ].join('\n');

  const opinionTpl = [
    '<h2 style="text-align: center;">法律意见书</h2>',
    '<p style="text-align: right;">编号：${matterNo}</p>',
    '<p><strong>致：${clientName}</strong></p>',
    '<p>根据贵方的委托，${firmName}指派${lawyerNames}律师就${matterName}事宜出具如下法律意见：</p>',
    '<p><strong>一、基本情况</strong></p>',
    '<p>（此处描述案件基本情况）</p>',
    '<p><strong>二、法律分析</strong></p>',
    '<p>（此处进行法律分析）</p>',
    '<p><strong>三、法律意见</strong></p>',
    '<p>（此处给出法律意见）</p>',
    '<div style="text-align: right; margin-top: 40px;">',
    '<p>${firmName}</p>',
    '<p>承办律师：${lawyerNames}</p>',
    '<p>${currentDate}</p>',
    '</div>',
  ].join('\n');

  const complaintTpl = [
    '<h2 style="text-align: center;">民事起诉状</h2>',
    '<p><strong>原告：</strong>${clientName}</p>',
    '<p>身份证号码：${clientIdNumber}</p>',
    '<p>住所地：${clientAddress}</p>',
    '<p>联系电话：${clientPhone}</p>',
    '<p><strong>被告：</strong>${opposingParty}</p>',
    '<p><strong>诉讼请求：</strong></p>',
    '<p>1. 判令被告________________；</p>',
    '<p>2. 判令被告承担本案诉讼费用。</p>',
    '<p><strong>事实与理由：</strong></p>',
    '<p>（此处陈述案件事实与理由）</p>',
    '<p style="text-align: center; margin-top: 20px;">此致</p>',
    '<p>${jurisdictionCourt}</p>',
    '<div style="text-align: right; margin-top: 40px;">',
    '<p>具状人：${clientName}</p>',
    '<p>${currentDate}</p>',
    '</div>',
  ].join('\n');

  // 自动归档 - 授权委托书模板（纯文本格式，用于PDF生成）
  const powerOfAttorneyTpl = `                          授 权 委 托 书


                          【委托人信息】

委托人：\${client.name}
\${client.idLabel}：\${client.idNumber}
联系电话：\${client.phone}
住所地址：\${client.address}


                          【受托人信息】

受托人：\${firm.name}
承办律师：\${lawyer.name}
执业证号：\${lawyer.licenseNo}
律所地址：\${firm.address}


                          【委托事项】

    本人因 \${matter.name}（\${matter.caseTypeName}）一案，特委托上述受托人
作为本人的诉讼代理人。

代理阶段：\${trialStage}


                          【代理权限】

代理权限类型：\${authorizationType}

\${authorizationScope}


                          【委托期限】

本委托书自签署之日起至本案\${trialStage}结案止。


                          【签字确认】

委托人（签章）：________________

日    期：    年  月  日


━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

生成日期：\${date.today}
【本授权委托书由系统自动生成，以签字盖章版本为准】`;

  const map: Record<string, string> = {
    contract: contractTpl,
    authorization: authTpl,
    legalOpinion: opinionTpl,
    complaint: complaintTpl,
    powerOfAttorney: powerOfAttorneyTpl,
  };
  return map[type] || '';
}

// 加载预设模板
function loadPresetTemplate(type: string) {
  formData.content = getPresetContent(type);
}

// 插入变量
function insertVariable(v: { value: string }) {
  formData.content += `\${${v.value}}`;
}

async function open(record?: DocumentTemplateDTO) {
  resetForm();
  if (record) {
    editingId.value = record.id;
    loading.value = true;
    try {
      const detail = await getTemplateDetail(record.id);
      Object.assign(formData, {
        name: detail.name,
        templateType: detail.templateType || 'HTML',
        businessType: detail.businessType || '',
        content: detail.content || '',
        description: detail.description || '',
      });
    } catch {
      message.error('加载模板详情失败');
    } finally {
      loading.value = false;
    }
  }
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <Modal
    :title="editingId ? '编辑模板' : '新建模板'"
    class="w-[1000px]"
    :loading="loading"
    confirm-text="保存"
  >
    <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
      <Row :gutter="16">
        <Col :span="12">
          <FormItem
            label="模板名称"
            required
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
          >
            <Input v-model:value="formData.name" placeholder="请输入模板名称" />
          </FormItem>
        </Col>
        <Col :span="6">
          <FormItem
            label="类型"
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
          >
            <Select
              v-model:value="formData.templateType"
              :options="templateTypeOptions"
              style="width: 100%"
            />
          </FormItem>
        </Col>
        <Col :span="6">
          <FormItem
            label="业务"
            :label-col="{ span: 8 }"
            :wrapper-col="{ span: 16 }"
          >
            <Select
              v-model:value="formData.businessType"
              :options="businessTypeOptions"
              placeholder="请选择"
              allow-clear
              style="width: 100%"
            />
          </FormItem>
        </Col>
      </Row>
      <FormItem
        label="描述"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 20 }"
      >
        <Input
          v-model:value="formData.description"
          placeholder="模板用途说明（可选）"
        />
      </FormItem>
    </Form>

    <Divider style="margin: 12px 0" />

    <div v-if="!editingId" style="margin-bottom: 12px">
      <span style="margin-right: 12px; font-size: 13px; color: #666"
        >快速加载预设模板：</span
      >
      <Space>
        <Button
          v-for="preset in presetTemplates"
          :key="preset.value"
          size="small"
          @click="loadPresetTemplate(preset.value)"
        >
          {{ preset.label }}
        </Button>
      </Space>
    </div>

    <Alert
      message="使用 ${变量名} 格式插入变量，生成文书时会自动替换为实际数据"
      type="info"
      show-icon
      style="margin-bottom: 12px"
    />

    <div v-if="useRichEditor">
      <RichTextEditor
        v-model="formData.content"
        height="400px"
        placeholder="请输入模板内容..."
        :variables="systemVariables"
        :show-variables="true"
      />
    </div>

    <div v-else>
      <div class="variable-panel">
        <div class="variable-title">可用变量（点击插入）：</div>
        <Space wrap :size="4">
          <Tag
            v-for="v in systemVariables"
            :key="v.value"
            color="blue"
            class="variable-tag"
            @click="insertVariable(v)"
          >
            {{ v.label }}
          </Tag>
        </Space>
      </div>
      <Textarea
        v-model:value="formData.content"
        :rows="15"
        placeholder="请输入模板内容，使用 ${变量名} 插入变量"
        style="font-family: monospace"
      />
    </div>
  </Modal>
</template>

<style scoped>
.variable-panel {
  padding: 12px;
  margin-bottom: 12px;
  background: linear-gradient(to bottom, #f0f5ff, #e6f0ff);
  border: 1px solid #d6e4ff;
  border-radius: 6px;
}

.variable-title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #1890ff;
}

.variable-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.variable-tag:hover {
  box-shadow: 0 2px 4px rgb(24 144 255 / 30%);
  transform: scale(1.05);
}
</style>
