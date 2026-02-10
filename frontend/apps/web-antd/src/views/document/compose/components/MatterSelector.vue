<script setup lang="ts">
import type { MatterDossierItem } from '../types';

import type { MatterDTO } from '#/api/matter/types';

/**
 * 项目选择器组件
 * 用于选择关联项目和卷宗目录
 * 支持远程搜索大量项目
 */
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';

import {
  Alert,
  Card,
  Col,
  message,
  Row,
  Select,
  Space,
  Spin,
  Switch,
  Tag,
} from 'ant-design-vue';

import {
  getMatterDossierItems,
  initMatterDossier,
} from '#/api/document/dossier';
import { getMyMatters } from '#/api/matter';

defineOptions({ name: 'MatterSelector' });

const props = withDefaults(
  defineProps<{
    /** 选中的卷宗目录 ID */
    dossierId?: number;
    /** 是否个人文书 */
    isPersonalDoc?: boolean;
    /** 选中的项目 ID */
    modelValue?: number;
    /** 是否显示卷宗目录选择 */
    showDossier?: boolean;
    /** 是否显示项目信息卡片 */
    showMatterCard?: boolean;
  }>(),
  {
    modelValue: undefined,
    isPersonalDoc: false,
    dossierId: undefined,
    showDossier: true,
    showMatterCard: true,
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: number | undefined): void;
  (e: 'update:isPersonalDoc', value: boolean): void;
  (e: 'update:dossierId', value: number | undefined): void;
  (e: 'matterChange', matter: MatterDTO | null): void;
  (e: 'dossierLoaded', items: MatterDossierItem[]): void;
}>();

// 状态
const matters = ref<MatterDTO[]>([]);
const dossierItems = ref<MatterDossierItem[]>([]);
const loading = ref(false);
const dossierLoading = ref(false);
const searchKeyword = ref('');
const searching = ref(false);
const totalMatters = ref(0);
const currentPage = ref(1);
const pageSize = 20;

// 计算属性
const selectedMatter = computed(() => {
  return matters.value.find((m) => m.id === props.modelValue) || null;
});

const matterOptions = computed(() => {
  return matters.value.map((m) => ({
    label: `[${m.matterNo}] ${m.name}`,
    value: m.id,
    matter: m, // 保存完整对象用于显示详情
  }));
});

const dossierOptions = computed(() => {
  const buildOptions = (parentId: number, level: number = 0): any[] => {
    return dossierItems.value
      .filter((item) => item.parentId === parentId)
      .map((item) => ({
        label: `${'　'.repeat(level)}📁 ${item.name}`,
        value: item.id,
        children: buildOptions(item.id, level + 1),
      }));
  };
  return [{ label: '📂 项目根目录', value: undefined }, ...buildOptions(0)];
});

const hasMoreMatters = computed(() => {
  return matters.value.length < totalMatters.value;
});

// 方法 - 加载项目（支持搜索和分页）
async function loadMatters(
  keyword?: string,
  page: number = 1,
  append: boolean = false,
) {
  if (searching.value) return;

  searching.value = true;
  loading.value = page === 1 && !append;

  try {
    // 智能搜索：判断输入是否像项目编号（数字开头或包含特定格式）
    const isLikelyMatterNo =
      keyword && /^[A-Z0-9-]/.test(keyword.toUpperCase());

    const res = await getMyMatters({
      pageNum: page,
      pageSize,
      // 按项目名称或编号搜索
      name: keyword || undefined,
      matterNo: isLikelyMatterNo ? keyword : undefined,
    });

    matters.value = append
      ? [...matters.value, ...(res.list || [])]
      : res.list || [];
    totalMatters.value = res.total || 0;
    currentPage.value = page;
  } catch (error: any) {
    message.error(error.message || '加载项目失败');
  } finally {
    loading.value = false;
    searching.value = false;
  }
}

// 远程搜索
let searchTimer: null | ReturnType<typeof setTimeout> = null;
function handleSearch(keyword: string) {
  searchKeyword.value = keyword;

  // 防抖处理
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
  searchTimer = setTimeout(() => {
    loadMatters(keyword, 1, false);
  }, 300);
}

// 加载更多
function handleLoadMore() {
  if (hasMoreMatters.value && !searching.value) {
    loadMatters(searchKeyword.value, currentPage.value + 1, true);
  }
}

// 下拉框滚动到底部加载更多
function handlePopupScroll(e: Event) {
  const target = e.target as HTMLElement;
  if (target.scrollTop + target.clientHeight >= target.scrollHeight - 50) {
    handleLoadMore();
  }
}

async function loadDossierItems(matterId: number) {
  dossierLoading.value = true;
  dossierItems.value = [];

  try {
    let items = await getMatterDossierItems(matterId);

    // 如果没有卷宗目录，尝试初始化
    if (!items || items.length === 0) {
      items = await initMatterDossier(matterId);
    }

    // 过滤出文件夹类型
    dossierItems.value = ((items || []) as MatterDossierItem[]).filter(
      (item) => item.itemType === 'FOLDER',
    );
    emit('dossierLoaded', dossierItems.value);
  } catch (error: any) {
    console.error('[MatterSelector] 加载卷宗目录失败:', error);
    message.error(error.message || '加载卷宗目录失败');
  } finally {
    dossierLoading.value = false;
  }
}

