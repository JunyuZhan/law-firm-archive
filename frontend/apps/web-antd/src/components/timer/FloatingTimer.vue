<script setup lang="ts">
import type { StartTimerCommand, TimerSessionDTO } from '#/api/matter/timer';

import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue';

import {
  Button,
  Card,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Tooltip,
} from 'ant-design-vue';
import {
  ClockCircleOutlined,
  MinusOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  StopOutlined,
} from '@vben/icons';

import { getMatterList } from '#/api/matter';
import {
  getTimerStatus,
  pauseTimer,
  resumeTimer,
  startTimer,
  stopTimer,
} from '#/api/matter/timer';

defineOptions({ name: 'FloatingTimer' });

// 计时器状态
const timerSession = ref<TimerSessionDTO | null>(null);
const isRunning = computed(() => timerSession.value?.status === 'RUNNING');
const isPaused = computed(() => timerSession.value?.status === 'PAUSED');
const isIdle = computed(
  () =>
    !timerSession.value ||
    timerSession.value.status === 'IDLE' ||
    timerSession.value.status === 'STOPPED',
);

// 显示状态
const isMinimized = ref(true);
const isVisible = ref(true);

// 计时显示
const displayTime = ref('00:00:00');
const elapsedSeconds = ref(0);
let timerInterval: ReturnType<typeof setInterval> | null = null;

// 新建弹窗
const startModalVisible = ref(false);
const startForm = reactive<StartTimerCommand>({
  matterId: 0,
  description: '',
});
const startLoading = ref(false);

// 项目选项
const matterOptions = ref<{ label: string; value: number }[]>([]);
const matterLoading = ref(false);

// 格式化时间
function formatTime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

// 更新显示时间
function updateDisplayTime() {
  if (timerSession.value && isRunning.value) {
    elapsedSeconds.value = (timerSession.value.elapsedSeconds || 0) + 1;
    timerSession.value.elapsedSeconds = elapsedSeconds.value;
  }
  displayTime.value = formatTime(elapsedSeconds.value);
}

// 开始计时器更新
function startTimerUpdate() {
  if (timerInterval) {
    clearInterval(timerInterval);
  }
  timerInterval = setInterval(updateDisplayTime, 1000);
}

// 停止计时器更新
function stopTimerUpdate() {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
}

// 加载项目列表
async function loadMatters(keyword?: string) {
  matterLoading.value = true;
  try {
    const res = await getMatterList({ pageNum: 1, pageSize: 50, name: keyword });
    matterOptions.value = (res.list || []).map((m: { matterNo: string; name: string; id: number }) => ({
      label: `${m.matterNo} - ${m.name}`,
      value: m.id,
    }));
  } catch (error: any) {
    console.error('加载项目列表失败', error);
  } finally {
    matterLoading.value = false;
  }
}

// 加载计时器状态
async function loadTimerStatus() {
  try {
    const res = await getTimerStatus();
    timerSession.value = res;
    if (res && (res.status === 'RUNNING' || res.status === 'PAUSED')) {
      elapsedSeconds.value = res.elapsedSeconds || 0;
      displayTime.value = formatTime(elapsedSeconds.value);
      if (res.status === 'RUNNING') {
        startTimerUpdate();
      }
      isMinimized.value = false;
    }
  } catch (error: any) {
    console.error('加载计时器状态失败', error);
  }
}

// 打开开始弹窗
function handleOpenStart() {
  Object.assign(startForm, {
    matterId: 0,
    description: '',
  });
  loadMatters();
  startModalVisible.value = true;
}

// 开始计时
async function handleStart() {
  if (!startForm.matterId) {
    message.warning('请选择项目');
    return;
  }

  startLoading.value = true;
  try {
    const res = await startTimer(startForm);
    timerSession.value = res;
    elapsedSeconds.value = 0;
    displayTime.value = '00:00:00';
    startTimerUpdate();
    startModalVisible.value = false;
    isMinimized.value = false;
    message.success('计时已开始');
  } catch (error: any) {
    message.error(`开始计时失败：${error.message || '未知错误'}`);
  } finally {
    startLoading.value = false;
  }
}

// 暂停计时
async function handlePause() {
  try {
    const res = await pauseTimer();
    timerSession.value = res;
    stopTimerUpdate();
    message.success('计时已暂停');
  } catch (error: any) {
    message.error(`暂停失败：${error.message || '未知错误'}`);
  }
}

// 继续计时
async function handleResume() {
  try {
    const res = await resumeTimer();
    timerSession.value = res;
    elapsedSeconds.value = res.elapsedSeconds || 0;
    startTimerUpdate();
    message.success('计时已继续');
  } catch (error: any) {
    message.error(`继续失败：${error.message || '未知错误'}`);
  }
}

