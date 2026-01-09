<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useVbenModal } from '@vben/common-ui';
import { useVbenForm } from '#/adapter/form';
import { message, Upload, Spin, Alert, Tooltip } from 'ant-design-vue';
import { IconifyIcon } from '@vben/icons';
import { createCase, updateCase, getCaseDetail, getCaseCategoryTree, type CaseLibraryDTO, type CaseCategoryDTO } from '#/api/knowledge';
import { recognizeGeneral, type OcrResultDTO } from '#/api/ocr';
import dayjs from 'dayjs';

const emit = defineEmits<{ success: [] }>();

const editingId = ref<number>();
const categories = ref<CaseCategoryDTO[]>([]);
const ocrLoading = ref(false);

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

// OCR识别裁判文书
async function handleOcrJudgment(file: File) {
  ocrLoading.value = true;
  try {
    const result: OcrResultDTO = await recognizeGeneral(file);
    if (result.success && result.rawText) {
      const text = result.rawText;
      
      // 智能解析裁判文书内容
      const parsed: Record<string, any> = {};
      
      // 提取案件名称（如：XXX诉XXX纠纷一案）
      const caseNameMatch = text.match(/([^，。]+诉[^，。]+(?:纠纷|案件|案)[^，。]*)/);
      if (caseNameMatch) {
        parsed.name = caseNameMatch[1].trim();
      }
      
      // 提取法院名称
      const courtMatch = text.match(/([\u4e00-\u9fa5]+(?:人民法院|仲裁委员会))/);
      if (courtMatch) {
        parsed.court = courtMatch[1];
      }
      
      // 提取判决日期
      const dateMatch = text.match(/(\d{4})年(\d{1,2})月(\d{1,2})日/);
      if (dateMatch) {
        parsed.judgmentDate = dayjs(`${dateMatch[1]}-${dateMatch[2]}-${dateMatch[3]}`);
      }
      
      // 提取摘要（裁判要旨或判决主文前100字）
      const summaryMatch = text.match(/(?:裁判要旨|本院认为|判决如下)[：:]\s*([^。]+。[^。]+。)/);
      if (summaryMatch) {
        parsed.summary = summaryMatch[1].substring(0, 500);
      } else if (text.length > 100) {
        parsed.summary = text.substring(0, 300) + '...';
      }
      
      // 判断案件类型
      if (/民事|合同|侵权|婚姻|继承|物权/.test(text)) {
        parsed.caseType = 'CIVIL';
      } else if (/刑事|犯罪|罪|刑罚/.test(text)) {
        parsed.caseType = 'CRIMINAL';
      } else if (/行政|行政处罚|行政复议/.test(text)) {
        parsed.caseType = 'ADMINISTRATIVE';
      } else if (/知识产权|专利|商标|著作权/.test(text)) {
        parsed.caseType = 'IP';
      }
      
      // 判断案件结果
      if (/支持.*诉讼请求|判决.*胜诉/.test(text)) {
        parsed.result = 'WIN';
      } else if (/驳回.*诉讼请求|判决.*败诉/.test(text)) {
        parsed.result = 'LOSE';
      } else if (/调解/.test(text)) {
        parsed.result = 'MEDIATION';
      } else if (/部分支持/.test(text)) {
        parsed.result = 'PARTIAL_WIN';
      }
      
      // 设置表单值
      if (Object.keys(parsed).length > 0) {
        formApi.setValues(parsed);
        message.success(`裁判文书识别成功！已自动填充 ${Object.keys(parsed).length} 个字段`);
      } else {
        message.warning('未能从文书中提取到结构化信息，请手动填写');
      }
    } else {
      message.error(result.errorMessage || '裁判文书识别失败');
    }
  } catch (e: any) {
    message.error(e?.message || '裁判文书识别失败');
  } finally {
    ocrLoading.value = false;
  }
  return false; // 阻止自动上传
}

defineExpose({ open });
</script>

<template>
  <Modal :title="editingId ? '编辑案例' : '添加案例'" class="w-[650px]">
    <Spin :spinning="ocrLoading" tip="正在识别裁判文书...">
      <!-- OCR识别区域 -->
      <Alert type="info" style="margin-bottom: 16px" show-icon>
        <template #message>
          <span class="font-medium text-blue-700">裁判文书智能识别</span>
          <span class="text-gray-500 text-xs ml-2">上传裁判文书截图自动填充</span>
        </template>
        <template #description>
          <div class="mt-2">
            <Upload
              :show-upload-list="false"
              :before-upload="handleOcrJudgment"
              accept="image/*"
            >
              <Tooltip title="上传裁判文书截图，自动识别案件名称、法院、判决日期等信息">
                <a class="text-blue-600 hover:text-blue-800 font-medium">
                  <IconifyIcon icon="ant-design:scan-outlined" class="mr-1" />识别裁判文书
                </a>
              </Tooltip>
            </Upload>
          </div>
        </template>
      </Alert>
      
    <Form />
    </Spin>
  </Modal>
</template>
