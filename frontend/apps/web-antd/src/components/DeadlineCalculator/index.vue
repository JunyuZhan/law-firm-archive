<template>
  <Card :title="title" size="small" :bordered="bordered">
    <Form layout="vertical" :model="form">
      <Row :gutter="16">
        <Col :span="8">
          <FormItem label="起算日期">
            <DatePicker
              v-model:value="form.startDate"
              style="width: 100%"
              placeholder="选择起算日期"
              :disabled-date="disabledDate"
            />
          </FormItem>
        </Col>
        <Col :span="6">
          <FormItem label="期限天数">
            <InputNumber
              v-model:value="form.days"
              :min="1"
              :max="365"
              style="width: 100%"
              placeholder="天数"
            />
          </FormItem>
        </Col>
        <Col :span="10">
          <FormItem label="计算方式">
            <RadioGroup v-model:value="form.workdaysOnly">
              <Radio :value="false">自然日（节假日顺延）</Radio>
              <Radio :value="true">工作日</Radio>
            </RadioGroup>
          </FormItem>
        </Col>
      </Row>
      <Row>
        <Col :span="24">
          <Space>
            <Button type="primary" :loading="loading" @click="calculate">
              🧮 计算截止日期
            </Button>
            <Button v-if="result" @click="reset">重置</Button>
          </Space>
        </Col>
      </Row>
    </Form>

    <!-- 计算结果 -->
    <Alert v-if="result" type="info" style="margin-top: 16px" show-icon>
      <template #message>
        <div class="result-content">
          <div class="result-main">
            <span class="result-label">截止日期：</span>
            <Tag color="blue" class="result-date">{{ result.deadline }}</Tag>
            <Tag v-if="result.isWorkday" color="green">工作日</Tag>
            <Tag v-else color="orange">{{ result.deadlineTypeName }}</Tag>
          </div>
          <div class="result-detail">
            {{ result.explanation }}
          </div>
        </div>
      </template>
    </Alert>

    <!-- 常用期限快捷按钮 -->
    <div v-if="showPresets" class="presets-section">
      <Divider orientation="left" plain>
        <span class="presets-title">常用期限</span>
      </Divider>
      <Space wrap>
        <Button
          v-for="preset in presets"
          :key="preset.days"
          size="small"
          @click="applyPreset(preset)"
        >
          {{ preset.label }}
        </Button>
      </Space>
    </div>
  </Card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import {
  Card,
  Form,
  FormItem,
  Row,
  Col,
  DatePicker,
  InputNumber,
  RadioGroup,
  Radio,
  Button,
  Space,
  Alert,
  Tag,
  Divider,
  message,
} from 'ant-design-vue';
// Use emoji instead of icon since @ant-design/icons-vue is not a direct dependency
import dayjs, { type Dayjs } from 'dayjs';
import { calculateDeadline, type DeadlineResult } from '#/api/system/holiday';

// Props
interface Props {
  title?: string;
  bordered?: boolean;
  showPresets?: boolean;
  defaultDays?: number;
}

const props = withDefaults(defineProps<Props>(), {
  title: '诉讼期限计算器',
  bordered: true,
  showPresets: true,
  defaultDays: 15,
});

// Emits
const emit = defineEmits<{
  (e: 'calculated', result: DeadlineResult): void;
}>();

// 常用期限预设
const presets = [
  { label: '上诉期15日', days: 15, workdaysOnly: false },
  { label: '上诉期10日', days: 10, workdaysOnly: false },
  { label: '答辩期15日', days: 15, workdaysOnly: false },
  { label: '举证期30日', days: 30, workdaysOnly: false },
  { label: '申请执行2年', days: 730, workdaysOnly: false },
  { label: '管辖异议15日', days: 15, workdaysOnly: false },
];

// 状态
const loading = ref(false);
const result = ref<DeadlineResult | null>(null);

const form = reactive({
  startDate: dayjs() as Dayjs | undefined,
  days: props.defaultDays,
  workdaysOnly: false,
});

// 禁用未来日期
const disabledDate = (current: Dayjs) => {
  return current && current > dayjs().endOf('day');
};

// 计算截止日期
async function calculate() {
  if (!form.startDate || !form.days) {
    message.warning('请填写起算日期和期限天数');
    return;
  }

  loading.value = true;
  try {
    const res = await calculateDeadline(
      form.startDate.format('YYYY-MM-DD'),
      form.days,
      form.workdaysOnly,
    );
    result.value = res;
    emit('calculated', res);
  } catch (error: any) {
    message.error(error.message || '计算失败');
  } finally {
    loading.value = false;
  }
}

// 应用预设
function applyPreset(preset: { days: number; workdaysOnly: boolean }) {
  form.days = preset.days;
  form.workdaysOnly = preset.workdaysOnly;
  if (form.startDate) {
    calculate();
  }
}

// 重置
function reset() {
  result.value = null;
  form.startDate = dayjs() as Dayjs | undefined;
  form.days = props.defaultDays;
  form.workdaysOnly = false;
}

// 暴露方法
defineExpose({
  calculate,
  reset,
  getResult: () => result.value,
});
</script>

<style scoped>
.result-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-main {
  display: flex;
  gap: 8px;
  align-items: center;
}

.result-label {
  font-weight: 500;
}

.result-date {
  font-size: 16px;
  font-weight: 600;
}

.result-detail {
  font-size: 13px;
  color: #666;
}

.presets-section {
  margin-top: 16px;
}

.presets-title {
  font-size: 12px;
  color: #999;
}
</style>
