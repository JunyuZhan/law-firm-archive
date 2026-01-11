<script setup lang="ts">
import { ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  Input,
  Progress,
  Row,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

defineOptions({ name: 'HrPerformance' });

// 状态
const loading = ref(false);
const searchText = ref('');
const period = ref('');
const dataSource = ref([]);

const columns = [
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName' },
  { title: '部门', dataIndex: 'department', key: 'department' },
  { title: '考核周期', dataIndex: 'period', key: 'period' },
  { title: '业绩得分', dataIndex: 'performanceScore', key: 'performanceScore' },
  { title: '能力得分', dataIndex: 'abilityScore', key: 'abilityScore' },
  { title: '综合得分', dataIndex: 'totalScore', key: 'totalScore' },
  { title: '考核等级', dataIndex: 'grade', key: 'grade' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action', width: 150 },
];
</script>

<template>
  <Page title="绩效考核" description="管理员工绩效考核">
    <Card>
      <!-- 搜索栏 -->
      <div style="margin-bottom: 16px">
        <Row :gutter="16">
          <Col :span="6">
            <Select
              v-model:value="period"
              placeholder="考核周期"
              style="width: 100%"
              allow-clear
            >
              <Select.Option value="2026Q1">2026年Q1</Select.Option>
              <Select.Option value="2025Q4">2025年Q4</Select.Option>
              <Select.Option value="2025Q3">2025年Q3</Select.Option>
            </Select>
          </Col>
          <Col :span="6">
            <Input
              v-model:value="searchText"
              placeholder="搜索员工"
              allow-clear
            />
          </Col>
          <Col :span="12">
            <Space>
              <Button type="primary">查询</Button>
              <Button>重置</Button>
              <Button type="primary">发起考核</Button>
            </Space>
          </Col>
        </Row>
      </div>
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'totalScore'">
            <Progress :percent="record.totalScore" :size="[100, 10]" />
          </template>
          <template v-if="column.key === 'grade'">
            <Tag
              :color="
                record.grade === 'A'
                  ? 'green'
                  : record.grade === 'B'
                    ? 'blue'
                    : record.grade === 'C'
                      ? 'orange'
                      : 'red'
              "
            >
              {{ record.grade }}
            </Tag>
          </template>
          <template v-if="column.key === 'status'">
            <Tag :color="record.status === 'COMPLETED' ? 'green' : 'orange'">
              {{ record.status === 'COMPLETED' ? '已完成' : '进行中' }}
            </Tag>
          </template>
          <template v-if="column.key === 'action'">
            <Space>
              <a>查看</a>
              <a>评分</a>
            </Space>
          </template>
        </template>
      </Table>
    </Card>
  </Page>
</template>
