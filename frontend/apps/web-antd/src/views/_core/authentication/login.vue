<script lang="ts" setup>
import type { Recordable } from '@vben/types';
import type { VbenFormSchema } from '@vben/common-ui';

import { computed, ref } from 'vue';

import { message } from 'ant-design-vue';
import { AuthenticationLogin, SliderCaptcha, z } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();

// 滑块验证码状态
const captchaVerified = ref(false);
const sliderCaptchaRef = ref<InstanceType<typeof SliderCaptcha> | null>(null);

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z.string().min(1, { message: $t('authentication.usernameTip') }),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.password'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z.string().min(1, { message: $t('authentication.passwordTip') }),
    },
  ];
});

// 处理滑块验证码成功
function handleCaptchaSuccess() {
  captchaVerified.value = true;
}

// 自定义登录处理
async function handleLogin(values: Recordable<any>) {
  // 验证滑块验证码
  if (!captchaVerified.value) {
    message.error('请先完成滑块验证');
    return;
  }
  
  try {
    await authStore.authLogin(values);
    // 登录成功后重置验证码
    captchaVerified.value = false;
    sliderCaptchaRef.value?.resume?.();
  } catch (error) {
    // 登录失败后重置验证码
    captchaVerified.value = false;
    sliderCaptchaRef.value?.resume?.();
    throw error;
  }
}
</script>

<template>
  <AuthenticationLogin
    :form-schema="formSchema"
    :loading="authStore.loginLoading"
    :show-code-login="false"
    :show-qrcode-login="false"
    :show-register="false"
    :show-third-party-login="false"
    :show-forget-password="true"
    :show-remember-me="true"
    @submit="handleLogin"
  >
    <!-- 在表单和记住密码之间插入滑块验证码 -->
    <template #after-form>
      <div class="mb-4">
        <SliderCaptcha
          ref="sliderCaptchaRef"
          v-model="captchaVerified"
          @success="handleCaptchaSuccess"
        />
      </div>
    </template>
  </AuthenticationLogin>
</template>
