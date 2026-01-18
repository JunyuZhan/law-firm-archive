<script setup lang="ts">
import type { OnlyOfficeConfig } from '#/api/document';

/**
 * OnlyOffice 文档编辑/预览页面
 *
 * 使用方式：
 * - 预览模式：/office-preview?documentId=123&mode=view
 * - 编辑模式：/office-preview?documentId=123&mode=edit
 * - 直接URL模式（兼容旧版）：/office-preview?url=xxx&filename=xxx
 */
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Alert, Button, Space, Spin } from 'ant-design-vue';

import {
  getDocumentEditConfig,
  getDocumentPreviewConfig,
} from '#/api/document';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref<null | string>(null);
const config = ref<null | OnlyOfficeConfig>(null);
const currentMode = ref<'edit' | 'view'>('view');
const documentTitle = ref('');
let editorInstance: any = null;

// 是否支持预览
const isSupported = computed(() => {
  return config.value?.supported ?? false;
});

onMounted(async () => {
  const { documentId, mode, url, filename, type, ext } = route.query as {
    documentId?: string;
    ext?: string;
    filename?: string;
    mode?: 'edit' | 'view';
    type?: string;
    url?: string;
  };

  currentMode.value = mode || 'view';

  // 如果有 documentId，从后端获取配置
  if (documentId) {
    await loadFromBackend(Number.parseInt(documentId), currentMode.value);
  }
  // 兼容旧版直接 URL 模式
  else if (url && filename) {
    await loadDirectUrl(url, filename, type || 'word', ext || 'docx');
  } else {
    error.value = '缺少必要参数：documentId 或 url';
    loading.value = false;
  }
});

onUnmounted(() => {
  // 清理编辑器实例
  if (editorInstance) {
    try {
      editorInstance.destroyEditor();
    } catch (error_) {
      console.warn('销毁编辑器失败:', error_);
    }
  }
});

/**
 * 从后端 API 加载配置
 */
async function loadFromBackend(documentId: number, mode: 'edit' | 'view') {
  try {
    loading.value = true;
    error.value = null;

    // 根据模式获取不同的配置
    const response =
      mode === 'edit'
        ? await getDocumentEditConfig(documentId)
        : await getDocumentPreviewConfig(documentId);

    config.value = response;

    if (!response.supported) {
      error.value = response.message || '该文件类型不支持在线预览';
      loading.value = false;
      return;
    }

    documentTitle.value = response.document?.title || '';

    // 智能检测 OnlyOffice URL
    // 优先使用后端返回的地址，如果没有则使用当前域名 + /onlyoffice
    const onlyOfficeUrl = getOnlyOfficeUrl(response.documentServerUrl);
    
    // 构建 API JS URL（始终使用智能检测后的 URL）
    const apiJsUrl = `${onlyOfficeUrl}/web-apps/apps/api/documents/api.js`;
    
    // 加载 OnlyOffice API 并初始化编辑器
    await loadOnlyOfficeApi(apiJsUrl);
    
    // 更新 response 中的 documentServerUrl 以便后续使用
    response.documentServerUrl = onlyOfficeUrl;
    initEditorFromConfig(response);
  } catch (error_: any) {
    console.error('加载文档配置失败:', error_);
    error.value = error_.message || '加载失败';
    loading.value = false;
  }
}

/**
 * 直接 URL 模式（兼容旧版）
 */
