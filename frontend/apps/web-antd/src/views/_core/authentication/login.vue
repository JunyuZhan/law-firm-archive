<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';
import type { Recordable } from '@vben/types';

import { computed, onMounted, ref } from 'vue';

import { AuthenticationLogin, SliderCaptcha, z } from '@vben/common-ui';
import { $t } from '@vben/locales';

import { Alert, Button, Input, message, Modal } from 'ant-design-vue';

import {
  getCaptchaApi,
  getSliderTokenApi,
  verifySliderApi,
} from '#/api/core/auth';
import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

// 版本号
const appVersion = import.meta.env.VITE_APP_VERSION || '1.0.0';

const authStore = useAuthStore();

// 滑块验证状态
const sliderCaptchaRef = ref<InstanceType<typeof SliderCaptcha> | null>(null);
const sliderTokenId = ref('');
const sliderVerifyToken = ref('');
const sliderStartTime = ref(0);
const sliderVerified = ref(false);

// 图形验证码状态（失败多次后显示）
const showImageCaptcha = ref(false);
const captchaId = ref('');
const captchaUrl = ref('');
const captchaCode = ref('');

// 账户锁定状态
const accountLocked = ref(false);
const lockMessage = ref('');

// 异地登录状态
const showNewLocation = ref(false);
const newLocationMessage = ref('');
const permitRequestId = ref(''); // 许可码请求ID
const permitCode = ref(''); // 用户输入的许可码
const pendingLoginParams = ref<null | Recordable<any>>(null); // 暂存登录参数

