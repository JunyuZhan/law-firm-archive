<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';

import { computed, ref } from 'vue';

import { ProfilePasswordSetting, z } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { changePassword } from '#/api';

const profilePasswordSettingRef = ref();
const loading = ref(false);

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      fieldName: 'oldPassword',
      label: '旧密码',
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: '请输入旧密码',
      },
      rules: z
        .string({ required_error: '请输入旧密码' })
        .min(1, { message: '请输入旧密码' }),
    },
    {
      fieldName: 'newPassword',
      label: '新密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请输入新密码',
      },
      rules: z
        .string({ required_error: '请输入新密码' })
        .min(6, { message: '密码长度不能少于6位' })
        .max(20, { message: '密码长度不能超过20位' }),
    },
    {
      fieldName: 'confirmPassword',
      label: '确认密码',
      component: 'VbenInputPassword',
      componentProps: {
        passwordStrength: true,
        placeholder: '请再次输入新密码',
      },
      dependencies: {
        rules(values) {
          const { newPassword } = values;
          return z
            .string({ required_error: '请再次输入新密码' })
            .min(1, { message: '请再次输入新密码' })
            .refine((value) => value === newPassword, {
              message: '两次输入的密码不一致',
            });
        },
        triggerFields: ['newPassword'],
      },
    },
  ];
});

async function handleSubmit(values: Record<string, any>) {
  loading.value = true;
  try {
    await changePassword({
      oldPassword: values.oldPassword,
      newPassword: values.newPassword,
    });
    message.success('密码修改成功，请重新登录');
    // 清空表单
    profilePasswordSettingRef.value?.getFormApi().resetForm();
    // 可以选择跳转到登录页
    // router.push('/login');
  } catch (error: any) {
    message.error(error.message || '密码修改失败');
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <ProfilePasswordSetting
    ref="profilePasswordSettingRef"
    class="w-full md:w-2/3 lg:w-1/2 xl:w-1/3"
    :form-schema="formSchema"
    :loading="loading"
    @submit="handleSubmit"
  />
</template>
