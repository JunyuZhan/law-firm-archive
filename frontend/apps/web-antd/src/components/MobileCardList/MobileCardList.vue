<script setup lang="ts">
import { computed } from 'vue';

import { Button, Card, Empty, Spin, Tag } from 'ant-design-vue';

/**
 * 移动端卡片列表组件
 * 用于在移动端替代表格展示数据
 */

interface CardItem {
  id: number | string;
  /** 标题字段 */
  title?: string;
  /** 副标题字段 */
  subtitle?: string;
  /** 状态 */
  status?: string;
  /** 状态颜色 */
  statusColor?: string;
  /** 信息行列表 */
  infoRows?: Array<{
    label: string;
    value: string | number;
  }>;
  /** 原始数据 */
  raw?: any;
}

interface Props {
  /** 数据列表 */
  data: CardItem[];
  /** 是否加载中 */
  loading?: boolean;
  /** 空状态描述 */
  emptyText?: string;
  /** 是否显示查看按钮 */
  showView?: boolean;
  /** 是否显示编辑按钮 */
  showEdit?: boolean;
  /** 是否显示删除按钮 */
  showDelete?: boolean;
  /** 自定义操作按钮 */
  customActions?: Array<{
    label: string;
    type?: 'primary' | 'default' | 'dashed' | 'link' | 'text';
    danger?: boolean;
    onClick: (item: CardItem) => void;
  }>;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  emptyText: '暂无数据',
  showView: true,
  showEdit: true,
  showDelete: false,
  customActions: () => [],
});

const emit = defineEmits<{
  view: [item: CardItem];
  edit: [item: CardItem];
  delete: [item: CardItem];
}>();

const hasActions = computed(() => {
  return (
    props.showView ||
    props.showEdit ||
    props.showDelete ||
    props.customActions.length > 0
  );
});

function handleView(item: CardItem) {
  emit('view', item);
}

function handleEdit(item: CardItem) {
  emit('edit', item);
}

function handleDelete(item: CardItem) {
  emit('delete', item);
}
</script>

<template>
  <div class="mobile-card-list">
    <Spin :spinning="loading">
      <!-- 空状态 -->
      <Empty v-if="!data || data.length === 0" :description="emptyText" />

      <!-- 卡片列表 -->
      <div v-else class="card-container">
        <Card
          v-for="item in data"
          :key="item.id"
          class="mobile-card-item"
          :bordered="false"
          size="small"
        >
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="card-title">
              <slot name="title" :item="item">
                {{ item.title }}
              </slot>
            </div>
            <div v-if="item.status" class="card-status">
              <Tag :color="item.statusColor || 'default'" size="small">
                {{ item.status }}
              </Tag>
            </div>
          </div>

          <!-- 副标题 -->
          <div v-if="item.subtitle" class="card-subtitle">
            <slot name="subtitle" :item="item">
              {{ item.subtitle }}
            </slot>
          </div>

          <!-- 卡片内容 -->
          <div class="card-body">
            <slot name="content" :item="item">
              <div
                v-for="(row, index) in item.infoRows"
                :key="index"
                class="info-row"
              >
                <span class="info-label">{{ row.label }}：</span>
                <span class="info-value">{{ row.value }}</span>
              </div>
            </slot>
          </div>

          <!-- 卡片操作区 -->
          <div v-if="hasActions" class="card-footer">
            <slot name="actions" :item="item">
              <Button
                v-if="showView"
                size="small"
                type="link"
                @click="handleView(item)"
              >
                查看
              </Button>
              <Button
                v-if="showEdit"
                size="small"
                type="link"
                @click="handleEdit(item)"
              >
                编辑
              </Button>
              <Button
                v-if="showDelete"
                size="small"
                type="link"
                danger
                @click="handleDelete(item)"
              >
                删除
              </Button>
              <Button
                v-for="(action, index) in customActions"
                :key="index"
                size="small"
                :type="action.type || 'link'"
                :danger="action.danger"
                @click="action.onClick(item)"
              >
                {{ action.label }}
              </Button>
            </slot>
          </div>
        </Card>
      </div>
    </Spin>
  </div>
</template>

<style scoped>
.mobile-card-list {
  min-height: 200px;
  padding: 12px;
  background: #f5f5f5;
}

.card-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mobile-card-item {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgb(0 0 0 / 8%);
}

.mobile-card-item :deep(.ant-card-body) {
  padding: 12px;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-title {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.4;
  color: #262626;
  word-break: break-all;
}

.card-status {
  flex-shrink: 0;
  margin-left: 8px;
}

.card-subtitle {
  margin-bottom: 8px;
  font-size: 13px;
  color: #8c8c8c;
}

.card-body {
  font-size: 13px;
  line-height: 1.6;
  color: #595959;
}

.info-row {
  display: flex;
  margin-bottom: 4px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  flex-shrink: 0;
  color: #8c8c8c;
}

.info-value {
  color: #262626;
  word-break: break-all;
}

.card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: flex-end;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.card-footer :deep(.ant-btn) {
  padding: 0 8px;
}
</style>
