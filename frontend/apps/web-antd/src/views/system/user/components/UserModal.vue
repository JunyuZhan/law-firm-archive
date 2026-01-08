<script setup lang="ts">
import { ref, computed } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import type { VbenFormSchema } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createUser, updateUser, getAllRoles, getDepartmentTree } from '#/api/system';
import type { RoleDTO, DepartmentDTO, CreateUserCommand, UpdateUserCommand } from '#/api/system/types';

const emit = defineEmits<{
  success: [];
}>();

const isEdit = ref(false);
const editId = ref<number>();
const roles = ref<RoleDTO[]>([]);
const departments = ref<DepartmentDTO[]>([]);

// 加载选项数据
async function loadOptions() {
  try {
    const [rolesRes, deptRes] = await Promise.all([
      getAllRoles(),
      getDepartmentTree(),
    ]);
    roles.value = rolesRes;
    departments.value = deptRes;
    
    // 更新表单选项
    formApi.updateSchema([
      {
        fieldName: 'departmentId',
        componentProps: {
          treeData: deptRes,
          fieldNames: { label: 'name', value: 'id', children: 'children' },
        },
      },
      {
        fieldName: 'roleIds',
        componentProps: {
          options: rolesRes.map((r: RoleDTO) => ({ label: r.roleName, value: r.id })),
        },
      },
    ]);
  } catch (error) {
    console.error('加载选项失败:', error);
  }
}

// 表单 Schema
const formSchema = computed<VbenFormSchema[]>(() => [
  {
    fieldName: 'username',
    label: '用户名',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入用户名',
      disabled: isEdit.value,
    },
  },
  {
    fieldName: 'password',
    label: '密码',
    component: 'InputPassword',
    rules: isEdit.value ? undefined : 'required',
    dependencies: {
      show: () => !isEdit.value,
    },
    componentProps: {
      placeholder: '请输入密码',
    },
  },
  {
    fieldName: 'realName',
    label: '姓名',
    component: 'Input',
    rules: 'required',
    componentProps: {
      placeholder: '请输入姓名',
    },
  },
  {
    fieldName: 'email',
    label: '邮箱',
    component: 'Input',
    componentProps: {
      placeholder: '请输入邮箱',
    },
  },
  {
    fieldName: 'phone',
    label: '手机号',
    component: 'Input',
    componentProps: {
      placeholder: '请输入手机号',
    },
  },
  {
    fieldName: 'departmentId',
    label: '部门',
    component: 'TreeSelect',
    componentProps: {
      placeholder: '请选择部门',
      treeData: departments.value,
      fieldNames: { label: 'name', value: 'id', children: 'children' },
      allowClear: true,
    },
  },
  {
    fieldName: 'position',
    label: '职位',
    component: 'Input',
    componentProps: {
      placeholder: '请输入职位',
    },
  },
  {
    fieldName: 'roleIds',
    label: '角色',
    component: 'Select',
    componentProps: {
      placeholder: '请选择角色',
      mode: 'multiple',
      options: roles.value.map((r: RoleDTO) => ({ label: r.roleName, value: r.id })),
    },
  },
  {
    fieldName: 'status',
    label: '状态',
    component: 'RadioGroup',
    defaultValue: 'ACTIVE',
    componentProps: {
      options: [
        { label: '正常', value: 'ACTIVE' },
        { label: '禁用', value: 'DISABLED' },
      ],
    },
  },
]);

const [Form, formApi] = useVbenForm({
  schema: formSchema,
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 80,
  },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    try {
      const values = await formApi.validate();
      
      if (isEdit.value && editId.value) {
        const updateData: UpdateUserCommand = {
          id: editId.value,
          ...values,
        };
        await updateUser(updateData);
        message.success('更新成功');
      } else {
        const createData: CreateUserCommand = values as CreateUserCommand;
        await createUser(createData);
        message.success('创建成功');
      }
      
      modalApi.close();
      emit('success');
    } catch (error: unknown) {
      const err = error as { errorFields?: unknown; message?: string };
      if (err?.errorFields) return;
      message.error(err.message || '操作失败');
    }
  },
  onOpenChange(isOpen: boolean) {
    if (isOpen) {
      loadOptions();
    }
  },
});

// 打开新增弹窗
function openCreate() {
  isEdit.value = false;
  editId.value = undefined;
  formApi.resetForm();
  modalApi.setState({ title: '新增用户' });
  modalApi.open();
}

// 打开编辑弹窗
function openEdit(record: Record<string, any>) {
  isEdit.value = true;
  editId.value = record.id;
  formApi.resetForm();
  formApi.setValues({
    username: record.username,
    realName: record.realName,
    email: record.email,
    phone: record.phone,
    departmentId: record.departmentId,
    position: record.position,
    roleIds: record.roleIds || [],
    status: record.status,
  });
  modalApi.setState({ title: '编辑用户' });
  modalApi.open();
}

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Modal class="w-[600px]">
    <Form />
  </Modal>
</template>
