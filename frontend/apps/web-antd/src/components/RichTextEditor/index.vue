<script setup lang="ts">
/**
 * 富文本模板编辑器组件
 * 支持变量插入的真正富文本编辑器
 */
import { ref, shallowRef, onBeforeUnmount, watch, computed } from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import type { IEditorConfig, IToolbarConfig, IDomEditor } from '@wangeditor/editor';
import { Dropdown, Menu, MenuItem, Button, Tooltip, Tag, Space } from 'ant-design-vue';
import '@wangeditor/editor/dist/css/style.css';

interface VariableItem {
  label: string;
  value: string;
  description?: string;
}

const props = withDefaults(defineProps<{
  modelValue?: string;
  placeholder?: string;
  height?: string;
  variables?: VariableItem[];
  mode?: 'default' | 'simple';
  showVariables?: boolean;
}>(), {
  modelValue: '',
  placeholder: '请输入内容...',
  height: '400px',
  mode: 'default',
  showVariables: true,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

// 编辑器实例
const editorRef = shallowRef<IDomEditor>();

// 本地HTML内容
const htmlContent = ref(props.modelValue || '');

// 默认变量列表
const defaultVariables: VariableItem[] = [
  { label: '委托人姓名', value: 'clientName', description: '委托人/当事人姓名' },
  { label: '委托人身份证号', value: 'clientIdNumber', description: '委托人身份证号码' },
  { label: '律所名称', value: 'firmName', description: '律师事务所全称' },
  { label: '律所地址', value: 'firmAddress', description: '律师事务所地址' },
  { label: '律所电话', value: 'firmPhone', description: '律师事务所电话' },
  { label: '承办律师', value: 'lawyerName', description: '承办律师姓名' },
  { label: '律师执业证号', value: 'lawyerLicenseNo', description: '承办律师执业证号' },
  { label: '项目名称', value: 'matterName', description: '委托项目/案件名称' },
  { label: '案由', value: 'causeOfAction', description: '案件案由' },
  { label: '合同金额', value: 'totalAmount', description: '合同总金额' },
  { label: '大写金额', value: 'totalAmountChinese', description: '合同金额大写' },
  { label: '签订日期', value: 'signDate', description: '合同签订日期' },
  { label: '合同编号', value: 'contractNo', description: '合同编号' },
  { label: '当前年份', value: 'currentYear', description: '当前年份' },
  { label: '当前日期', value: 'currentDate', description: '当前完整日期' },
];

// 使用传入的变量或默认变量
const variableList = computed(() => props.variables || defaultVariables);

// 工具栏配置
const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['insertVideo', 'codeBlock', 'todo', 'group-video'],
};

// 编辑器配置
const editorConfig: Partial<IEditorConfig> = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      customUpload(file: File, insertFn: (url: string) => void) {
        const reader = new FileReader();
        reader.onload = () => {
          insertFn(reader.result as string);
        };
        reader.readAsDataURL(file);
      },
    },
  },
};

// 计算编辑器高度
const editorHeight = computed(() => {
  const h = props.height;
  return props.showVariables ? `calc(${h} - 90px)` : `calc(${h} - 50px)`;
});

// 创建时
function handleCreated(editor: IDomEditor) {
  editorRef.value = editor;
}

// 内容变化
function handleChange(editor: IDomEditor) {
  emit('update:modelValue', editor.getHtml());
}

// 插入变量
function insertVariable(variable: VariableItem) {
  const editor = editorRef.value;
  if (!editor) {
    console.warn('编辑器未初始化');
    return;
  }
  
  // 先聚焦编辑器
  editor.focus();
  
  // 使用 restoreSelection 确保光标位置正确
  editor.restoreSelection();
  
  // 插入变量文本（使用纯文本格式，避免 HTML 解析问题）
  const varText = `\${${variable.value}}`;
  editor.insertText(varText);
}

// 监听外部值变化
watch(() => props.modelValue, (newVal) => {
  if (newVal !== htmlContent.value) {
    htmlContent.value = newVal || '';
    const editor = editorRef.value;
    if (editor && newVal !== editor.getHtml()) {
      editor.setHtml(newVal || '');
    }
  }
}, { immediate: true });

// 组件销毁时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value;
  if (editor) {
    editor.destroy();
  }
});
</script>

<template>
  <div class="rich-text-editor">
    <!-- 变量插入工具栏 -->
    <div v-if="showVariables && variableList.length > 0" class="variable-toolbar">
      <span class="toolbar-label">插入变量：</span>
      <Space wrap :size="4">
        <Tooltip v-for="v in variableList" :key="v.value" :title="v.description || v.label">
          <Tag 
            color="blue" 
            class="variable-tag"
            @click="insertVariable(v)"
          >
            {{ v.label }}
          </Tag>
        </Tooltip>
      </Space>
    </div>

    <!-- 工具栏 -->
    <Toolbar
      class="editor-toolbar"
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      :mode="mode"
    />

    <!-- 编辑区 -->
    <Editor
      class="editor-content"
      :style="{ height: editorHeight }"
      :defaultConfig="editorConfig"
      :mode="mode"
      v-model="htmlContent"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<style scoped>
.rich-text-editor {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
  background: #fff;
}

.variable-toolbar {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  background: linear-gradient(to bottom, #f0f5ff, #e6f0ff);
  border-bottom: 1px solid #d6e4ff;
  min-height: 44px;
}

.toolbar-label {
  color: #1890ff;
  font-size: 13px;
  font-weight: 500;
  flex-shrink: 0;
  line-height: 22px;
}

.variable-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.variable-tag:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 4px rgba(24, 144, 255, 0.3);
}

.editor-toolbar {
  border-bottom: 1px solid #e8e8e8;
}

.editor-content {
  overflow-y: auto;
}

/* 覆盖WangEditor默认样式 */
:deep(.w-e-toolbar) {
  background-color: #fafafa !important;
  border-bottom: 1px solid #e8e8e8 !important;
}

:deep(.w-e-text-container) {
  background-color: #fff !important;
}

:deep(.w-e-text-placeholder) {
  color: #bbb !important;
  font-style: normal !important;
}

/* 确保变量标签样式正确 */
:deep([data-variable]) {
  color: #1890ff !important;
  background: #e6f7ff !important;
  padding: 0 4px !important;
  border-radius: 2px !important;
  display: inline-block !important;
  margin: 0 2px !important;
}
</style>
