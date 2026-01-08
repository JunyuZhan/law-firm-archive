<script setup lang="ts">
/**
 * 卷宗目录管理组件
 * 显示项目的卷宗目录树，支持添加、编辑、删除目录项
 */
import { ref, computed, watch, h } from 'vue';
import { Tree, Button, Space, Spin, Empty, Modal, Form, FormItem, Input, Select, message, Popconfirm, Badge, Tooltip } from 'ant-design-vue';
import { Plus, RotateCw, ExternalLink } from '@vben/icons';
import type { Key } from 'ant-design-vue/es/_util/type';
import {
  getMatterDossierItems,
  initMatterDossier,
  addDossierItem,
  updateDossierItem,
  deleteDossierItem,
  type MatterDossierItem,
} from '#/api/document/dossier';

// 文件夹图标组件
const FolderIcon = () => h('span', { style: 'font-size: 14px; margin-right: 4px;' }, '📁');
const FileIcon = () => h('span', { style: 'font-size: 14px; margin-right: 4px;' }, '📄');

const props = defineProps<{
  matterId: number;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'select', item: MatterDossierItem | null): void;
  (e: 'upload', item: MatterDossierItem): void;
}>();

// 状态
const loading = ref(false);
const dossierItems = ref<MatterDossierItem[]>([]);
const selectedKeys = ref<string[]>([]);
const expandedKeys = ref<string[]>([]);

// 弹窗状态
const addModalVisible = ref(false);
const editModalVisible = ref(false);
const currentItem = ref<MatterDossierItem | null>(null);
const formData = ref({
  name: '',
  itemType: 'FOLDER',
  parentId: 0,
});

// 转换为树形数据
const treeData = computed(() => {
  const buildTree = (parentId: number): any[] => {
    return dossierItems.value
      .filter(item => item.parentId === parentId)
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(item => ({
        key: String(item.id),
        title: item.name,
        icon: item.itemType === 'FOLDER' ? FolderIcon : FileIcon,
        isLeaf: item.itemType === 'FILE',
        data: item,
        children: item.itemType === 'FOLDER' ? buildTree(item.id) : undefined,
      }));
  };
  return buildTree(0);
});

// 加载数据
async function loadData() {
  if (!props.matterId) return;
  
  loading.value = true;
  try {
    const items = await getMatterDossierItems(props.matterId);
    dossierItems.value = items || [];
    
    // 默认展开所有目录
    expandedKeys.value = dossierItems.value
      .filter(item => item.itemType === 'FOLDER')
      .map(item => String(item.id));
  } catch (error: any) {
    console.error('加载卷宗目录失败:', error);
  } finally {
    loading.value = false;
  }
}

// 初始化卷宗目录
async function handleInitialize() {
  loading.value = true;
  try {
    const items = await initMatterDossier(props.matterId);
    dossierItems.value = items || [];
    expandedKeys.value = dossierItems.value
      .filter(item => item.itemType === 'FOLDER')
      .map(item => String(item.id));
    message.success('卷宗目录初始化成功');
  } catch (error: any) {
    message.error(error.message || '初始化失败');
  } finally {
    loading.value = false;
  }
}

// 选择节点
function handleSelect(keys: Key[], info: any) {
  selectedKeys.value = keys.map(k => String(k));
  if (keys.length > 0 && info.node) {
    emit('select', info.node.data);
  } else {
    emit('select', null);
  }
}

// 显示添加弹窗
function showAddModal(parentId: number = 0) {
  formData.value = {
    name: '',
    itemType: 'FOLDER',
    parentId,
  };
  addModalVisible.value = true;
}

// 添加目录项
async function handleAdd() {
  if (!formData.value.name.trim()) {
    message.error('请输入名称');
    return;
  }
  
  try {
    await addDossierItem(props.matterId, {
      parentId: formData.value.parentId,
      name: formData.value.name.trim(),
      itemType: formData.value.itemType,
    });
    message.success('添加成功');
    addModalVisible.value = false;
    loadData();
  } catch (error: any) {
    message.error(error.message || '添加失败');
  }
}

// 显示编辑弹窗
function showEditModal(item: MatterDossierItem) {
  currentItem.value = item;
  formData.value = {
    name: item.name,
    itemType: item.itemType,
    parentId: item.parentId,
  };
  editModalVisible.value = true;
}

// 更新目录项
async function handleUpdate() {
  if (!currentItem.value) return;
  if (!formData.value.name.trim()) {
    message.error('请输入名称');
    return;
  }
  
  try {
    await updateDossierItem(props.matterId, currentItem.value.id, {
      name: formData.value.name.trim(),
    });
    message.success('更新成功');
    editModalVisible.value = false;
    loadData();
  } catch (error: any) {
    message.error(error.message || '更新失败');
  }
}

