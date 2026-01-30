<script setup lang="ts">
import type { CaseLibraryDTO, CaseStudyNoteDTO } from '#/api/knowledge';

import { ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import {
  Button,
  Descriptions,
  DescriptionsItem,
  Divider,
  message,
  Popconfirm,
  Space,
  Spin,
  Tag,
  Textarea,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  deleteCaseStudyNote,
  getCaseDetail,
  getMyCaseNote,
  saveCaseStudyNote,
} from '#/api/knowledge';

const currentCase = ref<CaseLibraryDTO | null>(null);
const myNote = ref<CaseStudyNoteDTO | null>(null);
const noteContent = ref('');
const noteLoading = ref(false);
const noteSaving = ref(false);
const noteEditing = ref(false);

const referenceValueMap: Record<string, { color: string; text: string }> = {
  HIGH: { color: 'red', text: '高' },
  MEDIUM: { color: 'orange', text: '中' },
  LOW: { color: 'blue', text: '低' },
};

const [Drawer, drawerApi] = useVbenDrawer({
  overlayBlur: 4,
  placement: 'left', // 右侧按钮触发，从左侧滑入
  onOpenChange(isOpen) {
    if (!isOpen) {
      currentCase.value = null;
      myNote.value = null;
      noteContent.value = '';
      noteEditing.value = false;
    }
  },
});

function formatDate(date?: string) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-';
}

async function loadMyNote(caseId: number) {
  noteLoading.value = true;
  try {
    const note = await getMyCaseNote(caseId);
    myNote.value = note;
    noteContent.value = note?.content || '';
  } catch {
    // 没有笔记时忽略错误
    myNote.value = null;
    noteContent.value = '';
  } finally {
    noteLoading.value = false;
  }
}

async function saveNote() {
  if (!currentCase.value || !noteContent.value.trim()) {
    message.warning('请输入笔记内容');
    return;
  }

  noteSaving.value = true;
  try {
    await saveCaseStudyNote({
      caseId: currentCase.value.id,
      content: noteContent.value.trim(),
    });
    message.success('笔记已保存');
    noteEditing.value = false;
    await loadMyNote(currentCase.value.id);
  } catch (error: any) {
    message.error(error.message || '保存失败');
  } finally {
    noteSaving.value = false;
  }
}

async function deleteNote() {
  if (!currentCase.value) return;

  try {
    await deleteCaseStudyNote(currentCase.value.id);
    message.success('笔记已删除');
    myNote.value = null;
    noteContent.value = '';
    noteEditing.value = false;
  } catch (error: any) {
    message.error(error.message || '删除失败');
  }
}

function startEdit() {
  noteContent.value = myNote.value?.content || '';
  noteEditing.value = true;
}

function cancelEdit() {
  noteContent.value = myNote.value?.content || '';
  noteEditing.value = false;
}

async function open(record: CaseLibraryDTO) {
  try {
    const detail = await getCaseDetail(record.id);
    currentCase.value = detail;
    drawerApi.open();
    // 加载我的笔记
    loadMyNote(record.id);
  } catch (error: any) {
    message.error(error.message || '获取详情失败');
  }
}

defineExpose({ open });
</script>

<template>
  <Drawer title="案例详情" class="w-[520px]" :show-footer="false">
    <Descriptions v-if="currentCase" :column="2" bordered size="small">
      <DescriptionsItem label="案例名称" :span="2">
        {{ currentCase.name }}
      </DescriptionsItem>
      <DescriptionsItem label="案由类型">
        {{ currentCase.caseTypeName }}
      </DescriptionsItem>
      <DescriptionsItem label="案例分类">
        {{ currentCase.categoryName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="审理法院">
        {{ currentCase.court || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="判决日期">
        {{ formatDate(currentCase.judgmentDate) }}
      </DescriptionsItem>
      <DescriptionsItem label="案件结果">
        {{ currentCase.resultName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="经办律师">
        {{ currentCase.lawyerName || '-' }}
      </DescriptionsItem>
      <DescriptionsItem label="参考价值">
        <Tag
          :color="referenceValueMap[currentCase.referenceValue || '']?.color"
        >
          {{
            currentCase.referenceValueName ||
            referenceValueMap[currentCase.referenceValue || '']?.text
          }}
        </Tag>
      </DescriptionsItem>
      <DescriptionsItem label="创建时间">
        {{ formatDate(currentCase.createdAt) }}
      </DescriptionsItem>
      <DescriptionsItem label="案例摘要" :span="2">
        <div style="white-space: pre-wrap">
          {{ currentCase.summary || '-' }}
        </div>
      </DescriptionsItem>
    </Descriptions>

    <!-- 学习笔记区域 -->
    <Divider>我的学习笔记</Divider>
    <Spin :spinning="noteLoading">
      <div v-if="noteEditing || !myNote">
        <Textarea
          v-model:value="noteContent"
          :rows="4"
          placeholder="记录学习心得、要点总结..."
          :disabled="noteSaving"
        />
        <div style="margin-top: 12px; text-align: right">
          <Space>
            <Button v-if="myNote" @click="cancelEdit">取消</Button>
            <Button type="primary" :loading="noteSaving" @click="saveNote">
              保存笔记
            </Button>
          </Space>
        </div>
      </div>
      <div v-else>
        <div
          style="
            padding: 12px;
            background: #f5f5f5;
            border-radius: 4px;
            white-space: pre-wrap;
            min-height: 60px;
          "
        >
          {{ myNote.content }}
        </div>
        <div
          style="
            margin-top: 8px;
            font-size: 12px;
            color: #999;
            display: flex;
            justify-content: space-between;
            align-items: center;
          "
        >
          <span>
            更新于 {{ formatDate(myNote.updatedAt || myNote.createdAt) }}
          </span>
          <Space>
            <Button size="small" type="link" @click="startEdit">编辑</Button>
            <Popconfirm title="确定要删除笔记吗？" @confirm="deleteNote">
              <Button size="small" type="link" danger>删除</Button>
            </Popconfirm>
          </Space>
        </div>
      </div>
    </Spin>
  </Drawer>
</template>
