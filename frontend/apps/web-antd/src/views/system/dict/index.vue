<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import { useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  Card,
  Button,
  Space,
  Tag,
  Input,
  Row,
  Col,
  List,
  ListItem,
  ListItemMeta,
  Descriptions,
  DescriptionsItem,
  Tooltip,
  Empty,
  Popconfirm,
} from 'ant-design-vue';
import {
  Plus,
  EditOutlined,
  DeleteOutlined,
} from '@vben/icons';
import {
  getDictTypeList,
  getDictItemsByTypeId,
  deleteDictType,
  deleteDictItem,
  toggleDictItemStatus,
} from '#/api/system';
import type {
  DictTypeDTO,
  DictDataDTO,
} from '#/api/system/types';
import DictTypeModal from './components/DictTypeModal.vue';
import DictItemModal from './components/DictItemModal.vue';

defineOptions({ name: 'SystemDict' });

// ==================== 状态定义 ====================

const loading = ref(false);
const dictTypes = ref<DictTypeDTO[]>([]);
const selectedTypeId = ref<number>();
const selectedType = ref<DictTypeDTO>();
const searchKeyword = ref('');

const typeModalRef = ref<InstanceType<typeof DictTypeModal>>();
const itemModalRef = ref<InstanceType<typeof DictItemModal>>();

// ==================== 计算属性 ====================

// 过滤后的字典类型列表
const filteredDictTypes = computed(() => {
  if (!searchKeyword.value) {
    return dictTypes.value;
  }
  const keyword = searchKeyword.value.toLowerCase();
  return dictTypes.value.filter(
    (type) =>
      type.name.toLowerCase().includes(keyword) ||
      type.code.toLowerCase().includes(keyword) ||
      type.description?.toLowerCase().includes(keyword),
  );
});

// ==================== 字典项表格配置 ====================

const gridColumns: any[] = [
  { type: 'seq', width: 50, title: '序号' },
  { field: 'label', title: '标签', minWidth: 120 },
  { field: 'value', title: '值', minWidth: 120 },
  { field: 'sortOrder', title: '排序', width: 80, align: 'center' },
  {
    field: 'status',
    title: '状态',
    width: 80,
    align: 'center',
    slots: { default: 'status' },
  },
  { field: 'description', title: '描述', minWidth: 150 },
  {
    field: 'action',
    title: '操作',
    width: 180,
    fixed: 'right',
    slots: { default: 'action' },
  },
];

// 加载字典项数据
async function loadDictItems() {
  if (!selectedTypeId.value) {
    return { items: [], total: 0 };
  }
  try {
    const items = await getDictItemsByTypeId(selectedTypeId.value);
    return { items, total: items.length };
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载字典项列表失败');
    return { items: [], total: 0 };
  }
}

const [Grid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    minHeight: 300,
    showOverflow: true,
    border: true,
    stripe: true,
    rowConfig: {
      keyField: 'id',
    },
    pagerConfig: {
      enabled: false,
    },
    proxyConfig: {
      ajax: {
        query: loadDictItems,
      },
    },
  },
});

// ==================== 数据加载 ====================

// 加载字典类型列表
async function fetchDictTypes() {
  loading.value = true;
  try {
    dictTypes.value = await getDictTypeList();
    // 如果有选中的类型，重新选中
    if (selectedTypeId.value) {
      const type = dictTypes.value.find((t) => t.id === selectedTypeId.value);
      if (type) {
        selectedType.value = type;
      } else {
        selectedTypeId.value = undefined;
        selectedType.value = undefined;
      }
    }
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载字典类型列表失败');
  } finally {
    loading.value = false;
  }
}

// ==================== 操作方法 ====================

// 选择字典类型
function handleSelectType(type: DictTypeDTO) {
  selectedTypeId.value = type.id;
  selectedType.value = type;
  gridApi.reload();
}

// 新增字典类型
function handleAddType() {
  typeModalRef.value?.openCreate();
}

// 编辑字典类型
function handleEditType(record: DictTypeDTO) {
  typeModalRef.value?.openEdit(record);
}

