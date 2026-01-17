<script setup lang="ts">
/**
 * 证据清单选择器组件
 * 用于管理案件的多个证据清单（一审清单、二审补充清单等）
 */
import { ref, watch, computed } from 'vue';
import {
  Button,
  Empty,
  message,
  Modal,
  Form,
  FormItem,
  Input,
  Popconfirm,
  Select,
  SelectOption,
  Space,
  Spin,
  Tag,
} from 'ant-design-vue';
import { Plus, Trash, RotateCw } from '@vben/icons';
import type { EvidenceListDTO, EvidenceDTO } from '#/api/evidence';
import {
  getEvidenceListsByMatter,
  createEvidenceList,
  deleteEvidenceList,
  compareEvidenceLists,
  EVIDENCE_LIST_TYPE_OPTIONS,
} from '#/api/evidence';

const props = defineProps<{
  matterId: number;
  evidences: EvidenceDTO[];
  readonly?: boolean;
}>();

const emit = defineEmits<{
  (e: 'select', list: EvidenceListDTO | null): void;
  (e: 'refresh'): void;
}>();

// 状态
const loading = ref(false);
const lists = ref<EvidenceListDTO[]>([]);
const selectedListId = ref<number | null>(null);
const showCreateModal = ref(false);
const showCompareModal = ref(false);
const createForm = ref({
  name: '',
  listType: 'SUBMISSION' as string,
  evidenceIds: [] as number[],
});
const compareForm = ref({
  listId1: undefined as number | undefined,
  listId2: undefined as number | undefined,
});
const compareResult = ref<any>(null);
const compareLoading = ref(false);

// 计算当前选中的清单
const selectedList = computed(() => {
  if (!selectedListId.value) return null;
  return lists.value.find((l) => l.id === selectedListId.value) || null;
});

// 加载证据清单列表
async function loadLists() {
  if (!props.matterId) return;

  loading.value = true;
  try {
    lists.value = await getEvidenceListsByMatter(props.matterId);
  } catch (error: any) {
    console.error('加载证据清单失败:', error);
  } finally {
    loading.value = false;
  }
}

// 选择清单
function handleSelectList(id: number | null) {
  selectedListId.value = id;
  emit('select', id ? lists.value.find((l) => l.id === id) || null : null);
}

// 打开创建弹窗
function openCreateModal() {
  createForm.value = {
    name: '',
    listType: 'SUBMISSION',
    evidenceIds: props.evidences.map((e) => e.id),
  };
  showCreateModal.value = true;
}

// 创建清单
async function handleCreate() {
  if (!createForm.value.name.trim()) {
    message.warning('请输入清单名称');
    return;
  }

  try {
    await createEvidenceList({
      matterId: props.matterId,
      name: createForm.value.name,
      listType: createForm.value.listType,
      evidenceIds: createForm.value.evidenceIds,
    });
    message.success('创建成功');
    showCreateModal.value = false;
    loadLists();
    emit('refresh');
  } catch (error: any) {
    message.error(error.message || '创建失败');
  }
}

