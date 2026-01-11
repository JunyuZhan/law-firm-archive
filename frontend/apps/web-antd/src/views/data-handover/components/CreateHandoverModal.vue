<script setup lang="ts">
import type {
  CreateHandoverCommand,
  DataHandoverPreviewDTO,
} from '#/api/system/types';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import {
  Alert,
  Button,
  Col,
  Divider,
  Empty,
  Form,
  FormItem,
  List,
  ListItem,
  ListItemMeta,
  message,
  Row,
  Select,
  Space,
  Statistic,
  Step,
  Steps,
  Switch,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  createClientHandover,
  createProjectHandover,
  createResignationHandover,
  previewHandover,
} from '#/api/system';
import { UserTreeSelect } from '#/components/UserTreeSelect';

const emit = defineEmits<{
  success: [];
}>();

// 获取当前用户
const userStore = useUserStore();
const currentUser = computed(() => userStore.userInfo);
const currentUserId = computed(() => Number(currentUser.value?.userId) || 0);
const currentUserName = computed(() => currentUser.value?.realName || '');

const loading = ref(false);
const createStep = ref(0);
const previewData = ref<DataHandoverPreviewDTO | null>(null);

// 表单数据
const formData = reactive<CreateHandoverCommand>({
  fromUserId: 0,
  toUserId: 0,
  handoverType: 'RESIGNATION',
  reason: '',
  matterIds: [],
  clientIds: [],
  leadIds: [],
  includeOriginator: false,
  remark: '',
});

// 项目状态映射
const matterStatusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { text: '草稿', color: 'default' },
  PENDING: { text: '待审批', color: 'orange' },
  ACTIVE: { text: '进行中', color: 'processing' },
  IN_PROGRESS: { text: '进行中', color: 'processing' },
  SUSPENDED: { text: '已暂停', color: 'error' },
  CLOSED: { text: '已结案', color: 'success' },
  ARCHIVED: { text: '已归档', color: 'purple' },
};

function getMatterStatusText(status: string): string {
  return matterStatusMap[status]?.text || status;
}

function getMatterStatusColor(status: string): string {
  return matterStatusMap[status]?.color || 'default';
}

// 计算总数
const totalDataCount = computed(() => {
  if (!previewData.value) return 0;
  return previewData.value.totalCount;
});

const [VbenModal, modalApi] = useVbenModal({
  footer: false,
});

// 重置表单
function resetForm() {
  Object.assign(formData, {
    fromUserId: currentUserId.value,
    toUserId: 0,
    handoverType: 'RESIGNATION',
    reason: '',
    matterIds: [],
    clientIds: [],
    leadIds: [],
    includeOriginator: false,
    remark: '',
  });
  createStep.value = 0;
  previewData.value = null;
}

// 步骤1 - 预览当前用户的数据
async function handleSelectFromUser() {
  if (!currentUserId.value) {
    message.warning('获取当前用户信息失败，请重新登录');
    return;
  }
  formData.fromUserId = currentUserId.value;
  loading.value = true;
  try {
    previewData.value = await previewHandover(currentUserId.value);
    createStep.value = 1;
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '预览失败');
  } finally {
    loading.value = false;
  }
}

// 提交创建
async function handleSubmitCreate() {
  if (!formData.toUserId) {
    message.warning('请选择接收人');
    return;
  }
  if (formData.fromUserId === formData.toUserId) {
    message.warning('移交人和接收人不能相同');
    return;
  }

  loading.value = true;
  try {
    let res;
    switch (formData.handoverType) {
      case 'CLIENT': {
        res = await createClientHandover(formData);

        break;
      }
      case 'PROJECT': {
        res = await createProjectHandover(formData);

        break;
      }
      case 'RESIGNATION': {
        res = await createResignationHandover(formData);

        break;
      }
      default: {
        res = await createResignationHandover(formData);
      }
    }
    message.success(`交接单创建成功：${res.handoverNo}`);
    modalApi.close();
    emit('success');
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '创建失败');
  } finally {
    loading.value = false;
  }
}

