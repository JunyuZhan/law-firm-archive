<script setup lang="ts">
import type { DossierTemplateItem } from '#/api/document/dossier';

import { h, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Edit, Plus, Trash } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  Input,
  message,
  Select,
  Space,
  Switch,
  Tree,
} from 'ant-design-vue';

import {
  addDossierTemplateItem,
  deleteDossierTemplateItem,
  FILE_CATEGORY_OPTIONS,
  getDossierTemplateItems,
  updateDossierTemplateItem,
} from '#/api/document/dossier';

// eslint-disable-next-line no-unused-vars
const _props = defineProps<{
  templateId?: number;
  templateName?: string;
}>();

const emit = defineEmits<{
  success: [];
}>();

const loading = ref(false);
const treeData = ref<any[]>([]);
const editingItem = ref<null | Partial<DossierTemplateItem>>(null);
const itemFormVisible = ref(false);

// 构建树形结构
function buildTree(
  items: DossierTemplateItem[],
  parentId: null | number | undefined = null,
): any[] {
  return items
    .filter((item) =>
      parentId === null || parentId === undefined
        ? !item.parentId
        : item.parentId === parentId,
    )
    .toSorted((a, b) => a.sortOrder - b.sortOrder)
    .map((item) => ({
      key: item.id,
      title: item.name,
      itemType: item.itemType,
      fileCategory: item.fileCategory,
      required: item.required,
      description: item.description,
      sortOrder: item.sortOrder,
      isLeaf: item.itemType === 'FILE',
      children: buildTree(items, item.id),
    }));
}

// 加载目录项
async function loadItems() {
  const templateId = currentTemplateId.value;
  if (!templateId) return;

  loading.value = true;
  try {
    const items = await getDossierTemplateItems(templateId);
    treeData.value = buildTree(items);
  } catch (error: any) {
    message.error(error.message || '加载目录结构失败');
  } finally {
    loading.value = false;
  }
}

// 打开新增/编辑表单
function openItemForm(node?: any, parentId?: number) {
  editingItem.value = node
    ? {
        // 编辑模式
        id: node.key,
        name: node.title,
        itemType: node.itemType,
        fileCategory: node.fileCategory,
        required: node.required,
        description: node.description,
        sortOrder: node.sortOrder,
        parentId: parentId || undefined,
      }
    : {
        // 新增模式
        itemType: 'FOLDER',
        required: false,
        sortOrder: 0,
        parentId: parentId || undefined,
      };
  itemFormVisible.value = true;
}

