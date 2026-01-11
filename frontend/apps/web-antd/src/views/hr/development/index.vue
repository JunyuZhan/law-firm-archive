<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { message, Card, Table, Button, Space, Tag, Input, Modal, Form, FormItem, Select, Textarea, Progress, Descriptions, DescriptionsItem, InputNumber, DatePicker, Popconfirm, Timeline, Checkbox } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import { Plus } from '@vben/icons';
import {
  getDevelopmentPlanList,
  getDevelopmentPlanDetail,
  createDevelopmentPlan,
  updateDevelopmentPlan,
  deleteDevelopmentPlan,
  submitDevelopmentPlan,
  reviewDevelopmentPlan,
  updateMilestoneStatus,
  type DevelopmentPlanDTO,
  type DevelopmentPlanQuery,
  type DevelopmentMilestoneDTO,
} from '#/api/hr/development-plan';
import { getCareerLevelList, type CareerLevelDTO } from '#/api/hr/promotion';
import { UserTreeSelect } from '#/components/UserTreeSelect';
import dayjs from 'dayjs';

defineOptions({ name: 'HrDevelopment' });

// 状态
const loading = ref(false);
const dataSource = ref<DevelopmentPlanDTO[]>([]);
const total = ref(0);
const careerLevels = ref<CareerLevelDTO[]>([]);

// 查询参数
const queryParams = ref<DevelopmentPlanQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  planYear: new Date().getFullYear(),
});

// 弹窗
const modalVisible = ref(false);
const detailVisible = ref(false);
const editingId = ref<number | null>(null);
const currentRecord = ref<DevelopmentPlanDTO | null>(null);

// 表单
const formData = ref({
  planYear: new Date().getFullYear(),
  planTitle: '',
  targetLevelId: undefined as number | undefined,
  targetDate: undefined as any,
  careerGoals: [] as string[],
  skillGoals: [] as string[],
  performanceGoals: [] as string[],
  actionPlans: [] as string[],
  requiredTraining: '',
  requiredResources: '',
  mentorId: undefined as number | undefined,
  milestones: [] as { milestoneName: string; description?: string; targetDate?: any; sortOrder: number }[],
});

// 临时输入
const tempGoal = ref({ career: '', skill: '', performance: '', action: '' });

// 表格列
const columns = [
  { title: '计划编号', dataIndex: 'planNo', key: 'planNo', width: 130 },
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName', width: 100 },
  { title: '计划年度', dataIndex: 'planYear', key: 'planYear', width: 90 },
  { title: '计划标题', dataIndex: 'planTitle', key: 'planTitle', width: 180, ellipsis: true },
  { title: '当前职级', dataIndex: 'currentLevelName', key: 'currentLevelName', width: 100 },
  { title: '目标职级', dataIndex: 'targetLevelName', key: 'targetLevelName', width: 100 },
  { title: '完成进度', dataIndex: 'progressPercentage', key: 'progressPercentage', width: 150 },
  { title: '导师', dataIndex: 'mentorName', key: 'mentorName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' as const },
];

// 状态映射
const statusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: '草稿' },
  SUBMITTED: { color: 'processing', text: '待审核' },
  APPROVED: { color: 'success', text: '进行中' },
  COMPLETED: { color: 'success', text: '已完成' },
  CANCELLED: { color: 'default', text: '已取消' },
};

