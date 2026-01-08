<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Tag,
  Input,
  Select,
  Form,
  FormItem,
  Row,
  Col,
  Popconfirm,
  Tree,
  InputNumber,
  DatePicker,
  Textarea,
} from 'ant-design-vue';
import {
  getLawRegulationList,
  createLawRegulation,
  updateLawRegulation,
  deleteLawRegulation,
  collectLawRegulation,
  uncollectLawRegulation,
  getLawCategoryTree,
  markLawRegulationRepealed,
} from '#/api/knowledge';
import type {
  LawRegulationDTO,
  LawRegulationQuery,
  CreateLawRegulationCommand,
  LawCategoryDTO,
} from '#/api/knowledge/types';

defineOptions({ name: 'KnowledgeLaw' });

// 状态
const loading = ref(false);
const dataSource = ref<LawRegulationDTO[]>([]);
const total = ref(0);
const modalVisible = ref(false);
const modalTitle = ref('新增法规');
const formRef = ref();
const categories = ref<LawCategoryDTO[]>([]);
const selectedCategoryId = ref<number | undefined>();

// 查询参数
const queryParams = reactive<LawRegulationQuery>({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  lawType: undefined,
  categoryId: undefined,
  status: undefined,
});

// 表单数据
const formData = reactive<CreateLawRegulationCommand & { id?: number }>({
  id: undefined,
  name: '',
  lawType: 'LAW',
  categoryId: undefined,
  issuer: '',
  issueDate: '',
  effectiveDate: '',
  content: '',
});

// 表格列
const columns = [
  { title: '法规名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '法规类型', dataIndex: 'lawTypeName', key: 'lawTypeName', width: 120 },
  { title: '分类', dataIndex: 'categoryName', key: 'categoryName', width: 120 },
  { title: '发布机关', dataIndex: 'issuer', key: 'issuer', width: 150 },
  { title: '发布日期', dataIndex: 'issueDate', key: 'issueDate', width: 120 },
  { title: '实施日期', dataIndex: 'effectiveDate', key: 'effectiveDate', width: 120 },
  { title: '效力状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

// 法规类型选项
const lawTypeOptions = [
  { label: '法律', value: 'LAW' },
  { label: '行政法规', value: 'REGULATION' },
  { label: '部门规章', value: 'RULE' },
  { label: '司法解释', value: 'INTERPRETATION' },
  { label: '地方性法规', value: 'LOCAL' },
];

// 状态选项
const statusOptions = [
  { label: '全部', value: undefined },
  { label: '现行有效', value: 'EFFECTIVE' },
  { label: '已修订', value: 'AMENDED' },
  { label: '已废止', value: 'REPEALED' },
];

// 加载数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await getLawRegulationList(queryParams);
    dataSource.value = res.list;
    total.value = res.total;
  } catch (error: any) {
    message.error(error.message || '加载法规列表失败');
  } finally {
    loading.value = false;
  }
}

// 加载分类树
async function loadCategories() {
  try {
    const data = await getLawCategoryTree();
    categories.value = data;
  } catch (error: any) {
    console.error('加载分类树失败:', error);
  }
}

// 搜索
function handleSearch() {
  queryParams.pageNum = 1;
  fetchData();
}

// 重置
function handleReset() {
  queryParams.name = undefined;
  queryParams.lawType = undefined;
  queryParams.categoryId = undefined;
  queryParams.status = undefined;
  queryParams.pageNum = 1;
  selectedCategoryId.value = undefined;
  fetchData();
}

// 新增
function handleAdd() {
  modalTitle.value = '新增法规';
  Object.assign(formData, {
    id: undefined,
    name: '',
    lawType: 'LAW',
    categoryId: undefined,
    issuer: '',
    issueDate: '',
    effectiveDate: '',
    content: '',
  });
  modalVisible.value = true;
}

// 编辑
function handleEdit(record: LawRegulationDTO) {
  modalTitle.value = '编辑法规';
  Object.assign(formData, {
    id: record.id,
    name: record.name,
    lawType: record.lawType,
    categoryId: record.categoryId,
    issuer: record.issuer || '',
    issueDate: record.issueDate || '',
    effectiveDate: record.effectiveDate || '',
    content: record.content || '',
  });
  modalVisible.value = true;
}

// 保存
async function handleSave() {
  try {
    await formRef.value?.validate();
    
    if (formData.id) {
      await updateLawRegulation(formData.id, formData);
      message.success('更新成功');
    } else {
      await createLawRegulation(formData);
      message.success('创建成功');
    }
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    if (error?.errorFields) {
      return;
    }
    message.error(error.message || '操作失败');
  }
}

// 删除
function handleDelete(record: LawRegulationDTO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除法规 "${record.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteLawRegulation(record.id);
        message.success('删除成功');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '删除失败');
      }
    },
  });
}

// 收藏/取消收藏
async function handleCollect(record: LawRegulationDTO) {
  try {
    if (record.collected) {
      await uncollectLawRegulation(record.id);
      message.success('已取消收藏');
    } else {
      await collectLawRegulation(record.id);
      message.success('收藏成功');
    }
    fetchData();
  } catch (error: any) {
    message.error(error.message || '操作失败');
  }
}

