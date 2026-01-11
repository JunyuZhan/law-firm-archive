<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { message, Form, FormItem, Input, Select, Textarea, Tabs, TabPane, Divider, Row, Col, Alert, Button, Space, Dropdown, Menu, MenuItem } from 'ant-design-vue';
import { Plus } from '@vben/icons';
import { requestClient } from '#/api/request';
import RichTextEditor from '#/components/RichTextEditor/index.vue';
import { defaultTemplates, templateList, contractTypeOptions, feeTypeOptions } from './contract-templates';

// 合同模板专用变量
const contractVariables = [
  // 合同基本信息
  { label: '合同编号', value: 'contractNo', description: '系统自动生成的合同编号' },
  { label: '合同名称', value: 'contractName', description: '合同名称' },
  { label: '签订日期', value: 'signDate', description: '合同签订日期' },
  { label: '生效日期', value: 'effectiveDate', description: '合同生效日期' },
  { label: '到期日期', value: 'expiryDate', description: '合同到期日期' },
  
  // 项目/案件信息
  { label: '项目名称', value: 'matterName', description: '委托项目/案件名称' },
  { label: '项目编号', value: 'matterNo', description: '案件编号' },
  { label: '案由', value: 'causeOfAction', description: '案件案由' },
  { label: '案件类型', value: 'caseType', description: '民事/刑事/行政等' },
  { label: '审理阶段', value: 'trialStage', description: '一审/二审/再审等' },
  { label: '对方当事人', value: 'opposingParty', description: '对方当事人姓名' },
  { label: '管辖法院', value: 'jurisdictionCourt', description: '管辖法院名称' },
  { label: '案情摘要', value: 'caseSummary', description: '案情简要描述' },
  
  // 委托人/客户信息
  { label: '委托人姓名', value: 'clientName', description: '委托人/当事人姓名' },
  { label: '委托人身份证号', value: 'clientIdNumber', description: '委托人身份证号码' },
  { label: '委托人地址', value: 'clientAddress', description: '委托人联系地址' },
  { label: '委托人电话', value: 'clientPhone', description: '委托人联系电话' },
  { label: '委托人邮箱', value: 'clientEmail', description: '委托人邮箱' },
  { label: '法定代表人', value: 'legalRepresentative', description: '企业法定代表人' },
  { label: '统一社会信用代码', value: 'creditCode', description: '企业统一社会信用代码' },
  
  // 律师/律所信息
  { label: '承办律师', value: 'lawyerName', description: '承办律师姓名' },
  { label: '律师执业证号', value: 'lawyerLicenseNo', description: '承办律师执业证号' },
  { label: '律所名称', value: 'firmName', description: '律师事务所全称' },
  { label: '律所地址', value: 'firmAddress', description: '律师事务所地址' },
  { label: '律所电话', value: 'firmPhone', description: '律师事务所电话' },
  { label: '律所负责人', value: 'firmLegalPerson', description: '律师事务所负责人' },
  
  // 收费信息
  { label: '合同金额', value: 'totalAmount', description: '合同总金额（数字）' },
  { label: '大写金额', value: 'totalAmountChinese', description: '合同金额大写（如：壹万元整）' },
  { label: '格式化金额', value: 'totalAmountFormatted', description: '带千分位的金额（如：10,000.00）' },
  { label: '标的金额', value: 'claimAmount', description: '诉讼标的金额' },
  { label: '标的大写', value: 'claimAmountChinese', description: '标的金额大写' },
  { label: '收费方式', value: 'feeType', description: '固定/计时/风险代理等' },
  { label: '付款方式', value: 'paymentTerms', description: '付款方式说明' },
  { label: '风险代理比例', value: 'riskRatio', description: '风险代理比例（%）' },
  { label: '预支差旅费', value: 'advanceTravelFee', description: '预支差旅费金额' },
  
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
  contractType: string;
  feeType: string;
  content: string;
  clauses: string;
  description: string;
}

interface ClauseItem {
  title: string;
  content: string;
}

const emit = defineEmits<{
  success: [];
}>();

const editingId = ref<number>();
const activeTab = ref('content');

const formData = reactive({
  name: '',
  contractType: 'SERVICE',
  feeType: 'FIXED',
  content: '',
  description: '',
});

// 标准条款列表
const clausesList = ref<ClauseItem[]>([]);

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
    contractType: 'SERVICE',
    feeType: 'FIXED',
    content: '',
    description: '',
  });
  clausesList.value = [];
  activeTab.value = 'content';
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
    contractType: record.contractType,
    feeType: record.feeType || 'FIXED',
    content: record.content || '',
    description: record.description || '',
  });
  // 解析条款
  try {
    clausesList.value = record.clauses ? JSON.parse(record.clauses) : [];
  } catch {
    clausesList.value = [];
  }
  activeTab.value = 'content';
  modalApi.setState({ title: '编辑合同模板' });
  modalApi.open();
}

