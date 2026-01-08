<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Form,
  FormItem,
  InputNumber,
  TreeSelect,
  Popconfirm,
} from 'ant-design-vue';
import {
  getDepartmentTree,
  createDepartment,
  updateDepartment,
  deleteDepartment,
} from '#/api/system';
import type { 
  DepartmentDTO, 
  CreateDepartmentCommand, 
  UpdateDepartmentCommand 
} from '#/api/system/types';
import { UserTreeSelect } from '#/components/UserTreeSelect';

defineOptions({ name: 'SystemDept' });

// ==================== 状态定义 ====================

const loading = ref(false);
const dataSource = ref<DepartmentDTO[]>([]);
const modalVisible = ref(false);
const modalTitle = ref('新增部门');
const formRef = ref();

// 表单数据
const formData = reactive<Partial<CreateDepartmentCommand> & { id?: number }>({
  id: undefined,
  name: '',
  parentId: 0,
  sortOrder: 0,
  leaderId: undefined,
});

// ==================== 常量配置 ====================

// 表格列
const columns = [
  { title: '部门名称', dataIndex: 'name', key: 'name', width: 200 },
  { title: '负责人', dataIndex: 'leaderName', key: 'leaderName', width: 120 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 220 },
];

// ==================== 数据加载 ====================

async function fetchData() {
  loading.value = true;
  try {
    dataSource.value = await getDepartmentTree();
  } catch (error: unknown) {
    const err = error as { message?: string };
    message.error(err.message || '加载部门列表失败');
  } finally {
    loading.value = false;
  }
}

// ==================== CRUD 操作 ====================

// 新增部门
function handleAdd(parentId?: number) {
  modalTitle.value = '新增部门';
  Object.assign(formData, {
    id: undefined,
    name: '',
    parentId: parentId || 0,
    sortOrder: 0,
    leaderId: undefined,
  });
  modalVisible.value = true;
}

// 编辑部门
function handleEdit(record: DepartmentDTO) {
  modalTitle.value = '编辑部门';
  Object.assign(formData, {
    id: record.id,
    name: record.name,
    parentId: record.parentId || 0,
    sortOrder: record.sortOrder || 0,
    leaderId: record.leaderId,
  });
  modalVisible.value = true;
}

// 保存部门
async function handleSave() {
  try {
    await formRef.value?.validate();
    
    if (formData.id) {
      const updateData: UpdateDepartmentCommand = {
        id: formData.id,
        ...formData,
      } as UpdateDepartmentCommand;
      await updateDepartment(updateData);
      message.success('更新成功');
    } else {
      const createData: CreateDepartmentCommand = {
        ...formData,
      } as CreateDepartmentCommand;
      await createDepartment(createData);
      message.success('创建成功');
    }
    modalVisible.value = false;
    fetchData();
  } catch (error: unknown) {
    const err = error as { errorFields?: unknown; message?: string };
    if (err?.errorFields) return;
    message.error(err.message || '操作失败');
  }
}

// 删除部门
function handleDelete(record: DepartmentDTO) {
  if (record.children && record.children.length > 0) {
    message.warning('请先删除子部门');
    return;
  }
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除部门 "${record.name}" 吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteDepartment(record.id);
        message.success('删除成功');
        fetchData();
      } catch (error: unknown) {
        const err = error as { message?: string };
        message.error(err.message || '删除失败');
      }
    },
  });
}

// ==================== 辅助方法 ====================

// 构建部门树数据（用于TreeSelect）
interface TreeNode {
  id: number;
  name: string;
  children: TreeNode[];
}

const departmentTreeData = computed<TreeNode[]>(() => {
  const flattenDepts = (depts: DepartmentDTO[]): DepartmentDTO[] => {
    const result: DepartmentDTO[] = [];
    const traverse = (items: DepartmentDTO[]) => {
      for (const item of items) {
        result.push(item);
        if (item.children && item.children.length > 0) {
          traverse(item.children);
        }
      }
    };
    traverse(depts);
    return result;
  };
  
  const buildTree = (depts: DepartmentDTO[], parentId: number = 0): TreeNode[] => {
    return depts
      .filter(dept => dept.parentId === parentId)
      .map(dept => ({
        id: dept.id,
        name: dept.name,
        children: buildTree(depts, dept.id),
      }));
  };
  
  const allDepts = flattenDepts(dataSource.value);
  return [{ id: 0, name: '顶级部门', children: buildTree(allDepts) }];
});

// ==================== 生命周期 ====================

onMounted(() => {
  fetchData();
});
</script>

<template>
  <Page title="部门管理" description="管理系统部门">
    <Card :bordered="false">
      <!-- 操作按钮 -->
      <div style="margin-bottom: 16px">
        <Button type="primary" @click="handleAdd()">新增部门</Button>
      </div>

      <!-- 数据表格（树形） -->
      <Table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        :default-expand-all-rows="true"
        row-key="id"
      >
        <template #bodyCell="{ column, record: rawRecord }">
          <template v-if="column.key === 'action'">
            <Space>
              <a @click="handleAdd((rawRecord as DepartmentDTO).id)">新增子部门</a>
              <a @click="handleEdit(rawRecord as DepartmentDTO)">编辑</a>
              <Popconfirm
                title="确定删除该部门？"
                @confirm="handleDelete(rawRecord as DepartmentDTO)"
              >
                <a style="color: #ff4d4f">删除</a>
              </Popconfirm>
            </Space>
          </template>
        </template>
      </Table>
    </Card>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="modalTitle"
      width="600px"
      @ok="handleSave"
    >
      <Form
        ref="formRef"
        :model="formData"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
        style="margin-top: 16px"
      >
        <FormItem label="上级部门" name="parentId">
          <TreeSelect
            v-model:value="formData.parentId"
            :tree-data="departmentTreeData"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级部门"
            allowClear
            style="width: 100%"
          />
        </FormItem>
        
        <FormItem 
          label="部门名称" 
          name="name" 
          :rules="[{ required: true, message: '请输入部门名称' }]"
        >
          <Input v-model:value="formData.name" placeholder="请输入部门名称" />
        </FormItem>
        
        <FormItem label="负责人" name="leaderId">
          <UserTreeSelect
            v-model:value="formData.leaderId"
            placeholder="选择负责人（按部门筛选）"
          />
        </FormItem>
        
        <FormItem label="排序" name="sortOrder">
          <InputNumber v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
        </FormItem>
      </Form>
    </Modal>
  </Page>
</template>

<style scoped>
:deep(.ant-table-cell) {
  vertical-align: middle;
}
</style>
