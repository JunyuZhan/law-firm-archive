<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { message } from 'ant-design-vue';
import { createCase, updateCase, getCaseDetail, getCaseCategoryTree, type CaseLibraryDTO, type CaseCategoryDTO } from '#/api/knowledge';
import dayjs from 'dayjs';

const emit = defineEmits<{ success: [] }>();

const editingId = ref<number>();
const categories = ref<CaseCategoryDTO[]>([]);

const caseTypeOptions = [
  { label: '民事', value: 'CIVIL' },
  { label: '刑事', value: 'CRIMINAL' },
  { label: '行政', value: 'ADMINISTRATIVE' },
  { label: '破产', value: 'BANKRUPTCY' },
  { label: '知识产权', value: 'IP' },
  { label: '仲裁', value: 'ARBITRATION' },
  { label: '执行', value: 'ENFORCEMENT' },
  { label: '法律顾问', value: 'LEGAL_COUNSEL' },
  { label: '专项服务', value: 'SPECIAL_SERVICE' },
];

const resultOptions = [
  { label: '胜诉', value: 'WIN' },
  { label: '部分胜诉', value: 'PARTIAL_WIN' },
  { label: '败诉', value: 'LOSE' },
  { label: '调解', value: 'MEDIATION' },
  { label: '撤诉', value: 'WITHDRAW' },
];

const referenceValueOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' },
];

function convertToTreeData(data: CaseCategoryDTO[]): any[] {
  return data.map(item => ({
    label: item.name,
    value: item.id,
    children: item.children ? convertToTreeData(item.children) : undefined,
  }));
}

const [Form, formApi] = useVbenForm({
  schema: [
    { fieldName: 'name', label: '案例名称', component: 'Input', rules: 'required', componentProps: { placeholder: '如：张某诉李某合同纠纷案' } },
    { fieldName: 'caseType', label: '案由类型', component: 'Select', componentProps: { options: caseTypeOptions }, defaultValue: 'CIVIL' },
    { fieldName: 'categoryId', label: '案例分类', component: 'TreeSelect', componentProps: { placeholder: '选择分类', treeData: [], allowClear: true } },
    { fieldName: 'court', label: '审理法院', component: 'Input', componentProps: { placeholder: '如：北京市朝阳区人民法院' } },
    { fieldName: 'judgmentDate', label: '判决日期', component: 'DatePicker', componentProps: { class: 'w-full' } },
    { fieldName: 'result', label: '案件结果', component: 'Select', componentProps: { options: resultOptions }, defaultValue: 'WIN' },
    { fieldName: 'lawyerId', label: '经办律师', component: 'ApiTreeSelect', componentProps: { api: () => import('#/api/system').then(m => m.getUserTree()), placeholder: '选择经办律师', fieldNames: { label: 'name', value: 'id', children: 'children' } } },
    { fieldName: 'referenceValue', label: '参考价值', component: 'Select', componentProps: { options: referenceValueOptions }, defaultValue: 'MEDIUM' },
    { fieldName: 'summary', label: '案例摘要', component: 'Textarea', componentProps: { rows: 4, placeholder: '案例摘要、裁判要旨等' } },
  ],
  showDefaultActions: false,
  commonConfig: { componentProps: { class: 'w-full' } },
});

const [Modal, modalApi] = useVbenModal({
  async onConfirm() {
    const values = await formApi.validate();
    try {
      const data = {
        ...values,
        judgmentDate: values.judgmentDate ? dayjs(values.judgmentDate).format('YYYY-MM-DD') : undefined,
      };
      if (editingId.value) {
        await updateCase(editingId.value, data);
        message.success('更新成功');
      } else {
        await createCase(data);
        message.success('创建成功');
      }
      emit('success');
      modalApi.close();
    } catch (error: any) {
      message.error(error.message || '操作失败');
    }
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      formApi.resetForm();
      editingId.value = undefined;
    }
  },
});

async function loadCategories() {
  try {
    const res = await getCaseCategoryTree();
    categories.value = res || [];
    formApi.updateSchema([{ fieldName: 'categoryId', componentProps: { treeData: convertToTreeData(categories.value) } }]);
  } catch (error) {
    console.error('加载分类失败', error);
  }
}

async function open(record?: CaseLibraryDTO) {
  await loadCategories();
  if (record) {
    try {
      const detail = await getCaseDetail(record.id);
      editingId.value = record.id;
      formApi.setValues({
        name: detail.name,
        caseType: detail.caseType || 'CIVIL',
        categoryId: detail.categoryId,
        court: detail.court || '',
        judgmentDate: detail.judgmentDate ? dayjs(detail.judgmentDate) : undefined,
        result: detail.result || 'WIN',
        lawyerId: detail.lawyerId,
        referenceValue: detail.referenceValue || 'MEDIUM',
        summary: detail.summary || '',
      });
    } catch (error: any) {
      message.error(error.message || '获取详情失败');
      return;
    }
  }
  modalApi.open();
}

defineExpose({ open });
</script>

<template>
  <Modal :title="editingId ? '编辑案例' : '添加案例'" class="w-[600px]">
    <Form />
  </Modal>
</template>