// 添加条款
function addClause() {
  clausesList.value.push({ title: '', content: '' });
}

// 删除条款
function removeClause(index: number) {
  clausesList.value.splice(index, 1);
}

// 保存
async function handleSave() {
  if (!formData.name) {
    message.error('请输入模板名称');
    return;
  }
  
  // 过滤空条款
  const validClauses = clausesList.value.filter(c => c.title && c.content);
  
  const submitData = {
    ...formData,
    clauses: validClauses.length > 0 ? JSON.stringify(validClauses) : '',
  };
  
  try {
    if (editingId.value) {
      await requestClient.put(`/system/contract-template/${editingId.value}`, submitData);
      message.success('模板更新成功');
    } else {
      await requestClient.post('/system/contract-template', submitData);
      message.success('模板创建成功');
    }
    modalApi.close();
    emit('success');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '保存失败');
  }
}

// 加载默认模板
function loadDefaultTemplate(key: string) {
  const template = defaultTemplates[key];
  if (template) {
    formData.content = template.content;
    if (!formData.name) {
      formData.name = template.name;
    }
    formData.contractType = template.type;
    message.success(`已加载模板：${template.name}`);
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
          <FormItem label="模板名称" required :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <Input v-model:value="formData.name" placeholder="如：民事委托代理合同（标准版）" />
          </FormItem>
        </Col>
        <Col :span="6">
          <FormItem label="合同类型" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <Select v-model:value="formData.contractType" :options="contractTypeOptions" />
          </FormItem>
        </Col>
        <Col :span="6">
          <FormItem label="收费方式" :label-col="{ span: 10 }" :wrapper-col="{ span: 14 }">
            <Select v-model:value="formData.feeType" :options="feeTypeOptions" />
          </FormItem>
        </Col>
      </Row>
      <FormItem label="描述" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <Textarea v-model:value="formData.description" :rows="2" placeholder="模板用途说明" />
      </FormItem>
    </Form>

    <Divider style="margin: 12px 0" />

    <Tabs v-model:activeKey="activeTab">
      <TabPane key="content" tab="合同正文">
        <Alert 
          message="提示：使用工具栏插入变量，变量会在生成实际合同时自动替换为真实数据。" 
          type="info" 
          show-icon 
          style="margin-bottom: 12px"
        />
        
        <div style=" display: flex; flex-wrap: wrap; gap: 8px; align-items: center;margin-bottom: 12px;">
          <span style=" font-size: 13px;color: #666;">快速加载模板：</span>
          <Dropdown>
            <Button>选择模板 ▼</Button>
            <template #overlay>
              <Menu>
                <MenuItem 
                  v-for="tpl in templateList" 
                  :key="tpl.key"
                  @click="loadDefaultTemplate(tpl.key)"
                >
                  {{ tpl.name }}
                </MenuItem>
              </Menu>
            </template>
          </Dropdown>
          <span style=" margin-left: 8px; font-size: 12px;color: #999;">
            选择后将加载标准模板内容，可在此基础上修改
          </span>
        </div>
        
        <RichTextEditor 
          v-model="formData.content" 
          height="500px"
          placeholder="请输入合同正文内容..."
          :variables="contractVariables"
          :show-variables="true"
        />
      </TabPane>
      
      <TabPane key="clauses" tab="标准条款">
        <Alert 
          message="添加可复用的标准条款，如保密条款、违约责任等，方便在不同合同中使用" 
          type="info" 
          show-icon 
          style="margin-bottom: 12px"
        />
        
        <div 
          v-for="(clause, index) in clausesList" 
          :key="index" 
          style=" padding: 12px;margin-bottom: 16px; background: #fafafa; border: 1px solid #e8e8e8; border-radius: 6px;"
        >
          <div style="display: flex; align-items: center; margin-bottom: 8px;">
            <span style="width: 80px; font-weight: 500;">条款标题：</span>
            <Input v-model:value="clause.title" placeholder="如：保密条款" style="flex: 1" />
            <Button type="text" danger @click="removeClause(index)" style="margin-left: 8px;">
              删除
            </Button>
          </div>
          <div style="display: flex;">
            <span style="width: 80px; font-weight: 500;">条款内容：</span>
            <Textarea v-model:value="clause.content" :rows="4" placeholder="条款具体内容" style="flex: 1" />
          </div>
        </div>
        
        <Button type="dashed" block @click="addClause">
          <Plus class="size-4" /> 添加条款
        </Button>
      </TabPane>
    </Tabs>

    <div style=" padding-top: 16px;margin-top: 16px; text-align: right; border-top: 1px solid #e8e8e8;">
      <Space>
        <Button @click="modalApi.close()">取消</Button>
        <Button type="primary" @click="handleSave">保存模板</Button>
      </Space>
    </div>
  </Modal>
</template>