// 保存目录项
async function saveItem() {
  const templateId = currentTemplateId.value;
  if (!templateId || !editingItem.value) return;

  const form = editingItem.value;
  if (!form.name?.trim()) {
    message.warning('请输入目录项名称');
    return;
  }

  try {
    if (form.id) {
      // 更新
      await updateDossierTemplateItem(form.id, {
        name: form.name,
        itemType: form.itemType,
        fileCategory: form.fileCategory,
        required: form.required,
        description: form.description,
        sortOrder: form.sortOrder,
      });
      message.success('更新成功');
    } else {
      // 新增
      await addDossierTemplateItem(templateId, {
        name: form.name,
        itemType: form.itemType,
        fileCategory: form.fileCategory,
        required: form.required,
        description: form.description,
        sortOrder: form.sortOrder,
        parentId: form.parentId || undefined,
      });
      message.success('添加成功');
    }
    itemFormVisible.value = false;
    editingItem.value = null;
    await loadItems();
    emit('success');
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 删除目录项
function handleDelete(node: any) {
  AntModal.confirm({
    title: '确认删除',
    content: `确定要删除目录项 "${node.title}" 吗？${node.children?.length ? '删除后其子项也会被删除。' : ''}`,
    onOk: async () => {
      try {
        await deleteDossierTemplateItem(node.key);
        message.success('删除成功');
        await loadItems();
        emit('success');
      } catch (error: any) {
        message.error(error.message || '删除失败');
      }
    },
  });
}

// 树节点标题渲染函数
function renderTitle(node: any) {
  return h(
    'div',
    {
      style: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        width: '100%',
      },
    },
    [
      h('span', [
        h(
          'span',
          { style: { marginRight: '8px' } },
          node.itemType === 'FOLDER' ? '📁' : '📄',
        ),
        h('span', node.title),
        node.required &&
          h('span', { style: { color: '#ff4d4f', marginLeft: '4px' } }, '*'),
        node.fileCategory &&
          h(
            'span',
            {
              style: { color: '#999', marginLeft: '8px', fontSize: '12px' },
            },
            `(${FILE_CATEGORY_OPTIONS.find((o: any) => o.value === node.fileCategory)?.label || node.fileCategory})`,
          ),
      ]),
      h(
        Space,
        {
          size: 'small',
          onClick: (e: Event) => e.stopPropagation(),
        },
        {
          default: () => [
            h(
              Button,
              {
                type: 'link',
                size: 'small',
                onClick: () => openItemForm(node),
              },
              {
                default: () => [h(Edit, { class: 'size-3' }), ' 编辑'],
              },
            ),
            h(
              Button,
              {
                type: 'link',
                size: 'small',
                onClick: () => openItemForm(undefined, node.key),
              },
              {
                default: () => [h(Plus, { class: 'size-3' }), ' 添加子项'],
              },
            ),
            h(
              Button,
              {
                type: 'link',
                size: 'small',
                danger: true,
                onClick: () => handleDelete(node),
              },
              {
                default: () => [h(Trash, { class: 'size-3' }), ' 删除'],
              },
            ),
          ],
        },
      ),
    ],
  );
}

const currentTemplateId = ref<number>();
const currentTemplateName = ref<string>('');

const [Modal, modalApi] = useVbenModal({
  onOpenChange(isOpen) {
    if (isOpen) {
      if (currentTemplateId.value) {
        loadItems();
      }
    } else {
      treeData.value = [];
      editingItem.value = null;
      itemFormVisible.value = false;
      currentTemplateId.value = undefined;
      currentTemplateName.value = '';
    }
  },
});

function open(templateId: number, templateName: string) {
  currentTemplateId.value = templateId;
  currentTemplateName.value = templateName;
  modalApi.setState({ title: `配置目录结构 - ${templateName}` });
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <Modal class="w-[800px]">
    <div v-loading="loading" style="min-height: 400px">
      <div style="margin-bottom: 16px">
        <Button type="primary" @click="openItemForm()">
          <Plus class="size-4" /> 添加根目录项
        </Button>
      </div>

      <Tree
        v-if="treeData.length > 0"
        :tree-data="treeData"
        :field-names="{ title: 'title', key: 'key', children: 'children' }"
        default-expand-all
        :show-line="true"
      >
        <template #title="{ data }">
          <component :is="renderTitle(data)" />
        </template>
      </Tree>

      <div v-else style="padding: 40px; color: #999; text-align: center">
        暂无目录项，请添加根目录项
      </div>
    </div>

    <!-- 目录项编辑表单 -->
    <AntModal
      v-model:open="itemFormVisible"
      :title="editingItem?.id ? '编辑目录项' : '新增目录项'"
      width="600"
      @ok="saveItem"
      @cancel="
        () => {
          itemFormVisible = false;
          editingItem = null;
        }
      "
    >
      <div v-if="editingItem" style="padding: 16px 0">
        <div style="margin-bottom: 16px">
          <label style="display: block; margin-bottom: 4px">
            目录项名称 <span style="color: #ff4d4f">*</span>
          </label>
          <Input
            v-model:value="editingItem.name"
            placeholder="请输入目录项名称"
            style="width: 100%"
          />
        </div>

        <div style="margin-bottom: 16px">
          <label style="display: block; margin-bottom: 4px">类型</label>
          <Select v-model:value="editingItem.itemType" style="width: 100%">
            <Select.Option value="FOLDER">文件夹</Select.Option>
            <Select.Option value="FILE">文件</Select.Option>
          </Select>
        </div>

        <div v-if="editingItem.itemType === 'FILE'" style="margin-bottom: 16px">
          <label style="display: block; margin-bottom: 4px">文件分类</label>
          <Select
            v-model:value="editingItem.fileCategory"
            placeholder="请选择文件分类"
            style="width: 100%"
            allow-clear
          >
            <Select.Option
              v-for="option in FILE_CATEGORY_OPTIONS"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </Select.Option>
          </Select>
        </div>

        <div style="margin-bottom: 16px">
          <label style="display: block; margin-bottom: 4px">排序</label>
          <Input
            v-model:value="editingItem.sortOrder"
            type="number"
            placeholder="请输入排序值"
            style="width: 100%"
          />
        </div>

        <div style="margin-bottom: 16px">
          <label style="display: block; margin-bottom: 4px">是否必填</label>
          <Switch v-model:checked="editingItem.required" />
        </div>

        <div>
          <label style="display: block; margin-bottom: 4px">描述</label>
          <Input.TextArea
            v-model:value="editingItem.description"
            placeholder="请输入描述（可选）"
            :rows="3"
          />
        </div>
      </div>
    </AntModal>
  </Modal>
</template>