// 标注失效
function handleMarkRepealed(record: LawRegulationDTO) {
  Modal.confirm({
    title: '标注失效',
    content: '请输入失效原因（可选）',
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      const reason = prompt('请输入失效原因:');
      try {
        await markLawRegulationRepealed(record.id, reason || undefined);
        message.success('标注成功');
        fetchData();
      } catch (error: any) {
        message.error(error.message || '操作失败');
      }
    },
  });
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    EFFECTIVE: 'green',
    AMENDED: 'orange',
    REPEALED: 'red',
  };
  return colorMap[status] || 'default';
}

// 分类树选择
function onCategorySelect(selectedKeys: any[]) {
  if (selectedKeys.length > 0) {
    queryParams.categoryId = selectedKeys[0];
  } else {
    queryParams.categoryId = undefined;
  }
  handleSearch();
}

onMounted(() => {
  fetchData();
  loadCategories();
});
</script>

<template>
  <Page title="法规库" description="查询和管理法律法规">
    <Row :gutter="16">
      <!-- 左侧分类树 -->
      <Col :span="6">
        <Card title="法规分类" style="height: calc(100vh - 200px); overflow-y: auto">
          <Tree
            :tree-data="categories"
            :field-names="{ children: 'children', title: 'name', key: 'id' }"
            :selected-keys="selectedCategoryId ? [selectedCategoryId] : []"
            @select="onCategorySelect"
          />
        </Card>
      </Col>

      <!-- 右侧内容区 -->
      <Col :span="18">
        <Card>
          <!-- 搜索栏 -->
          <div style="margin-bottom: 16px">
            <Row :gutter="[16, 16]">
              <Col :xs="24" :sm="12" :md="8" :lg="6">
                <Input
                  v-model:value="queryParams.name"
                  placeholder="法规名称"
                  allowClear
                  @pressEnter="handleSearch"
                />
              </Col>
              <Col :xs="24" :sm="12" :md="8" :lg="6">
                <Select
                  v-model:value="queryParams.lawType"
                  placeholder="法规类型"
                  allowClear
                  style="width: 100%"
                  :options="lawTypeOptions"
                />
              </Col>
              <Col :xs="24" :sm="12" :md="8" :lg="6">
                <Select
                  v-model:value="queryParams.status"
                  placeholder="效力状态"
                  allowClear
                  style="width: 100%"
                  :options="statusOptions"
                />
              </Col>
              <Col :xs="24" :sm="12" :md="24" :lg="6">
                <Space wrap>
                  <Button type="primary" @click="handleSearch">查询</Button>
                  <Button @click="handleReset">重置</Button>
                  <Button type="primary" @click="handleAdd">新增法规</Button>
                </Space>
              </Col>
            </Row>
          </div>

          <!-- 表格 -->
          <Table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="{
              current: queryParams.pageNum,
              pageSize: queryParams.pageSize,
              total: total,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条`,
              onChange: (page, size) => {
                queryParams.pageNum = page;
                queryParams.pageSize = size;
                fetchData();
              },
            }"
            row-key="id"
            :scroll="{ x: 1200 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'statusName'">
                <Tag :color="getStatusColor((record as LawRegulationDTO).status)">
                  {{ (record as LawRegulationDTO).statusName }}
                </Tag>
              </template>
              <template v-if="column.key === 'action'">
                <Space>
                  <a @click="handleEdit(record as LawRegulationDTO)">编辑</a>
                  <a @click="handleCollect(record as LawRegulationDTO)">
                    {{ (record as LawRegulationDTO).collected ? '取消收藏' : '收藏' }}
                  </a>
                  <Popconfirm
                    title="确定删除？"
                    @confirm="handleDelete(record as LawRegulationDTO)"
                  >
                    <a style="color: red">删除</a>
                  </Popconfirm>
                  <a @click="handleMarkRepealed(record as LawRegulationDTO)">标注失效</a>
                </Space>
              </template>
            </template>
          </Table>
        </Card>
      </Col>
    </Row>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="700px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <FormItem label="法规名称" name="name" :rules="[{ required: true, message: '请输入法规名称' }]">
          <Input v-model:value="formData.name" placeholder="请输入法规名称" />
        </FormItem>
        <FormItem label="法规类型" name="lawType" :rules="[{ required: true, message: '请选择法规类型' }]">
          <Select v-model:value="formData.lawType" :options="lawTypeOptions" />
        </FormItem>
        <FormItem label="发布机关" name="issuer">
          <Input v-model:value="formData.issuer" placeholder="请输入发布机关" />
        </FormItem>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="发布日期" name="issueDate">
              <DatePicker
                v-model:value="formData.issueDate"
                placeholder="请选择发布日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="实施日期" name="effectiveDate">
              <DatePicker
                v-model:value="formData.effectiveDate"
                placeholder="请选择实施日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </FormItem>
          </Col>
        </Row>
        <FormItem label="法规内容" name="content">
          <Textarea v-model:value="formData.content" :rows="6" placeholder="请输入法规内容" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>
