<script setup lang="ts">
import type { CauseTreeNodeDTO } from '#/api/system/types';

import { computed, onMounted, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import {
  BookOutlined,
  FileProtectOutlined,
  WarningOutlined,
} from '@vben/icons';

import {
  Card,
  Col,
  Empty,
  Input,
  message,
  Row,
  Space,
  Spin,
  Tag,
  Tree,
  Button,
} from 'ant-design-vue';

import { useResponsive } from '#/hooks/useResponsive';

import {
  getCauseName,
  getCauseTree,
  getCauseTypeOptions,
  searchCauses,
  type CauseType,
} from '#/api/system/cause-of-action';

defineOptions({ name: 'SystemCauseOfAction' });

// 响应式布局
const { isMobile } = useResponsive();

// ==================== 状态定义 ====================

const loading = ref(false);
const searchLoading = ref(false);
const selectedType = ref<CauseType>('CIVIL');
const causeTree = ref<CauseTreeNodeDTO[]>([]);
const searchKeyword = ref('');
const searchResults = ref<CauseTreeNodeDTO[]>([]);
const showSearchResults = ref(false);
const expandedKeys = ref<string[]>([]);
const selectedKeys = ref<string[]>([]);
const selectedNode = ref<CauseTreeNodeDTO>();
const isAllExpanded = ref(true); // 默认展开

// 案由类型选项
const causeTypeOptions = getCauseTypeOptions();

// 获取当前选中类型的图标
const currentIcon = computed(() => {
  const iconMap = {
    CIVIL: BookOutlined,
    CRIMINAL: WarningOutlined,
    ADMIN: FileProtectOutlined,
  };
  return iconMap[selectedType.value];
});

// 获取当前选中类型的标签
const currentTypeLabel = computed(() => {
  return causeTypeOptions.find((t) => t.value === selectedType.value)?.label || '';
});

// ==================== 树形数据转换 ====================

/**
 * 将后端树形数据转换为 ant-design-vue Tree 组件所需格式
 */
function transformTreeData(nodes: CauseTreeNodeDTO[]): any[] {
  return nodes.map((node) => ({
    key: node.code,
    title: node.name,
    children: node.children ? transformTreeData(node.children) : undefined,
    isLeaf: node.isLeaf,
    ...node,
  }));
}

// 树形数据
const treeData = computed(() => transformTreeData(causeTree.value));

// 搜索结果树形数据
const searchTreeData = computed(() => transformTreeData(searchResults.value));

// ==================== 数据加载 ====================

/**
 * 加载指定类型的案由树
 */
async function fetchCauseTree() {
  loading.value = true;
  showSearchResults.value = false;
  searchResults.value = [];
  selectedKeys.value = [];
  selectedNode.value = undefined;

  try {
    const data = await getCauseTree(selectedType.value);
    causeTree.value = data;
    // 默认展开所有节点
    expandAllNodes(data);
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载案由数据失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 递归展开所有节点
 */
function expandAllNodes(nodes: CauseTreeNodeDTO[]) {
  const keys: string[] = [];
  function collectKeys(nodeList: CauseTreeNodeDTO[]) {
    nodeList.forEach((node) => {
      keys.push(node.code);
      if (node.children && node.children.length > 0) {
        collectKeys(node.children);
      }
    });
  }
  collectKeys(nodes);
  expandedKeys.value = keys;
  isAllExpanded.value = true;
}

/**
 * 折叠所有节点
 */
function collapseAllNodes() {
  expandedKeys.value = [];
  isAllExpanded.value = false;
}

/**
 * 切换展开/折叠所有节点
 */
function toggleExpandAll() {
  if (isAllExpanded.value) {
    collapseAllNodes();
  } else {
    expandAllNodes(causeTree.value);
  }
}

/**
 * 搜索案由
 */
async function handleSearch() {
  const keyword = searchKeyword.value.trim();
  if (!keyword) {
    showSearchResults.value = false;
    searchResults.value = [];
    return;
  }

  searchLoading.value = true;
  showSearchResults.value = true;

  try {
    const data = await searchCauses(selectedType.value, keyword);
    // 将搜索结果转换为树形结构
    searchResults.value = buildSearchTree(data, keyword);
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '搜索案由失败');
  } finally {
    searchLoading.value = false;
  }
}

/**
 * 将搜索结果构建为树形结构（保留父级路径）
 */
function buildSearchTree(
  results: any[],
  keyword: string,
): CauseTreeNodeDTO[] {
  // 简化处理：直接展示搜索结果作为扁平列表的树形结构
  return results.map((item) => ({
    code: item.code,
    name: item.name,
    parentCode: item.parentCode,
    level: item.level,
    isLeaf: true,
    children: undefined,
  }));
}

// ==================== 节点操作 ====================

/**
 * 选中节点
 */
function handleSelect(keys: string[], info: any) {
  if (keys.length > 0) {
    selectedNode.value = info.node;
  } else {
    selectedNode.value = undefined;
  }
}

/**
 * 展开/收起节点
 */
function handleExpand(keys: string[]) {
  expandedKeys.value = keys;
}

/**
 * 切换案由类型
 */
function handleTypeChange(type: CauseType) {
  selectedType.value = type;
  searchKeyword.value = '';
  showSearchResults.value = false;
  fetchCauseTree();
}

/**
 * 清空搜索
 */
function clearSearch() {
  searchKeyword.value = '';
  showSearchResults.value = false;
  searchResults.value = [];
}

// ==================== 获取案由详情 ====================

/**
 * 获取层级标签颜色
 */
function getLevelColor(level: number): string {
  const colorMap: Record<number, string> = {
    1: 'red',
    2: 'orange',
    3: 'blue',
    4: 'green',
    5: 'cyan',
  };
  return colorMap[level] || 'default';
}

/**
 * 获取类型标签颜色
 */
function getTypeColor(type: CauseType): string {
  const colorMap: Record<CauseType, string> = {
    CIVIL: 'blue',
    CRIMINAL: 'red',
    ADMIN: 'orange',
  };
  return colorMap[type];
}

/**
 * 根据代码获取完整案由名称
 */
async function fetchCauseName(code: string) {
  try {
    const name = await getCauseName(code, selectedType.value);
    return name;
  } catch (error) {
    return '';
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchCauseTree();
});
</script>

<template>
  <Page
    :title="`${currentTypeLabel}管理`"
    description="管理系统中的案由数据，包括民事案由、刑事罪名和行政案由"
  >
    <!-- 类型切换和搜索栏 -->
    <Row :gutter="16" style="margin-bottom: 16px">
      <!-- 案由类型切换 -->
      <Col :xs="24" :sm="12" :md="8" :lg="6">
        <div class="type-switcher">
          <span class="type-label">案由类型：</span>
          <Space size="small">
            <Tag
              v-for="option in causeTypeOptions"
              :key="option.value"
              :color="selectedType === option.value ? 'blue' : 'default'"
              :style="{ cursor: 'pointer' }"
              @click="handleTypeChange(option.value)"
            >
              <component :is="option.value === 'CIVIL' ? BookOutlined : option.value === 'CRIMINAL' ? WarningOutlined : FileProtectOutlined" class="size-3" />
              {{ option.label }}
            </Tag>
          </Space>
        </div>
      </Col>

      <!-- 搜索框 -->
      <Col :xs="24" :sm="12" :md="16" :lg="18">
        <Input
          v-model:value="searchKeyword"
          :placeholder="`搜索${currentTypeLabel}...`"
          allow-clear
          :loading="searchLoading"
          @input="handleSearch"
          @clear="clearSearch"
        >
          <template #prefix>
            <span style="color: #999">🔍</span>
          </template>
        </Input>
      </Col>
    </Row>

    <!-- 主内容区域 -->
    <Row :gutter="16">
      <!-- 左侧：案由树 -->
      <Col :xs="24" :sm="24" :md="12" :lg="10" :xl="8">
        <Card :bordered="false" :body-style="{ padding: '12px' }">
          <template #title>
            <div class="card-title">
              <component :is="currentIcon" class="size-4" style="margin-right: 8px" />
              <span>{{ currentTypeLabel }}目录</span>
            </div>
          </template>
          <template #extra>
            <Button
              v-if="!showSearchResults"
              size="small"
              @click="toggleExpandAll"
            >
              {{ isAllExpanded ? '全部折叠' : '全部展开' }}
            </Button>
          </template>

          <Spin :spinning="loading">
            <!-- 搜索结果 -->
            <div v-if="showSearchResults" class="search-results">
              <div v-if="searchResults.length > 0" class="result-count">
                找到 {{ searchResults.length }} 个相关案由
              </div>
              <Tree
                :tree-data="searchTreeData"
                :expanded-keys="expandedKeys"
                :selected-keys="selectedKeys"
                show-icon
                @select="handleSelect"
                @expand="handleExpand"
              >
                <template #icon>
                  <component :is="currentIcon" class="size-3" />
                </template>
              </Tree>
              <Empty v-if="!searchLoading && searchResults.length === 0" description="未找到相关案由" />
            </div>

            <!-- 完整案由树 -->
            <div v-else class="cause-tree">
              <Tree
                :tree-data="treeData"
                :expanded-keys="expandedKeys"
                :selected-keys="selectedKeys"
                show-icon
                show-line
                @select="handleSelect"
                @expand="handleExpand"
              >
                <template #icon="{ dataRef }">
                  <component
                    :is="dataRef?.isLeaf ? currentIcon : 'FolderOutlined'"
                    class="size-3"
                  />
                </template>
              </Tree>
              <Empty v-if="!loading && treeData.length === 0" description="暂无案由数据" />
            </div>
          </Spin>
        </Card>
      </Col>

      <!-- 右侧：案由详情 -->
      <Col :xs="24" :sm="24" :md="12" :lg="14" :xl="16">
        <Card :bordered="false">
          <template #title>案由详情</template>

          <!-- 已选中节点 -->
          <div v-if="selectedNode" class="cause-detail">
            <Row :gutter="16">
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">案由代码：</span>
                  <span class="detail-value code">{{ selectedNode.code }}</span>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">层级：</span>
                  <Tag :color="getLevelColor(selectedNode.level)">
                    第 {{ selectedNode.level }} 级
                  </Tag>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">案由名称：</span>
                  <span class="detail-value name">{{ selectedNode.name }}</span>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">是否叶子节点：</span>
                  <Tag :color="selectedNode.isLeaf ? 'green' : 'orange'">
                    {{ selectedNode.isLeaf ? '是' : '否' }}
                  </Tag>
                </div>
              </Col>
              <Col v-if="selectedNode.parentCode" :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">上级代码：</span>
                  <span class="detail-value">{{ selectedNode.parentCode }}</span>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">类型：</span>
                  <Tag :color="getTypeColor(selectedType)">
                    {{ currentTypeLabel }}
                  </Tag>
                </div>
              </Col>
              <Col v-if="selectedNode.description" :xs="24">
                <div class="detail-item">
                  <span class="detail-label">描述：</span>
                  <span class="detail-value">{{ selectedNode.description }}</span>
                </div>
              </Col>
            </Row>
          </div>

          <!-- 未选中状态 -->
          <Empty
            v-else
            description="请从左侧选择案由查看详情"
            :image="Empty.PRESENTED_IMAGE_SIMPLE"
            style="padding: 60px 0"
          />
        </Card>
      </Col>
    </Row>
  </Page>
</template>

<style scoped>
.type-switcher {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: var(--vben-background-color);
  border-radius: 6px;
}

.type-label {
  font-size: 14px;
  font-weight: 500;
  margin-right: 8px;
  white-space: nowrap;
}

.card-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
}

.cause-tree,
.search-results {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.result-count {
  padding: 8px 12px;
  margin-bottom: 12px;
  background: var(--ant-primary-1, #e6f7ff);
  border-radius: 4px;
  font-size: 13px;
  color: var(--ant-primary-color, #1890ff);
}

.cause-detail {
  padding: 8px 0;
}

.detail-item {
  margin-bottom: 16px;
}

.detail-label {
  color: #666;
  font-size: 13px;
}

.detail-value {
  margin-left: 8px;
  font-size: 14px;
  color: #333;
}

.detail-value.code {
  font-family: Monaco, Menlo, monospace;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
}

.detail-value.name {
  font-weight: 500;
  color: #1890ff;
}

:deep(.ant-tree) {
  background: transparent;
}

:deep(.ant-tree-node-content-wrapper) {
  border-radius: 4px;
}

:deep(.ant-tree-node-content-wrapper:hover) {
  background-color: var(--ant-primary-1, #e6f7ff);
}

:deep(.ant-tree-node-selected .ant-tree-node-content-wrapper) {
  background-color: var(--ant-primary-2, #bae7ff) !important;
}

:deep(.ant-tree-switcher) {
  width: 20px;
}

:deep(.ant-tree-icon-el) {
  vertical-align: middle;
}
</style>
