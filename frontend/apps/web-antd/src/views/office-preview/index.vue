<script setup lang="ts">
/**
 * OnlyOffice 文档编辑/预览页面
 * 
 * 使用方式：
 * - 预览模式：/office-preview?documentId=123&mode=view
 * - 编辑模式：/office-preview?documentId=123&mode=edit
 * - 直接URL模式（兼容旧版）：/office-preview?url=xxx&filename=xxx
 */
import { onMounted, onUnmounted, ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Button, Space, Spin, Alert } from 'ant-design-vue';
import { getDocumentPreviewConfig, getDocumentEditConfig, type OnlyOfficeConfig } from '#/api/document';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref<string | null>(null);
const config = ref<OnlyOfficeConfig | null>(null);
const currentMode = ref<'view' | 'edit'>('view');
const documentTitle = ref('');
let editorInstance: any = null;

// 是否支持编辑
const canEdit = computed(() => {
  return config.value?.document?.permissions?.edit ?? false;
});

// 是否支持预览
const isSupported = computed(() => {
  return config.value?.supported ?? false;
});

onMounted(async () => {
  const { documentId, mode, url, filename, type, ext } = route.query as {
    documentId?: string;
    mode?: 'view' | 'edit';
    url?: string;
    filename?: string;
    type?: string;
    ext?: string;
  };

  currentMode.value = mode || 'view';

  // 如果有 documentId，从后端获取配置
  if (documentId) {
    await loadFromBackend(parseInt(documentId), currentMode.value);
  } 
  // 兼容旧版直接 URL 模式
  else if (url && filename) {
    await loadDirectUrl(url, filename, type || 'word', ext || 'docx');
  } 
  else {
    error.value = '缺少必要参数：documentId 或 url';
    loading.value = false;
  }
});

onUnmounted(() => {
  // 清理编辑器实例
  if (editorInstance) {
    try {
      editorInstance.destroyEditor();
    } catch (e) {
      console.warn('销毁编辑器失败:', e);
    }
  }
});

/**
 * 从后端 API 加载配置
 */
async function loadFromBackend(documentId: number, mode: 'view' | 'edit') {
  try {
    loading.value = true;
    error.value = null;

    // 根据模式获取不同的配置
    const response = mode === 'edit' 
      ? await getDocumentEditConfig(documentId)
      : await getDocumentPreviewConfig(documentId);

    config.value = response;

    if (!response.supported) {
      error.value = response.message || '该文件类型不支持在线预览';
      loading.value = false;
      return;
    }

    documentTitle.value = response.document?.title || '';

    // 加载 OnlyOffice API 并初始化编辑器
    await loadOnlyOfficeApi(response.apiJsUrl || `${response.documentServerUrl}/web-apps/apps/api/documents/api.js`);
    initEditorFromConfig(response);

  } catch (e: any) {
    console.error('加载文档配置失败:', e);
    error.value = e.message || '加载失败';
    loading.value = false;
  }
}

/**
 * 直接 URL 模式（兼容旧版）
 */
async function loadDirectUrl(url: string, filename: string, type: string, ext: string) {
  try {
    const ONLYOFFICE_URL = import.meta.env.VITE_ONLYOFFICE_URL || 'http://localhost:8088';
    documentTitle.value = filename;

    await loadOnlyOfficeApi(`${ONLYOFFICE_URL}/web-apps/apps/api/documents/api.js`);
    
    const directConfig = {
      document: {
        fileType: ext,
        key: Date.now().toString(),
        title: filename,
        url: url,
      },
      documentType: type,
      editorConfig: {
        mode: 'view',
        lang: 'zh-CN',
        customization: {
          chat: false,
          comments: false,
          help: false,
          plugins: false,
        },
      },
      height: '100%',
      width: '100%',
    };

    initEditor(directConfig);
  } catch (e: any) {
    error.value = e.message || '加载失败';
    loading.value = false;
  }
}

/**
 * 加载 OnlyOffice API
 */
function loadOnlyOfficeApi(apiUrl: string): Promise<void> {
  return new Promise((resolve, reject) => {
    // 检查是否已加载
    if ((window as any).DocsAPI) {
      resolve();
      return;
    }

    const script = document.createElement('script');
    script.src = apiUrl;
    script.onload = () => {
      console.log('OnlyOffice API loaded');
      resolve();
    };
    script.onerror = (e) => {
      console.error('OnlyOffice API load error:', e);
      reject(new Error('OnlyOffice 服务未启动，请确保 Docker 中的 OnlyOffice 容器正在运行'));
    };
    document.head.appendChild(script);
  });
}