async function loadDirectUrl(
  url: string,
  filename: string,
  type: string,
  ext: string,
) {
  try {
    // 使用智能检测获取 OnlyOffice URL
    const onlyOfficeUrl = getOnlyOfficeUrl(
      import.meta.env.VITE_ONLYOFFICE_URL,
    );
    documentTitle.value = filename;

    await loadOnlyOfficeApi(
      `${onlyOfficeUrl}/web-apps/apps/api/documents/api.js`,
    );

    const directConfig = {
      document: {
        fileType: ext,
        key: Date.now().toString(),
        title: filename,
        url,
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
  } catch (error_: any) {
    error.value = error_.message || '加载失败';
    loading.value = false;
  }
}

/**
 * 智能获取 OnlyOffice URL
 * 优先使用后端配置的地址，如果是无效地址，则使用当前域名
 */
function getOnlyOfficeUrl(backendUrl?: string): string {
  // 检查是否为无效地址（localhost、127.0.0.1、Docker 内部地址等）
  const isInvalidUrl = (url: string): boolean => {
    if (!url) return true;
    // localhost 或 127.0.0.1
    if (url.includes('localhost') || url.includes('127.0.0.1')) return true;
    // Docker 内部地址（没有点号的主机名，如 http://onlyoffice）
    try {
      const urlObj = new URL(url);
      // 如果主机名不包含点号且不是 localhost，可能是 Docker 容器名
      if (!urlObj.hostname.includes('.') && urlObj.hostname !== 'localhost') {
        return true;
      }
    } catch {
      return true;
    }
    return false;
  };

  // 如果后端返回了有效的 URL，直接使用
  if (backendUrl && !isInvalidUrl(backendUrl)) {
    return backendUrl;
  }
  
  // 否则使用当前域名 + /onlyoffice 路径
  const { protocol, host } = window.location;
  return `${protocol}//${host}/onlyoffice`;
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
    script.addEventListener('load', () => {
      resolve();
    });
    script.onerror = (e) => {
      console.error('OnlyOffice API load error:', e);
      reject(
        new Error(
          'OnlyOffice 服务未启动，请确保 Docker 中的 OnlyOffice 容器正在运行',
        ),
      );
    };
    document.head.append(script);
  });
}

/**
 * 从后端配置初始化编辑器
 */
function initEditorFromConfig(cfg: OnlyOfficeConfig) {
  // 处理 document.url：将 Docker 内部 URL 替换为浏览器可访问的相对路径
  // Docker 内部 URL 格式：http://frontend:8080/api/document/1/content?token=...
  // 浏览器需要：/api/document/1/content?token=...
  if (cfg.document?.url) {
    const originalUrl = cfg.document.url as string;
    // 如果是 Docker 内部地址，提取相对路径
    try {
      const urlObj = new URL(originalUrl);
      // 检查是否是 Docker 内部地址
      if (urlObj.hostname === 'frontend' || urlObj.hostname === 'backend' ||
          urlObj.hostname === 'localhost' || urlObj.hostname.includes('127.0.0.1')) {
        // 提取路径和查询参数，组合成相对路径
        const relativePath = urlObj.pathname + urlObj.search;
        cfg.document.url = relativePath;
      }
    } catch {
      // URL 解析失败，保持原样
    }
  }

  const editorConfig: any = {
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
        loading.value = false;
      },
      onError: (e: any) => {
        error.value = `文档加载失败: ${e.data?.errorDescription || '未知错误'}`;
        loading.value = false;
      },
      onDocumentStateChange: () => {
        // 文档状态变化（是否有未保存的修改）
      },
    },
    height: '100%',
    width: '100%',
    type: cfg.type || 'desktop',
  };

  // 如果后端返回了 JWT token，需要传递给 OnlyOffice 编辑器
  // OnlyOffice Document Server 会验证这个 token
  if (cfg.token) {
    editorConfig.token = cfg.token;
  }

  initEditor(editorConfig);
}

/**
 * 初始化编辑器
 */
function initEditor(editorConfig: any) {
  const container = document.querySelector('#onlyoffice-editor');
  if (!container) {
    error.value = '编辑器容器未找到';
    loading.value = false;
    return;
  }

  try {
    // @ts-ignore
    editorInstance = new DocsAPI.DocEditor('onlyoffice-editor', editorConfig);
  } catch (error_: any) {
    error.value = `编辑器创建失败: ${error_.message}`;
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
        <Button @click="goBack"> ← 关闭 </Button>
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
      <Alert type="error" :message="error" show-icon>
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