const formSchema = computed((): VbenFormSchema[] => {
  const schemas: VbenFormSchema[] = [
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

  return schemas;
});

// 获取滑块验证令牌
async function fetchSliderToken() {
  try {
    const result = await getSliderTokenApi();
    sliderTokenId.value = result.tokenId;
    sliderVerified.value = false;
    sliderVerifyToken.value = '';
  } catch (error) {
    console.error('获取滑块令牌失败:', error);
    message.warning('验证组件加载失败，请刷新页面重试');
  }
}

// 获取图形验证码
async function fetchImageCaptcha() {
  try {
    const result = await getCaptchaApi();
    captchaId.value = result.captchaId;
    captchaUrl.value = result.captchaUrl;
    captchaCode.value = '';
  } catch (error) {
    console.error('获取验证码失败:', error);
    message.warning('验证码加载失败，请点击刷新');
  }
}

// 滑块开始拖动
function handleSliderStart() {
  sliderStartTime.value = Date.now();
}

// 滑块验证成功（前端判定拖到最右边）
async function handleSliderSuccess() {
  if (!sliderTokenId.value) {
    message.error('验证令牌已过期，请刷新页面');
    return;
  }

  const slideTime = Date.now() - sliderStartTime.value;

  try {
    // 调用后端验证
    const result = await verifySliderApi({
      tokenId: sliderTokenId.value,
      slideTime,
      slideTrack: [], // 简化处理，不传轨迹
    });

    if (result.success && result.verifyToken) {
      sliderVerifyToken.value = result.verifyToken;
      sliderVerified.value = true;
      message.success('验证成功');
    } else {
      message.error(result.message || '验证失败，请重试');
      resetSlider();
    }
  } catch (error: any) {
    message.error(error?.message || '验证失败，请重试');
    resetSlider();
  }
}

// 重置滑块
function resetSlider() {
  sliderVerified.value = false;
  sliderVerifyToken.value = '';
  sliderCaptchaRef.value?.resume?.();
  fetchSliderToken();
}

// 自定义登录处理
async function handleLogin(values: Recordable<any>) {
  // 检查账户是否锁定
  if (accountLocked.value) {
    message.error(lockMessage.value || '账户已锁定，请稍后重试');
    return;
  }

  // 验证滑块
  if (!sliderVerified.value || !sliderVerifyToken.value) {
    message.error('请先完成滑块验证');
    return;
  }

  // 如果需要图形验证码，检查是否填写
  if (showImageCaptcha.value && !captchaCode.value) {
    message.error('请输入图形验证码');
    return;
  }

  try {
    // 构建登录参数
    const loginParams: Recordable<any> = {
      ...values,
      sliderVerifyToken: sliderVerifyToken.value,
    };

    // 如果需要图形验证码
    if (showImageCaptcha.value && captchaId.value && captchaCode.value) {
      loginParams.captchaId = captchaId.value;
      loginParams.captchaCode = captchaCode.value;
    }

    // 如果有异地登录许可码
    if (permitRequestId.value && permitCode.value) {
      loginParams.permitRequestId = permitRequestId.value;
      loginParams.permitCode = permitCode.value;
    }

    await authStore.authLogin(loginParams);

    // 登录成功，重置所有状态
    resetAllState();
  } catch (error: any) {
    // 处理特殊错误码
    // 错误可能来自：error.data（响应拦截器抛出）或 error.response.data（axios 错误）
    const errorCode =
      error?.data?.code || error?.response?.data?.code || error?.code;
    const errorMsg =
      error?.data?.message ||
      error?.response?.data?.message ||
      error?.message ||
      '登录失败';
    const errorData = error?.data?.data || error?.response?.data?.data;

    switch (errorCode) {
      case 'ACCOUNT_LOCKED': {
        accountLocked.value = true;
        lockMessage.value = errorMsg;
        message.error(errorMsg);

        break;
      }
      case 'CAPTCHA_REQUIRED': {
        // 需要图形验证码
        showImageCaptcha.value = true;
        await fetchImageCaptcha();
        message.warning('请完成图形验证码');

        break;
      }
      case 'NEW_LOCATION': {
        // 异地登录，需要管理员许可码
        showNewLocation.value = true;
        newLocationMessage.value = `检测到您从 ${errorData?.currentLocation || '未知位置'} 登录，请联系管理员获取许可码`;
        permitRequestId.value = errorData?.requestId || '';
        permitCode.value = ''; // 清空之前输入的许可码
        pendingLoginParams.value = values; // 保存登录参数
        message.warning('异地登录需要管理员许可');
        // 不重置滑块，保留验证状态
        return;
      }
      case 'PERMIT_CODE_ERROR': {
        // 许可码错误
        message.error(errorMsg);
        // 不关闭对话框，让用户重新输入
        return;
      }
      default: {
        message.error(errorMsg);
      }
    }

    // 重置滑块验证
    resetSlider();
  }
}

// 提交许可码验证
async function submitPermitCode() {
  if (!pendingLoginParams.value || !permitRequestId.value) {
    message.error('验证信息已过期，请重新登录');
    resetAllState();
    return;
  }

  if (!permitCode.value || permitCode.value.length < 6) {
    message.error('请输入6位许可码');
    return;
  }

  // 重新执行登录（带上许可码）
  await handleLogin(pendingLoginParams.value);
}

// 取消异地登录
function cancelNewLocation() {
  showNewLocation.value = false;
  newLocationMessage.value = '';
  permitRequestId.value = '';
  permitCode.value = '';
  pendingLoginParams.value = null;
  resetSlider();
}

// 重置所有状态
function resetAllState() {
  sliderVerified.value = false;
  sliderVerifyToken.value = '';
  showImageCaptcha.value = false;
  captchaId.value = '';
  captchaUrl.value = '';
  captchaCode.value = '';
  accountLocked.value = false;
  lockMessage.value = '';
  showNewLocation.value = false;
  newLocationMessage.value = '';
  permitRequestId.value = '';
  permitCode.value = '';
  pendingLoginParams.value = null;
  sliderCaptchaRef.value?.resume?.();
}

// 组件挂载时获取滑块令牌
onMounted(() => {
  fetchSliderToken();
});
</script>

<template>
  <div class="login-container">
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
      <!-- 在表单和记住密码之间插入验证码区域 -->
      <template #after-form>
        <!-- 滑块验证 -->
        <div class="mb-4">
          <SliderCaptcha
            ref="sliderCaptchaRef"
            v-model="sliderVerified"
            @start="handleSliderStart"
            @success="handleSliderSuccess"
          />
        </div>

        <!-- 图形验证码（登录失败多次后显示） -->
        <div v-if="showImageCaptcha" class="mb-4">
          <div class="captcha-container">
            <Input
              v-model:value="captchaCode"
              placeholder="请输入验证码结果"
              :maxlength="10"
              allow-clear
              class="captcha-input"
            />
            <button
              v-if="captchaUrl"
              type="button"
              class="captcha-image-btn"
              title="点击刷新验证码"
              aria-label="刷新验证码"
              @click="fetchImageCaptcha"
            >
              <img :src="captchaUrl" alt="验证码图片" class="captcha-image" />
            </button>
          </div>
          <Alert
            message="登录失败次数过多，请完成图形验证"
            type="warning"
            show-icon
            class="mt-2"
          />
        </div>

        <!-- 账户锁定提示 -->
        <Alert
          v-if="accountLocked"
          :message="lockMessage"
          type="error"
          show-icon
          class="mb-4"
        />
      </template>
    </AuthenticationLogin>

    <!-- 版本号显示 -->
    <div class="version-info">v{{ appVersion }}</div>

    <!-- 异地登录许可码对话框 -->
    <Modal
      v-model:open="showNewLocation"
      title="异地登录验证"
      :closable="false"
      :mask-closable="false"
    >
      <div class="new-location-content">
        <Alert
          :message="newLocationMessage"
          type="warning"
          show-icon
          class="mb-4"
        />
        <p class="mb-4 text-gray-600">
          为保障数据安全，从新位置登录需要管理员授权。请联系管理员获取许可码。
        </p>
        <div class="permit-code-input">
          <Input
            v-model:value="permitCode"
            placeholder="请输入6位许可码"
            :maxlength="6"
            size="large"
            allow-clear
            @press-enter="submitPermitCode"
          />
        </div>
      </div>
      <template #footer>
        <Button @click="cancelNewLocation">取消登录</Button>
        <Button
          type="primary"
          :loading="authStore.loginLoading"
          :disabled="!permitCode || permitCode.length < 6"
          @click="submitPermitCode"
        >
          验证许可码
        </Button>
      </template>
    </Modal>
  </div>
</template>
<style scoped>
/* 移动端响应式 */
@media (max-width: 768px) {
  .version-info {
    right: 16px;
  }
}

.version-info {
  position: fixed;
  right: 24px;
  bottom: 12px;
  z-index: 10;
  font-size: 12px;
  color: rgb(0 0 0 / 35%);
}

/* 图形验证码容器 */
.captcha-container {
  display: flex;
  gap: 8px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image-btn {
  padding: 0;
  cursor: pointer;
  background: transparent;
  border: none;
}

.captcha-image {
  height: 40px;
  border-radius: 6px;
}

/* 许可码输入框 */
.permit-code-input {
  margin-top: 16px;
}

.permit-code-input :deep(.ant-input) {
  font-size: 18px;
  text-align: center;
  letter-spacing: 4px;
}

/* 版本号固定在视口底部，与版权信息同一行靠右 */
</style>

<style>
/* 暗色模式（全局样式） */
.dark .version-info {
  color: rgb(255 255 255 / 50%) !important;
}
</style>
