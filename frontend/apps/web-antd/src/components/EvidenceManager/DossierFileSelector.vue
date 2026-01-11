<script setup lang="ts">
import type { Key } from 'ant-design-vue/es/_util/type';

import type { DocumentDTO } from '#/api/document';
import type { MatterDossierItem } from '#/api/document/dossier';

/**
 * 卷宗文件选择器组件
 * 用于从卷宗管理中选择文件作为证据材料
 */
import { computed, ref, watch } from 'vue';

import {
  Button,
  Empty,
  Input,
  Modal,
  Space,
  Spin,
  Tag,
  Tree,
} from 'ant-design-vue';

import { getDocumentsByMatter } from '#/api/document';
import { getMatterDossierItems } from '#/api/document/dossier';

const props = defineProps<{
  matterId: number;
  open: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'select', doc: DocumentDTO): void;
}>();

// 状态
const loading = ref(false);
const dossierItems = ref<MatterDossierItem[]>([]);
const documents = ref<DocumentDTO[]>([]);
const selectedKeys = ref<Key[]>([]);
const expandedKeys = ref<Key[]>([]);
const searchKeyword = ref('');

// 过滤后的文档列表
const filteredDocuments = computed(() => {
  if (!searchKeyword.value) return documents.value;
  const keyword = searchKeyword.value.toLowerCase();
  return documents.value.filter(
    (doc) =>
      doc.title?.toLowerCase().includes(keyword) ||
      doc.fileName?.toLowerCase().includes(keyword),
  );
});

// 构建树形数据，包含目录和文件
const treeData = computed(() => {
  const buildTree = (parentId: number): any[] => {
    const items = dossierItems.value
      .filter((item) => item.parentId === parentId)
      .sort((a, b) => a.sortOrder - b.sortOrder);

    return items.map((item) => {
      const children = item.itemType === 'FOLDER' ? buildTree(item.id) : [];

      // 如果是目录，查找该目录下的文档
      const folderDocs = documents.value.filter(
        (doc) => doc.dossierItemId === item.id,
      );
      const docNodes = folderDocs.map((doc) => ({
        key: `doc-${doc.id}`,
        title: doc.title || doc.fileName,
        isLeaf: true,
        selectable: true,
        data: doc,
        icon: () => getFileIcon(doc.fileType),
      }));

      return {
        key: `folder-${item.id}`,
        title: item.name,
        selectable: false,
        children: [...children, ...docNodes],
        icon: () => '📁',
      };
    });
  };

  // 未分类文档
  const uncategorizedDocs = documents.value.filter((doc) => !doc.dossierItemId);
  const uncategorizedNodes = uncategorizedDocs.map((doc) => ({
    key: `doc-${doc.id}`,
    title: doc.title || doc.fileName,
    isLeaf: true,
    selectable: true,
    data: doc,
    icon: () => getFileIcon(doc.fileType),
  }));

  const tree = buildTree(0);

  if (uncategorizedNodes.length > 0) {
    tree.push({
      key: 'uncategorized',
      title: '未分类文件',
      selectable: false,
      children: uncategorizedNodes,
      icon: () => '📂',
    });
  }

  return tree;
});

function getFileIcon(fileType?: string): string {
  switch (fileType?.toLowerCase()) {
    case 'audio': {
      return '🎵';
    }
    case 'excel': {
      return '📗';
    }
    case 'image': {
      return '🖼️';
    }
    case 'pdf': {
      return '📕';
    }
    case 'video': {
      return '🎬';
    }
    case 'word': {
      return '📘';
    }
    default: {
      return '📄';
    }
  }
}

// 加载数据
async function loadData() {
  if (!props.matterId) return;

  loading.value = true;
  try {
    const [items, docs] = await Promise.all([
      getMatterDossierItems(props.matterId),
      getDocumentsByMatter(props.matterId),
    ]);
    dossierItems.value = items || [];
    documents.value = docs || [];

    // 默认展开所有目录
    expandedKeys.value = dossierItems.value
      .filter((item) => item.itemType === 'FOLDER')
      .map((item) => `folder-${item.id}`);
  } catch (error: any) {
    console.error('加载卷宗文件失败:', error);
  } finally {
    loading.value = false;
  }
}

// 选择文件
function handleSelect(keys: Key[], info: any) {
  if (keys.length > 0 && info.node?.data) {
    selectedKeys.value = keys;
  }
}

// 确认选择
function handleConfirm() {
  const selectedKey = selectedKeys.value[0];
  if (selectedKey && String(selectedKey).startsWith('doc-')) {
    const docId = Number(String(selectedKey).replace('doc-', ''));
    const doc = documents.value.find((d) => d.id === docId);
    if (doc) {
      emit('select', doc);
      emit('update:open', false);
    }
  }
}

// 取消
function handleCancel() {
  emit('update:open', false);
}

// 监听弹窗打开
watch(
  () => props.open,
  (val) => {
    if (val) {
      selectedKeys.value = [];
      searchKeyword.value = '';
      loadData();
    }
  },
);
</script>

<script lang="ts">
function formatFileSize(bytes?: number): string {
  if (!bytes) return '';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
</script>

<template>
  <Modal
    :open="open"
    title="从卷宗选择文件"
    :width="600"
    @cancel="handleCancel"
  >
    <template #footer>
      <Space>
        <Button @click="handleCancel">取消</Button>
        <Button
          type="primary"
          :disabled="selectedKeys.length === 0"
          @click="handleConfirm"
        >
          确认选择
        </Button>
      </Space>
    </template>

    <Spin :spinning="loading">
      <!-- 搜索框 -->
      <Input.Search
        v-model:value="searchKeyword"
        placeholder="搜索文件名..."
        style="margin-bottom: 12px"
      />

      <!-- 文件树 -->
      <div
        v-if="treeData.length > 0 || filteredDocuments.length > 0"
        style="
          max-height: 400px;
          padding: 12px;
          overflow-y: auto;
          border: 1px solid #f0f0f0;
          border-radius: 6px;
        "
      >
        <Tree
          v-if="!searchKeyword"
          v-model:selected-keys="selectedKeys"
          v-model:expanded-keys="expandedKeys"
          :tree-data="treeData"
          show-icon
          block-node
          @select="handleSelect"
        >
          <template #title="{ title, data }">
            <span>{{ title }}</span>
            <Tag v-if="data?.fileSize" style="margin-left: 8px" size="small">
              {{ formatFileSize(data.fileSize) }}
            </Tag>
          </template>
        </Tree>

        <!-- 搜索结果列表 -->
        <div v-else>
          <div
            v-for="doc in filteredDocuments"
            :key="doc.id"
            class="file-item"
            :class="{ selected: selectedKeys.includes(`doc-${doc.id}`) }"
            style="
              display: flex;
              gap: 8px;
              align-items: center;
              padding: 8px 12px;
              cursor: pointer;
              border-radius: 4px;
            "
            @click="selectedKeys = [`doc-${doc.id}`]"
          >
            <span>{{ getFileIcon(doc.fileType) }}</span>
            <span style="flex: 1">{{ doc.title || doc.fileName }}</span>
            <Tag v-if="doc.fileSize" size="small">
              {{ formatFileSize(doc.fileSize) }}
            </Tag>
          </div>
        </div>
      </div>

      <Empty v-else description="暂无文件，请先在卷宗管理中上传文件" />
    </Spin>
  </Modal>
</template>

<style scoped>
.file-item:hover {
  background-color: #f5f5f5;
}

.file-item.selected {
  background-color: #e6f7ff;
  border: 1px solid #1890ff;
}
</style>
