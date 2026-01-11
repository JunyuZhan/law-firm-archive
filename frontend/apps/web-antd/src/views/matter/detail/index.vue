<script setup lang="ts">
import type {
  CreateLetterApplicationCommand,
  LetterApplicationDTO,
  LetterTemplateDTO,
} from '#/api/admin';
import type { MatterDTO, MatterTimelineDTO } from '#/api/matter';
import type { CreateDeadlineCommand, DeadlineDTO } from '#/api/matter/deadline';
import type { CreateScheduleCommand, ScheduleDTO } from '#/api/matter/schedule';

import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import {
  Button,
  Card,
  Checkbox,
  DatePicker,
  Descriptions,
  DescriptionsItem,
  Empty,
  Form,
  FormItem,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  TabPane,
  Tabs,
  Tag,
  Textarea,
  TimeRangePicker,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createLetterApplication,
  getActiveTemplatesPublic,
  getApplicationsByMatter,
} from '#/api/admin';
import {
  applyCloseMatter,
  getMatterCloseApprovers,
  getMatterDetail,
  getMatterTimeline,
} from '#/api/matter';
import {
  autoCreateDeadlines,
  completeDeadline,
  createDeadline,
  DEADLINE_TYPE_OPTIONS,
  deleteDeadline,
  getDeadlinesByMatterId,
} from '#/api/matter/deadline';
import {
  cancelSchedule,
  createSchedule,
  deleteSchedule,
  getSchedules,
  REMINDER_OPTIONS,
  SCHEDULE_TYPE_OPTIONS,
} from '#/api/matter/schedule';
import { DossierManager } from '#/components/DossierManager';
import { EvidenceListManager } from '#/components/EvidenceManager';
import { UserTreeSelect } from '#/components/UserTreeSelect';

defineOptions({ name: 'MatterDetail' });

const route = useRoute();
const router = useRouter();

// 获取当前用户
const userStore = useUserStore();
const currentUserId = computed(() => Number(userStore.userInfo?.userId) || 0);

// 状态
const loading = ref(false);
const matter = ref<MatterDTO | null>(null);
const timeline = ref<MatterTimelineDTO[]>([]);
const activeTab = ref('dossier');

// 出函相关
const letterModalVisible = ref(false);
const templates = ref<LetterTemplateDTO[]>([]);
const letterApplications = ref<LetterApplicationDTO[]>([]);
const letterDetailVisible = ref(false);
const letterDetailRecord = ref<LetterApplicationDTO | null>(null);
const userTreeSelectKey = ref(0); // 用于强制重新渲染组件

// 期限提醒相关
const deadlines = ref<DeadlineDTO[]>([]);
const deadlineModalVisible = ref(false);
const deadlineForm = ref<Partial<CreateDeadlineCommand>>({
  deadlineType: undefined,
  deadlineName: '',
  deadlineDate: undefined,
  reminderDays: 3,
  description: '',
});

// 日程管理相关
const schedules = ref<ScheduleDTO[]>([]);
const scheduleModalVisible = ref(false);
const scheduleForm = ref<
  Partial<CreateScheduleCommand> & { date?: any; time?: [any, any] | null }
>({
  title: '',
  description: '',
  location: '',
  scheduleType: 'COURT',
  allDay: false,
  reminderMinutes: 30,
  date: undefined,
  time: null,
});
const letterForm = ref<
  Partial<CreateLetterApplicationCommand> & {
    approverId?: number;
    expectedDate?: any;
  }
>({
  templateId: undefined,
  targetUnit: '',
  targetContact: '',
  targetPhone: '',
  targetAddress: '',
  purpose: '',
  lawyerIds: [],
  approverId: undefined,
  copies: 1,
  expectedDate: undefined,
  remark: '',
});

// 项目是否可编辑文档/证据（只有已归档后才禁止编辑，结案后仍可整理文档）
const canEditDocument = computed(() => {
  if (!matter.value) return false;
  // 只有 ARCHIVED（已入库归档）状态才禁止编辑
  // CLOSED（已结案）状态仍可编辑，因为可能需要补充文档以符合入库条件
  return matter.value.status !== 'ARCHIVED';
});

// 项目是否可编辑基本操作（期限、日程等）
const canEditOperations = computed(() => {
  if (!matter.value) return false;
  // 待审批结案、已结案、已归档状态禁止新增期限和日程
  return !['ARCHIVED', 'CLOSED', 'PENDING_CLOSE'].includes(matter.value.status);
});

// 项目类型映射
const matterTypeMap: Record<string, string> = {
  LITIGATION: '诉讼案件',
  NON_LITIGATION: '非诉项目',
  LEGAL_COUNSEL: '法律顾问',
  SPECIAL_SERVICE: '专项服务',
};

// 状态映射
const statusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { text: '草稿', color: 'default' },
  PENDING: { text: '待审批', color: 'orange' },
  ACTIVE: { text: '进行中', color: 'processing' },
  SUSPENDED: { text: '已暂停', color: 'error' },
  PENDING_CLOSE: { text: '待审批结案', color: 'orange' },
  CLOSED: { text: '已结案', color: 'success' },
  ARCHIVED: { text: '已归档', color: 'purple' },
};

// 收费类型映射
const feeTypeMap: Record<string, string> = {
  HOURLY: '计时收费',
  FIXED: '固定收费',
  CONTINGENCY: '风险代理',
  MIXED: '混合收费',
};

// 角色映射
const roleMap: Record<string, string> = {
  LEAD: '主办律师',
  ASSISTANT: '协办律师',
  ORIGINATOR: '案源人',
  CONSULTANT: '顾问',
};

