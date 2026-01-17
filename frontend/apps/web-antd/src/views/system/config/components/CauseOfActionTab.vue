<script setup lang="ts">
import type { CauseTreeNodeDTO } from '#/api/system/types';
import type { Key } from 'ant-design-vue/es/_util/type';

import { computed, onMounted, ref } from 'vue';

import {
  BookOutlined,
  DeleteOutlined,
  EditOutlined,
  FileProtectOutlined,
  FolderOutlined,
  Plus,
  WarningOutlined,
} from '@vben/icons';

import {
  Button,
  Card,
  Col,
  Dropdown,
  Empty,
  Input,
  Menu,
  MenuItem,
  message,
  Popconfirm,
  Row,
  Space,
  Spin,
  Tag,
  Tree,
} from 'ant-design-vue';

import { usePermission } from '#/hooks/usePermission';

import {
  deleteCause,
  getCauseById,
  getCauseTree,
  getCauseTypeOptions,
  searchCauses,
  toggleCauseStatus,
  type CauseType,
} from '#/api/system/cause-of-action';

import CauseModal from './CauseModal.vue';

// 权限控制
// 能看到系统配置页面的用户默认有案由管理权限
const { hasPermission } = usePermission();
// 检查是否有系统配置权限（sys:config:list），有则可以管理案由
const hasSystemConfigAccess = computed(() => hasPermission('sys:config:list'));
const canCreate = computed(
  () => hasSystemConfigAccess.value || hasPermission('system:cause:create'),
);
const canUpdate = computed(
  () => hasSystemConfigAccess.value || hasPermission('system:cause:update'),
);
const canDelete = computed(
  () => hasSystemConfigAccess.value || hasPermission('system:cause:delete'),
);

// ==================== 状态定义 ====================

const loading = ref(false);
const searchLoading = ref(false);
const selectedType = ref<CauseType>('CIVIL');
const causeTree = ref<CauseTreeNodeDTO[]>([]);
const searchKeyword = ref('');
const searchResults = ref<CauseTreeNodeDTO[]>([]);
const showSearchResults = ref(false);
const expandedKeys = ref<Key[]>([]);
const selectedKeys = ref<Key[]>([]);
const selectedNode = ref<CauseTreeNodeDTO>();
const isAllExpanded = ref(true); // 默认展开
const selectedCauseId = ref<number>();
const selectedCause = ref<any>();

// 案由类型选项
const causeTypeOptions = getCauseTypeOptions();

// 表单弹窗
const causeModalRef = ref<InstanceType<typeof CauseModal>>();

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
  return (
    causeTypeOptions.find((t) => t.value === selectedType.value)?.label || ''
  );
});

// ==================== 树形数据转换 ====================

/**
 * 将后端树形数据转换为 ant-design-vue Tree 组件所需格式
 * 使用 id 作为 key 确保唯一性（code 可能在同类型下重复）
 */
