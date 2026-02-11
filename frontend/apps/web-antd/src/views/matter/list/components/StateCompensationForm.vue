<script setup lang="ts">
/**
 * 国家赔偿案件扩展表单组件
 * 根据案件类型（行政赔偿/刑事赔偿）动态显示相应字段
 */
import { computed, onMounted, reactive, ref, watch } from 'vue';

import {
  Button,
  Col,
  DatePicker,
  Divider,
  Form,
  FormItem,
  Input,
  InputNumber,
  Row,
  Select,
  SelectOption,
  Switch,
  Table,
  Textarea,
} from 'ant-design-vue';

import { getDictDataByCode } from '#/api/system';

// Props 定义
interface Props {
  /** 案件类型：STATE_COMP_ADMIN | STATE_COMP_CRIMINAL */
  caseType: string;
  /** 案件ID（编辑模式时传入） */
  matterId?: number;
  /** 初始数据 */
  initialData?: StateCompensationData;
  /** 是否只读 */
  readonly?: boolean;
}

// 表单数据结构
export interface StateCompensationData {
  obligorOrgName?: string;
  obligorOrgType?: string;
  caseSource?: string;
  damageDescription?: string;
  criminalCaseTerminated?: boolean;
  criminalCaseNo?: string;
  compensationCommittee?: string;
  applicationDate?: string;
  acceptanceDate?: string;
  decisionDate?: string;
  reconsiderationDate?: string;
  reconsiderationDecisionDate?: string;
  committeeAppDate?: string;
  committeeDecisionDate?: string;
  adminLitigationFilingDate?: string;
  adminLitigationCourtName?: string;
  claimAmount?: number;
  compensationItems?: CompensationItem[];
  decisionResult?: string;
  approvedAmount?: number;
  paymentStatus?: string;
  paymentDate?: string;
  remark?: string;
}

// 赔偿项目结构
interface CompensationItem {
  type: string;
  days?: number;
  dailyAmount?: number;
  amount: number;
}

const props = withDefaults(defineProps<Props>(), {
  matterId: undefined,
  initialData: undefined,
  readonly: false,
});

const emit = defineEmits<{
  (e: 'update:data', data: StateCompensationData): void;
}>();

// 是否为刑事赔偿
const isCriminalCompensation = computed(
  () => props.caseType === 'STATE_COMP_CRIMINAL',
);

// 是否为行政赔偿
const isAdminCompensation = computed(
  () => props.caseType === 'STATE_COMP_ADMIN',
);

// 表单数据
const formData = reactive<StateCompensationData>({
  obligorOrgName: '',
  obligorOrgType: undefined,
  caseSource: undefined,
  damageDescription: '',
  criminalCaseTerminated: false,
  criminalCaseNo: '',
  compensationCommittee: '',
  applicationDate: undefined,
  acceptanceDate: undefined,
  decisionDate: undefined,
  reconsiderationDate: undefined,
  reconsiderationDecisionDate: undefined,
  committeeAppDate: undefined,
  committeeDecisionDate: undefined,
  adminLitigationFilingDate: undefined,
  adminLitigationCourtName: '',
  claimAmount: undefined,
  compensationItems: [],
  decisionResult: undefined,
  approvedAmount: undefined,
  paymentStatus: undefined,
  paymentDate: undefined,
  remark: '',
});

// 字典选项
const obligorOrgTypeOptions = ref<{ label: string; value: string }[]>([]);
const damageCauseTypeOptions = ref<{ label: string; value: string }[]>([]);
const decisionResultOptions = ref<{ label: string; value: string }[]>([]);
const paymentStatusOptions = ref<{ label: string; value: string }[]>([]);

// 赔偿项目类型选项
const compensationItemTypes = [
  '人身自由赔偿',
  '生命健康赔偿',
  '精神损害抚慰金',
  '财产损害赔偿',
  '其他',
];

// 2024年人身自由赔偿标准
const DAILY_RATE_2024 = 436.89;

// 赔偿项目表格列定义
const compensationItemColumns = [
  { title: '项目类型', dataIndex: 'type', key: 'type', width: 150 },
  { title: '天数', dataIndex: 'days', key: 'days', width: 100 },
  {
    title: '日赔偿金(元)',
    dataIndex: 'dailyAmount',
    key: 'dailyAmount',
    width: 120,
  },
  { title: '金额(元)', dataIndex: 'amount', key: 'amount', width: 120 },
  { title: '操作', key: 'action', width: 80 },
];

// 加载字典数据
async function loadDictData() {
  try {
    const [orgTypes, causeTypes, resultTypes, statusTypes] = await Promise.all([
      getDictDataByCode('obligor_org_type'),
      getDictDataByCode('damage_cause_type'),
      getDictDataByCode('compensation_decision_result'),
      getDictDataByCode('compensation_payment_status'),
    ]);

    obligorOrgTypeOptions.value = orgTypes.map((item) => ({
      label: item.label,
      value: item.value,
    }));
    damageCauseTypeOptions.value = causeTypes.map((item) => ({
      label: item.label,
      value: item.value,
    }));
    decisionResultOptions.value = resultTypes.map((item) => ({
      label: item.label,
      value: item.value,
    }));
    paymentStatusOptions.value = statusTypes.map((item) => ({
      label: item.label,
      value: item.value,
    }));
  } catch (error) {
    console.error('加载国家赔偿字典失败:', error);
  }
}

