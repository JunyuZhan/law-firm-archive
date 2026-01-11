<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  DatePicker,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Textarea,
  Descriptions,
  DescriptionsItem,
  Divider,
  Popconfirm,
  Tabs,
  Row,
  Col,
  Upload,
  Spin,
  Tooltip,
} from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import dayjs from 'dayjs';

import {
  getEmployeeList,
  getEmployeeDetail,
  createEmployee,
  updateEmployee,
  deleteEmployee,
} from '#/api/hr/employee';
import { recognizeIdCard, type OcrResultDTO, OCR_DISABLED, OCR_DISABLED_MESSAGE } from '#/api/ocr';
import type {
  EmployeeDTO,
  EmployeeQuery,
  CreateEmployeeCommand,
  UpdateEmployeeCommand,
} from '#/api/hr/employee';
import { getUserSelectOptions } from '#/api/system';
import type { UserDTO, UserQuery } from '#/api/system/types';
import { getDepartmentTreePublic } from '#/api/system';
import type { DepartmentDTO } from '#/api/system/types';

defineOptions({ name: 'EmployeeManagement' });

// Tab切换
const activeTab = ref('users');

// ========== Tab 1: 用户列表（用于批量创建员工档案） ==========
const userLoading = ref(false);
const userList = ref<UserDTO[]>([]);
const userTotal = ref(0);
const selectedUserIds = ref<number[]>([]);
const userQueryParams = reactive<UserQuery>({
  pageNum: 1,
  pageSize: 10,
  username: undefined,
  realName: undefined,
  departmentId: undefined,
  status: 'ACTIVE',
});

const userPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const userColumns = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 120 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 100 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 120 },
  { title: '职位', dataIndex: 'position', key: 'position', width: 100 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 150 },
  { title: '工号', dataIndex: 'employeeNo', key: 'employeeNo', width: 120 },
  { title: '入职日期', dataIndex: 'joinDate', key: 'joinDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
];

// 已创建员工档案的用户ID列表（用于标记）
const employeeUserIds = ref<Set<number>>(new Set());

// ========== Tab 2: 员工档案列表 ==========
const searchForm = reactive<EmployeeQuery>({
  pageNum: 1,
  pageSize: 10,
  employeeNo: undefined,
  realName: undefined,
  departmentId: undefined,
  workStatus: undefined,
  position: undefined,
});

const workStatusOptions = [
  { label: '在职', value: 'ACTIVE' },
  { label: '试用期', value: 'PROBATION' },
  { label: '离职', value: 'RESIGNED' },
  { label: '停薪留职', value: 'SUSPENDED' },
];

const workStatusColorMap: Record<string, string> = {
  ACTIVE: 'green',
  PROBATION: 'orange',
  RESIGNED: 'red',
  SUSPENDED: 'default',
};

const workStatusTextMap: Record<string, string> = {
  ACTIVE: '在职',
  PROBATION: '试用期',
  RESIGNED: '离职',
  SUSPENDED: '停薪留职',
};

const columns = [
  { title: '员工编号', dataIndex: 'employeeNo', key: 'employeeNo', width: 120 },
  { title: '姓名', dataIndex: 'realName', key: 'realName', width: 100 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 120 },
  { title: '职位', dataIndex: 'position', key: 'position', width: 100 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 120 },
  { title: '邮箱', dataIndex: 'email', key: 'email', width: 150 },
  { title: '入职日期', dataIndex: 'entryDate', key: 'entryDate', width: 120 },
  { title: '工作状态', dataIndex: 'workStatus', key: 'workStatus', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
];

const tableData = ref<EmployeeDTO[]>([]);
const loading = ref(false);
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
});

const departmentTree = ref<DepartmentDTO[]>([]);

// 新增/编辑弹窗
const modalVisible = ref(false);
const modalLoading = ref(false);
const editingId = ref<number | null>(null);
const ocrLoading = ref(false);
const employeeForm = reactive<CreateEmployeeCommand>({
  userId: undefined as number | undefined,
  employeeNo: '',
  gender: undefined,
  birthDate: undefined,
  idCard: '',
  nationality: '中国',
  nativePlace: '',
  politicalStatus: undefined,
  education: undefined,
  major: '',
  graduationSchool: '',
  graduationDate: undefined,
  emergencyContact: '',
  emergencyPhone: '',
  address: '',
  lawyerLicenseNo: '',
  licenseIssueDate: undefined,
  licenseExpireDate: undefined,
  licenseStatus: undefined,
  practiceArea: '',
  practiceYears: undefined,
  position: '',
  level: undefined,
  entryDate: undefined,
  probationEndDate: undefined,
  workStatus: 'ACTIVE',
  remark: '',
});

