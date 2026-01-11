<script lang="ts" setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue';

// import { useAntdDesignTokens } from '@vben/hooks';
// import { initPreferences } from '@vben/preferences';
import { ConfigProvider, theme } from 'ant-design-vue';
import mediumZoom from 'medium-zoom';
import { useRoute } from 'vitepress';
import DefaultTheme from 'vitepress/theme';

import Login from './Login.vue';
import { handleTokenFromURL, isAuthenticated, logout } from './auth';

const { Layout } = DefaultTheme;
const route = useRoute();
// const { tokens } = useAntdDesignTokens();

const initZoom = () => {
  // mediumZoom('[data-zoomable]', { background: 'var(--vp-c-bg)' });
  mediumZoom('.VPContent img', { background: 'var(--vp-c-bg)' });
};

const isDark = ref(true);

watch(
  () => route.path,
  () => nextTick(() => initZoom()),
);

// initPreferences({
//   namespace: 'docs',
// });

onMounted(() => {
  initZoom();
  // 检查URL中的token参数
  const hasToken = handleTokenFromURL();
  if (hasToken) {
    // 如果有token，刷新页面以应用认证状态
    window.location.reload();
  }
});

// 使用该函数
const observer = watchDarkModeChange((dark) => {
  isDark.value = dark;
});

onBeforeUnmount(() => {
  observer?.disconnect();
});

function watchDarkModeChange(callback: (isDark: boolean) => void) {
  if (typeof window === 'undefined') {
    return;
  }
  const htmlElement = document.documentElement;

  const observer = new MutationObserver(() => {
    const isDark = htmlElement.classList.contains('dark');
    callback(isDark);
  });

  observer.observe(htmlElement, {
    attributeFilter: ['class'],
    attributes: true,
  });

  const initialIsDark = htmlElement.classList.contains('dark');
  callback(initialIsDark);

  return observer;
}

const tokenTheme = computed(() => {
  const algorithm = isDark.value
    ? [theme.darkAlgorithm]
    : [theme.defaultAlgorithm];

  return {
    algorithm,
    // token: tokens,
  };
});

// 检查当前路径是否需要认证（排除登录页面）
const requireAuth = computed(() => {
  const path = route.path;
  // 登录页面不需要认证
  if (path === '/login') {
    return false;
  }
  // 运维手册和API文档需要登录
  return path.startsWith('/guide/ops/') || path.startsWith('/guide/api/');
});

// 检查当前是否在登录页面
const isLoginPage = computed(() => route.path === '/login');

// 监视路由变化，检查权限（不需要重定向，因为条件渲染会显示登录组件）
watch(
  () => route.path,
  () => {
    // 这里可以留空，或者用于其他逻辑
    // 不需要重定向，因为requireAuth和isAuthenticated()的计算属性会控制显示
  }
);
</script>

<template>
  <ConfigProvider :theme="tokenTheme">
    <!-- 如果是登录页面，显示登录组件 -->
    <div v-if="isLoginPage" class="auth-required">
      <Login />
    </div>
    <!-- 如果需要认证但未登录，且不是登录页面，则显示登录组件 -->
    <div v-else-if="requireAuth && !isAuthenticated()" class="auth-required">
      <Login />
    </div>
    <!-- 否则显示正常布局 -->
    <Layout v-else />
  </ConfigProvider>
</template>

<style>
.medium-zoom-overlay,
.medium-zoom-image--opened {
  z-index: 2147483647;
}

.auth-required {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style>
