<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message, Form, FormItem, Input, Select, Textarea, Divider, Alert, Space, Button, Row, Col } from 'ant-design-vue';
import { createTemplate, updateTemplate, type LetterTemplateDTO } from '#/api/admin';
import RichTextEditor from '#/components/RichTextEditor/index.vue';

const emit = defineEmits<{
  success: [];
}>();

const editingId = ref<number | null>(null);
const formData = reactive({
  name: '',
  letterType: 'INTRODUCTION',
  content: '',
  description: '',
});

// 出函模板专用变量
const letterVariables = [
  // 项目/案件信息
  { label: '项目名称', value: 'matterName', description: '委托项目/案件名称' },
  { label: '项目编号', value: 'matterNo', description: '案件编号' },
  { label: '案由', value: 'causeOfAction', description: '案件案由' },
  { label: '案件类型', value: 'caseType', description: '民事/刑事/行政等' },
  { label: '对方当事人', value: 'opposingParty', description: '对方当事人姓名' },
  { label: '对方律师', value: 'opposingLawyerName', description: '对方律师姓名' },
  { label: '对方律所', value: 'opposingLawyerFirm', description: '对方律师所在律所' },
  { label: '标的金额', value: 'claimAmount', description: '诉讼标的金额' },
  { label: '管辖法院', value: 'jurisdictionCourt', description: '管辖法院名称' },
  
  // 委托人/客户信息
  { label: '委托人姓名', value: 'clientName', description: '委托人/当事人姓名' },
  { label: '委托人身份证号', value: 'clientIdNumber', description: '委托人身份证号码' },
  { label: '委托人地址', value: 'clientAddress', description: '委托人联系地址' },
  { label: '委托人电话', value: 'clientPhone', description: '委托人联系电话' },
  { label: '委托人邮箱', value: 'clientEmail', description: '委托人邮箱' },
  { label: '法定代表人', value: 'legalRepresentative', description: '企业法定代表人' },
  { label: '统一社会信用代码', value: 'creditCode', description: '企业统一社会信用代码' },
  
  // 律师/律所信息
  { label: '承办律师', value: 'lawyerNames', description: '承办律师姓名（多人逗号分隔）' },
  { label: '律师执业证号', value: 'lawyerLicenseNo', description: '承办律师执业证号' },
  { label: '律所名称', value: 'firmName', description: '律师事务所全称' },
  { label: '律所地址', value: 'firmAddress', description: '律师事务所地址' },
  { label: '律所电话', value: 'firmPhone', description: '律师事务所电话' },
  { label: '律所负责人', value: 'firmLegalPerson', description: '律师事务所负责人' },
  
  // 函件信息
  { label: '目标单位', value: 'targetUnit', description: '函件送达单位' },
  { label: '目标地址', value: 'targetAddress', description: '函件送达地址' },
  { label: '函件编号', value: 'letterNo', description: '出函编号' },
  { label: '出函日期', value: 'date', description: '出函日期' },
  
  // 日期变量
  { label: '当前年份', value: 'currentYear', description: '当前年份' },
  { label: '当前日期', value: 'currentDate', description: '当前完整日期' },
];

// 函件类型选项
const letterTypeOptions = [
  { label: '介绍信', value: 'INTRODUCTION' },
  { label: '会见函', value: 'MEETING' },
  { label: '调查函', value: 'INVESTIGATION' },
  { label: '阅卷函', value: 'FILE_REVIEW' },
  { label: '法律意见函', value: 'LEGAL_OPINION' },
  { label: '其他', value: 'OTHER' },
];

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
    try {
      if (editingId.value) {
        await updateTemplate(editingId.value, formData);
        message.success('模板更新成功');
      } else {
        await createTemplate(formData);
        message.success('模板创建成功');
      }
      modalApi.close();
      emit('success');
    } catch (error: unknown) {
      const err = error as { message?: string };
      message.error(err.message || '保存失败');
    }
  },
});

