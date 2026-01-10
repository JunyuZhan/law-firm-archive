<script setup lang="ts">
/**
 * 文书制作页面
 * 支持两种模式：
 * 1. 模板制作：选择模板 → 选择项目（可选）→ 填写内容 → 预览生成
 * 2. AI 制作：描述需求 → 选择项目（可选）→ AI 生成 → 保存
 * 
 * 文书保存位置：
 * - 关联项目：保存到项目卷宗
 * - 不关联项目：保存到个人文书库（"我的文书"中查看）
 */
import { ref } from 'vue';
import { Page } from '@vben/common-ui';
import { Card, Radio, Divider } from 'ant-design-vue';
import TemplateMode from './components/TemplateMode.vue';
import AiMode from './components/AiMode.vue';
import type { ComposeMode } from './types';

defineOptions({ name: 'DocumentCompose' });

// 制作模式
const composeMode = ref<ComposeMode>('template');

// 组件引用
const templateModeRef = ref<InstanceType<typeof TemplateMode>>();
const aiModeRef = ref<InstanceType<typeof AiMode>>();

// 模式切换
function handleModeChange() {
  // 切换模式时重置对应组件状态
  templateModeRef.value?.handleReset();
  aiModeRef.value?.handleReset();
}

// 成功回调
function handleSuccess() {
  // 可以在这里添加额外的成功处理逻辑
  console.log('文书生成成功');
}

// 重置回调
function handleReset() {
  // 可以在这里添加额外的重置处理逻辑
  console.log('已重置');
}
</script>

<template>
  <Page title="文书制作" description="使用模板或 AI 快速制作法律文书">
    <Card>
      <!-- 模式选择 -->
      <div class="mode-selector">
        <Radio.Group 
          v-model:value="composeMode" 
          button-style="solid" 
          size="large" 
          @change="handleModeChange"
        >
          <Radio.Button value="template">
            📄 模板制作
          </Radio.Button>
          <Radio.Button value="ai">
            🤖 AI 智能生成
          </Radio.Button>
        </Radio.Group>
      </div>

      <Divider />

      <!-- 模板模式 -->
      <TemplateMode 
        v-if="composeMode === 'template'"
        ref="templateModeRef"
        @success="handleSuccess"
        @reset="handleReset"
      />

      <!-- AI 模式 -->
      <AiMode 
        v-if="composeMode === 'ai'"
        ref="aiModeRef"
        @success="handleSuccess"
        @reset="handleReset"
      />
    </Card>
  </Page>
</template>

<style scoped>
.mode-selector {
  margin-bottom: 24px;
  text-align: center;
}

:deep(.ant-radio-button-wrapper) {
  padding: 0 24px;
}
</style>