function handleMatterChange(value: any) {
  const numValue = typeof value === 'number' ? value : undefined;
  emit('update:modelValue', numValue);
  emit('update:dossierId', undefined);
  dossierItems.value = [];

  if (numValue) {
    loadDossierItems(numValue);
  }

  const matter = matters.value.find((m) => m.id === numValue) || null;
  emit('matterChange', matter);
}

function handlePersonalDocChange(checked: boolean | number | string) {
  const value = Boolean(checked);
  emit('update:isPersonalDoc', value);
  if (value) {
    emit('update:modelValue', undefined);
    emit('update:dossierId', undefined);
    dossierItems.value = [];
    emit('matterChange', null);
  }
}

function handleDossierChange(value: any) {
  emit('update:dossierId', value);
}

// 监听
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal && dossierItems.value.length === 0) {
      loadDossierItems(newVal);
    }
  },
);

onMounted(() => {
  loadMatters();
});

// 组件卸载时清理定时器
onUnmounted(() => {
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }
});

// 暴露方法
defineExpose({
  loadMatters,
  selectedMatter,
});
</script>

<template>
  <div class="matter-selector">
    <!-- 个人文书开关 -->
    <div class="personal-doc-switch">
      <Space align="center">
        <span>个人文书（不关联项目）：</span>
        <Switch :checked="isPersonalDoc" @change="handlePersonalDocChange" />
      </Space>
      <p class="hint-text">
        开启后，文书将保存到"我的文书"中，不会关联到任何项目
      </p>
    </div>

    <!-- 项目选择 -->
    <template v-if="!isPersonalDoc">
      <h4 class="section-title">📁 选择关联项目</h4>

      <!-- 增强的项目选择器：支持远程搜索 -->
      <Select
        :value="modelValue"
        placeholder="输入项目编号或名称搜索..."
        style="width: 100%"
        allow-clear
        show-search
        :filter-option="false"
        :options="matterOptions"
        :loading="loading"
        :not-found-content="searching ? '搜索中...' : '未找到匹配项目'"
        @search="handleSearch"
        @change="handleMatterChange"
        @popup-scroll="handlePopupScroll"
      >
        <template #suffixIcon>
          <span>🔍</span>
        </template>
        <template #dropdownRender="{ menuNode }">
          <div>
            <component :is="menuNode" />
            <div v-if="hasMoreMatters" class="load-more-hint">
              <Spin v-if="searching" size="small" />
              <span v-else class="load-more-text">
                已加载 {{ matters.length }} /
                {{ totalMatters }} 条，滚动加载更多
              </span>
            </div>
          </div>
        </template>
      </Select>

      <!-- 搜索提示 -->
      <p class="search-hint">
        <Tag color="blue">{{ totalMatters }}</Tag>
        个可选项目，支持按编号/名称/客户搜索
      </p>

      <!-- 项目信息卡片 -->
      <Card v-if="showMatterCard && selectedMatter" class="matter-card">
        <Row :gutter="16">
          <Col :span="18">
            <p class="matter-name">
              <strong>{{ selectedMatter.name }}</strong>
            </p>
            <p class="matter-info">
              <Tag size="small">{{ selectedMatter.matterNo }}</Tag>
              <span>客户：{{ selectedMatter.clientName || '-' }}</span>
              <span style="margin-left: 12px"
                >类型：{{
                  selectedMatter.matterTypeName ||
                  selectedMatter.matterType ||
                  '-'
                }}</span
              >
            </p>
          </Col>
          <Col :span="6" style="text-align: right">
            <Tag
              :color="selectedMatter.status === 'ACTIVE' ? 'green' : 'default'"
            >
              {{ selectedMatter.statusName || selectedMatter.status }}
            </Tag>
          </Col>
        </Row>
      </Card>

      <!-- 卷宗目录选择 -->
      <template v-if="showDossier && modelValue">
        <h4 class="section-title" style="margin-top: 16px">
          📂 保存到卷宗目录
        </h4>
        <Spin :spinning="dossierLoading">
          <Select
            :value="dossierId"
            placeholder="选择卷宗目录（可选）"
            style="width: 100%"
            allow-clear
            :options="dossierOptions"
            :disabled="dossierLoading"
            @change="handleDossierChange"
          >
            <template #notFoundContent>
              <span v-if="dossierLoading">加载中...</span>
              <span v-else>该项目暂无卷宗目录</span>
            </template>
          </Select>
        </Spin>
        <p class="hint-text">
          {{
            dossierItems.length > 0
              ? `共 ${dossierItems.length} 个目录可选`
              : '不选择则保存到项目根目录'
          }}
        </p>
      </template>
    </template>

    <!-- 个人文书模式提示 -->
    <Alert
      v-else
      type="info"
      message="个人文书模式"
      description="文书将保存到【我的文书】列表中"
      show-icon
      style="margin-top: 24px"
    />
  </div>
</template>

<style scoped>
.matter-selector {
  width: 100%;
}

.personal-doc-switch {
  margin-bottom: 24px;
}

.hint-text {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.search-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.section-title {
  margin-bottom: 16px;
}

.matter-card {
  margin-top: 16px;
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7ff 100%);
  border: 1px solid #b7eb8f;
  border-radius: 8px;
}

.matter-card :deep(.ant-card-body) {
  padding: 16px;
}

.matter-name {
  margin: 0 0 8px;
  font-size: 15px;
}

.matter-info {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin: 0;
  font-size: 12px;
  color: #666;
}

.load-more-hint {
  padding: 8px 12px;
  text-align: center;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.load-more-text {
  font-size: 12px;
  color: #999;
}
</style>