// 加载项目详情
async function loadMatterDetail() {
  const id = Number(route.params.id);
  if (!id) {
    message.error('项目ID无效');
    router.back();
    return;
  }

  loading.value = true;
  try {
    const [matterRes, timelineRes, letterRes, deadlineRes, scheduleRes] =
      await Promise.all([
        getMatterDetail(id),
        getMatterTimeline(id).catch(() => []),
        getApplicationsByMatter(id).catch(() => []),
        getDeadlinesByMatterId(id).catch(() => []),
        getSchedules({ matterId: id, pageNum: 1, pageSize: 100 }).catch(
          () => [],
        ),
      ]);
    matter.value = matterRes;
    timeline.value = timelineRes || [];
    letterApplications.value = letterRes || [];
    deadlines.value = deadlineRes || [];
    schedules.value = scheduleRes || [];
  } catch (error: any) {
    message.error(error.message || '加载项目详情失败');
    router.back();
  } finally {
    loading.value = false;
  }
}

// 加载出函选项数据（使用公共接口，无需 admin:letter:list 权限）
async function loadLetterOptions() {
  try {
    const templateRes = await getActiveTemplatesPublic();
    templates.value = templateRes || [];
  } catch (error) {
    console.error('加载出函选项失败', error);
  }
}

// 打开申请出函弹窗
async function handleApplyLetter() {
  // 计算默认律师：优先选择项目办理人（团队成员中的律师），如果没有则选择当前登录用户
  const defaultLawyerIds: number[] = [];
  if (matter.value) {
    // 优先选择项目团队成员中的律师（包括主办律师）
    if (matter.value.participants && matter.value.participants.length > 0) {
      matter.value.participants.forEach((participant) => {
        // 只添加律师角色（LEAD, CO_COUNSEL），排除律师助理（PARALEGAL）
        if (
          participant.userId &&
          ['CO_COUNSEL', 'LEAD'].includes(participant.role) &&
          !defaultLawyerIds.includes(participant.userId)
        ) {
          defaultLawyerIds.push(participant.userId);
        }
      });
    }
    // 如果没有团队成员，则添加主办律师
    if (defaultLawyerIds.length === 0 && matter.value.leadLawyerId) {
      defaultLawyerIds.push(matter.value.leadLawyerId);
    }
  }

  // 如果项目没有办理人，则选择当前登录用户
  if (defaultLawyerIds.length === 0 && currentUserId.value > 0) {
    defaultLawyerIds.push(currentUserId.value);
  }

  // 设置表单初始值（包含默认律师）
  letterForm.value = {
    templateId: undefined,
    targetUnit: '',
    targetContact: '',
    targetPhone: '',
    targetAddress: '',
    purpose: '',
    lawyerIds: defaultLawyerIds,
    approverId: undefined,
    copies: 1,
    expectedDate: undefined,
    remark: '',
  };

  // 更新 key 强制重新渲染组件
  userTreeSelectKey.value++;

  // 打开弹窗
  letterModalVisible.value = true;

  // 加载模板数据
  await loadLetterOptions();
}

// 提交出函申请
async function handleSubmitLetter() {
  if (!letterForm.value.templateId) {
    message.error('请选择函件模板');
    return;
  }
  if (!letterForm.value.targetUnit) {
    message.error('请输入接收单位');
    return;
  }
  if (!letterForm.value.purpose) {
    message.error('请输入出函事由');
    return;
  }
  if (!letterForm.value.approverId) {
    message.error('请选择审批人');
    return;
  }

  try {
    const data: CreateLetterApplicationCommand = {
      templateId: letterForm.value.templateId,
      matterId: matter.value!.id,
      targetUnit: letterForm.value.targetUnit!,
      targetContact: letterForm.value.targetContact,
      targetPhone: letterForm.value.targetPhone,
      targetAddress: letterForm.value.targetAddress,
      purpose: letterForm.value.purpose!,
      lawyerIds: letterForm.value.lawyerIds,
      approverId: letterForm.value.approverId,
      copies: letterForm.value.copies,
      expectedDate: letterForm.value.expectedDate
        ? typeof letterForm.value.expectedDate === 'string'
          ? letterForm.value.expectedDate
          : letterForm.value.expectedDate.format('YYYY-MM-DD')
        : undefined,
      remark: letterForm.value.remark,
    };
    await createLetterApplication(data);
    message.success('出函申请已提交，等待审批');
    letterModalVisible.value = false;
    const letterRes = await getApplicationsByMatter(matter.value!.id);
    letterApplications.value = letterRes || [];
  } catch (error: any) {
    message.error(error.message || '提交申请失败');
  }
}