function transformTreeData(nodes: CauseTreeNodeDTO[]): any[] {
  return nodes.map((node) => ({
    ...node,
    // 使用 id 作为 key，确保唯一性；如果没有 id 则使用 code + 随机数作为备用
    key: node.id
      ? String(node.id)
      : `${node.code}_${Math.random().toString(36).slice(2, 8)}`,
    title: node.name,
    children: node.children ? transformTreeData(node.children) : undefined,
    isLeaf: node.isLeaf ?? false,
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
 * 使用转换后的树数据收集 key，确保与 Tree 组件的 key 一致
 */
function expandAllNodes(_nodes?: CauseTreeNodeDTO[]) {
  const keys: Key[] = [];
  // 使用已转换的 treeData，确保 key 与 Tree 组件一致
  function collectKeys(nodeList: any[]) {
    nodeList.forEach((node) => {
      if (node.key) {
        keys.push(node.key);
      }
      if (node.children && node.children.length > 0) {
        collectKeys(node.children);
      }
    });
  }
  collectKeys(treeData.value);
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
function buildSearchTree(results: any[], _keyword: string): CauseTreeNodeDTO[] {
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
async function handleSelect(keys: Key[], info: any) {
  if (keys.length > 0) {
    selectedNode.value = info.node;
    // 如果有ID，加载完整详情
    if (info.node.id) {
      try {
        selectedCause.value = await getCauseById(info.node.id);
        selectedCauseId.value = info.node.id;
      } catch (error) {
        // 如果获取失败，使用节点数据
        selectedCause.value = info.node;
      }
    } else {
      selectedCause.value = info.node;
      selectedCauseId.value = undefined;
    }
  } else {
    selectedNode.value = undefined;
    selectedCause.value = undefined;
    selectedCauseId.value = undefined;
  }
}

/**
 * 展开/收起节点
 */
function handleExpand(keys: Key[]) {
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

// ==================== CRUD 操作 ====================

/**
 * 新增案由
 */
function handleAddCause() {
  if (!selectedNode.value) {
    // 如果没有选中节点，创建一级案由
    causeModalRef.value?.openCreate(undefined, 1);
  } else {
    // 如果选中了一级案由，创建二级案由
    if (selectedNode.value.level === 1) {
      causeModalRef.value?.openCreate(
        selectedNode.value.code,
        2,
        selectedNode.value.categoryCode,
        selectedNode.value.categoryName,
      );
    } else {
      // 如果选中的是二级案由，在同级创建
      causeModalRef.value?.openCreate(
        selectedNode.value.parentCode,
        2,
        selectedNode.value.categoryCode,
        selectedNode.value.categoryName,
      );
    }
  }
}

/**
 * 编辑案由
 */
function handleEditCause() {
  if (!selectedCauseId.value) {
    message.warning('请先选择要编辑的案由');
    return;
  }
  causeModalRef.value?.openEdit(selectedCauseId.value);
}

/**
 * 通过ID编辑案由（从树节点操作）
 */
function handleEditCauseById(id: number) {
  causeModalRef.value?.openEdit(id);
  // 选中该节点
  const findNode = (nodes: CauseTreeNodeDTO[]): CauseTreeNodeDTO | null => {
    for (const node of nodes) {
      if (node.id === id) {
        return node;
      }
      if (node.children) {
        const found = findNode(node.children);
        if (found) return found;
      }
    }
    return null;
  };
  const node = findNode(causeTree.value);
  if (node && node.id) {
    selectedKeys.value = [String(node.id)];
    handleSelect([String(node.id)], { node });
  }
}

/**
 * 添加子案由（从树节点操作）
 */
function handleAddChildCause(node: any) {
  const level = (node.level || 1) + 1;
  causeModalRef.value?.openCreate(
    node.code,
    level,
    node.categoryCode,
    node.categoryName,
  );
}

/**
 * 通过ID删除案由（从树节点操作）
 */
async function handleDeleteCauseById(id: number) {
  try {
    await deleteCause(id);
    message.success('删除成功');
    if (selectedCauseId.value === id) {
      selectedNode.value = undefined;
      selectedCause.value = undefined;
      selectedCauseId.value = undefined;
    }
    fetchCauseTree();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '删除失败');
  }
}

/**
 * 通过ID切换状态（从树节点操作）
 */
async function handleToggleStatusById(id: number) {
  try {
    await toggleCauseStatus(id);
    message.success('操作成功');
    if (selectedCauseId.value === id) {
      selectedCause.value = await getCauseById(id);
    }
    fetchCauseTree();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

/**
 * 删除案由
 */
async function handleDeleteCause() {
  if (!selectedCauseId.value) {
    message.warning('请先选择要删除的案由');
    return;
  }
  try {
    await deleteCause(selectedCauseId.value);
    message.success('删除成功');
    selectedNode.value = undefined;
    selectedCause.value = undefined;
    selectedCauseId.value = undefined;
    fetchCauseTree();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '删除失败');
  }
}

/**
 * 启用/禁用案由
 */
async function handleToggleStatus() {
  if (!selectedCauseId.value) {
    message.warning('请先选择案由');
    return;
  }
  try {
    await toggleCauseStatus(selectedCauseId.value);
    message.success('操作成功');
    // 重新加载详情
    if (selectedCauseId.value) {
      selectedCause.value = await getCauseById(selectedCauseId.value);
    }
    fetchCauseTree();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '操作失败');
  }
}

/**
 * 表单成功回调
 */
function handleModalSuccess() {
  fetchCauseTree();
  // 如果当前选中的节点被更新，重新加载
  if (selectedCauseId.value) {
    getCauseById(selectedCauseId.value).then((data) => {
      selectedCause.value = data;
      selectedNode.value = {
        code: data.code,
        name: data.name,
        level: data.level,
        parentCode: data.parentCode,
        categoryCode: data.categoryCode,
        categoryName: data.categoryName,
        isLeaf: true,
      };
    });
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  fetchCauseTree();
});
</script>

<template>
  <div>
    <!-- 类型切换和搜索栏 -->
    <Row :gutter="[16, 12]" style="margin-bottom: 16px" align="middle">
      <!-- 案由类型切换 -->
      <Col :xs="24" :sm="24" :md="12" :lg="10" :xl="8">
        <div class="type-switcher">
          <span class="type-label">案由类型：</span>
          <Space size="small" wrap>
            <Tag
              v-for="option in causeTypeOptions"
              :key="option.value"
              :color="selectedType === option.value ? 'blue' : 'default'"
              :style="{ cursor: 'pointer' }"
              @click="handleTypeChange(option.value)"
            >
              <component
                :is="
                  option.value === 'CIVIL'
                    ? BookOutlined
                    : option.value === 'CRIMINAL'
                      ? WarningOutlined
                      : FileProtectOutlined
                "
                class="size-3"
              />
              {{ option.label }}
            </Tag>
          </Space>
        </div>
      </Col>

      <!-- 搜索框 -->
      <Col :xs="24" :sm="24" :md="12" :lg="14" :xl="16">
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
              <component
                :is="currentIcon"
                class="size-4"
                style="margin-right: 8px"
              />
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
              <Empty
                v-if="!searchLoading && searchResults.length === 0"
                description="未找到相关案由"
              />
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
                    :is="dataRef?.isLeaf ? currentIcon : FolderOutlined"
                    class="size-3"
                  />
                </template>
                <template #title="{ title, dataRef }">
                  <div class="tree-node-title">
                    <span class="node-name">{{ title }}</span>
                    <Dropdown
                      :trigger="['click']"
                      @click.stop
                      v-if="dataRef?.id && (canUpdate || canDelete)"
                    >
                      <Button
                        type="text"
                        size="small"
                        class="node-action-btn"
                        @click.stop
                      >
                        <template #icon
                          ><EditOutlined class="size-3"
                        /></template>
                      </Button>
                      <template #overlay>
                        <Menu>
                          <MenuItem
                            v-if="canUpdate"
                            key="edit"
                            @click="
                              () => {
                                if (dataRef?.id) {
                                  handleEditCauseById(dataRef.id);
                                }
                              }
                            "
                          >
                            <EditOutlined class="size-3" />
                            编辑
                          </MenuItem>
                          <MenuItem
                            v-if="canCreate"
                            key="add-child"
                            @click="
                              () => {
                                handleAddChildCause(dataRef);
                              }
                            "
                          >
                            <Plus class="size-3" />
                            添加子案由
                          </MenuItem>
                          <MenuItem
                            v-if="canUpdate && dataRef?.isActive !== undefined"
                            key="toggle"
                            @click="
                              () => {
                                if (dataRef?.id) {
                                  handleToggleStatusById(dataRef.id);
                                }
                              }
                            "
                          >
                            {{ dataRef?.isActive ? '禁用' : '启用' }}
                          </MenuItem>
                          <MenuItem
                            v-if="canDelete"
                            key="delete"
                            danger
                            @click="
                              () => {
                                if (dataRef?.id) {
                                  handleDeleteCauseById(dataRef.id);
                                }
                              }
                            "
                          >
                            <DeleteOutlined class="size-3" />
                            删除
                          </MenuItem>
                        </Menu>
                      </template>
                    </Dropdown>
                  </div>
                </template>
              </Tree>
              <Empty
                v-if="!loading && treeData.length === 0"
                description="暂无案由数据"
              />
            </div>
          </Spin>
        </Card>
      </Col>

      <!-- 右侧：案由详情 -->
      <Col :xs="24" :sm="24" :md="12" :lg="14" :xl="16">
        <Card :bordered="false">
          <template #title>
            <span>案由详情</span>
          </template>
          <template #extra>
            <Space>
              <!-- 新增按钮始终显示（有权限时） -->
              <Button
                v-if="canCreate"
                type="primary"
                size="small"
                @click="handleAddCause"
              >
                <template #icon><Plus class="size-3" /></template>
                新增
              </Button>
              <!-- 以下按钮需要选中节点才显示 -->
              <Button
                v-if="canUpdate && selectedCauseId"
                size="small"
                @click="handleEditCause"
              >
                <template #icon><EditOutlined class="size-3" /></template>
                编辑
              </Button>
              <Popconfirm
                v-if="canDelete && selectedCauseId"
                title="确定删除该案由吗？"
                @confirm="handleDeleteCause"
              >
                <Button size="small" danger>
                  <template #icon><DeleteOutlined class="size-3" /></template>
                  删除
                </Button>
              </Popconfirm>
              <Button
                v-if="canUpdate && selectedCauseId && selectedCause"
                size="small"
                @click="handleToggleStatus"
              >
                {{ selectedCause.isActive ? '禁用' : '启用' }}
              </Button>
            </Space>
          </template>

          <!-- 已选中节点 -->
          <div v-if="selectedNode || selectedCause" class="cause-detail">
            <Row :gutter="16">
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">案由代码：</span>
                  <span class="detail-value code">
                    {{ selectedCause?.code || selectedNode?.code }}
                  </span>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">层级：</span>
                  <Tag
                    :color="
                      getLevelColor(
                        selectedCause?.level || selectedNode?.level || 1,
                      )
                    "
                  >
                    第 {{ selectedCause?.level || selectedNode?.level || 1 }} 级
                  </Tag>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">案由名称：</span>
                  <span class="detail-value name">
                    {{ selectedCause?.name || selectedNode?.name }}
                  </span>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">状态：</span>
                  <Tag
                    v-if="selectedCause"
                    :color="selectedCause.isActive ? 'success' : 'error'"
                  >
                    {{ selectedCause.isActive ? '启用' : '禁用' }}
                  </Tag>
                  <Tag
                    v-else
                    :color="selectedNode?.isLeaf ? 'green' : 'orange'"
                  >
                    {{ selectedNode?.isLeaf ? '叶子节点' : '非叶子节点' }}
                  </Tag>
                </div>
              </Col>
              <Col
                v-if="selectedCause?.parentCode || selectedNode?.parentCode"
                :xs="24"
                :sm="12"
              >
                <div class="detail-item">
                  <span class="detail-label">上级代码：</span>
                  <span class="detail-value">
                    {{ selectedCause?.parentCode || selectedNode?.parentCode }}
                  </span>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">分类代码：</span>
                  <span class="detail-value">
                    {{ selectedCause?.categoryCode || '-' }}
                  </span>
                </div>
              </Col>
              <Col :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">分类名称：</span>
                  <span class="detail-value">
                    {{ selectedCause?.categoryName || '-' }}
                  </span>
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
              <Col v-if="selectedCause?.sortOrder" :xs="24" :sm="12">
                <div class="detail-item">
                  <span class="detail-label">排序号：</span>
                  <span class="detail-value">{{
                    selectedCause.sortOrder
                  }}</span>
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

    <!-- 案由表单弹窗 -->
    <CauseModal
      ref="causeModalRef"
      :cause-type="selectedType"
      @success="handleModalSuccess"
    />
  </div>
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
  margin-right: 8px;
  font-size: 14px;
  font-weight: 500;
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
  font-size: 13px;
  color: var(--ant-primary-color, #1890ff);
  background: var(--ant-primary-1, #e6f7ff);
  border-radius: 4px;
}

.cause-detail {
  padding: 8px 0;
}

.detail-item {
  margin-bottom: 16px;
}

.detail-label {
  font-size: 13px;
  color: #666;
}

.detail-value {
  margin-left: 8px;
  font-size: 14px;
  color: #333;
}

.detail-value.code {
  padding: 2px 6px;
  font-family: Monaco, Menlo, monospace;
  background: #f5f5f5;
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

.tree-node-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-action-btn {
  height: 20px;
  padding: 0 4px;
  line-height: 20px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node-title:hover .node-action-btn {
  opacity: 1;
}

:deep(.ant-tree-node-selected .node-action-btn) {
  opacity: 1;
}
</style>