// 详情弹窗
const detailVisible = ref(false);
const currentRecord = ref<EmployeeDTO | null>(null);

// 批量导入弹窗
const batchImportVisible = ref(false);
const batchImportLoading = ref(false);

// ========== 用户列表相关方法 ==========
async function fetchUsers() {
  userLoading.value = true;
  try {
    const params = {
      ...userQueryParams,
      pageNum: userPagination.current,
      pageSize: userPagination.pageSize,
    };
    // 使用公共接口，无需 sys:user:list 权限
    const res = await getUserSelectOptions(params);
    userList.value = res.list || [];
    userPagination.total = res.total || 0;
    userTotal.value = res.total || 0;
    
    // 标记已创建员工档案的用户
    await markEmployeesWithProfile();
  } catch (error) {
    console.error('获取用户列表失败:', error);
    message.error('获取用户列表失败');
  } finally {
    userLoading.value = false;
  }
}

// 标记已创建员工档案的用户
async function markEmployeesWithProfile() {
  try {
    const res = await getEmployeeList({ pageNum: 1, pageSize: 10000 });
    employeeUserIds.value = new Set((res.list || []).map((emp: EmployeeDTO) => emp.userId).filter(Boolean));
  } catch (error) {
    console.error('获取员工档案列表失败:', error);
  }
}

function handleUserSearch() {
  userPagination.current = 1;
  userQueryParams.pageNum = 1;
  fetchUsers();
}

function handleUserReset() {
  Object.assign(userQueryParams, {
    pageNum: 1,
    pageSize: 10,
    username: undefined,
    realName: undefined,
    departmentId: undefined,
    status: 'ACTIVE',
  });
  userPagination.current = 1;
  fetchUsers();
}

function handleUserTableChange(pag: any) {
  userPagination.current = pag.current;
  userPagination.pageSize = pag.pageSize;
  userQueryParams.pageNum = pag.current;
  userQueryParams.pageSize = pag.pageSize;
  fetchUsers();
}

// 批量创建员工档案
async function handleBatchImport() {
  if (selectedUserIds.value.length === 0) {
    message.warning('请至少选择一个用户');
    return;
  }

  // 过滤掉已创建档案的用户
  const toCreate = selectedUserIds.value.filter(id => !employeeUserIds.value.has(id));
  if (toCreate.length === 0) {
    message.warning('所选用户均已创建员工档案');
    return;
  }

  batchImportLoading.value = true;
  try {
    let successCount = 0;
    let failCount = 0;
    
    for (const userId of toCreate) {
      try {
        const user = userList.value.find(u => u.id === userId);
        if (!user) continue;

        await createEmployee({
          userId,
          employeeNo: user.employeeNo || undefined,
          position: user.position || undefined,
          entryDate: user.joinDate || undefined,
          workStatus: 'ACTIVE',
        });
        successCount++;
      } catch (error: any) {
        console.error(`创建用户 ${userId} 的员工档案失败:`, error);
        failCount++;
      }
    }

    message.success(`批量创建完成：成功 ${successCount} 条，失败 ${failCount} 条`);
    batchImportVisible.value = false;
    selectedUserIds.value = [];
    await markEmployeesWithProfile();
    await fetchUsers();
    // 切换到员工档案Tab
    activeTab.value = 'employees';
    await fetchData();
  } catch (error) {
    console.error('批量创建失败:', error);
    message.error('批量创建失败');
  } finally {
    batchImportLoading.value = false;
  }
}

// ========== 员工档案相关方法 ==========
async function fetchData() {
  loading.value = true;
  try {
    const params = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    };
    const res = await getEmployeeList(params);
    tableData.value = res.list || [];
    pagination.total = res.total || 0;
  } catch (error) {
    console.error('获取员工列表失败:', error);
    message.error('获取员工列表失败');
  } finally {
    loading.value = false;
  }
}