// 加载数据
async function loadData() {
  loading.value = true;
  try {
    const res = await getDevelopmentPlanList(queryParams.value);
    dataSource.value = res.list || [];
    total.value = res.total || 0;
  } catch (error: any) {
    message.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

// 加载职级选项
async function loadOptions() {
  try {
    const levelRes = await getCareerLevelList({ pageNum: 1, pageSize: 100, status: 'ACTIVE' });
    careerLevels.value = levelRes.list || [];
  } catch (error) {
    console.error('加载选项失败', error);
  }
}

// 搜索
function handleSearch() {
  queryParams.value.pageNum = 1;
  loadData();
}

// 分页
function handleTableChange(pagination: any) {
  queryParams.value.pageNum = pagination.current;
  queryParams.value.pageSize = pagination.pageSize;
  loadData();
}

// 打开新建弹窗
function handleAdd() {
  editingId.value = null;
  formData.value = {
    planYear: new Date().getFullYear(),
    planTitle: '',
    targetLevelId: undefined,
    targetDate: undefined,
    careerGoals: [],
    skillGoals: [],
    performanceGoals: [],
    actionPlans: [],
    requiredTraining: '',
    requiredResources: '',
    mentorId: undefined,
    milestones: [],
  };
  modalVisible.value = true;
}

// 编辑
async function handleEdit(record: DevelopmentPlanDTO) {
  try {
    const detail = await getDevelopmentPlanDetail(record.id);
    editingId.value = record.id;
    formData.value = {
      planYear: detail.planYear,
      planTitle: detail.planTitle,
      targetLevelId: detail.targetLevelId,
      targetDate: detail.targetDate ? dayjs(detail.targetDate) : undefined,
      careerGoals: detail.careerGoals || [],
      skillGoals: detail.skillGoals || [],
      performanceGoals: detail.performanceGoals || [],
      actionPlans: detail.actionPlans || [],
      requiredTraining: detail.requiredTraining || '',
      requiredResources: detail.requiredResources || '',
      mentorId: detail.mentorId,
      milestones: (detail.milestones || []).map((m, i) => ({
        milestoneName: m.milestoneName,
        description: m.description,
        targetDate: m.targetDate ? dayjs(m.targetDate) : undefined,
        sortOrder: m.sortOrder || i + 1,
      })),
    };
    modalVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

// 保存
async function handleSave() {
  if (!formData.value.planTitle) {
    message.error('请输入计划标题');
    return;
  }
  try {
    const data = {
      ...formData.value,
      targetDate: formData.value.targetDate?.format('YYYY-MM-DD'),
      milestones: formData.value.milestones.map(m => ({
        ...m,
        targetDate: m.targetDate?.format('YYYY-MM-DD'),
      })),
    };
    if (editingId.value) {
      await updateDevelopmentPlan(editingId.value, data);
      message.success('更新成功');
    } else {
      await createDevelopmentPlan(data);
      message.success('创建成功');
    }
    modalVisible.value = false;
    loadData();
  } catch (error: any) {
    message.error(error.message || '保存失败');
  }
}

// 查看详情
async function handleView(record: DevelopmentPlanDTO) {
  try {
    const detail = await getDevelopmentPlanDetail(record.id);
    currentRecord.value = detail;
    detailVisible.value = true;
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

// 删除
async function handleDelete(id: number) {
  try {
    await deleteDevelopmentPlan(id);
    message.success('删除成功');
    loadData();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 提交审核
async function handleSubmit(id: number) {
  try {
    await submitDevelopmentPlan(id);
    message.success('已提交审核');
    loadData();
  } catch (error: any) {
    message.error(error.message || '提交失败');
  }
}

// 审核通过
async function handleReview(id: number) {
  try {
    await reviewDevelopmentPlan(id);
    message.success('审核通过');
    loadData();
  } catch (error: any) {
    message.error(error.message || '审核失败');
  }
}

// 更新里程碑状态
async function handleMilestoneComplete(milestone: DevelopmentMilestoneDTO) {
  try {
    await updateMilestoneStatus(milestone.id, 'COMPLETED');
    message.success('里程碑已完成');
    if (currentRecord.value) {
      const detail = await getDevelopmentPlanDetail(currentRecord.value.id);
      currentRecord.value = detail;
    }
    loadData();
  } catch (error: any) {
    message.error(error.message || '更新失败');
  }
}

// 添加目标
function addGoal(type: 'career' | 'skill' | 'performance' | 'action') {
  const value = tempGoal.value[type].trim();
  if (!value) return;
  const key = type === 'career' ? 'careerGoals' : type === 'skill' ? 'skillGoals' : type === 'performance' ? 'performanceGoals' : 'actionPlans';
  formData.value[key].push(value);
  tempGoal.value[type] = '';
}

// 删除目标
function removeGoal(type: 'career' | 'skill' | 'performance' | 'action', index: number) {
  const key = type === 'career' ? 'careerGoals' : type === 'skill' ? 'skillGoals' : type === 'performance' ? 'performanceGoals' : 'actionPlans';
  formData.value[key].splice(index, 1);
}

// 添加里程碑
function addMilestone() {
  formData.value.milestones.push({
    milestoneName: '',
    description: '',
    targetDate: undefined,
    sortOrder: formData.value.milestones.length + 1,
  });
}

// 删除里程碑
function removeMilestone(index: number) {
  formData.value.milestones.splice(index, 1);
}

// 格式化日期
function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

onMounted(() => {
  loadData();
  loadOptions();
});
</script>

<template>
  <Page title="发展计划" description="管理员工职业发展计划">
    <Card>
      <div style=" display: flex; justify-content: space-between;margin-bottom: 16px;">
        <Space>
          <Select
            v-model:value="queryParams.planYear"
            placeholder="计划年度"
            style="width: 120px"
            allowClear
            :options="[2024, 2025, 2026, 2027].map(y => ({ label: y + '年', value: y }))"
            @change="handleSearch"
          />
          <Select
            v-model:value="queryParams.status"
            placeholder="状态"
            style="width: 120px"
            allowClear
            :options="Object.entries(statusMap).map(([k, v]) => ({ label: v.text, value: k }))"
            @change="handleSearch"
          />
          <Input
            v-model:value="queryParams.keyword"
            placeholder="搜索员工姓名"
            style="width: 200px"
            allowClear
            @pressEnter="handleSearch"
          />
          <Button @click="handleSearch">查询</Button>
        </Space>
        <Button type="primary" @click="handleAdd">
          <Plus />
          新建计划
        </Button>
      </div>
      
      <Table
        :columns="columns"
        :dataSource="dataSource"
        :loading="loading"
        :pagination="{
          current: queryParams.pageNum,
          pageSize: queryParams.pageSize,
          total: total,
          showSizeChanger: true,
          showQuickJumper: true,
        }"
        :scroll="{ x: 1300 }"
        rowKey="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'progressPercentage'">
            <Progress :percent="record.progressPercentage || 0" :size="[100, 10]" />
          </template>
          <template v-else-if="column.key === 'status'">
            <Tag :color="statusMap[record.status]?.color || 'default'">
              {{ record.statusName || statusMap[record.status]?.text || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <Space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 'DRAFT'" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 'DRAFT'" @click="handleSubmit(record.id)">提交</a>
              <a v-if="record.status === 'SUBMITTED'" @click="handleReview(record.id)">审核</a>
              <Popconfirm
                v-if="record.status === 'DRAFT'"
                title="确定要删除此计划吗？"
                @confirm="handleDelete(record.id)"
              >
                <a style="color: #ff4d4f">删除</a>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新建/编辑弹窗 -->
    <Modal v-model:open="modalVisible" :title="editingId ? '编辑发展计划' : '新建发展计划'" width="800px" @ok="handleSave">
      <Form :labelCol="{ span: 5 }" :wrapperCol="{ span: 18 }">
        <FormItem label="计划年度" required>
          <InputNumber v-model:value="formData.planYear" :min="2020" :max="2030" style="width: 120px" />
        </FormItem>
        <FormItem label="计划标题" required>
          <Input v-model:value="formData.planTitle" placeholder="如：2026年度职业发展计划" />
        </FormItem>
        <FormItem label="目标职级">
          <Select
            v-model:value="formData.targetLevelId"
            placeholder="选择目标职级"
            allowClear
            :options="careerLevels.map(l => ({ label: l.levelName, value: l.id }))"
          />
        </FormItem>
        <FormItem label="目标日期">
          <DatePicker v-model:value="formData.targetDate" style="width: 100%" />
        </FormItem>
        <FormItem label="导师">
          <UserTreeSelect
            v-model:value="formData.mentorId"
            placeholder="选择导师（按部门筛选）"
          />
        </FormItem>
        <FormItem label="职业目标">
          <div>
            <Space v-for="(goal, i) in formData.careerGoals" :key="i" style="margin-bottom: 4px">
              <Tag closable @close="removeGoal('career', i)">{{ goal }}</Tag>
            </Space>
          </div>
          <Space>
            <Input v-model:value="tempGoal.career" placeholder="输入职业目标" style="width: 300px" @pressEnter="addGoal('career')" />
            <Button size="small" @click="addGoal('career')">添加</Button>
          </Space>
        </FormItem>
        <FormItem label="技能目标">
          <div>
            <Space v-for="(goal, i) in formData.skillGoals" :key="i" style="margin-bottom: 4px">
              <Tag closable @close="removeGoal('skill', i)">{{ goal }}</Tag>
            </Space>
          </div>
          <Space>
            <Input v-model:value="tempGoal.skill" placeholder="输入技能目标" style="width: 300px" @pressEnter="addGoal('skill')" />
            <Button size="small" @click="addGoal('skill')">添加</Button>
          </Space>
        </FormItem>
        <FormItem label="所需培训">
          <Textarea v-model:value="formData.requiredTraining" :rows="2" placeholder="需要参加的培训课程" />
        </FormItem>
        <FormItem label="里程碑">
          <div v-for="(m, i) in formData.milestones" :key="i" style=" padding: 8px;margin-bottom: 8px; background: #fafafa; border-radius: 4px;">
            <Space style="width: 100%">
              <Input v-model:value="m.milestoneName" placeholder="里程碑名称" style="width: 200px" />
              <DatePicker v-model:value="m.targetDate" placeholder="目标日期" />
              <Button size="small" danger @click="removeMilestone(i)">删除</Button>
            </Space>
          </div>
          <Button size="small" @click="addMilestone">+ 添加里程碑</Button>
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal v-model:open="detailVisible" title="发展计划详情" width="800px" :footer="null">
      <Descriptions v-if="currentRecord" :column="2" bordered size="small">
        <DescriptionsItem label="计划编号">{{ currentRecord.planNo }}</DescriptionsItem>
        <DescriptionsItem label="员工姓名">{{ currentRecord.employeeName }}</DescriptionsItem>
        <DescriptionsItem label="计划年度">{{ currentRecord.planYear }}</DescriptionsItem>
        <DescriptionsItem label="计划标题">{{ currentRecord.planTitle }}</DescriptionsItem>
        <DescriptionsItem label="当前职级">{{ currentRecord.currentLevelName || '-' }}</DescriptionsItem>
        <DescriptionsItem label="目标职级">{{ currentRecord.targetLevelName || '-' }}</DescriptionsItem>
        <DescriptionsItem label="目标日期">{{ formatDate(currentRecord.targetDate) }}</DescriptionsItem>
        <DescriptionsItem label="导师">{{ currentRecord.mentorName || '-' }}</DescriptionsItem>
        <DescriptionsItem label="完成进度" :span="2">
          <Progress :percent="currentRecord.progressPercentage || 0" />
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="statusMap[currentRecord.status]?.color">
            {{ currentRecord.statusName || statusMap[currentRecord.status]?.text }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="审核人">{{ currentRecord.reviewedByName || '-' }}</DescriptionsItem>
        <DescriptionsItem v-if="currentRecord.careerGoals?.length" label="职业目标" :span="2">
          <Tag v-for="(g, i) in currentRecord.careerGoals" :key="i" style="margin: 2px">{{ g }}</Tag>
        </DescriptionsItem>
        <DescriptionsItem v-if="currentRecord.skillGoals?.length" label="技能目标" :span="2">
          <Tag v-for="(g, i) in currentRecord.skillGoals" :key="i" style="margin: 2px">{{ g }}</Tag>
        </DescriptionsItem>
        <DescriptionsItem v-if="currentRecord.requiredTraining" label="所需培训" :span="2">
          {{ currentRecord.requiredTraining }}
        </DescriptionsItem>
      </Descriptions>
      
      <div v-if="currentRecord?.milestones?.length" style="margin-top: 16px">
        <h4>里程碑</h4>
        <Timeline>
          <Timeline.Item
            v-for="m in currentRecord.milestones"
            :key="m.id"
            :color="m.status === 'COMPLETED' ? 'green' : 'blue'"
          >
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <div>
                <p><strong>{{ m.milestoneName }}</strong></p>
                <p v-if="m.description" style="color: #666">{{ m.description }}</p>
                <p style=" font-size: 12px;color: #999">目标日期: {{ formatDate(m.targetDate) }}</p>
              </div>
              <div>
                <Tag v-if="m.status === 'COMPLETED'" color="success">已完成</Tag>
                <Button
                  v-else-if="currentRecord.status === 'APPROVED'"
                  size="small"
                  type="primary"
                  @click="handleMilestoneComplete(m)"
                >
                  标记完成
                </Button>
              </div>
            </div>
          </Timeline.Item>
        </Timeline>
      </div>
    </Modal>
  </Page>
</template>