/**
 * 从后端配置初始化编辑器
 */
function initEditorFromConfig(cfg: OnlyOfficeConfig) {
  const editorConfig = {
    document: cfg.document,
    documentType: cfg.documentType,
    editorConfig: {
      ...cfg.editorConfig,
      customization: {
        ...cfg.editorConfig?.customization,
        // 添加返回按钮
        goback: {
          blank: false,
          text: '返回',
          url: 'javascript:history.back()',
        },
      },
    },
    events: {
      onDocumentReady: () => {
        console.log('Document ready');
        loading.value = false;
      },
      onError: (e: any) => {
        console.error('OnlyOffice error:', e);
        error.value = `文档加载失败: ${e.data?.errorDescription || '未知错误'}`;
        loading.value = false;
      },
      onDocumentStateChange: (e: any) => {
        // 文档状态变化（是否有未保存的修改）
        console.log('Document state changed:', e.data);
      },
    },
    height: '100%',
    width: '100%',
    type: cfg.type || 'desktop',
  };

  initEditor(editorConfig);
}

/**
 * 初始化编辑器
 */
function initEditor(editorConfig: any) {
  const container = document.getElementById('onlyoffice-editor');
  if (!container) {
    error.value = '编辑器容器未找到';
    loading.value = false;
    return;
  }

  console.log('Initializing OnlyOffice editor with config:', editorConfig);

  try {
    // @ts-ignore
    editorInstance = new DocsAPI.DocEditor('onlyoffice-editor', editorConfig);
  } catch (e: any) {
    console.error('DocEditor creation error:', e);
    error.value = `编辑器创建失败: ${e.message}`;
    loading.value = false;
  }
}

/**
 * 切换编辑模式
 */
async function toggleMode() {
  const documentId = route.query.documentId as string;
  if (!documentId) return;

  const newMode = currentMode.value === 'view' ? 'edit' : 'view';
  
  // 刷新页面切换模式
  router.replace({
    path: route.path,
    query: { ...route.query, mode: newMode },
  });
  
  // 重新加载
  window.location.reload();
}

/**
 * 返回/关闭窗口
 * 如果是新标签页打开的，则关闭窗口
 * 如果有历史记录，则返回上一页
 */
function goBack() {
  // 如果是通过 window.open 打开的，可以直接关闭
  if (window.opener) {
    window.close();
    return;
  }
  
  // 检查是否有历史记录可以返回
  if (window.history.length > 1) {
    router.back();
  } else {
    // 没有历史记录，尝试关闭窗口
    window.close();
    // 如果关闭失败（非 window.open 打开的页面），跳转到首页
    setTimeout(() => {
      router.push('/');
    }, 100);
  }
}
</script>

<template>
  <div class="office-preview">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <Space>
        <Button @click="goBack">
          ← 关闭
        </Button>
        <span class="document-title">{{ documentTitle }}</span>
      </Space>
      <Space>
        <Button 
          v-if="isSupported && route.query.documentId"
          :type="currentMode === 'edit' ? 'primary' : 'default'"
          @click="toggleMode"
        >
          {{ currentMode === 'view' ? '✏️ 进入编辑' : '👁️ 切换预览' }}
        </Button>
      </Space>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">
      <Spin size="large" />
      <div class="loading-text">正在加载文档...</div>
    </div>

    <!-- 错误提示 -->
    <div v-if="error" class="error-container">
      <Alert
        type="error"
        :message="error"
        show-icon
      >
        <template #description>
          <div>
            <p>可能的原因：</p>
            <ul>
              <li>OnlyOffice 服务未启动</li>
              <li>文件类型不支持在线预览</li>
              <li>文件访问权限不足</li>
            </ul>
            <Button type="primary" @click="goBack" style="margin-top: 16px">
              返回
            </Button>
          </div>
        </template>
      </Alert>
    </div>

    <!-- 编辑器容器 -->
    <div id="onlyoffice-editor" class="editor-container"></div>
  </div>
</template>

<style scoped>
.office-preview {
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #f0f2f5;
}

.toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.document-title {
  margin-left: 16px;
  font-weight: 500;
  color: #333;
}

.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 10;
  text-align: center;
  transform: translate(-50%, -50%);
}

.loading-text {
  margin-top: 16px;
  color: #666;
}

.error-container {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 10;
  max-width: 500px;
  transform: translate(-50%, -50%);
}

.editor-container {
  flex: 1;
  width: 100%;
  min-height: 0;
}
</style>
