<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';

import { computed, onMounted, ref } from 'vue';

import { ProfileBaseSetting } from '@vben/common-ui';
import { message } from 'ant-design-vue';

import { getProfileInfo, updateProfile } from '#/api';

const profileBaseSettingRef = ref();
const loading = ref(false);
const userRoles = ref<string[]>([]);

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      fieldName: 'realName',
      component: 'Input',
      label: '姓名',
      componentProps: {
        disabled: true,
      },
    },
    {
      fieldName: 'username',
      component: 'Input',
      label: '用户名',
      componentProps: {
        disabled: true,
      },
    },
    {
      fieldName: 'email',
      component: 'Input',
      label: '邮箱',
    },
    {
      fieldName: 'phone',
      component: 'Input',
      label: '手机号',
    },
    {
      fieldName: 'position',
      component: 'Input',
      label: '职位',
      componentProps: {
        disabled: true,
      },
    },
    {
      fieldName: 'employeeNo',
      component: 'Input',
      label: '工号',
      componentProps: {
        disabled: true,
      },
    },
    {
      fieldName: 'introduction',
      component: 'Textarea',
      label: '个人简介',
      componentProps: {
        rows: 4,
        maxlength: 500,
        showCount: true,
      },
    },
  ];
});

async function loadUserInfo() {
  try {
    const data = await getProfileInfo();
    profileBaseSettingRef.value?.getFormApi().setValues({
      ...data,
      roles: data.roleCodes || [],
    });
    userRoles.value = data.roleCodes || [];
  } catch (error) {
    console.error('加载用户信息失败:', error);
  }
}

async function handleSubmit(values: Record<string, any>) {
  loading.value = true;
  try {
    await updateProfile({
      email: values.email,
      phone: values.phone,
      introduction: values.introduction,
    });
    message.success('保存成功');
  } catch (error: any) {
    message.error(error.message || '保存失败');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadUserInfo();
});
</script>
<template>
  <ProfileBaseSetting 
    ref="profileBaseSettingRef" 
    :form-schema="formSchema"
    :loading="loading"
    @submit="handleSubmit"
  />
</template>