// 删除字典类型
function handleDeleteType(record: DictTypeDTO) {
  if (record.isSystem) {
    message.warning('系统内置字典不能删除');
    return;
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除字典类型 "${record.name}" 吗？删除后该类型下的所有字典项也将无法使用。`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteDictType(record.id);
        message.success('删除成功');
        if (selectedTypeId.value === record.id) {
          selectedTypeId.value = undefined;
          selectedType.value = undefined;
        }
        fetchDictTypes();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

// 新增字典项
function handleAddItem() {
  if (!selectedType.value) {
    message.warning('请先选择字典类型');
    return;
  }
  itemModalRef.value?.openCreate(selectedType.value.id);
}

// 编辑字典项
function handleEditItem(record: DictDataDTO) {
  itemModalRef.value?.openEdit(record);
}

// 删除字典项
function handleDeleteItem(record: DictDataDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除字典项 "${record.label}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteDictItem(record.id);
        message.success('删除成功');
        gridApi.reload();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

// 启用/禁用字典项
async function handleToggleItemStatus(record: DictDataDTO) {
  try {
    await toggleDictItemStatus(record.id);
    message.success(record.status === 'ENABLED' ? '已禁用' : '已启用');
    gridApi.reload();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

// 弹窗成功回调
function handleTypeModalSuccess() {
  fetchDictTypes();
}

function handleItemModalSuccess() {
  gridApi.reload();
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchDictTypes();
});
</script>

<template>
  <Page title="字典管理" description="管理系统数据字典，字典编码用于在代码中获取对应的选项列表">
    <Row :gutter="16">
      <!-- 左侧：字典类型列表 -->
      <Col :xs="24" :sm="24" :md="8" :lg="6" :xl="5">
        <Card :bordered="false" :body-style="{ padding: '12px' }">
          <template #title>
            <span style="font-size: 14px">字典类型</span>
          </template>
          <template #extra>
            <Button type="primary" size="small" @click="handleAddType">
              <template #icon><Plus class="size-3" /></template>
              新增
            </Button>
          </template>

          <!-- 搜索框 -->
          <Input
            v-model:value="searchKeyword"
            placeholder="搜索名称或编码"
            style="margin-bottom: 12px"
            allow-clear
            size="small"
          />

          <!-- 字典类型列表 -->
          <div class="dict-type-list">
            <List
              :loading="loading"
              :data-source="filteredDictTypes"
              size="small"
              :split="false"
            >
              <template #renderItem="{ item }">
                <ListItem
                  :class="['dict-type-item', { 'selected': item.id === selectedTypeId }]"
                  @click="handleSelectType(item)"
                >
                  <ListItemMeta>
                    <template #title>
                      <div class="dict-type-title">
                        <span>{{ item.name }}</span>
                        <Tag v-if="item.isSystem" color="blue" class="system-tag">系统</Tag>
                      </div>
                    </template>
                    <template #description>
                      <Tooltip :title="`编码: ${item.code}`">
                        <span class="dict-type-code">{{ item.code }}</span>
                      </Tooltip>
                    </template>
                  </ListItemMeta>
                  <template #actions>
                    <Space size="small">
                      <Tooltip title="编辑">
                        <Button
                          type="text"
                          size="small"
                          @click.stop="handleEditType(item)"
                        >
                          <EditOutlined class="size-3" />
                        </Button>
                      </Tooltip>
                      <Popconfirm
                        v-if="!item.isSystem"
                        title="确定删除？"
                        @confirm="handleDeleteType(item)"
                      >
                        <Tooltip title="删除">
                          <Button
                            type="text"
                            size="small"
                            danger
                            @click.stop
                          >
                            <DeleteOutlined class="size-3" />
                          </Button>
                        </Tooltip>
                      </Popconfirm>
                    </Space>
                  </template>
                </ListItem>
              </template>
            </List>
            <Empty v-if="!loading && filteredDictTypes.length === 0" description="暂无字典类型" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </Card>
      </Col>

      <!-- 右侧：字典项列表 -->
      <Col :xs="24" :sm="24" :md="16" :lg="18" :xl="19">
        <Card :bordered="false">
          <!-- 选中的字典类型信息 -->
          <template v-if="selectedType">
            <Descriptions :column="{ xs: 1, sm: 2, md: 4 }" size="small" style="margin-bottom: 16px">
              <DescriptionsItem label="字典名称">{{ selectedType.name }}</DescriptionsItem>
              <DescriptionsItem label="字典编码">
                <Tag color="processing">{{ selectedType.code }}</Tag>
              </DescriptionsItem>
              <DescriptionsItem label="状态">
                <Tag :color="selectedType.status === 'ENABLED' ? 'success' : 'error'">
                  {{ selectedType.status === 'ENABLED' ? '启用' : '禁用' }}
                </Tag>
              </DescriptionsItem>
              <DescriptionsItem label="系统内置">
                <Tag :color="selectedType.isSystem ? 'blue' : 'default'">
                  {{ selectedType.isSystem ? '是' : '否' }}
                </Tag>
              </DescriptionsItem>
              <DescriptionsItem v-if="selectedType.description" label="描述" :span="4">
                {{ selectedType.description }}
              </DescriptionsItem>
            </Descriptions>

            <!-- 字典项工具栏 -->
            <div class="toolbar">
              <span class="toolbar-title">字典项列表</span>
              <Button type="primary" size="small" @click="handleAddItem">
                <template #icon><Plus class="size-3" /></template>
                新增字典项
              </Button>
            </div>

            <!-- 字典项表格 -->
            <Grid class="dict-item-grid">
              <template #status="{ row }">
                <Tag :color="row.status === 'ENABLED' ? 'success' : 'error'">
                  {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
                </Tag>
              </template>
              <template #action="{ row }">
                <Space size="small">
                  <a @click="handleEditItem(row)">编辑</a>
                  <a 
                    :class="row.status === 'ENABLED' ? 'danger-link' : 'success-link'"
                    @click="handleToggleItemStatus(row)"
                  >
                    {{ row.status === 'ENABLED' ? '禁用' : '启用' }}
                  </a>
                  <Popconfirm
                    title="确定删除该字典项？"
                    @confirm="handleDeleteItem(row)"
                  >
                    <a class="danger-link">删除</a>
                  </Popconfirm>
                </Space>
              </template>
            </Grid>
          </template>

          <!-- 未选中状态 -->
          <template v-else>
            <Empty
              description="请从左侧选择字典类型"
              style="padding: 100px 0"
            />
          </template>
        </Card>
      </Col>
    </Row>

    <!-- 字典类型弹窗 -->
    <DictTypeModal ref="typeModalRef" @success="handleTypeModalSuccess" />

    <!-- 字典项弹窗 -->
    <DictItemModal ref="itemModalRef" @success="handleItemModalSuccess" />
  </Page>
</template>

<style scoped>
.dict-type-list {
  max-height: calc(100vh - 280px);
  overflow-y: auto;
}

.dict-type-item {
  cursor: pointer;
  padding: 8px 12px !important;
  margin-bottom: 4px;
  border-radius: 6px;
  transition: all 0.2s;
}

.dict-type-item:hover {
  background-color: var(--ant-primary-1, #e6f7ff);
}

.dict-type-item.selected {
  background-color: var(--ant-primary-1, #e6f7ff);
  border-left: 3px solid var(--ant-primary-color, #1890ff);
}

.dict-type-title {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.system-tag {
  margin-left: 4px;
  font-size: 10px;
  line-height: 16px;
  padding: 0 4px;
}

.dict-type-code {
  font-size: 12px;
  color: #999;
  font-family: 'Monaco', 'Menlo', monospace;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-title {
  font-weight: 500;
  font-size: 14px;
}

.dict-item-grid {
  min-height: 300px;
}

.danger-link {
  color: #ff4d4f;
}

.danger-link:hover {
  color: #ff7875;
}

.success-link {
  color: #52c41a;
}

.success-link:hover {
  color: #73d13d;
}

:deep(.ant-list-item-meta-title) {
  margin-bottom: 0 !important;
}

:deep(.ant-list-item-meta-description) {
  margin-top: 2px;
}

:deep(.ant-list-item-action) {
  margin-left: 8px !important;
}
</style>