// 出函状态颜色
function getLetterStatusColor(status: string) {
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

// 查看出函详情
function handleViewLetterDetail(record: LetterApplicationDTO) {
  letterDetailRecord.value = record;
  letterDetailVisible.value = true;
}

// 返回列表
function handleBack() {
  router.back();
}

// 编辑项目
function handleEdit() {
  if (matter.value?.id) {
    router.push({ path: '/matter/list', query: { id: matter.value.id } });
  }
}

// 结案申请弹窗
const closeModalVisible = ref(false);
const closeForm = ref({
  closingDate: undefined as any,
  closingReason: 'WIN', // 默认胜诉
  outcome: '',
  summary: '',
  approverId: undefined as number | undefined, // 审批人
});

// 结案审批人列表
const closeApprovers = ref<
  Array<{
    departmentName: string;
    id: number;
    position: string;
    realName: string;
    recommended: boolean;
  }>
>([]);

// 结案原因选项
const closingReasonOptions = [
  { value: 'WIN', label: '胜诉' },
  { value: 'LOSE', label: '败诉' },
  { value: 'SETTLEMENT', label: '和解' },
  { value: 'WITHDRAWAL', label: '撤诉' },
  { value: 'COMPLETED', label: '项目完成' },
  { value: 'OTHER', label: '其他' },
];

// 加载结案审批人列表
async function loadCloseApprovers() {
  try {
    const approvers = await getMatterCloseApprovers();
    closeApprovers.value = approvers || [];
    // 自动选择推荐的审批人（团队负责人）
    const recommended = approvers.find((a) => a.recommended);
    if (recommended) {
      closeForm.value.approverId = recommended.id;
    }
  } catch (error) {
    console.error('加载结案审批人列表失败', error);
  }
}

// 申请结案
async function handleApplyClose() {
  if (!matter.value) return;

  // 重置表单
  closeForm.value = {
    closingDate: undefined,
    closingReason: 'COMPLETED',
    outcome: '',
    summary: '',
    approverId: undefined,
  };

  // 加载审批人列表并默认选择推荐的审批人
  await loadCloseApprovers();

  closeModalVisible.value = true;
}

// 提交结案申请
async function handleSubmitClose() {
  if (!matter.value) return;

  if (!closeForm.value.closingDate) {
    message.error('请选择结案日期');
    return;
  }
  if (!closeForm.value.closingReason) {
    message.error('请选择结案原因');
    return;
  }
  if (!closeForm.value.approverId) {
    message.error('请选择审批人');
    return;
  }

  try {
    await applyCloseMatter(matter.value.id, {
      closingDate: closeForm.value.closingDate.format
        ? closeForm.value.closingDate.format('YYYY-MM-DD')
        : closeForm.value.closingDate,
      closingReason: closeForm.value.closingReason,
      outcome: closeForm.value.outcome,
      summary: closeForm.value.summary,
      approverId: closeForm.value.approverId,
    });
    message.success('结案申请已提交，请等待审批');
    closeModalVisible.value = false;
    // 重新加载详情
    loadMatterDetail();
  } catch (error: any) {
    message.error(error.message || '结案申请失败');
  }
}

// 创建档案（跳转到档案列表页）
function handleArchive() {
  if (!matter.value) return;

  router.push({
    path: '/archive/list',
    query: { matterId: matter.value.id },
  });
}

// ========== 期限提醒功能 ==========

// 加载期限列表
async function loadDeadlines() {
  if (!matter.value?.id) return;
  try {
    const res = await getDeadlinesByMatterId(matter.value.id);
    deadlines.value = res || [];
  } catch (error) {
    console.error('加载期限列表失败', error);
  }
}

// 打开添加期限弹窗
function handleAddDeadline() {
  deadlineForm.value = {
    deadlineType: undefined,
    deadlineName: '',
    deadlineDate: undefined,
    reminderDays: 3,
    description: '',
  };
  deadlineModalVisible.value = true;
}

// 提交添加期限
async function handleSubmitDeadline() {
  if (!deadlineForm.value.deadlineType) {
    message.error('请选择期限类型');
    return;
  }
  if (!deadlineForm.value.deadlineName) {
    message.error('请输入期限名称');
    return;
  }
  if (!deadlineForm.value.deadlineDate) {
    message.error('请选择截止日期');
    return;
  }

  try {
    const data: CreateDeadlineCommand = {
      matterId: matter.value!.id,
      deadlineType: deadlineForm.value.deadlineType,
      deadlineName: deadlineForm.value.deadlineName,
      deadlineDate:
        typeof deadlineForm.value.deadlineDate === 'string'
          ? deadlineForm.value.deadlineDate
          : (deadlineForm.value.deadlineDate as any).format('YYYY-MM-DD'),
      reminderDays: deadlineForm.value.reminderDays,
      description: deadlineForm.value.description,
    };
    await createDeadline(data);
    message.success('期限添加成功');
    deadlineModalVisible.value = false;
    await loadDeadlines();
  } catch (error: any) {
    message.error(error.message || '添加期限失败');
  }
}

// 自动创建期限
async function handleAutoCreateDeadlines() {
  if (!matter.value?.id) return;
  try {
    await autoCreateDeadlines(matter.value.id);
    message.success('已自动创建常用期限');
    await loadDeadlines();
  } catch (error: any) {
    message.error(error.message || '自动创建期限失败');
  }
}

// 完成期限
async function handleCompleteDeadline(id: number) {
  try {
    await completeDeadline(id);
    message.success('期限已标记为完成');
    await loadDeadlines();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 删除期限
async function handleDeleteDeadline(id: number) {
  try {
    await deleteDeadline(id);
    message.success('期限已删除');
    await loadDeadlines();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 期限状态颜色
function getDeadlineStatusColor(status: string, daysRemaining?: number) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'EXPIRED') return 'error';
  if (daysRemaining !== undefined && daysRemaining <= 3) return 'warning';
  return 'processing';
}

// 期限状态文本
function getDeadlineStatusText(status: string, daysRemaining?: number) {
  if (status === 'COMPLETED') return '已完成';
  if (status === 'EXPIRED') return '已过期';
  if (daysRemaining !== undefined) {
    if (daysRemaining < 0) return `已过期${Math.abs(daysRemaining)}天`;
    if (daysRemaining === 0) return '今天到期';
    return `剩余${daysRemaining}天`;
  }
  return '待处理';
}

// ========== 日程管理功能 ==========

// 加载日程列表
async function loadSchedules() {
  if (!matter.value?.id) return;
  try {
    const res = await getSchedules({
      matterId: matter.value.id,
      pageNum: 1,
      pageSize: 100,
    });
    schedules.value = res || [];
  } catch (error) {
    console.error('加载日程列表失败', error);
  }
}

// 打开添加日程弹窗
function handleAddSchedule() {
  scheduleForm.value = {
    title: '',
    description: '',
    location: '',
    scheduleType: 'COURT',
    allDay: false,
    reminderMinutes: 30,
    date: dayjs(),
    time: [dayjs().hour(9).minute(0), dayjs().hour(10).minute(0)],
  };
  scheduleModalVisible.value = true;
}

// 提交添加日程
async function handleSubmitSchedule() {
  if (!scheduleForm.value.title) {
    message.error('请输入日程标题');
    return;
  }
  if (!scheduleForm.value.date) {
    message.error('请选择日期');
    return;
  }

  try {
    const date = dayjs(scheduleForm.value.date);
    let startTime: string;
    let endTime: string | undefined;

    if (scheduleForm.value.allDay) {
      startTime = date.startOf('day').format('YYYY-MM-DDTHH:mm:ss');
      endTime = date.endOf('day').format('YYYY-MM-DDTHH:mm:ss');
    } else if (scheduleForm.value.time && scheduleForm.value.time.length >= 2) {
      const startHour = dayjs(scheduleForm.value.time[0]);
      const endHour = dayjs(scheduleForm.value.time[1]);
      startTime = date
        .hour(startHour.hour())
        .minute(startHour.minute())
        .format('YYYY-MM-DDTHH:mm:ss');
      endTime = date
        .hour(endHour.hour())
        .minute(endHour.minute())
        .format('YYYY-MM-DDTHH:mm:ss');
    } else {
      startTime = date.hour(9).minute(0).format('YYYY-MM-DDTHH:mm:ss');
      endTime = date.hour(10).minute(0).format('YYYY-MM-DDTHH:mm:ss');
    }

    const data: CreateScheduleCommand = {
      matterId: matter.value!.id,
      title: scheduleForm.value.title,
      description: scheduleForm.value.description,
      location: scheduleForm.value.location,
      scheduleType: scheduleForm.value.scheduleType || 'COURT',
      startTime,
      endTime,
      allDay: scheduleForm.value.allDay,
      reminderMinutes: scheduleForm.value.reminderMinutes,
    };
    await createSchedule(data);
    message.success('日程添加成功');
    scheduleModalVisible.value = false;
    await loadSchedules();
  } catch (error: any) {
    message.error(error.message || '添加日程失败');
  }
}

// 取消日程
async function handleCancelSchedule(id: number) {
  try {
    await cancelSchedule(id);
    message.success('日程已取消');
    await loadSchedules();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 删除日程
async function handleDeleteSchedule(id: number) {
  try {
    await deleteSchedule(id);
    message.success('日程已删除');
    await loadSchedules();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 日程类型颜色
function getScheduleTypeColor(type: string) {
  const opt = SCHEDULE_TYPE_OPTIONS.find((o) => o.value === type);
  return opt?.color || '#722ed1';
}

// 日程状态颜色
function getScheduleStatusColor(status: string) {
  const map: Record<string, string> = {
    ACTIVE: 'processing',
    COMPLETED: 'success',
    CANCELLED: 'default',
  };
  return map[status] || 'default';
}

// 日程状态文本
function getScheduleStatusText(status: string) {
  const map: Record<string, string> = {
    ACTIVE: '进行中',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  };
  return map[status] || status;
}

onMounted(() => {
  loadMatterDetail();
});
</script>

<template>
  <Page>
    <Spin :spinning="loading">
      <div v-if="matter" class="matter-detail">
        <!-- 头部操作栏 -->
        <div style="margin-bottom: 16px">
          <Button @click="handleBack">← 返回</Button>
          <Space style="float: right">
            <Button
              v-if="
                !['ARCHIVED', 'CLOSED', 'PENDING_CLOSE'].includes(matter.status)
              "
              @click="handleEdit"
            >
              编辑
            </Button>
            <Button
              v-if="matter.status === 'ACTIVE'"
              type="primary"
              danger
              @click="handleApplyClose"
            >
              申请结案
            </Button>
            <Button
              v-if="matter.status === 'CLOSED'"
              type="primary"
              style="background-color: #722ed1; border-color: #722ed1"
              @click="handleArchive"
            >
              创建档案
            </Button>
          </Space>
        </div>

        <!-- 基本信息卡片 -->
        <Card title="基本信息" style="margin-bottom: 16px">
          <Descriptions :column="2" bordered>
            <DescriptionsItem label="项目编号">
              {{ matter.matterNo }}
            </DescriptionsItem>
            <DescriptionsItem label="项目名称">
              {{ matter.name }}
            </DescriptionsItem>
            <DescriptionsItem label="项目类型">
              <Tag>
                {{ matterTypeMap[matter.matterType] || matter.matterType }}
              </Tag>
            </DescriptionsItem>
            <DescriptionsItem label="业务类型">
              {{ matter.businessType || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="客户">
              {{ matter.clientName || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="状态">
              <Tag :color="statusMap[matter.status]?.color || 'default'">
                {{ statusMap[matter.status]?.text || matter.status }}
              </Tag>
            </DescriptionsItem>
            <DescriptionsItem label="案源人">
              {{ matter.originatorName || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="主办律师">
              {{ matter.leadLawyerName || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="部门">
              {{ matter.departmentName || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="收费类型">
              {{ feeTypeMap[matter.feeType || ''] || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="预估费用">
              {{
                matter.estimatedFee
                  ? `¥${matter.estimatedFee.toLocaleString()}`
                  : '-'
              }}
            </DescriptionsItem>
            <DescriptionsItem label="实际费用">
              {{
                matter.actualFee ? `¥${matter.actualFee.toLocaleString()}` : '-'
              }}
            </DescriptionsItem>
            <DescriptionsItem label="立案日期">
              {{
                matter.filingDate
                  ? dayjs(matter.filingDate).format('YYYY-MM-DD')
                  : '-'
              }}
            </DescriptionsItem>
            <DescriptionsItem label="预计结案日期">
              {{
                matter.expectedClosingDate
                  ? dayjs(matter.expectedClosingDate).format('YYYY-MM-DD')
                  : '-'
              }}
            </DescriptionsItem>
            <DescriptionsItem label="实际结案日期">
              {{
                matter.actualClosingDate
                  ? dayjs(matter.actualClosingDate).format('YYYY-MM-DD')
                  : '-'
              }}
            </DescriptionsItem>
            <DescriptionsItem label="标的金额">
              {{
                matter.claimAmount
                  ? `¥${matter.claimAmount.toLocaleString()}`
                  : '-'
              }}
            </DescriptionsItem>
            <DescriptionsItem label="创建时间" :span="2">
              {{
                matter.createdAt
                  ? dayjs(matter.createdAt).format('YYYY-MM-DD HH:mm:ss')
                  : '-'
              }}
            </DescriptionsItem>
            <DescriptionsItem label="项目描述" :span="2">
              {{ matter.description || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="备注" :span="2">
              {{ matter.remark || '-' }}
            </DescriptionsItem>
          </Descriptions>
        </Card>

        <!-- 标签页内容 -->
        <Card style="margin-bottom: 16px">
          <Tabs v-model:active-key="activeTab">
            <!-- 卷宗文件 - 显示项目卷宗目录结构（优先级最高） -->
            <TabPane key="dossier" tab="卷宗文件">
              <DossierManager
                :matter-id="matter.id"
                :readonly="!canEditDocument"
              />
            </TabPane>

            <!-- 证据整理 - 支持表格式和清单式两种编辑模式 -->
            <TabPane key="evidence" tab="证据整理">
              <EvidenceListManager
                ref="evidenceManagerRef"
                :matter-id="matter.id"
                :readonly="!canEditDocument"
              />
            </TabPane>

            <!-- 出函记录 -->
            <TabPane key="letters" tab="出函记录">
              <div
                v-if="matter.status === 'ACTIVE' || matter.status === 'DRAFT'"
                style="margin-bottom: 16px"
              >
                <Button type="primary" @click="handleApplyLetter">
                  申请出函
                </Button>
              </div>
              <Table
                v-if="letterApplications.length > 0"
                :columns="[
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
                    title: '接收单位',
                    dataIndex: 'targetUnit',
                    key: 'targetUnit',
                    width: 180,
                    ellipsis: true,
                  },
                  {
                    title: '联系人',
                    dataIndex: 'targetContact',
                    key: 'targetContact',
                    width: 100,
                  },
                  {
                    title: '联系电话',
                    dataIndex: 'targetPhone',
                    key: 'targetPhone',
                    width: 120,
                  },
                  {
                    title: '出函律师',
                    dataIndex: 'lawyerNames',
                    key: 'lawyerNames',
                    width: 120,
                  },
                  {
                    title: '份数',
                    dataIndex: 'copies',
                    key: 'copies',
                    width: 60,
                  },
                  {
                    title: '期望日期',
                    dataIndex: 'expectedDate',
                    key: 'expectedDate',
                    width: 100,
                  },
                  {
                    title: '申请人',
                    dataIndex: 'applicantName',
                    key: 'applicantName',
                    width: 100,
                  },
                  {
                    title: '状态',
                    dataIndex: 'statusName',
                    key: 'statusName',
                    width: 100,
                  },
                  {
                    title: '申请时间',
                    dataIndex: 'createdAt',
                    key: 'createdAt',
                    width: 160,
                  },
                  {
                    title: '操作',
                    key: 'action',
                    width: 100,
                    fixed: 'right' as const,
                  },
                ]"
                :data-source="letterApplications"
                :pagination="false"
                row-key="id"
                size="small"
                :scroll="{ x: 1400 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'statusName'">
                    <Tag :color="getLetterStatusColor(record.status)">
                      {{ record.statusName }}
                    </Tag>
                  </template>
                  <template v-else-if="column.key === 'expectedDate'">
                    {{
                      record.expectedDate
                        ? dayjs(record.expectedDate).format('YYYY-MM-DD')
                        : '-'
                    }}
                  </template>
                  <template v-else-if="column.key === 'createdAt'">
                    {{ dayjs(record.createdAt).format('YYYY-MM-DD HH:mm') }}
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <Button
                      type="link"
                      size="small"
                      @click="
                        handleViewLetterDetail(record as LetterApplicationDTO)
                      "
                    >
                      查看详情
                    </Button>
                  </template>
                </template>
              </Table>
              <Empty v-else description="暂无出函记录">
                <Button
                  v-if="matter.status === 'ACTIVE' || matter.status === 'DRAFT'"
                  type="primary"
                  @click="handleApplyLetter"
                >
                  申请出函
                </Button>
              </Empty>
            </TabPane>

            <!-- 期限提醒 -->
            <TabPane key="deadlines" tab="期限提醒">
              <div v-if="canEditOperations" style="margin-bottom: 16px">
                <Space>
                  <Button type="primary" @click="handleAddDeadline">
                    添加期限
                  </Button>
                  <Button @click="handleAutoCreateDeadlines">
                    自动创建常用期限
                  </Button>
                </Space>
              </div>
              <Table
                v-if="deadlines.length > 0"
                :columns="[
                  {
                    title: '期限名称',
                    dataIndex: 'deadlineName',
                    key: 'deadlineName',
                    width: 120,
                  },
                  {
                    title: '类型',
                    dataIndex: 'deadlineTypeName',
                    key: 'deadlineTypeName',
                    width: 80,
                  },
                  {
                    title: '截止日期',
                    dataIndex: 'deadlineDate',
                    key: 'deadlineDate',
                    width: 100,
                  },
                  { title: '状态', key: 'status', width: 80 },
                  {
                    title: '提醒',
                    dataIndex: 'reminderDays',
                    key: 'reminderDays',
                    width: 60,
                  },
                  {
                    title: '说明',
                    dataIndex: 'description',
                    key: 'description',
                    ellipsis: true,
                  },
                  { title: '操作', key: 'action', width: 100 },
                ]"
                :data-source="deadlines"
                :pagination="false"
                row-key="id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'deadlineDate'">
                    {{
                      record.deadlineDate
                        ? dayjs(record.deadlineDate).format('YYYY-MM-DD')
                        : '-'
                    }}
                  </template>
                  <template v-else-if="column.key === 'status'">
                    <Tag
                      :color="
                        getDeadlineStatusColor(
                          record.status,
                          record.daysRemaining,
                        )
                      "
                    >
                      {{
                        getDeadlineStatusText(
                          record.status,
                          record.daysRemaining,
                        )
                      }}
                    </Tag>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <Space v-if="canEditOperations">
                      <Button
                        v-if="record.status === 'PENDING'"
                        type="link"
                        size="small"
                        @click="handleCompleteDeadline(record.id)"
                      >
                        完成
                      </Button>
                      <Popconfirm
                        title="确定要删除这个期限吗？"
                        @confirm="handleDeleteDeadline(record.id)"
                      >
                        <Button type="link" size="small" danger>删除</Button>
                      </Popconfirm>
                    </Space>
                    <span v-else>-</span>
                  </template>
                </template>
              </Table>
              <Empty
                v-else
                :description="
                  canEditOperations
                    ? '暂无期限提醒，点击上方按钮添加'
                    : '暂无期限提醒'
                "
              />
            </TabPane>

            <!-- 日程安排 -->
            <TabPane key="schedules" tab="日程安排">
              <div v-if="canEditOperations" style="margin-bottom: 16px">
                <Button type="primary" @click="handleAddSchedule">
                  添加日程
                </Button>
              </div>
              <Table
                v-if="schedules.length > 0"
                :columns="[
                  {
                    title: '日程标题',
                    dataIndex: 'title',
                    key: 'title',
                    width: 180,
                    ellipsis: true,
                  },
                  { title: '类型', key: 'scheduleType', width: 80 },
                  {
                    title: '开始时间',
                    dataIndex: 'startTime',
                    key: 'startTime',
                    width: 140,
                  },
                  {
                    title: '结束时间',
                    dataIndex: 'endTime',
                    key: 'endTime',
                    width: 140,
                  },
                  {
                    title: '地点',
                    dataIndex: 'location',
                    key: 'location',
                    width: 150,
                    ellipsis: true,
                  },
                  { title: '状态', key: 'status', width: 80 },
                  { title: '操作', key: 'action', width: 120 },
                ]"
                :data-source="schedules"
                :pagination="false"
                row-key="id"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'scheduleType'">
                    <Tag :color="getScheduleTypeColor(record.scheduleType)">
                      {{ record.scheduleTypeName || record.scheduleType }}
                    </Tag>
                  </template>
                  <template v-else-if="column.key === 'startTime'">
                    {{
                      record.startTime
                        ? dayjs(record.startTime).format('MM-DD HH:mm')
                        : '-'
                    }}
                  </template>
                  <template v-else-if="column.key === 'endTime'">
                    {{
                      record.endTime
                        ? dayjs(record.endTime).format('MM-DD HH:mm')
                        : '-'
                    }}
                  </template>
                  <template v-else-if="column.key === 'status'">
                    <Tag :color="getScheduleStatusColor(record.status)">
                      {{ getScheduleStatusText(record.status) }}
                    </Tag>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <Space v-if="canEditOperations">
                      <Popconfirm
                        v-if="record.status === 'ACTIVE'"
                        title="确定要取消这个日程吗？"
                        @confirm="handleCancelSchedule(record.id)"
                      >
                        <Button type="link" size="small">取消</Button>
                      </Popconfirm>
                      <Popconfirm
                        title="确定要删除这个日程吗？"
                        @confirm="handleDeleteSchedule(record.id)"
                      >
                        <Button type="link" size="small" danger>删除</Button>
                      </Popconfirm>
                    </Space>
                    <span v-else>-</span>
                  </template>
                </template>
              </Table>
              <Empty
                v-else
                :description="
                  canEditOperations
                    ? '暂无日程安排，点击上方按钮添加'
                    : '暂无日程安排'
                "
              />
            </TabPane>

            <!-- 时间线 -->
            <TabPane key="timeline" tab="时间线">
              <div v-if="timeline.length > 0" class="timeline-container">
                <div
                  v-for="(item, index) in timeline"
                  :key="index"
                  class="timeline-item"
                >
                  <div class="timeline-dot"></div>
                  <div class="timeline-content">
                    <div class="timeline-title">
                      {{ item.title || item.eventTypeName || item.eventType }}
                    </div>
                    <div class="timeline-description">
                      {{ item.description }}
                    </div>
                    <div class="timeline-time">
                      {{ dayjs(item.eventTime).format('YYYY-MM-DD HH:mm:ss') }}
                    </div>
                  </div>
                </div>
              </div>
              <Empty v-else description="暂无时间线记录" />
            </TabPane>

            <!-- 团队成员 -->
            <TabPane key="participants" tab="团队成员">
              <Table
                v-if="matter.participants && matter.participants.length > 0"
                :columns="[
                  {
                    title: '姓名',
                    dataIndex: 'userName',
                    key: 'userName',
                    width: 100,
                  },
                  {
                    title: '角色',
                    dataIndex: 'role',
                    key: 'role',
                    width: 90,
                    customRender: ({ text }) => roleMap[text] || text,
                  },
                  {
                    title: '提成比例',
                    dataIndex: 'commissionRate',
                    key: 'commissionRate',
                    width: 80,
                    customRender: ({ text }) => (text ? `${text}%` : '-'),
                  },
                  {
                    title: '是否案源',
                    dataIndex: 'isOriginator',
                    key: 'isOriginator',
                    width: 80,
                    customRender: ({ text }) => (text ? '是' : '否'),
                  },
                ]"
                :data-source="matter.participants"
                :pagination="false"
                row-key="id"
                size="small"
              />
              <Empty v-else description="暂无团队成员" />
            </TabPane>
          </Tabs>
        </Card>

        <!-- 对方当事人信息 -->
        <Card v-if="matter.opposingParty" title="对方当事人信息">
          <Descriptions :column="2" bordered>
            <DescriptionsItem label="对方当事人">
              {{ matter.opposingParty }}
            </DescriptionsItem>
            <DescriptionsItem label="对方律师">
              {{ matter.opposingLawyerName || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="律师执业证号">
              {{ matter.opposingLawyerLicenseNo || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="律师事务所">
              {{ matter.opposingLawyerFirm || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="联系电话">
              {{ matter.opposingLawyerPhone || '-' }}
            </DescriptionsItem>
            <DescriptionsItem label="邮箱">
              {{ matter.opposingLawyerEmail || '-' }}
            </DescriptionsItem>
          </Descriptions>
        </Card>
      </div>
    </Spin>

    <!-- 结案申请弹窗 -->
    <Modal
      v-model:open="closeModalVisible"
      title="申请结案"
      width="500px"
      @ok="handleSubmitClose"
      ok-text="提交申请"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="结案日期" required>
          <DatePicker
            v-model:value="closeForm.closingDate"
            style="width: 100%"
            format="YYYY-MM-DD"
            placeholder="请选择结案日期"
          />
        </FormItem>
        <FormItem label="结案原因" required>
          <Select
            v-model:value="closeForm.closingReason"
            placeholder="请选择结案原因"
            :options="closingReasonOptions"
          />
        </FormItem>
        <FormItem label="审批人" required>
          <Select
            v-model:value="closeForm.approverId"
            placeholder="请选择审批人（团队负责人）"
            style="width: 100%"
            show-search
            :filter-option="
              (input: string, option: any) => {
                const label = option?.label || '';
                return label.toLowerCase().includes(input.toLowerCase());
              }
            "
          >
            <Select.Option
              v-for="approver in closeApprovers"
              :key="approver.id"
              :value="approver.id"
              :label="approver.realName"
            >
              <div
                style="
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                "
              >
                <span>
                  {{ approver.realName }}
                  <Tag
                    v-if="approver.recommended"
                    color="green"
                    style="margin-left: 4px; font-size: 10px"
                    >推荐</Tag
                  >
                </span>
                <span style="font-size: 12px; color: #999"
                  >{{ approver.position }} · {{ approver.departmentName }}</span
                >
              </div>
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="结果描述">
          <Textarea
            v-model:value="closeForm.outcome"
            :rows="2"
            placeholder="请输入判决/调解结果描述"
          />
        </FormItem>
        <FormItem label="结案总结">
          <Textarea
            v-model:value="closeForm.summary"
            :rows="3"
            placeholder="请输入结案总结（可选）"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 添加期限弹窗 -->
    <Modal
      v-model:open="deadlineModalVisible"
      title="添加期限提醒"
      width="500px"
      @ok="handleSubmitDeadline"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="期限类型" required>
          <Select
            v-model:value="deadlineForm.deadlineType"
            placeholder="请选择期限类型"
            :options="DEADLINE_TYPE_OPTIONS"
            @change="
              (val: any) => {
                const opt = DEADLINE_TYPE_OPTIONS.find((o) => o.value === val);
                if (opt && !deadlineForm.deadlineName) {
                  deadlineForm.deadlineName = opt.label;
                }
              }
            "
          />
        </FormItem>
        <FormItem label="期限名称" required>
          <Input
            v-model:value="deadlineForm.deadlineName"
            placeholder="如：一审举证期限"
          />
        </FormItem>
        <FormItem label="截止日期" required>
          <DatePicker
            v-model:value="deadlineForm.deadlineDate"
            style="width: 100%"
            format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="提前提醒">
          <Select v-model:value="deadlineForm.reminderDays" style="width: 100%">
            <Select.Option :value="1">提前1天</Select.Option>
            <Select.Option :value="3">提前3天</Select.Option>
            <Select.Option :value="5">提前5天</Select.Option>
            <Select.Option :value="7">提前7天</Select.Option>
            <Select.Option :value="14">提前14天</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="说明">
          <Textarea
            v-model:value="deadlineForm.description"
            :rows="2"
            placeholder="备注说明"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 申请出函弹窗 -->
    <Modal
      v-model:open="letterModalVisible"
      title="申请出函"
      width="600px"
      @ok="handleSubmitLetter"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="函件模板" required>
          <Select
            v-model:value="letterForm.templateId"
            placeholder="请选择函件模板"
            :options="templates.map((t) => ({ label: t.name, value: t.id }))"
          />
        </FormItem>
        <FormItem label="接收单位" required>
          <Input
            v-model:value="letterForm.targetUnit"
            placeholder="如：XX市中级人民法院"
          />
        </FormItem>
        <FormItem label="联系人">
          <Input
            v-model:value="letterForm.targetContact"
            placeholder="接收单位联系人"
          />
        </FormItem>
        <FormItem label="联系电话">
          <Input
            v-model:value="letterForm.targetPhone"
            placeholder="联系电话"
          />
        </FormItem>
        <FormItem label="出函事由" required>
          <Textarea
            v-model:value="letterForm.purpose"
            :rows="2"
            placeholder="请输入出函事由"
          />
        </FormItem>
        <FormItem label="出函律师">
          <UserTreeSelect
            :key="userTreeSelectKey"
            v-model:value="letterForm.lawyerIds"
            :multiple="true"
            placeholder="选择出函律师（按部门筛选）"
          />
        </FormItem>
        <FormItem label="审批人" required>
          <UserTreeSelect
            :key="userTreeSelectKey + 1"
            v-model:value="letterForm.approverId"
            :multiple="false"
            placeholder="选择审批人（主任/团队负责人）"
          />
        </FormItem>
        <FormItem label="份数">
          <InputNumber
            v-model:value="letterForm.copies"
            :min="1"
            :max="10"
            style="width: 100%"
          />
        </FormItem>
        <FormItem label="期望日期">
          <DatePicker
            v-model:value="letterForm.expectedDate"
            style="width: 100%"
            format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="备注">
          <Textarea
            v-model:value="letterForm.remark"
            :rows="2"
            placeholder="备注信息"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 添加日程弹窗 -->
    <Modal
      v-model:open="scheduleModalVisible"
      title="添加日程"
      width="550px"
      @ok="handleSubmitSchedule"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="日程标题" required>
          <Input
            v-model:value="scheduleForm.title"
            placeholder="如：XX案开庭"
          />
        </FormItem>
        <FormItem label="日程类型" required>
          <Select
            v-model:value="scheduleForm.scheduleType"
            :options="SCHEDULE_TYPE_OPTIONS"
          />
        </FormItem>
        <FormItem label="日期" required>
          <DatePicker
            v-model:value="scheduleForm.date"
            style="width: 100%"
            format="YYYY-MM-DD"
          />
        </FormItem>
        <FormItem label="全天">
          <Checkbox v-model:checked="scheduleForm.allDay">全天日程</Checkbox>
        </FormItem>
        <FormItem v-show="!scheduleForm.allDay" label="时间">
          <TimeRangePicker
            v-model:value="scheduleForm.time"
            format="HH:mm"
            style="width: 100%"
            :placeholder="['开始时间', '结束时间']"
          />
        </FormItem>
        <FormItem label="地点">
          <Input
            v-model:value="scheduleForm.location"
            placeholder="如：XX法院第3法庭"
          />
        </FormItem>
        <FormItem label="提前提醒">
          <Select
            v-model:value="scheduleForm.reminderMinutes"
            :options="REMINDER_OPTIONS"
          />
        </FormItem>
        <FormItem label="备注">
          <Textarea
            v-model:value="scheduleForm.description"
            :rows="2"
            placeholder="备注信息"
          />
        </FormItem>
      </Form>
    </Modal>

    <!-- 出函详情弹框 -->
    <Modal
      v-model:open="letterDetailVisible"
      title="出函申请详情"
      width="800px"
      :footer="null"
    >
      <div v-if="letterDetailRecord">
        <Descriptions :column="2" bordered>
          <DescriptionsItem label="申请编号">
            {{ letterDetailRecord.applicationNo }}
          </DescriptionsItem>
          <DescriptionsItem label="函件类型">
            <Tag>{{ letterDetailRecord.letterTypeName }}</Tag>
          </DescriptionsItem>
          <DescriptionsItem label="模板名称">
            {{ letterDetailRecord.templateName || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="状态">
            <Tag :color="getLetterStatusColor(letterDetailRecord.status)">
              {{ letterDetailRecord.statusName }}
            </Tag>
          </DescriptionsItem>
          <DescriptionsItem label="接收单位" :span="2">
            {{ letterDetailRecord.targetUnit }}
          </DescriptionsItem>
          <DescriptionsItem label="联系人">
            {{ letterDetailRecord.targetContact || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="联系电话">
            {{ letterDetailRecord.targetPhone || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="接收地址" :span="2">
            {{ letterDetailRecord.targetAddress || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="出函律师">
            {{ letterDetailRecord.lawyerNames || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="份数">
            {{ letterDetailRecord.copies }}
          </DescriptionsItem>
          <DescriptionsItem label="期望日期">
            {{
              letterDetailRecord.expectedDate
                ? dayjs(letterDetailRecord.expectedDate).format('YYYY-MM-DD')
                : '-'
            }}
          </DescriptionsItem>
          <DescriptionsItem label="申请人">
            {{ letterDetailRecord.applicantName }}
          </DescriptionsItem>
          <DescriptionsItem label="出函事由" :span="2">
            {{ letterDetailRecord.purpose }}
          </DescriptionsItem>
          <DescriptionsItem label="备注" :span="2">
            {{ letterDetailRecord.remark || '-' }}
          </DescriptionsItem>
          <DescriptionsItem label="申请时间">
            {{
              dayjs(letterDetailRecord.createdAt).format('YYYY-MM-DD HH:mm:ss')
            }}
          </DescriptionsItem>
          <DescriptionsItem label="更新时间">
            {{
              dayjs(letterDetailRecord.updatedAt).format('YYYY-MM-DD HH:mm:ss')
            }}
          </DescriptionsItem>
          <DescriptionsItem
            v-if="letterDetailRecord.approvedAt"
            label="审批时间"
          >
            {{
              dayjs(letterDetailRecord.approvedAt).format('YYYY-MM-DD HH:mm:ss')
            }}
          </DescriptionsItem>
          <DescriptionsItem
            v-if="letterDetailRecord.approverName"
            label="审批人"
          >
            {{ letterDetailRecord.approverName }}
          </DescriptionsItem>
          <DescriptionsItem
            v-if="letterDetailRecord.approvalComment"
            label="审批意见"
            :span="2"
          >
            {{ letterDetailRecord.approvalComment }}
          </DescriptionsItem>
          <DescriptionsItem
            v-if="letterDetailRecord.printedAt"
            label="打印时间"
          >
            {{
              dayjs(letterDetailRecord.printedAt).format('YYYY-MM-DD HH:mm:ss')
            }}
          </DescriptionsItem>
          <DescriptionsItem
            v-if="letterDetailRecord.printerName"
            label="打印人"
          >
            {{ letterDetailRecord.printerName }}
          </DescriptionsItem>
          <DescriptionsItem
            v-if="letterDetailRecord.receivedAt"
            label="领取时间"
          >
            {{
              dayjs(letterDetailRecord.receivedAt).format('YYYY-MM-DD HH:mm:ss')
            }}
          </DescriptionsItem>
          <DescriptionsItem
            v-if="letterDetailRecord.receiverName"
            label="领取人"
          >
            {{ letterDetailRecord.receiverName }}
          </DescriptionsItem>
        </Descriptions>
        <div v-if="letterDetailRecord.content" style="margin-top: 16px">
          <div style="margin-bottom: 8px; font-weight: 500">函件内容：</div>
          <div
            style="
              max-height: 300px;
              padding: 12px;
              overflow-y: auto;
              background: #f5f5f5;
              border-radius: 4px;
            "
            v-html="letterDetailRecord.content"
          ></div>
        </div>
      </div>
    </Modal>
  </Page>
</template>

<style scoped lang="less">
.matter-detail {
  .timeline-container {
    padding: 20px 0;
  }

  .timeline-item {
    display: flex;
    margin-bottom: 24px;
    position: relative;

    &:not(:last-child)::after {
      content: '';
      position: absolute;
      left: 7px;
      top: 24px;
      bottom: -24px;
      width: 2px;
      background: #e8e8e8;
    }

    .timeline-dot {
      width: 16px;
      height: 16px;
      border-radius: 50%;
      background: #1890ff;
      border: 2px solid #fff;
      box-shadow: 0 0 0 2px #1890ff;
      flex-shrink: 0;
      margin-right: 16px;
      margin-top: 4px;
    }

    .timeline-content {
      flex: 1;
      padding-bottom: 16px;

      .timeline-title {
        font-size: 16px;
        font-weight: 500;
        margin-bottom: 8px;
        color: #262626;
      }

      .timeline-description {
        color: #595959;
        margin-bottom: 8px;
        line-height: 1.6;
      }

      .timeline-time {
        color: #8c8c8c;
        font-size: 12px;
      }
    }
  }
}
</style>
