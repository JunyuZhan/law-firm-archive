<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Alert, Button, Space, Modal, message } from 'ant-design-vue';
import { UpCircleOutlined, CloseOutlined } from '@ant-design/icons-vue';
import { requestClient } from '#/api/request';

interface VersionInfo {
  currentVersion: string;
  latestVersion: string;
  hasUpdate: boolean;
  releaseNotes?: string;
  releaseUrl?: string;
  publishedAt?: string;
  error?: string;
}

const router = useRouter();

const updateInfo = ref<VersionInfo>({
  currentVersion: '',
  latestVersion: '',
  hasUpdate: false,
});

const dismissed = ref(false);
const loading = ref(false);

// 检查版本更新
async function checkForUpdates() {
  // 检查是否已在本次会话中关闭了提示
  const sessionDismissed = sessionStorage.getItem('version_update_dismissed');
  if (sessionDismissed) {
    dismissed.value = true;
    return;
  }

  try {
    const res = await requestClient.get<VersionInfo>('/system/version/check');
    if (res) {
      updateInfo.value = res;
    }
  } catch (e) {
    console.log('版本检查失败', e);
  }
}

// 查看更新内容
function viewReleaseNotes() {
  if (updateInfo.value.releaseUrl) {
    window.open(updateInfo.value.releaseUrl, '_blank');
  } else if (updateInfo.value.releaseNotes) {
    Modal.info({
      title: `v${updateInfo.value.latestVersion} 更新内容`,
      content: updateInfo.value.releaseNotes,
      width: 600,
    });
  } else {
    message.info('暂无更新说明');
  }
}

// 前往系统配置-版本信息页面
function goToMaintenance() {
  router.push('/system/config?tab=version');
}

// 稍后提醒（本次会话不再显示）
function dismissUpdate() {
  dismissed.value = true;
  sessionStorage.setItem('version_update_dismissed', 'true');
}

// 忽略此版本
async function ignoreVersion() {
  loading.value = true;
  try {
    await requestClient.post(`/system/version/ignore?version=${updateInfo.value.latestVersion}`);
    updateInfo.value.hasUpdate = false;
    message.success('已忽略此版本，下次有新版本时会再次提醒');
  } catch (e) {
    message.error('操作失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  // 延迟检查，避免影响首屏加载
  setTimeout(checkForUpdates, 3000);
});
</script>

<template>
  <div v-if="updateInfo.hasUpdate && !dismissed" class="version-update-banner">
    <Alert
      type="info"
      show-icon
      closable
      @close="dismissUpdate"
    >
      <template #icon>
        <UpCircleOutlined class="update-icon" />
      </template>
      <template #message>
        <div class="update-content">
          <span class="update-text">
            发现新版本 <strong>v{{ updateInfo.latestVersion }}</strong>
            <span class="current-version">（当前 v{{ updateInfo.currentVersion }}）</span>
          </span>
          <Space class="update-actions">
            <Button size="small" @click="viewReleaseNotes">
              查看更新
            </Button>
            <Button size="small" type="primary" @click="goToMaintenance">
              前往升级
            </Button>
            <Button size="small" type="text" @click="ignoreVersion" :loading="loading">
              忽略此版本
            </Button>
          </Space>
        </div>
      </template>
    </Alert>
  </div>
</template>

<style scoped>
.version-update-banner {
  position: fixed;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  max-width: 800px;
  width: calc(100% - 48px);
}

.version-update-banner :deep(.ant-alert) {
  background: linear-gradient(90deg, #e6f4ff 0%, #f6ffed 100%);
  border: 1px solid #91caff;
}

.update-icon {
  font-size: 18px;
  color: #1890ff;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.update-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.update-text {
  font-size: 14px;
}

.update-text strong {
  color: #1890ff;
  font-size: 15px;
}

.current-version {
  color: #888;
  font-size: 12px;
}

.update-actions {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .version-update-banner {
    top: 56px;
  }
  
  .update-content {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
