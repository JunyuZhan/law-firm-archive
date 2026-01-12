<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type {
  CreateUserCommand,
  DepartmentDTO,
  RoleDTO,
  UpdateUserCommand,
} from '#/api/system/types';

import { ref, watch } from 'vue';

import { useVbenDrawer, z } from '@vben/common-ui';

import { Divider, message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import {
  createUser,
  getAllRoles,
  getDepartmentTree,
  updateUser,
} from '#/api/system';

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
    updateFormSchema();
  } catch (error) {
    console.error('加载选项失败:', error);
  }
}

// 基础表单 Schema - 使用更适合抽屉的布局
function getFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'username',
      label: '用户名',
      component: 'Input',
      rules: z.string().min(1, '请输入用户名'),
      componentProps: {
        placeholder: '请输入用户名',
        disabled: isEdit.value,
      },
      help: '用户登录时使用的账号名称',
    },
    {
      fieldName: 'password',
      label: '密码',
      component: 'InputPassword',
      rules: isEdit.value
        ? z.string().optional()
        : z.string().min(6, '密码长度至少6位'),
      hide: isEdit.value,
      componentProps: {
        placeholder: '请输入密码（至少6位）',
      },
      help: '密码需包含大小写字母和数字',
    },
    {
      fieldName: 'realName',
      label: '姓名',
      component: 'Input',
      rules: z.string().min(1, '请输入姓名'),
      componentProps: {
        placeholder: '请输入真实姓名',
      },
    },
    {
      fieldName: 'employeeNo',
      label: '工号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入工号',
      },
    },
    {
      fieldName: 'email',
      label: '邮箱',
      component: 'Input',
      componentProps: {
        placeholder: '请输入邮箱地址',
      },
      help: '用于接收系统通知',
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
      label: '所属部门',
      component: 'TreeSelect',
      componentProps: {
        placeholder: '请选择部门',
        treeData: departments.value,
        fieldNames: { label: 'name', value: 'id', children: 'children' },
        allowClear: true,
        style: { width: '100%' },
        treeDefaultExpandAll: true,
        dropdownStyle: { maxHeight: '400px', overflow: 'auto' },
      },
    },
    {
      fieldName: 'position',
      label: '职位',
      component: 'Input',
      componentProps: {
        placeholder: '请输入职位名称',
      },
    },
    {
      fieldName: 'lawyerLicenseNo',
      label: '执业证号',
      component: 'Input',
      componentProps: {
        placeholder: '请输入律师执业证号',
      },
      help: '如为律师请填写执业资格证号',
    },
    {
      fieldName: 'joinDate',
      label: '入职日期',
      component: 'DatePicker',
      componentProps: {
        placeholder: '请选择入职日期',
        style: { width: '100%' },
        format: 'YYYY-MM-DD',
        valueFormat: 'YYYY-MM-DD',
      },
    },
    {
      fieldName: 'compensationType',
      label: '薪酬模式',
      component: 'Select',
      defaultValue: 'COMMISSION',
      componentProps: {
        placeholder: '请选择薪酬模式',
        options: [
          { label: '提成制', value: 'COMMISSION' },
          { label: '固定工资', value: 'SALARIED' },
          { label: '混合制', value: 'HYBRID' },
        ],
      },
      help: '决定是否参与项目提成分配',
    },
    {
      fieldName: 'canBeOriginator',
      label: '案源人资格',
      component: 'Switch',
      defaultValue: true,
      componentProps: {
        checkedChildren: '是',
        unCheckedChildren: '否',
      },
      help: '是否可作为案件案源人',
    },
    {
      fieldName: 'roleIds',
      label: '角色权限',
      component: 'Select',
      componentProps: {
        placeholder: '请选择角色（可多选）',
        mode: 'multiple',
        style: { width: '100%' },
        options: roles.value.map((r: RoleDTO) => ({
          label: r.roleName,
          value: r.id,
        })),
      },
      help: '用户拥有的系统角色决定其权限范围',
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
  ];
}

const [Form, formApi] = useVbenForm({
  schema: getFormSchema(),
  showDefaultActions: false,
  commonConfig: {
    labelWidth: 100,
  },
});

// 更新表单schema
function updateFormSchema() {
  formApi.setState({ schema: getFormSchema() });
}

// 监听isEdit变化，更新表单schema
watch(isEdit, () => {
  updateFormSchema();
});

const [Drawer, drawerApi] = useVbenDrawer({
  overlayBlur: 4, // 遮罩层模糊效果
  placement: 'right', // 默认从右侧滑出
  async onConfirm() {
    try {
      await formApi.validate();
      const values = await formApi.getValues();

      if (isEdit.value && editId.value) {
        const updateData: UpdateUserCommand = {
          id: editId.value,
          realName: values.realName,
          employeeNo: values.employeeNo,
          email: values.email,
          phone: values.phone,
          departmentId: values.departmentId,
          position: values.position,
          lawyerLicenseNo: values.lawyerLicenseNo,
          joinDate: values.joinDate,
          compensationType: values.compensationType,
          canBeOriginator: values.canBeOriginator,
          roleIds: values.roleIds,
        };
        await updateUser(updateData);
        message.success('更新成功');
      } else {
        const createData: CreateUserCommand = {
          username: values.username,
          password: values.password,
          realName: values.realName,
          employeeNo: values.employeeNo,
          email: values.email,
          phone: values.phone,
          departmentId: values.departmentId,
          position: values.position,
          lawyerLicenseNo: values.lawyerLicenseNo,
          joinDate: values.joinDate,
          compensationType: values.compensationType,
          canBeOriginator: values.canBeOriginator,
          roleIds: values.roleIds,
        };
        await createUser(createData);
        message.success('创建成功');
      }

      drawerApi.close();
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

// 设置抽屉方向
type DrawerPlacement = 'bottom' | 'left' | 'right' | 'top';
function setPlacement(placement: DrawerPlacement) {
  drawerApi.setState({ placement });
}

// 打开新增抽屉（左侧按钮 → 右侧抽屉）
function openCreate(placement: DrawerPlacement = 'right') {
  isEdit.value = false;
  editId.value = undefined;
  formApi.resetForm();
  setPlacement(placement);
  drawerApi.setState({ title: '新增用户' });
  drawerApi.open();
}

// 打开编辑抽屉（右侧按钮 → 左侧抽屉）
function openEdit(record: Record<string, any>, placement: DrawerPlacement = 'left') {
  isEdit.value = true;
  editId.value = record.id;
  formApi.resetForm();
  formApi.setValues({
    username: record.username,
    realName: record.realName,
    employeeNo: record.employeeNo,
    email: record.email,
    phone: record.phone,
    departmentId: record.departmentId,
    position: record.position,
    lawyerLicenseNo: record.lawyerLicenseNo,
    joinDate: record.joinDate,
    compensationType: record.compensationType || 'COMMISSION',
    canBeOriginator: record.canBeOriginator ?? true,
    roleIds: record.roleIds || [],
    status: record.status,
  });
  setPlacement(placement);
  drawerApi.setState({ title: '编辑用户' });
  drawerApi.open();
}

defineExpose({ openCreate, openEdit });
</script>

<template>
  <Drawer class="w-[480px]">
    <div class="px-2">
      <div class="mb-4 text-gray-500 text-sm">
        {{ isEdit ? '修改用户信息，用户名不可更改' : '创建新用户账号' }}
      </div>
      <Divider class="!my-3">基本信息</Divider>
      <Form />
    </div>
  </Drawer>
</template>