async function fetchDepartments() {
  try {
    departmentTree.value = await getDepartmentTreePublic();
  } catch (error) {
    console.error('获取部门列表失败:', error);
    departmentTree.value = [];
  }
}

function handleSearch() {
  pagination.current = 1;
  searchForm.pageNum = 1;
  fetchData();
}

function handleReset() {
  Object.assign(searchForm, {
    pageNum: 1,
    pageSize: 10,
    employeeNo: undefined,
    realName: undefined,
    departmentId: undefined,
    workStatus: undefined,
    position: undefined,
  });
  pagination.current = 1;
  fetchData();
}

function handleTableChange(pag: any) {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  searchForm.pageNum = pag.current;
  searchForm.pageSize = pag.pageSize;
  fetchData();
}

function handleAdd() {
  editingId.value = null;
  Object.assign(employeeForm, {
    userId: undefined,
    employeeNo: '',
    gender: undefined,
    birthDate: undefined,
    idCard: '',
    nationality: '中国',
    nativePlace: '',
    politicalStatus: undefined,
    education: undefined,
    major: '',
    graduationSchool: '',
    graduationDate: undefined,
    emergencyContact: '',
    emergencyPhone: '',
    address: '',
    lawyerLicenseNo: '',
    licenseIssueDate: undefined,
    licenseExpireDate: undefined,
    licenseStatus: undefined,
    practiceArea: '',
    practiceYears: undefined,
    position: '',
    level: undefined,
    entryDate: undefined,
    probationEndDate: undefined,
    workStatus: 'ACTIVE',
    remark: '',
  });
  modalVisible.value = true;
}

async function handleEdit(record: EmployeeDTO) {
  editingId.value = record.id;
  try {
    const detail = await getEmployeeDetail(record.id);
    Object.assign(employeeForm, {
      userId: detail.userId,
      employeeNo: detail.employeeNo || '',
      gender: detail.gender,
      birthDate: detail.birthDate ? dayjs(detail.birthDate) : undefined,
      idCard: detail.idCard || '',
      nationality: detail.nationality || '中国',
      nativePlace: detail.nativePlace || '',
      politicalStatus: detail.politicalStatus,
      education: detail.education,
      major: detail.major || '',
      graduationSchool: detail.graduationSchool || '',
      graduationDate: detail.graduationDate ? dayjs(detail.graduationDate) : undefined,
      emergencyContact: detail.emergencyContact || '',
      emergencyPhone: detail.emergencyPhone || '',
      address: detail.address || '',
      lawyerLicenseNo: detail.lawyerLicenseNo || '',
      licenseIssueDate: detail.licenseIssueDate ? dayjs(detail.licenseIssueDate) : undefined,
      licenseExpireDate: detail.licenseExpireDate ? dayjs(detail.licenseExpireDate) : undefined,
      licenseStatus: detail.licenseStatus,
      practiceArea: detail.practiceArea || '',
      practiceYears: detail.practiceYears,
      position: detail.position || '',
      level: detail.level,
      entryDate: detail.entryDate ? dayjs(detail.entryDate) : undefined,
      probationEndDate: detail.probationEndDate ? dayjs(detail.probationEndDate) : undefined,
      workStatus: detail.workStatus || 'ACTIVE',
      remark: detail.remark || '',
    });
    modalVisible.value = true;
  } catch (error) {
    console.error('获取员工详情失败:', error);
    message.error('获取员工详情失败');
  }
}

async function handleView(record: EmployeeDTO) {
  try {
    currentRecord.value = await getEmployeeDetail(record.id);
    detailVisible.value = true;
  } catch (error) {
    console.error('获取员工详情失败:', error);
    message.error('获取员工详情失败');
  }
}

async function handleDelete(id: number) {
  try {
    await deleteEmployee(id);
    message.success('删除成功');
    fetchData();
    await markEmployeesWithProfile();
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  }
}