// 停止计时
async function handleStop() {
  Modal.confirm({
    title: '停止计时',
    content: '停止计时后将自动保存为工时记录，确定停止吗？',
    onOk: async () => {
      try {
        await stopTimer();
        timerSession.value = null;
        elapsedSeconds.value = 0;
        displayTime.value = '00:00:00';
        stopTimerUpdate();
        isMinimized.value = true;
        message.success('计时已停止，工时记录已保存');
      } catch (error: any) {
        message.error(`停止失败：${error.message || '未知错误'}`);
      }
    },
  });
}

// 切换最小化
function toggleMinimize() {
  isMinimized.value = !isMinimized.value;
}


// 监听状态变化
watch(isRunning, (running) => {
  if (running) {
    startTimerUpdate();
  } else {
    stopTimerUpdate();
  }
});

// 初始化
onMounted(() => {
  loadTimerStatus();
});

// 清理
onUnmounted(() => {
  stopTimerUpdate();
});
</script>

<template>
  <div
    v-if="isVisible"
    class="floating-timer"
    :class="{ minimized: isMinimized }"
  >
    <!-- 最小化状态 -->
    <div v-if="isMinimized" class="timer-mini" @click="toggleMinimize">
      <Tooltip title="展开计时器">
        <div
          class="timer-icon"
          :class="{ running: isRunning, paused: isPaused }"
        >
          <ClockCircleOutlined />
          <span v-if="!isIdle" class="mini-time">{{ displayTime }}</span>
        </div>
      </Tooltip>
    </div>

    <!-- 展开状态 -->
    <Card v-else class="timer-card" size="small">
      <template #title>
        <Space>
          <ClockCircleOutlined />
          <span>计时器</span>
        </Space>
      </template>
      <template #extra>
        <Button type="text" size="small" @click="toggleMinimize">
          <MinusOutlined />
        </Button>
      </template>

      <!-- 计时显示 -->
      <div class="timer-display">
        <div class="time" :class="{ running: isRunning, paused: isPaused }">
          {{ displayTime }}
        </div>
        <div v-if="timerSession?.matterName" class="matter-name">
          {{ timerSession.matterName }}
        </div>
        <div v-if="timerSession?.description" class="description">
          {{ timerSession.description }}
        </div>
      </div>

      <!-- 控制按钮 -->
      <div class="timer-controls">
        <template v-if="isIdle">
          <Button type="primary" block @click="handleOpenStart">
            <PlayCircleOutlined /> 开始计时
          </Button>
        </template>
        <template v-else>
          <Space>
            <Button v-if="isRunning" type="default" @click="handlePause">
              <PauseCircleOutlined /> 暂停
            </Button>
            <Button v-if="isPaused" type="primary" @click="handleResume">
              <PlayCircleOutlined /> 继续
            </Button>
            <Button danger @click="handleStop"> <StopOutlined /> 停止 </Button>
          </Space>
        </template>
      </div>
    </Card>

    <!-- 开始计时弹窗 -->
    <Modal
      v-model:open="startModalVisible"
      title="开始计时"
      :confirm-loading="startLoading"
      width="450px"
      @ok="handleStart"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <Form.Item label="项目" required>
          <Select
            v-model:value="startForm.matterId"
            placeholder="请选择项目"
            show-search
            :filter-option="false"
            :loading="matterLoading"
            :options="matterOptions"
            @search="loadMatters"
          />
        </Form.Item>
        <Form.Item label="工作描述">
          <Input.TextArea
            v-model:value="startForm.description"
            :rows="3"
            placeholder="请输入工作内容描述"
          />
        </Form.Item>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.floating-timer {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
}

.floating-timer.minimized {
  right: 24px;
  bottom: 24px;
}

.timer-mini {
  cursor: pointer;
}

.timer-icon {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 24px;
  box-shadow: 0 4px 12px rgb(0 0 0 / 15%);
  transition: all 0.3s;
}

.timer-icon:hover {
  box-shadow: 0 6px 16px rgb(0 0 0 / 20%);
}

.timer-icon.running {
  color: #fff;
  background: linear-gradient(135deg, #52c41a, #73d13d);
}

.timer-icon.paused {
  color: #fff;
  background: linear-gradient(135deg, #faad14, #ffc53d);
}

.mini-time {
  font-family: Monaco, Consolas, monospace;
  font-size: 14px;
  font-weight: 500;
}

.timer-card {
  width: 280px;
  box-shadow: 0 4px 12px rgb(0 0 0 / 15%);
}

.timer-display {
  margin-bottom: 16px;
  text-align: center;
}

.timer-display .time {
  font-family: Monaco, Consolas, monospace;
  font-size: 32px;
  font-weight: 600;
  line-height: 1.2;
  color: #333;
}

.timer-display .time.running {
  color: #52c41a;
}

.timer-display .time.paused {
  color: #faad14;
}

.timer-display .matter-name {
  margin-top: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.timer-display .description {
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.timer-controls {
  display: flex;
  justify-content: center;
}
</style>