// 加载默认模板
function loadDefaultTemplate(type: string) {
  const templates: Record<string, string> = {
    introduction: `
<h2 style="text-align: center;">介 绍 信</h2>
<p style="text-align: center;">编号：\${letterNo}</p>

<p><strong>\${targetUnit}：</strong></p>

<p>兹介绍本所律师<strong>\${lawyerNames}</strong>（执业证号：\${lawyerLicenseNo}）前往贵处，就<strong>\${clientName}</strong>与<strong>\${opposingParty}</strong>\${causeOfAction}一案，进行会见、调查取证、阅卷等相关工作，请予接洽为盼。</p>

<p>此致</p>

<div style="text-align: right; margin-top: 2em;">
  <p style="text-indent: 0;">\${firmName}</p>
  <p style="text-indent: 0;">\${date}</p>
</div>

<hr style="margin-top: 3em; border-style: dashed;" />

<p style="font-size: 12px; color: #666;">
  律所地址：\${firmAddress}<br/>
  联系电话：\${firmPhone}
</p>
    `,
    meeting: `
<h2 style="text-align: center;">会 见 函</h2>
<p style="text-align: center;">编号：\${letterNo}</p>

<p><strong>\${targetUnit}：</strong></p>

<p>本所接受<strong>\${clientName}</strong>（身份证号：\${clientIdNumber}）的委托，指派<strong>\${lawyerNames}</strong>律师（执业证号：\${lawyerLicenseNo}）担任其涉嫌<strong>\${causeOfAction}</strong>一案的辩护人/诉讼代理人。</p>

<p>现因案件办理需要，特申请会见犯罪嫌疑人/被告人<strong>\${clientName}</strong>。根据《中华人民共和国刑事诉讼法》第三十九条之规定，请予安排会见时间和地点。</p>

<p>此致</p>

<div style="text-align: right; margin-top: 2em;">
  <p style="text-indent: 0;">\${firmName}</p>
  <p style="text-indent: 0;">承办律师：\${lawyerNames}</p>
  <p style="text-indent: 0;">\${date}</p>
</div>
    `,
    fileReview: `
<h2 style="text-align: center;">阅 卷 函</h2>
<p style="text-align: center;">编号：\${letterNo}</p>

<p><strong>\${targetUnit}：</strong></p>

<p>本所接受<strong>\${clientName}</strong>的委托，指派<strong>\${lawyerNames}</strong>律师担任<strong>\${matterName}</strong>案件的诉讼代理人/辩护人。</p>

<p>根据《中华人民共和国刑事诉讼法》及相关法律规定，辩护律师自人民检察院对案件审查起诉之日起，可以查阅、摘抄、复制本案的案卷材料。</p>

<p>现因案件办理需要，特申请查阅本案全部案卷材料，请予安排。</p>

<p>此致</p>

<div style="text-align: right; margin-top: 2em;">
  <p style="text-indent: 0;">\${firmName}</p>
  <p style="text-indent: 0;">承办律师：\${lawyerNames}</p>
  <p style="text-indent: 0;">\${date}</p>
</div>
    `,
    investigation: `
<h2 style="text-align: center;">调 查 函</h2>
<p style="text-align: center;">编号：\${letterNo}</p>

<p><strong>\${targetUnit}：</strong></p>

<p>本所接受<strong>\${clientName}</strong>的委托，指派<strong>\${lawyerNames}</strong>律师办理<strong>\${matterName}</strong>法律事务。</p>

<p>现因案件办理需要，需向贵单位调取以下材料/了解以下情况：</p>

<p>1. _______________________</p>
<p>2. _______________________</p>
<p>3. _______________________</p>

<p>根据《中华人民共和国律师法》第三十五条之规定，律师自行调查取证的，凭律师执业证书和律师事务所证明，可以向有关单位或者个人调查与承办法律事务有关的情况。</p>

<p>请贵单位予以协助配合，谢谢！</p>

<div style="text-align: right; margin-top: 2em;">
  <p style="text-indent: 0;">\${firmName}</p>
  <p style="text-indent: 0;">承办律师：\${lawyerNames}</p>
  <p style="text-indent: 0;">联系电话：\${firmPhone}</p>
  <p style="text-indent: 0;">\${date}</p>
</div>
    `,
  };
  
  formData.content = templates[type] || '';
}

// 打开新增弹窗
function openCreate() {
  editingId.value = null;
  Object.assign(formData, {
    name: '',
    letterType: 'INTRODUCTION',
    content: '',
    description: '',
  });
  modalApi.setState({ title: '新增出函模板' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: LetterTemplateDTO) {
  editingId.value = record.id;
  Object.assign(formData, {
    name: record.name,
    letterType: record.letterType,
    content: record.content,
    description: record.description || '',
  });
  modalApi.setState({ title: '编辑出函模板' });
  modalApi.open();
}

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Modal class="w-[1000px]" confirm-text="保存模板">
    <Form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="模板名称" required :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <Input v-model:value="formData.name" placeholder="如：律师介绍信" />
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="函件类型" required :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <Select v-model:value="formData.letterType" :options="letterTypeOptions" style="width: 200px" />
          </FormItem>
        </Col>
      </Row>
      <FormItem label="描述">
        <Textarea v-model:value="formData.description" :rows="2" placeholder="模板用途说明" />
      </FormItem>
    </Form>

    <Divider style="margin: 12px 0" />

    <Alert 
      message="提示：使用工具栏插入变量，变量会在生成实际函件时自动替换为真实数据" 
      type="info" 
      show-icon 
      style="margin-bottom: 12px"
    />
    
    <div style="margin-bottom: 12px;">
      <span style="color: #666; font-size: 13px; margin-right: 12px;">快速加载模板：</span>
      <Space>
        <Button size="small" @click="loadDefaultTemplate('introduction')">介绍信</Button>
        <Button size="small" @click="loadDefaultTemplate('meeting')">会见函</Button>
        <Button size="small" @click="loadDefaultTemplate('fileReview')">阅卷函</Button>
        <Button size="small" @click="loadDefaultTemplate('investigation')">调查函</Button>
      </Space>
    </div>
    
    <RichTextEditor 
      v-model="formData.content" 
      height="400px"
      placeholder="请输入函件模板内容..."
      :variables="letterVariables"
      :show-variables="true"
    />
  </Modal>
</template>