// 身份证OCR识别
async function handleIdCardOcr(info: any) {
  const file = info.file.originFileObj || info.file;
  if (!file) return;
  
  ocrLoading.value = true;
  try {
    const result = await recognizeIdCard(file, true); // 识别正面
    
    if (result.success) {
      // 自动填充表单
      if (result.idNumber) employeeForm.idCard = result.idNumber;
      if (result.name && !employeeForm.realName) {
        employeeForm.realName = result.name;
      }
      if (result.ethnicity) employeeForm.nationality = result.ethnicity;
      // 注意：身份证地址是住址，不是籍贯，这里不自动填充籍贯
      if (result.gender) {
        employeeForm.gender = result.gender === '男' ? 'MALE' : 'FEMALE';
      }
      if (result.birthDate) {
        employeeForm.birthDate = dayjs(result.birthDate) as any;
      }
      
      message.success(`身份证识别成功！置信度: ${Math.round((result.confidence || 0) * 100)}%`);
    } else {
      message.error(result.errorMessage || '身份证识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || '身份证识别失败');
  } finally {
    ocrLoading.value = false;
  }
}

async function handleSave() {
  if (!employeeForm.userId && !editingId.value) {
    message.warning('请选择用户');
    return;
  }

  modalLoading.value = true;
  try {
    const formData: any = {
      ...employeeForm,
      birthDate: employeeForm.birthDate ? dayjs(employeeForm.birthDate).format('YYYY-MM-DD') : undefined,
      graduationDate: employeeForm.graduationDate ? dayjs(employeeForm.graduationDate).format('YYYY-MM-DD') : undefined,
      licenseIssueDate: employeeForm.licenseIssueDate ? dayjs(employeeForm.licenseIssueDate).format('YYYY-MM-DD') : undefined,
      licenseExpireDate: employeeForm.licenseExpireDate ? dayjs(employeeForm.licenseExpireDate).format('YYYY-MM-DD') : undefined,
      entryDate: employeeForm.entryDate ? dayjs(employeeForm.entryDate).format('YYYY-MM-DD') : undefined,
      probationEndDate: employeeForm.probationEndDate ? dayjs(employeeForm.probationEndDate).format('YYYY-MM-DD') : undefined,
    };

    if (editingId.value) {
      await updateEmployee(editingId.value, formData);
      message.success('更新成功');
    } else {
      await createEmployee(formData);
      message.success('创建成功');
      await markEmployeesWithProfile();
    }
    modalVisible.value = false;
    fetchData();
  } catch (error: any) {
    console.error('保存失败:', error);
    message.error(error?.message || '保存失败');
  } finally {
    modalLoading.value = false;
  }
}

// 从用户创建员工档案
function handleCreateFromUser(user: UserDTO) {
  editingId.value = null;
  Object.assign(employeeForm, {
    userId: user.id,
    employeeNo: user.employeeNo || '',
    position: user.position || '',
    entryDate: user.joinDate ? dayjs(user.joinDate) : undefined,
    workStatus: 'ACTIVE',
    gender: undefined,
    birthDate: undefined,
    idCard: '',
    nationality: '中国',
    nativePlace: '',
    politicalStatus: undefined,
    education: undefined,
    major: '',
    graduationSchool: '',
    graduationDate: undefined,
    emergencyContact: '',
    emergencyPhone: '',
    address: '',
    lawyerLicenseNo: user.lawyerLicenseNo || '',
    licenseIssueDate: undefined,
    licenseExpireDate: undefined,
    licenseStatus: undefined,
    practiceArea: '',
    practiceYears: undefined,
    level: undefined,
    probationEndDate: undefined,
    remark: '',
  });
  modalVisible.value = true;
}

function renderDepartmentOptions(departments: DepartmentDTO[]): any[] {
  const options: any[] = [];
  departments.forEach((dept) => {
    options.push({
      label: dept.name,
      value: dept.id,
    });
    if (dept.children && dept.children.length > 0) {
      options.push(...renderDepartmentOptions(dept.children));
    }
  });
  return options;
}

// Tab切换
function handleTabChange(key: string) {
  activeTab.value = key;
  if (key === 'users') {
    fetchUsers();
  } else if (key === 'employees') {
    fetchData();
  }
}

onMounted(() => {
  fetchDepartments();
  if (activeTab.value === 'users') {
    fetchUsers();
  } else {
    fetchData();
  }
});
</script>

<template>
  <Page title="员工档案" description="管理员工档案信息，可从用户列表批量导入">
    <Card>
      <Tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <!-- Tab 1: 用户列表 -->
        <Tabs.TabPane key="users" tab="用户列表">
          <div style="margin-bottom: 16px">
            <Form layout="inline" :model="userQueryParams" @finish="handleUserSearch">
              <FormItem label="用户名">
                <Input v-model:value="userQueryParams.username" placeholder="请输入用户名" allow-clear style="width: 150px" />
              </FormItem>
              <FormItem label="姓名">
                <Input v-model:value="userQueryParams.realName" placeholder="请输入姓名" allow-clear style="width: 150px" />
              </FormItem>
              <FormItem label="部门">
                <Select
                  v-model:value="userQueryParams.departmentId"
                  placeholder="请选择部门"
                  allow-clear
                  style="width: 150px"
                >
                  <Select.Option
                    v-for="dept in renderDepartmentOptions(departmentTree)"
                    :key="dept.value"
                    :value="dept.value"
                  >
                    {{ dept.label }}
                  </Select.Option>
                </Select>
              </FormItem>
              <FormItem>
                <Space>
                  <Button type="primary" html-type="submit">查询</Button>
                  <Button @click="handleUserReset">重置</Button>
                  <Button
                    type="primary"
                    :disabled="selectedUserIds.length === 0"
                    @click="batchImportVisible = true"
                  >
                    批量创建员工档案 ({{ selectedUserIds.length }})
                  </Button>
                </Space>
              </FormItem>
            </Form>
          </div>

          <Table
            :columns="userColumns"
            :data-source="userList"
            :loading="userLoading"
            :pagination="userPagination"
            row-key="id"
            :scroll="{ x: 1200 }"
            :row-selection="{
              selectedRowKeys: selectedUserIds,
              onChange: (keys: number[]) => {
                selectedUserIds = keys;
              },
              getCheckboxProps: (record: UserDTO) => ({
                disabled: employeeUserIds.has(record.id || 0),
              }),
            }"
            @change="handleUserTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'joinDate'">
                {{ record.joinDate ? dayjs(record.joinDate).format('YYYY-MM-DD') : '-' }}
              </template>
              <template v-else-if="column.key === 'status'">
                <Tag :color="record.status === 'ACTIVE' ? 'green' : 'default'">
                  {{ record.status === 'ACTIVE' ? '启用' : '禁用' }}
                </Tag>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>

        <!-- Tab 2: 员工档案 -->
        <Tabs.TabPane key="employees" tab="员工档案">
          <div style="margin-bottom: 16px">
            <Form layout="inline" :model="searchForm" @finish="handleSearch">
              <FormItem label="员工编号">
                <Input v-model:value="searchForm.employeeNo" placeholder="请输入员工编号" allow-clear style="width: 150px" />
              </FormItem>
              <FormItem label="姓名">
                <Input v-model:value="searchForm.realName" placeholder="请输入姓名" allow-clear style="width: 150px" />
              </FormItem>
              <FormItem label="部门">
                <Select
                  v-model:value="searchForm.departmentId"
                  placeholder="请选择部门"
                  allow-clear
                  style="width: 150px"
                >
                  <Select.Option
                    v-for="dept in renderDepartmentOptions(departmentTree)"
                    :key="dept.value"
                    :value="dept.value"
                  >
                    {{ dept.label }}
                  </Select.Option>
                </Select>
              </FormItem>
              <FormItem label="工作状态">
                <Select v-model:value="searchForm.workStatus" placeholder="请选择状态" allow-clear style="width: 120px">
                  <Select.Option v-for="item in workStatusOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </Select.Option>
                </Select>
              </FormItem>
              <FormItem label="职位">
                <Input v-model:value="searchForm.position" placeholder="请输入职位" allow-clear style="width: 120px" />
              </FormItem>
              <FormItem>
                <Space>
                  <Button type="primary" html-type="submit">查询</Button>
                  <Button @click="handleReset">重置</Button>
                  <Button type="primary" @click="handleAdd">新建员工档案</Button>
                </Space>
              </FormItem>
            </Form>
          </div>

          <Table
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
            :scroll="{ x: 1400 }"
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'entryDate'">
                {{ record.entryDate ? dayjs(record.entryDate).format('YYYY-MM-DD') : '-' }}
              </template>
              <template v-else-if="column.key === 'workStatus'">
                <Tag :color="workStatusColorMap[record.workStatus || '']">
                  {{ workStatusTextMap[record.workStatus || ''] || record.workStatus }}
                </Tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <Space>
                  <Button type="link" size="small" @click="handleView(record)">查看</Button>
                  <Button type="link" size="small" @click="handleEdit(record)">编辑</Button>
                  <Popconfirm title="确定要删除吗？" @confirm="handleDelete(record.id)">
                    <Button type="link" size="small" danger>删除</Button>
                  </Popconfirm>
                </Space>
              </template>
            </template>
          </Table>
        </Tabs.TabPane>
      </Tabs>
    </Card>

    <!-- 批量导入确认弹窗 -->
    <Modal
      v-model:open="batchImportVisible"
      title="批量创建员工档案"
      :confirm-loading="batchImportLoading"
      @ok="handleBatchImport"
      @cancel="batchImportVisible = false"
    >
      <p>确定要为以下 {{ selectedUserIds.length }} 个用户创建员工档案吗？</p>
      <p style=" margin-top: 8px; font-size: 12px;color: #999">
        注意：已创建员工档案的用户将被自动跳过
      </p>
    </Modal>

    <!-- 新增/编辑弹窗 -->
    <Modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑员工档案' : '新建员工档案'"
      :width="800"
      :confirm-loading="modalLoading"
      @ok="handleSave"
      @cancel="modalVisible = false"
    >
      <Form :model="employeeForm" layout="vertical">
        <FormItem label="关联用户" :required="!editingId">
          <Select
            v-model:value="employeeForm.userId"
            placeholder="请选择用户"
            :disabled="!!editingId"
            show-search
            :filter-option="(input: string, option: any) => option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0"
          >
            <Select.Option v-for="user in userList" :key="user.id" :value="user.id">
              {{ user.realName }} ({{ user.username }})
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="员工编号">
          <Input v-model:value="employeeForm.employeeNo" placeholder="请输入员工编号" />
        </FormItem>

        <!-- OCR智能识别区域 -->
        <div v-if="!OCR_DISABLED" class="mb-4 p-3 bg-blue-50 rounded border border-blue-200">
          <div class="flex items-center mb-2">
            <IconifyIcon icon="ant-design:scan-outlined" class="text-blue-500 mr-2" />
            <span class="font-medium text-blue-700">身份证智能识别</span>
            <span class="text-gray-500 text-xs ml-2">上传身份证正面自动填充</span>
          </div>
          <Spin :spinning="ocrLoading" size="small">
            <Upload
              :show-upload-list="false"
              :before-upload="() => false"
              accept="image/*"
              @change="handleIdCardOcr"
            >
              <Tooltip title="上传身份证正面照片，自动识别姓名、身份证号、出生日期等">
                <Button :loading="ocrLoading" :disabled="ocrLoading" size="small">
                  <template #icon><IconifyIcon icon="ant-design:idcard-outlined" /></template>
                  识别身份证
                </Button>
              </Tooltip>
            </Upload>
          </Spin>
        </div>
        <!-- OCR禁用提示 -->
        <div v-else class="mb-4 p-3 bg-gray-50 rounded border border-gray-200">
          <div class="flex items-center">
            <IconifyIcon icon="ant-design:scan-outlined" class="text-gray-400 mr-2" />
            <span class="font-medium text-gray-500">身份证智能识别</span>
            <Tag color="default" class="ml-2">暂不可用</Tag>
          </div>
          <div class="text-gray-400 text-xs mt-1">{{ OCR_DISABLED_MESSAGE }}</div>
        </div>

        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="性别">
              <Select v-model:value="employeeForm.gender" placeholder="请选择性别" allow-clear>
                <Select.Option value="MALE">男</Select.Option>
                <Select.Option value="FEMALE">女</Select.Option>
              </Select>
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="出生日期">
              <DatePicker v-model:value="employeeForm.birthDate" style="width: 100%" />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="身份证号">
              <Input v-model:value="employeeForm.idCard" placeholder="请输入身份证号" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="民族">
              <Input v-model:value="employeeForm.nationality" placeholder="请输入民族" />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="籍贯">
              <Input v-model:value="employeeForm.nativePlace" placeholder="请输入籍贯" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="政治面貌">
              <Select v-model:value="employeeForm.politicalStatus" placeholder="请选择政治面貌" allow-clear>
                <Select.Option value="PARTY_MEMBER">党员</Select.Option>
                <Select.Option value="LEAGUE_MEMBER">团员</Select.Option>
                <Select.Option value="MASSES">群众</Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="学历">
              <Select v-model:value="employeeForm.education" placeholder="请选择学历" allow-clear>
                <Select.Option value="DOCTOR">博士</Select.Option>
                <Select.Option value="MASTER">硕士</Select.Option>
                <Select.Option value="BACHELOR">本科</Select.Option>
                <Select.Option value="COLLEGE">专科</Select.Option>
                <Select.Option value="HIGH_SCHOOL">高中</Select.Option>
              </Select>
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="专业">
              <Input v-model:value="employeeForm.major" placeholder="请输入专业" />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="毕业院校">
              <Input v-model:value="employeeForm.graduationSchool" placeholder="请输入毕业院校" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="毕业日期">
              <DatePicker v-model:value="employeeForm.graduationDate" style="width: 100%" />
            </FormItem>
          </Col>
        </Row>
        <Divider>紧急联系人</Divider>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="紧急联系人">
              <Input v-model:value="employeeForm.emergencyContact" placeholder="请输入紧急联系人" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="紧急联系电话">
              <Input v-model:value="employeeForm.emergencyPhone" placeholder="请输入紧急联系电话" />
            </FormItem>
          </Col>
        </Row>
        <FormItem label="联系地址">
          <Input v-model:value="employeeForm.address" placeholder="请输入联系地址" />
        </FormItem>
        <Divider>律师执业信息</Divider>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="律师执业证号">
              <Input v-model:value="employeeForm.lawyerLicenseNo" placeholder="请输入律师执业证号" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="执业证状态">
              <Select v-model:value="employeeForm.licenseStatus" placeholder="请选择状态" allow-clear>
                <Select.Option value="VALID">有效</Select.Option>
                <Select.Option value="EXPIRED">过期</Select.Option>
                <Select.Option value="SUSPENDED">暂停</Select.Option>
              </Select>
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="执业证发证日期">
              <DatePicker v-model:value="employeeForm.licenseIssueDate" style="width: 100%" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="执业证到期日期">
              <DatePicker v-model:value="employeeForm.licenseExpireDate" style="width: 100%" />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="执业领域">
              <Input v-model:value="employeeForm.practiceArea" placeholder="请输入执业领域" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="执业年限">
              <Input
                v-model:value="employeeForm.practiceYears"
                type="number"
                placeholder="请输入执业年限"
              />
            </FormItem>
          </Col>
        </Row>
        <Divider>工作信息</Divider>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="职位">
              <Input v-model:value="employeeForm.position" placeholder="请输入职位" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="级别">
              <Input v-model:value="employeeForm.level" placeholder="请输入级别" />
            </FormItem>
          </Col>
        </Row>
        <Row :gutter="16">
          <Col :span="12">
            <FormItem label="入职日期">
              <DatePicker v-model:value="employeeForm.entryDate" style="width: 100%" />
            </FormItem>
          </Col>
          <Col :span="12">
            <FormItem label="试用期结束日期">
              <DatePicker v-model:value="employeeForm.probationEndDate" style="width: 100%" />
            </FormItem>
          </Col>
        </Row>
        <FormItem label="工作状态">
          <Select v-model:value="employeeForm.workStatus" placeholder="请选择工作状态">
            <Select.Option v-for="item in workStatusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </Select.Option>
          </Select>
        </FormItem>
        <FormItem label="备注">
          <Textarea v-model:value="employeeForm.remark" :rows="3" placeholder="请输入备注" />
        </FormItem>
      </Form>
    </Modal>

    <!-- 详情弹窗 -->
    <Modal
      v-model:open="detailVisible"
      title="员工档案详情"
      :width="800"
      :footer="null"
    >
      <Descriptions v-if="currentRecord" :column="2" bordered>
        <DescriptionsItem label="员工编号">{{ currentRecord.employeeNo || '-' }}</DescriptionsItem>
        <DescriptionsItem label="姓名">{{ currentRecord.realName || '-' }}</DescriptionsItem>
        <DescriptionsItem label="部门">{{ currentRecord.departmentName || '-' }}</DescriptionsItem>
        <DescriptionsItem label="职位">{{ currentRecord.position || '-' }}</DescriptionsItem>
        <DescriptionsItem label="手机号">{{ currentRecord.phone || '-' }}</DescriptionsItem>
        <DescriptionsItem label="邮箱">{{ currentRecord.email || '-' }}</DescriptionsItem>
        <DescriptionsItem label="性别">
          {{ currentRecord.gender === 'MALE' ? '男' : currentRecord.gender === 'FEMALE' ? '女' : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="出生日期">
          {{ currentRecord.birthDate ? dayjs(currentRecord.birthDate).format('YYYY-MM-DD') : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="身份证号">{{ currentRecord.idCard || '-' }}</DescriptionsItem>
        <DescriptionsItem label="民族">{{ currentRecord.nationality || '-' }}</DescriptionsItem>
        <DescriptionsItem label="籍贯">{{ currentRecord.nativePlace || '-' }}</DescriptionsItem>
        <DescriptionsItem label="政治面貌">
          {{
            currentRecord.politicalStatus === 'PARTY_MEMBER'
              ? '党员'
              : currentRecord.politicalStatus === 'LEAGUE_MEMBER'
                ? '团员'
                : currentRecord.politicalStatus === 'MASSES'
                  ? '群众'
                  : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="学历">
          {{
            currentRecord.education === 'DOCTOR'
              ? '博士'
              : currentRecord.education === 'MASTER'
                ? '硕士'
                : currentRecord.education === 'BACHELOR'
                  ? '本科'
                  : currentRecord.education === 'COLLEGE'
                    ? '专科'
                    : currentRecord.education === 'HIGH_SCHOOL'
                      ? '高中'
                      : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="专业">{{ currentRecord.major || '-' }}</DescriptionsItem>
        <DescriptionsItem label="毕业院校">{{ currentRecord.graduationSchool || '-' }}</DescriptionsItem>
        <DescriptionsItem label="毕业日期">
          {{ currentRecord.graduationDate ? dayjs(currentRecord.graduationDate).format('YYYY-MM-DD') : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="紧急联系人">{{ currentRecord.emergencyContact || '-' }}</DescriptionsItem>
        <DescriptionsItem label="紧急联系电话">{{ currentRecord.emergencyPhone || '-' }}</DescriptionsItem>
        <DescriptionsItem label="联系地址" :span="2">{{ currentRecord.address || '-' }}</DescriptionsItem>
        <DescriptionsItem label="律师执业证号">{{ currentRecord.lawyerLicenseNo || '-' }}</DescriptionsItem>
        <DescriptionsItem label="执业证状态">
          {{
            currentRecord.licenseStatus === 'VALID'
              ? '有效'
              : currentRecord.licenseStatus === 'EXPIRED'
                ? '过期'
                : currentRecord.licenseStatus === 'SUSPENDED'
                  ? '暂停'
                  : '-'
          }}
        </DescriptionsItem>
        <DescriptionsItem label="执业证发证日期">
          {{ currentRecord.licenseIssueDate ? dayjs(currentRecord.licenseIssueDate).format('YYYY-MM-DD') : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="执业证到期日期">
          {{ currentRecord.licenseExpireDate ? dayjs(currentRecord.licenseExpireDate).format('YYYY-MM-DD') : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="执业领域">{{ currentRecord.practiceArea || '-' }}</DescriptionsItem>
        <DescriptionsItem label="执业年限">{{ currentRecord.practiceYears || '-' }}</DescriptionsItem>
        <DescriptionsItem label="级别">{{ currentRecord.level || '-' }}</DescriptionsItem>
        <DescriptionsItem label="入职日期">
          {{ currentRecord.entryDate ? dayjs(currentRecord.entryDate).format('YYYY-MM-DD') : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="试用期结束日期">
          {{ currentRecord.probationEndDate ? dayjs(currentRecord.probationEndDate).format('YYYY-MM-DD') : '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="工作状态">
          <Tag :color="workStatusColorMap[currentRecord.workStatus || '']">
            {{ workStatusTextMap[currentRecord.workStatus || ''] || currentRecord.workStatus }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="备注" :span="2">{{ currentRecord.remark || '-' }}</DescriptionsItem>
      </Descriptions>
    </Modal>
  </Page>
</template>