// 添加赔偿项目
function addCompensationItem() {
  if (!formData.compensationItems) {
    formData.compensationItems = [];
  }
  formData.compensationItems.push({
    type: '人身自由赔偿',
    days: undefined,
    dailyAmount: DAILY_RATE_2024,
    amount: 0,
  });
}

// 删除赔偿项目
function removeCompensationItem(index: number) {
  formData.compensationItems?.splice(index, 1);
}

// 计算赔偿项目金额
function calculateItemAmount(item: CompensationItem) {
  if (item.type === '人身自由赔偿') {
    // 当天数或日赔偿金被清空时，金额重置为 0
    item.amount =
      item.days && item.dailyAmount
        ? Number((item.days * item.dailyAmount).toFixed(2))
        : 0;
  }
}

// 计算总金额
const totalClaimAmount = computed(() => {
  if (!formData.compensationItems?.length) return 0;
  return formData.compensationItems.reduce(
    (sum, item) => sum + (item.amount || 0),
    0,
  );
});

// 同步总金额到表单
watch(totalClaimAmount, (val) => {
  formData.claimAmount = val;
});

// 防止 watch 循环的标志
let isUpdatingFromParent = false;

// 监听表单变化，向外发送数据
watch(
  formData,
  (val) => {
    // 如果是从父组件同步过来的数据，不再向上 emit，避免循环
    if (isUpdatingFromParent) return;
    emit('update:data', { ...val });
  },
  { deep: true },
);

// 初始化数据
watch(
  () => props.initialData,
  (val) => {
    if (val) {
      isUpdatingFromParent = true;
      Object.assign(formData, val);
      // 使用 nextTick 确保本轮更新完成后再重置标志
      setTimeout(() => {
        isUpdatingFromParent = false;
      }, 0);
    }
  },
  { immediate: true },
);

onMounted(() => {
  loadDictData();
});

// 暴露获取表单数据的方法
function getData(): StateCompensationData {
  return { ...formData };
}

// 暴露验证方法
function validate(): Promise<boolean> {
  // 刑事赔偿必须确认刑事诉讼终结
  if (isCriminalCompensation.value && !formData.criminalCaseTerminated) {
    return Promise.reject(new Error('刑事赔偿必须确认刑事诉讼已终结'));
  }
  return Promise.resolve(true);
}

defineExpose({
  getData,
  validate,
});
</script>

