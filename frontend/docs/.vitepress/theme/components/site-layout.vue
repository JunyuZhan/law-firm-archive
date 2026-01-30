<script lang="ts" setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from 'vue';

import { ConfigProvider, theme } from 'ant-design-vue';
import mediumZoom from 'medium-zoom';
import { useRoute } from 'vitepress';
import DefaultTheme from 'vitepress/theme';

import { handleTokenFromURL, isAdminAuthenticated } from './auth';
import Login from './Login.vue';

const { Layout } = DefaultTheme;
const route = useRoute();

const initZoom = () => {
  mediumZoom('.VPContent img', { background: 'var(--vp-c-bg)' });
};

const isDark = ref(
  typeof window === 'undefined'
    ? false
    : document.documentElement.classList.contains('dark'),
);
let darkModeObserver: MutationObserver | undefined;

watch(
  () => route.path,
  () => nextTick(() => initZoom()),
);

onMounted(() => {
  initZoom();
  // 处理 URL 中的 token
  handleTokenFromURL();

  // 监听暗色模式变化
  if (typeof window !== 'undefined') {
    const htmlElement = document.documentElement;
    darkModeObserver = new MutationObserver(() => {
      isDark.value = htmlElement.classList.contains('dark');
    });
    darkModeObserver.observe(htmlElement, {
      attributeFilter: ['class'],
      attributes: true,
    });
    isDark.value = htmlElement.classList.contains('dark');
  }
});

onBeforeUnmount(() => {
  darkModeObserver?.disconnect();
});

const tokenTheme = computed(() => ({
  algorithm: isDark.value ? [theme.darkAlgorithm] : [theme.defaultAlgorithm],
}));

// 检查是否需要登录（只有 /guide/ops/ 和 /guide/api/ 需要）
const needsAuth = computed(() => {
  const path = route.path.replace(/^\/docs\//, '/');
  return path.startsWith('/guide/ops/') || path.startsWith('/guide/api/');
});

// 检查是否是登录页面
const isLoginPage = computed(() => {
  return /^\/(?:docs\/)?login(?:\.html)?$/.test(route.path);
});

// 是否显示登录组件
const showLogin = computed(() => {
  if (isLoginPage.value) return true;
  if (needsAuth.value && !isAdminAuthenticated()) return true;
  return false;
});
</script>

<template>
  <ConfigProvider :theme="tokenTheme">
    <div v-if="showLogin" class="auth-required">
      <Login />
    </div>
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