// 删除清单
async function handleDelete(id: number) {
  try {
    await deleteEvidenceList(id);
    message.success('删除成功');
    if (selectedListId.value === id) {
      selectedListId.value = null;
      emit('select', null);
    }
    loadLists();
    emit('refresh');
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

// 打开对比弹窗
function openCompareModal() {
  if (lists.value.length < 2) {
    message.warning('至少需要两个清单才能进行对比');
    return;
  }
  compareForm.value = {
    listId1: lists.value[0]?.id,
    listId2: lists.value[1]?.id,
  };
  compareResult.value = null;
  showCompareModal.value = true;
}

// 执行对比
async function handleCompare() {
  if (!compareForm.value.listId1 || !compareForm.value.listId2) {
    message.warning('请选择要对比的两个清单');
    return;
  }

  compareLoading.value = true;
  try {
    compareResult.value = await compareEvidenceLists(
      compareForm.value.listId1,
      compareForm.value.listId2,
    );
  } catch (error: any) {
    message.error(error.message || '对比失败');
  } finally {
    compareLoading.value = false;
  }
}

// 获取类型标签颜色
function getTypeColor(type?: string) {
  switch (type) {
    case 'SUBMISSION':
      return 'blue';
    case 'EXCHANGE':
      return 'green';
    case 'COURT':
      return 'orange';
    default:
      return 'default';
  }
}

// 获取类型名称
function getTypeName(type?: string) {
  return (
    EVIDENCE_LIST_TYPE_OPTIONS.find((o) => o.value === type)?.label ||
    type ||
    '未分类'
  );
}

// 监听 matterId 变化
watch(
  () => props.matterId,
  () => {
    if (props.matterId) {
      loadLists();
    }
  },
  { immediate: true },
);

// 暴露方法
defineExpose({
  refresh: loadLists,
});
</script>

<template>
  <div class="evidence-list-selector">
    <Spin :spinning="loading">
      <!-- 头部操作区 -->
      <div class="selector-header">
        <div class="header-left">
          <span class="title">📋 证据清单</span>
          <Tag v-if="lists.length > 0" color="blue">{{ lists.length }} 个</Tag>
        </div>
        <Space v-if="!readonly">
          <Button type="primary" size="small" @click="openCreateModal">
            <Plus class="mr-1 h-4 w-4" /> 新建清单
          </Button>
          <Button
            size="small"
            @click="openCompareModal"
            :disabled="lists.length < 2"
          >
            <RotateCw class="mr-1 h-4 w-4" /> 对比清单
          </Button>
        </Space>
      </div>

      <!-- 清单列表 -->
      <div v-if="lists.length > 0" class="list-container">
        <div
          v-for="list in lists"
          :key="list.id"
          :class="['list-item', { active: selectedListId === list.id }]"
          @click="handleSelectList(list.id)"
        >
          <div class="list-info">
            <div class="list-name">
              <span>{{ list.name }}</span>
              <Tag :color="getTypeColor(list.listType)" size="small">
                {{ getTypeName(list.listType) }}
              </Tag>
            </div>
            <div class="list-meta">
              <span>编号: {{ list.listNo }}</span>
              <span>证据: {{ list.evidenceIdList?.length || 0 }} 项</span>
              <span>{{ list.createdAt?.slice(0, 10) }}</span>
            </div>
          </div>
          <div v-if="!readonly" class="list-actions" @click.stop>
            <Popconfirm
              title="确定删除此清单吗？"
              ok-text="删除"
              cancel-text="取消"
              @confirm="handleDelete(list.id)"
            >
              <Button type="text" size="small" danger>
                <Trash class="h-4 w-4" />
              </Button>
            </Popconfirm>
          </div>
        </div>
      </div>
      <Empty v-else description="暂无证据清单，点击上方按钮创建" />

      <!-- 当前选中提示 -->
      <div v-if="selectedList" class="selected-hint">
        <Tag color="processing">当前编辑: {{ selectedList.name }}</Tag>
        <Button type="link" size="small" @click="handleSelectList(null)">
          取消选择
        </Button>
      </div>
    </Spin>

    <!-- 创建清单弹窗 -->
    <Modal
      v-model:open="showCreateModal"
      title="新建证据清单"
      @ok="handleCreate"
      :width="500"
    >
      <Form layout="vertical" :model="createForm">
        <FormItem label="清单名称" required>
          <Input
            v-model:value="createForm.name"
            placeholder="如：一审原告证据清单、二审补充证据"
          />
        </FormItem>
        <FormItem label="清单类型">
          <Select v-model:value="createForm.listType">
            <SelectOption
              v-for="opt in EVIDENCE_LIST_TYPE_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </SelectOption>
          </Select>
        </FormItem>
        <FormItem label="包含证据">
          <div class="evidence-count-hint">
            将包含当前 {{ createForm.evidenceIds.length }} 条证据
          </div>
        </FormItem>
      </Form>
    </Modal>

    <!-- 对比清单弹窗 -->
    <Modal
      v-model:open="showCompareModal"
      title="对比证据清单"
      :width="700"
      :footer="null"
    >
      <div class="compare-container">
        <div class="compare-selects">
          <Select
            v-model:value="compareForm.listId1"
            placeholder="选择清单1"
            style="width: 200px"
          >
            <SelectOption v-for="list in lists" :key="list.id" :value="list.id">
              {{ list.name }}
            </SelectOption>
          </Select>
          <span class="vs-text">VS</span>
          <Select
            v-model:value="compareForm.listId2"
            placeholder="选择清单2"
            style="width: 200px"
          >
            <SelectOption v-for="list in lists" :key="list.id" :value="list.id">
              {{ list.name }}
            </SelectOption>
          </Select>
          <Button
            type="primary"
            @click="handleCompare"
            :loading="compareLoading"
          >
            开始对比
          </Button>
        </div>

        <div v-if="compareResult" class="compare-result">
          <div class="result-section">
            <Tag color="green"
              >新增 {{ compareResult.addedIds?.length || 0 }} 项</Tag
            >
            <div
              v-if="compareResult.addedEvidences?.length"
              class="evidence-list"
            >
              <div
                v-for="e in compareResult.addedEvidences"
                :key="e.id"
                class="evidence-item"
              >
                {{ e.name }}
              </div>
            </div>
          </div>
          <div class="result-section">
            <Tag color="red"
              >删除 {{ compareResult.removedIds?.length || 0 }} 项</Tag
            >
            <div
              v-if="compareResult.removedEvidences?.length"
              class="evidence-list"
            >
              <div
                v-for="e in compareResult.removedEvidences"
                :key="e.id"
                class="evidence-item"
              >
                {{ e.name }}
              </div>
            </div>
          </div>
          <div class="result-section">
            <Tag color="blue"
              >共同 {{ compareResult.commonIds?.length || 0 }} 项</Tag
            >
          </div>
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.evidence-list-selector {
  padding: 12px;
  margin-bottom: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.selector-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.header-left {
  display: flex;
  gap: 8px;
  align-items: center;
}

.title {
  font-size: 14px;
  font-weight: 600;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  cursor: pointer;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  transition: all 0.2s;
}

.list-item:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgb(24 144 255 / 10%);
}

.list-item.active {
  background: #e6f7ff;
  border-color: #1890ff;
}

.list-info {
  flex: 1;
}

.list-name {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
  font-weight: 500;
}

.list-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.selected-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin-top: 12px;
  background: #e6f7ff;
  border-radius: 4px;
}

.evidence-count-hint {
  padding: 8px 12px;
  color: #666;
  background: #f5f5f5;
  border-radius: 4px;
}

.compare-container {
  padding: 16px 0;
}

.compare-selects {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}

.vs-text {
  font-weight: bold;
  color: #999;
}

.compare-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-section {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}

.evidence-list {
  padding-left: 12px;
  margin-top: 8px;
}

.evidence-item {
  padding: 4px 0;
  font-size: 13px;
  color: #666;
}
</style>