<template>
  <div class="state-compensation-form">
    <Form layout="vertical" :disabled="readonly">
      <!-- 赔偿义务机关信息 -->
      <Divider orientation="left">赔偿义务机关</Divider>
      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="赔偿义务机关名称">
            <Input
              v-model:value="formData.obligorOrgName"
              placeholder="请输入赔偿义务机关名称"
            />
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="机关类型">
            <Select
              v-model:value="formData.obligorOrgType"
              placeholder="请选择机关类型"
              allow-clear
            >
              <SelectOption
                v-for="opt in obligorOrgTypeOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </SelectOption>
            </Select>
          </FormItem>
        </Col>
      </Row>

      <!-- 致损行为 -->
      <Divider orientation="left">致损行为</Divider>
      <Row :gutter="16">
        <Col :span="12">
          <FormItem label="致损行为类型">
            <Select
              v-model:value="formData.caseSource"
              placeholder="请选择致损行为类型"
              allow-clear
            >
              <SelectOption
                v-for="opt in damageCauseTypeOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </SelectOption>
            </Select>
          </FormItem>
        </Col>
        <Col :span="12">
          <FormItem label="损害情况描述">
            <Textarea
              v-model:value="formData.damageDescription"
              placeholder="请描述损害情况"
              :rows="2"
            />
          </FormItem>
        </Col>
      </Row>

      <!-- 刑事赔偿特有字段 -->
      <template v-if="isCriminalCompensation">
        <Divider orientation="left">
          刑事赔偿信息
          <span style="margin-left: 8px; font-size: 12px; color: #ff4d4f">
            （刑事诉讼终结是刑事赔偿的前置条件）
          </span>
        </Divider>
        <Row :gutter="16">
          <Col :span="8">
            <FormItem label="刑事诉讼是否终结" required>
              <Switch
                v-model:checked="formData.criminalCaseTerminated"
                checked-children="是"
                un-checked-children="否"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="原刑事案件编号">
              <Input
                v-model:value="formData.criminalCaseNo"
                placeholder="请输入原刑事案件编号"
              />
            </FormItem>
          </Col>
          <Col :span="8">
            <FormItem label="赔偿委员会">
              <Input
                v-model:value="formData.compensationCommittee"
                placeholder="请输入受理的赔偿委员会"
              />
            </FormItem>
          </Col>
        </Row>
      </template>

      <!-- 行政赔偿特有字段 -->
      <template v-if="isAdminCompensation">
        <Divider orientation="left">行政赔偿诉讼信息</Divider>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="行政诉讼立案日">
              <DatePicker
                v-model:value="formData.adminLitigationFilingDate"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="行政诉讼法院">
              <Input
                v-model:value="formData.adminLitigationCourtName"
                placeholder="请输入行政诉讼法院"
              />
            </FormItem>
          </Col>
        </Row>
      </template>

      <!-- 程序日期 -->
      <Divider orientation="left">程序日期</Divider>
      <Row :gutter="16">
        <Col :span="8">
          <FormItem label="赔偿申请日">
            <DatePicker
              v-model:value="formData.applicationDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              placeholder="2年时效"
            />
          </FormItem>
        </Col>
        <Col :span="8">
          <FormItem label="受理日">
            <DatePicker
              v-model:value="formData.acceptanceDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
        </Col>
        <Col :span="8">
          <FormItem label="赔偿决定日">
            <DatePicker
              v-model:value="formData.decisionDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              placeholder="受理后2个月内"
            />
          </FormItem>
        </Col>
      </Row>
      <Row :gutter="16">
        <Col :span="8">
          <FormItem label="复议申请日">
            <DatePicker
              v-model:value="formData.reconsiderationDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              placeholder="收到决定后30日内"
            />
          </FormItem>
        </Col>
        <Col :span="8">
          <FormItem label="复议决定日">
            <DatePicker
              v-model:value="formData.reconsiderationDecisionDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
        </Col>
        <Col v-if="isCriminalCompensation" :span="8">
          <FormItem label="赔偿委员会申请日">
            <DatePicker
              v-model:value="formData.committeeAppDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
        </Col>
      </Row>

      <!-- 赔偿项目 -->
      <Divider orientation="left">赔偿项目明细</Divider>
      <div class="compensation-items-section">
        <Table
          :columns="compensationItemColumns"
          :data-source="formData.compensationItems"
          :pagination="false"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'type'">
              <Select
                v-model:value="record.type"
                style="width: 100%"
                size="small"
              >
                <SelectOption
                  v-for="t in compensationItemTypes"
                  :key="t"
                  :value="t"
                >
                  {{ t }}
                </SelectOption>
              </Select>
            </template>
            <template v-else-if="column.key === 'days'">
              <InputNumber
                v-model:value="record.days"
                :min="0"
                size="small"
                style="width: 100%"
                @change="calculateItemAmount(record as CompensationItem)"
              />
            </template>
            <template v-else-if="column.key === 'dailyAmount'">
              <InputNumber
                v-model:value="record.dailyAmount"
                :min="0"
                :precision="2"
                size="small"
                style="width: 100%"
                @change="calculateItemAmount(record as CompensationItem)"
              />
            </template>
            <template v-else-if="column.key === 'amount'">
              <InputNumber
                v-model:value="record.amount"
                :min="0"
                :precision="2"
                size="small"
                style="width: 100%"
              />
            </template>
            <template v-else-if="column.key === 'action'">
              <Button
                type="link"
                danger
                size="small"
                @click="removeCompensationItem(index)"
              >
                删除
              </Button>
            </template>
          </template>
        </Table>
        <div
          style="
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-top: 8px;
          "
        >
          <Button type="dashed" size="small" @click="addCompensationItem">
            + 添加赔偿项目
          </Button>
          <span style="font-weight: bold">
            请求赔偿总额：¥{{ totalClaimAmount.toLocaleString() }}
          </span>
        </div>
      </div>

      <!-- 决定结果 -->
      <Divider orientation="left">决定结果</Divider>
      <Row :gutter="16">
        <Col :span="8">
          <FormItem label="决定结果">
            <Select
              v-model:value="formData.decisionResult"
              placeholder="请选择决定结果"
              allow-clear
            >
              <SelectOption
                v-for="opt in decisionResultOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </SelectOption>
            </Select>
          </FormItem>
        </Col>
        <Col :span="8">
          <FormItem label="决定赔偿金额">
            <InputNumber
              v-model:value="formData.approvedAmount"
              :min="0"
              :precision="2"
              style="width: 100%"
              :formatter="
                (value: any) =>
                  `¥ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
              "
              :parser="(value: any) => value.replace(/¥\s?|(,*)/g, '')"
            />
          </FormItem>
        </Col>
        <Col :span="8">
          <FormItem label="支付状态">
            <Select
              v-model:value="formData.paymentStatus"
              placeholder="请选择支付状态"
              allow-clear
            >
              <SelectOption
                v-for="opt in paymentStatusOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </SelectOption>
            </Select>
          </FormItem>
        </Col>
      </Row>
      <Row :gutter="16">
        <Col :span="8">
          <FormItem label="支付日期">
            <DatePicker
              v-model:value="formData.paymentDate"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </FormItem>
        </Col>
        <Col :span="16">
          <FormItem label="备注">
            <Textarea
              v-model:value="formData.remark"
              placeholder="请输入备注"
              :rows="2"
            />
          </FormItem>
        </Col>
      </Row>
    </Form>
  </div>
</template>

<style scoped>
.state-compensation-form {
  padding: 16px;
}

.compensation-items-section {
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}
</style>