// 删除目录项
async function handleDelete(item: MatterDossierItem) {
  try {
    await deleteDossierItem(props.matterId, item.id);
    message.success('删除成功');
    loadData();
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 跳转到卷宗管理页面
function goToDossierList() {
  // 打开卷宗管理页面并过滤当前项目
  window.open(`/document/list?matterId=${props.matterId}`, '_blank');
}

// 监听 matterId 变化
watch(() => props.matterId, () => {
  if (props.matterId) {
    loadData();
  }
}, { immediate: true });

// 暴露方法
defineExpose({
  refresh: loadData,
});
</script>

<template>
  <div class="dossier-manager">
    <Spin :spinning="loading">
      <!-- 工具栏 -->
      <div class="toolbar" style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center;">
        <Space>
          <Button v-if="!readonly" type="primary" size="small" @click="showAddModal(0)">
            <Plus class="w-4 h-4" />
            添加目录
          </Button>
          <Button size="small" @click="loadData">
            <RotateCw class="w-4 h-4" />
            刷新
          </Button>
        </Space>
        <Tooltip title="在卷宗管理页面查看完整文件列表">
          <Button type="link" size="small" @click="goToDossierList">
            <ExternalLink class="w-4 h-4" />
            查看全部文件
          </Button>
        </Tooltip>
      </div>

      <!-- 目录树 -->
      <div v-if="dossierItems.length > 0" class="tree-container" style="border: 1px solid #f0f0f0; border-radius: 6px; padding: 12px; min-height: 300px;">
        <Tree
          v-model:selectedKeys="selectedKeys"
          v-model:expandedKeys="expandedKeys"
          :tree-data="treeData"
          show-icon
          block-node
          @select="handleSelect"
        >
          <template #title="{ data }">
            <div class="tree-node" style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
              <span class="node-title" style="flex: 1;">
                {{ data.title }}
                <Badge 
                  v-if="data.data?.documentCount > 0" 
                  :count="data.data.documentCount" 
                  :number-style="{ backgroundColor: '#52c41a', fontSize: '10px', minWidth: '16px', height: '16px', lineHeight: '16px' }"
                  style="margin-left: 8px;"
                />
              </span>
              <Space v-if="!readonly" class="node-actions" size="small" style="opacity: 0; transition: opacity 0.2s;">
                <Tooltip v-if="data.data?.itemType === 'FOLDER'" title="添加子目录">
                  <Button type="text" size="small" @click.stop="showAddModal(data.data.id)">
                    <Plus class="w-3 h-3" />
                  </Button>
                </Tooltip>
                <Tooltip title="编辑">
                  <Button type="text" size="small" @click.stop="showEditModal(data.data)">
                    ✏️
                  </Button>
                </Tooltip>
                <Popconfirm
                  title="确定删除此目录项吗？"
                  @confirm="handleDelete(data.data)"
                >
                  <Tooltip title="删除">
                    <Button type="text" size="small" danger @click.stop>
                      🗑️
                    </Button>
                  </Tooltip>
                </Popconfirm>
              </Space>
            </div>
          </template>
        </Tree>
      </div>

      <!-- 空状态 -->
      <Empty v-else description="暂无卷宗目录">
        <Button type="primary" @click="handleInitialize">
          初始化卷宗目录
        </Button>
      </Empty>
    </Spin>

    <!-- 添加弹窗 -->
    <Modal
      v-model:open="addModalVisible"
      title="添加目录项"
      @ok="handleAdd"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="名称" required>
          <Input v-model:value="formData.name" placeholder="请输入目录名称" />
        </FormItem>
        <FormItem label="类型">
          <Select v-model:value="formData.itemType">
            <Select.Option value="FOLDER">目录</Select.Option>
            <Select.Option value="FILE">文件占位</Select.Option>
          </Select>
        </FormItem>
      </Form>
    </Modal>

    <!-- 编辑弹窗 -->
    <Modal
      v-model:open="editModalVisible"
      title="编辑目录项"
      @ok="handleUpdate"
    >
      <Form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <FormItem label="名称" required>
          <Input v-model:value="formData.name" placeholder="请输入目录名称" />
        </FormItem>
      </Form>
    </Modal>
  </div>
</template>

<style scoped>
.dossier-manager .tree-node:hover .node-actions {
  opacity: 1 !important;
}
</style>