// 打开弹窗
function open() {
  resetForm();
  modalApi.setState({ title: '新建数据交接' });
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <VbenModal class="w-[800px]">
    <Steps :current="createStep" style="margin-bottom: 24px">
      <Step title="确认交接信息" />
      <Step title="预览数据" />
      <Step title="选择接收人" />
    </Steps>

    <!-- 步骤1：确认交接信息 -->
    <div v-if="createStep === 0">
      <Form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <FormItem label="移交人">
          <div
            style="
              padding: 4px 11px;
              color: #333;
              background: #f5f5f5;
              border-radius: 6px;
            "
          >
            {{ currentUserName }}
            <Tag color="blue" style="margin-left: 8px">当前登录用户</Tag>
          </div>
        </FormItem>
        <FormItem label="交接类型">
          <Select v-model:value="formData.handoverType" style="width: 100%">
            <Select.Option value="RESIGNATION">
              离职交接（全部移交）
            </Select.Option>
            <Select.Option value="PROJECT">项目移交（指定项目）</Select.Option>
            <Select.Option value="CLIENT">客户移交（指定客户）</Select.Option>
          </Select>
        </FormItem>
        <FormItem label="交接原因">
          <Textarea
            v-model:value="formData.reason"
            placeholder="请输入交接原因"
            :rows="3"
          />
        </FormItem>
      </Form>
      <div style="margin-top: 24px; text-align: right">
        <Space>
          <Button @click="modalApi.close()">取消</Button>
          <Button
            type="primary"
            :loading="loading"
            @click="handleSelectFromUser"
          >
            下一步：预览数据
          </Button>
        </Space>
      </div>
    </div>

    <!-- 步骤2：预览数据 -->
    <div v-if="createStep === 1 && previewData">
      <Alert
        type="info"
        show-icon
        :message="`${previewData.userName} 共有 ${totalDataCount} 条数据需要交接`"
        style="margin-bottom: 16px"
      />

      <Row :gutter="16">
        <Col :span="6">
          <Statistic
            title="主办项目"
            :value="previewData.leadMatterCount"
            suffix="个"
          />
        </Col>
        <Col :span="6">
          <Statistic
            title="负责客户"
            :value="previewData.clientCount"
            suffix="个"
          />
        </Col>
        <Col :span="6">
          <Statistic
            title="跟进案源"
            :value="previewData.leadCount"
            suffix="个"
          />
        </Col>
        <Col :span="6">
          <Statistic
            title="待办任务"
            :value="previewData.taskCount"
            suffix="个"
          />
        </Col>
      </Row>

      <Divider />

      <!-- 项目列表 -->
      <div v-if="previewData.leadMatters && previewData.leadMatters.length > 0">
        <h4>主办项目</h4>
        <List
          size="small"
          :data-source="previewData.leadMatters"
          style="max-height: 200px; overflow-y: auto"
        >
          <template #renderItem="{ item }">
            <ListItem>
              <ListItemMeta :title="item.name" :description="item.matterNo" />
              <Tag :color="getMatterStatusColor(item.status)">
                {{ item.statusName || getMatterStatusText(item.status) }}
              </Tag>
            </ListItem>
          </template>
        </List>
      </div>

      <!-- 客户列表 -->
      <div
        v-if="previewData.clients && previewData.clients.length > 0"
        style="margin-top: 16px"
      >
        <h4>负责客户</h4>
        <List
          size="small"
          :data-source="previewData.clients"
          style="max-height: 150px; overflow-y: auto"
        >
          <template #renderItem="{ item }">
            <ListItem>
              <ListItemMeta :title="item.name" :description="item.clientNo" />
            </ListItem>
          </template>
        </List>
      </div>

      <div v-if="totalDataCount === 0">
        <Empty description="该用户暂无需要交接的数据" />
      </div>

      <div style="margin-top: 24px; text-align: right">
        <Space>
          <Button @click="createStep = 0">上一步</Button>
          <Button
            type="primary"
            @click="createStep = 2"
            :disabled="totalDataCount === 0"
          >
            下一步：选择接收人
          </Button>
        </Space>
      </div>
    </div>

    <!-- 步骤3：选择接收人 -->
    <div v-if="createStep === 2">
      <Form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <FormItem label="接收人" required>
          <UserTreeSelect
            v-model:value="formData.toUserId"
            placeholder="选择接收数据的用户（按部门筛选）"
            style="width: 100%"
            :exclude-user-ids="formData.fromUserId ? [formData.fromUserId] : []"
          />
        </FormItem>
        <FormItem
          v-if="formData.handoverType === 'RESIGNATION'"
          label="包含案源人身份"
        >
          <Switch v-model:checked="formData.includeOriginator" />
          <span style="margin-left: 8px; color: #999"
            >勾选后案源人身份也会一并移交（涉及提成分配）</span
          >
        </FormItem>
        <FormItem label="备注">
          <Textarea
            v-model:value="formData.remark"
            placeholder="备注信息"
            :rows="3"
          />
        </FormItem>
      </Form>

      <Alert
        type="warning"
        show-icon
        message="提交后将创建交接单，需要管理员确认后才会执行数据迁移"
        style="margin-top: 16px"
      />

      <div style="margin-top: 24px; text-align: right">
        <Space>
          <Button @click="createStep = 1">上一步</Button>
          <Button type="primary" :loading="loading" @click="handleSubmitCreate">
            提交交接单
          </Button>
        </Space>
      </div>
    </div>
  </VbenModal>
</template>
