<script lang="ts" setup>
import { ref } from 'vue';
import { Button, Input, Card, message } from 'ant-design-vue';

import { loginWithCredentials } from './auth';

const username = ref('');
const password = ref('');
const loading = ref(false);

function isSafeRedirectPath(value: string | null): value is string {
  return !!value && value.startsWith('/') && !value.startsWith('//');
}

function getBasePrefix(): string {
  return window.location.pathname.startsWith('/docs/') ? '/docs' : '';
}

const handleLogin = () => {
  if (!username.value || !password.value) {
    message.error('请输入用户名和密码');
    return;
  }

  loading.value = true;

  // 模拟API调用延迟
  setTimeout(() => {
    if (loginWithCredentials(username.value, password.value)) {
      message.success('登录成功');

      // 跳转到原来的页面或首页
      const basePrefix = getBasePrefix();
      const redirectFromQuery = new URLSearchParams(window.location.search).get(
        'redirect',
      );
      const safeRedirectFromQuery = isSafeRedirectPath(redirectFromQuery)
        ? redirectFromQuery
        : null;
      const isRootLoginPage = /^\/(docs\/)?login(\.html)?$/.test(
        window.location.pathname,
      );
      const redirect =
        safeRedirectFromQuery ||
        (isRootLoginPage
          ? `${basePrefix}/guide/ops/introduction`
          : window.location.pathname);
      window.location.href = redirect;
    } else {
      message.error('用户名或密码错误');
      loading.value = false;
    }
  }, 500);
};

const handleKeyPress = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleLogin();
  }
};
</script>

<template>
  <div class="login-container">
    <Card class="login-card">
      <template #title>
        <div class="login-title">
          <h2>文档站点登录</h2>
          <p class="login-subtitle">运维手册和API文档需要登录后查看</p>
        </div>
      </template>

      <div class="login-form">
        <div class="form-group">
          <label>用户名</label>
          <Input
            v-model:value="username"
            placeholder="请输入用户名"
            size="large"
            @keypress="handleKeyPress"
          />
        </div>

        <div class="form-group">
          <label>密码</label>
          <Input
            v-model:value="password"
            type="password"
            placeholder="请输入密码"
            size="large"
            @keypress="handleKeyPress"
          />
        </div>

        <div class="login-actions">
          <Button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            block
          >
            登录
          </Button>
        </div>

        <div class="login-hint">
          <p>请联系管理员获取账号密码</p>
        </div>
      </div>
    </Card>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  padding: 2rem;
}

.login-card {
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.login-title {
  text-align: center;
  margin-bottom: 1.5rem;
}

.login-title h2 {
  margin: 0;
  color: var(--vp-c-text-1);
}

.login-subtitle {
  margin: 0.5rem 0 0;
  color: var(--vp-c-text-2);
  font-size: 0.9rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--vp-c-text-1);
}

.login-actions {
  margin-top: 2rem;
}

.login-hint {
  margin-top: 1.5rem;
  padding: 1rem;
  background-color: var(--vp-c-bg-soft);
  border-radius: 6px;
  font-size: 0.9rem;
}

.login-hint p {
  margin: 0.25rem 0;
  color: var(--vp-c-text-2);
}

.hint-note {
  margin-top: 0.5rem !important;
  font-style: italic;
  color: var(--vp-c-text-3) !important;
}
</style>
