<script setup lang="ts">
import type { Props } from './types';

import { preferences } from '@vben-core/preferences';
import {
  Card,
  Separator,
  Tabs,
  TabsList,
  TabsTrigger,
  VbenAvatar,
} from '@vben-core/shadcn-ui';

import { Page } from '../../components';

defineOptions({
  name: 'ProfileUI',
});

withDefaults(defineProps<Props>(), {
  title: '关于项目',
  tabs: () => [],
});

const tabsValue = defineModel<string>('modelValue');
</script>
<template>
  <Page auto-content-height>
    <!-- 移动端：垂直布局，桌面端：水平布局 -->
    <div class="flex h-full w-full flex-col gap-4 lg:flex-row">
      <!-- 左侧/顶部用户信息卡片 -->
      <Card class="w-full flex-none lg:w-1/5 xl:w-1/6">
        <!-- 用户头像和信息 -->
        <div
          class="flex flex-row items-center gap-4 p-4 lg:mt-4 lg:h-40 lg:flex-col lg:justify-center lg:p-0"
        >
          <VbenAvatar
            :src="userInfo?.avatar ?? preferences.app.defaultAvatar"
            class="size-16 lg:size-20"
          />
          <div class="flex flex-col lg:items-center">
            <span class="text-base font-semibold lg:text-lg">
              {{ userInfo?.realName ?? '' }}
            </span>
            <span class="text-foreground/80 text-sm">
              {{ userInfo?.username ?? '' }}
            </span>
          </div>
        </div>
        <Separator class="my-2 lg:my-4" />
        <!-- 移动端：水平滚动标签，桌面端：垂直标签 -->
        <Tabs
          v-model="tabsValue"
          :orientation="'vertical'"
          class="m-2 lg:m-4"
        >
          <!-- 移动端水平滚动容器 -->
          <div class="overflow-x-auto lg:overflow-visible">
            <TabsList
              class="bg-card flex w-max flex-row gap-1 lg:grid lg:w-full lg:grid-cols-1 lg:gap-0"
            >
              <TabsTrigger
                v-for="tab in tabs"
                :key="tab.value"
                :value="tab.value"
                class="data-[state=active]:bg-primary data-[state=active]:text-primary-foreground h-10 whitespace-nowrap px-4 lg:h-12 lg:justify-start lg:px-2"
              >
                {{ tab.label }}
              </TabsTrigger>
            </TabsList>
          </div>
        </Tabs>
      </Card>
      <!-- 右侧/底部内容区域 -->
      <Card class="min-h-0 w-full flex-auto p-4 lg:p-8">
        <slot name="content"></slot>
      </Card>
    </div>
  </Page>
</template>
