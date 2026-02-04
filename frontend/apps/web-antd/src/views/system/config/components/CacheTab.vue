<script setup lang="ts">
import type { CacheStats } from '#/api/system/types';

import { onMounted, ref } from 'vue';

import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  DescriptionsItem,
  message,
  Popconfirm,
  Row,
  Space,
  Spin,
  Statistic,
} from 'ant-design-vue';

import {
  clearAllCache,
  clearConfigCache,
  clearDeptCache,
  clearMenuCache,
  getCacheStats,
} from '#/api/system';

defineOptions({ name: 'CacheTab' });

// ==================== 状态定义 ====================

const loading = ref(false);
const stats = ref<CacheStats | null>(null);
const clearLoading = ref<Record<string, boolean>>({
  all: false,
  config: false,
  menu: false,
  dept: false,
});

// ==================== 方法 ====================

async function loadStats() {
  loading.value = true;
  try {
    stats.value = await getCacheStats();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载缓存统计失败');
  } finally {
    loading.value = false;
  }
}

async function handleClearAll() {
  clearLoading.value.all = true;
  try {
    await clearAllCache();
    message.success('所有缓存已清除');
    await loadStats();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '清除缓存失败');
  } finally {
    clearLoading.value.all = false;
  }
}

async function handleClearConfig() {
  clearLoading.value.config = true;
  try {
    await clearConfigCache();
    message.success('配置缓存已清除');
    await loadStats();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '清除缓存失败');
  } finally {
    clearLoading.value.config = false;
  }
}

async function handleClearMenu() {
  clearLoading.value.menu = true;
  try {
    await clearMenuCache();
    message.success('菜单缓存已清除');
    await loadStats();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '清除缓存失败');
  } finally {
    clearLoading.value.menu = false;
  }
}

async function handleClearDept() {
  clearLoading.value.dept = true;
  try {
    await clearDeptCache();
    message.success('部门缓存已清除');
    await loadStats();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '清除缓存失败');
  } finally {
    clearLoading.value.dept = false;
  }
}

function formatHitRate(rate: number | undefined) {
  if (rate === undefined || rate === null) return '-';
  return `${(rate * 100).toFixed(2)}%`;
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadStats();
});
</script>

<template>
  <div>
    <Alert
      type="warning"
      show-icon
      style="margin-bottom: 16px"
      message="注意"
      description="清除缓存会导致下次访问时需要重新从数据库加载数据，可能会短暂影响系统性能。请谨慎操作。"
    />

    <Spin :spinning="loading">
      <!-- 缓存统计 -->
      <Row :gutter="16" style="margin-bottom: 16px">
        <Col :xs="24" :sm="12" :md="6">
          <Card size="small">
            <Statistic
              title="缓存键总数"
              :value="stats?.totalKeys || 0"
              :value-style="{ color: '#1890ff' }"
            />
          </Card>
        </Col>
        <Col :xs="24" :sm="12" :md="6">
          <Card size="small">
            <Statistic
              title="已用内存"
              :value="stats?.usedMemory || '-'"
              :value-style="{ color: '#52c41a' }"
            />
          </Card>
        </Col>
        <Col :xs="24" :sm="12" :md="6">
          <Card size="small">
            <Statistic
              title="命中率"
              :value="formatHitRate(stats?.hitRate)"
              :value-style="{ color: '#722ed1' }"
            />
          </Card>
        </Col>
        <Col :xs="24" :sm="12" :md="6">
          <Card size="small">
            <Statistic
              title="命中/未命中"
              :value="`${stats?.hitCount || 0} / ${stats?.missCount || 0}`"
              :value-style="{ color: '#fa8c16' }"
            />
          </Card>
        </Col>
      </Row>

      <!-- 分类缓存统计 -->
      <Card title="分类缓存统计" size="small" style="margin-bottom: 16px">
        <Descriptions :column="{ xs: 1, sm: 2, md: 4 }" size="small">
          <DescriptionsItem label="配置缓存">
            {{ stats?.configCacheSize || 0 }} 条
          </DescriptionsItem>
          <DescriptionsItem label="菜单缓存">
            {{ stats?.menuCacheSize || 0 }} 条
          </DescriptionsItem>
          <DescriptionsItem label="部门缓存">
            {{ stats?.deptCacheSize || 0 }} 条
          </DescriptionsItem>
          <DescriptionsItem label="用户权限缓存">
            {{ stats?.userPermissionCacheSize || 0 }} 条
          </DescriptionsItem>
        </Descriptions>
      </Card>

      <!-- 缓存操作 -->
      <Card title="缓存操作" size="small">
        <Row :gutter="[16, 16]">
          <Col :xs="24" :sm="12" :md="6">
            <Card size="small" title="清除所有缓存">
              <p style="margin-bottom: 12px; font-size: 12px; color: #666">
                清除系统中的所有缓存数据
              </p>
              <Popconfirm
                title="确定要清除所有缓存吗？"
                @confirm="handleClearAll"
              >
                <Button type="primary" danger :loading="clearLoading.all" block>
                  清除所有
                </Button>
              </Popconfirm>
            </Card>
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Card size="small" title="清除配置缓存">
              <p style="margin-bottom: 12px; font-size: 12px; color: #666">
                修改系统配置后刷新缓存
              </p>
              <Popconfirm
                title="确定要清除配置缓存吗？"
                @confirm="handleClearConfig"
              >
                <Button :loading="clearLoading.config" block>
                  清除配置缓存
                </Button>
              </Popconfirm>
            </Card>
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Card size="small" title="清除菜单缓存">
              <p style="margin-bottom: 12px; font-size: 12px; color: #666">
                修改菜单/权限后刷新缓存
              </p>
              <Popconfirm
                title="确定要清除菜单缓存吗？"
                @confirm="handleClearMenu"
              >
                <Button :loading="clearLoading.menu" block>清除菜单缓存</Button>
              </Popconfirm>
            </Card>
          </Col>
          <Col :xs="24" :sm="12" :md="6">
            <Card size="small" title="清除部门缓存">
              <p style="margin-bottom: 12px; font-size: 12px; color: #666">
                修改部门结构后刷新缓存
              </p>
              <Popconfirm
                title="确定要清除部门缓存吗？"
                @confirm="handleClearDept"
              >
                <Button :loading="clearLoading.dept" block>清除部门缓存</Button>
              </Popconfirm>
            </Card>
          </Col>
        </Row>

        <div style="margin-top: 16px; text-align: right">
          <Space>
            <Button @click="loadStats">刷新统计</Button>
          </Space>
        </div>
      </Card>
    </Spin>
  </div>
</template>
