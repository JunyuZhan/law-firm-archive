<script setup lang="ts">
import type {
  IDomEditor,
  IEditorConfig,
  IToolbarConfig,
} from '@wangeditor/editor';

/**
 * 富文本模板编辑器组件
 * 支持变量插入的真正富文本编辑器
 */
import { computed, onBeforeUnmount, ref, shallowRef, watch } from 'vue';

import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import { Space, Tag, Tooltip } from 'ant-design-vue';

import '@wangeditor/editor/dist/css/style.css';

const props = withDefaults(
  defineProps<{
    height?: string;
    mode?: 'default' | 'simple';
    modelValue?: string;
    placeholder?: string;
    showVariables?: boolean;
    variables?: VariableItem[];
  }>(),
  {
    height: '400px',
    mode: 'default',
    modelValue: '',
    placeholder: '请输入内容...',
    showVariables: true,
    variables: undefined,
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

// HTML 实体解码函数
function decodeHtmlEntities(text: string): string {
  if (!text) return text;
  const entities: Record<string, string> = {
    '&quot;': '"',
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&apos;': "'",
    '&#39;': "'",
    '&#x27;': "'",
    '&nbsp;': ' ',
  };
  let result = text;
  for (const [entity, char] of Object.entries(entities)) {
    result = result.split(entity).join(char);
  }
  result = result.replaceAll(/&#(\d+);/g, (_, num) =>
    String.fromCodePoint(Number.parseInt(num, 10)),
  );
  result = result.replaceAll(/&#x([0-9a-fA-F]+);/g, (_, hex) =>
    String.fromCodePoint(Number.parseInt(hex, 16)),
  );
  return result;
}

interface VariableItem {
  label: string;
  value: string;
  description?: string;
}

// 编辑器实例
const editorRef = shallowRef<IDomEditor>();

// 本地HTML内容
const htmlContent = ref(props.modelValue || '');

// 默认变量列表
const defaultVariables: VariableItem[] = [
  {
    label: '委托人姓名',
    value: 'clientName',
    description: '委托人/当事人姓名',
  },
  {
    label: '委托人身份证号',
    value: 'clientIdNumber',
    description: '委托人身份证号码',
  },
  { label: '律所名称', value: 'firmName', description: '律师事务所全称' },
  { label: '律所地址', value: 'firmAddress', description: '律师事务所地址' },
  { label: '律所电话', value: 'firmPhone', description: '律师事务所电话' },
  { label: '承办律师', value: 'lawyerName', description: '承办律师姓名' },
  {
    label: '律师执业证号',
    value: 'lawyerLicenseNo',
    description: '承办律师执业证号',
  },
  { label: '项目名称', value: 'matterName', description: '委托项目/案件名称' },
  { label: '案由', value: 'causeOfAction', description: '案件案由' },
  { label: '合同金额', value: 'totalAmount', description: '合同总金额' },
  {
    label: '大写金额',
    value: 'totalAmountChinese',
    description: '合同金额大写',
  },
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
  // XSS 防护：wangeditor 默认会过滤危险标签和属性
  // 但为了更安全，我们显式配置
  readOnly: false,
  // 自定义粘贴过滤（增强XSS防护）
  customPaste: (_editor, _event) => {
    // wangeditor 会自动过滤危险内容，这里可以添加额外的过滤逻辑
    // 返回 true 表示继续执行默认粘贴行为
    return true;
  },
  MENU_CONF: {
    uploadImage: {
      customUpload(file: File, insertFn: (url: string) => void) {
        // 文件类型校验
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
          console.warn('不支持的图片格式:', file.type);
          return;
        }

        // 文件扩展名校验
        const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
        const ext = file.name.toLowerCase().slice(file.name.lastIndexOf('.'));
        if (!allowedExtensions.includes(ext)) {
          console.warn('不支持的图片扩展名:', ext);
          return;
        }

        // 文件大小校验（最大 5MB）
        const maxSize = 5 * 1024 * 1024;
        if (file.size > maxSize) {
          console.warn('图片文件过大，最大支持 5MB');
          return;
        }

        const reader = new FileReader();
        // 使用 onload/onerror 赋值代替 addEventListener，避免需要手动移除监听器
        reader.onload = () => {
          insertFn(reader.result as string);
        };
        reader.onerror = () => {
          console.error('读取图片文件失败:', reader.error);
          // 静默失败，不阻止用户继续编辑
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
watch(
  () => props.modelValue,
  (newVal) => {
    // 解码可能被 HTML 编码的内容
    const decodedVal = decodeHtmlEntities(newVal || '');
    if (decodedVal !== htmlContent.value) {
      htmlContent.value = decodedVal;
      const editor = editorRef.value;
      if (editor && decodedVal !== editor.getHtml()) {
        editor.setHtml(decodedVal);
      }
    }
  },
  { immediate: true },
);

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
    <div
      v-if="showVariables && variableList.length > 0"
      class="variable-toolbar"
    >
      <span class="toolbar-label">插入变量：</span>
      <Space wrap :size="4">
        <Tooltip
          v-for="v in variableList"
          :key="v.value"
          :title="v.description || v.label"
        >
          <Tag color="blue" class="variable-tag" @click="insertVariable(v)">
            {{ v.label }}
          </Tag>
        </Tooltip>
      </Space>
    </div>

    <!-- 工具栏 -->
    <Toolbar
      class="editor-toolbar"
      :editor="editorRef"
      :default-config="toolbarConfig"
      :mode="mode"
    />

    <!-- 编辑区 -->
    <Editor
      class="editor-content"
      :style="{ height: editorHeight }"
      :default-config="editorConfig"
      :mode="mode"
      v-model="htmlContent"
      @on-created="handleCreated"
      @on-change="handleChange"
    />
  </div>
</template>

<style scoped>
.rich-text-editor {
  overflow: hidden;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.variable-toolbar {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  min-height: 44px;
  padding: 10px 12px;
  background: linear-gradient(to bottom, #f0f5ff, #e6f0ff);
  border-bottom: 1px solid #d6e4ff;
}

.toolbar-label {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 500;
  line-height: 22px;
  color: #1890ff;
}

.variable-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.variable-tag:hover {
  box-shadow: 0 2px 4px rgb(24 144 255 / 30%);
  transform: scale(1.05);
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
  font-style: normal !important;
  color: #bbb !important;
}

/* 确保变量标签样式正确 */
:deep([data-variable]) {
  display: inline-block !important;
  padding: 0 4px !important;
  margin: 0 2px !important;
  color: #1890ff !important;
  background: #e6f7ff !important;
  border-radius: 2px !important;
}
</style>
