<template>
  <div class="archive-detail">
    <div class="page-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="title">{{ archive?.title || '档案详情' }}</span>
          <el-tag
            v-if="archive"
            :type="getStatusType(archive.status)"
            class="ml-2"
          >
            {{ getStatusName(archive.status) }}
          </el-tag>
        </template>
        <template #extra>
          <el-button-group>
            <el-button
              v-if="showApproveAction"
              type="success"
              :loading="approving"
              @click="handleApproveArchive"
            >
              <el-icon><CircleCheck /></el-icon>
              审核通过
            </el-button>
            <el-button
              v-if="showStoreAction"
              type="success"
              :loading="storing"
              @click="handleStoreArchive"
            >
              <el-icon><FolderChecked /></el-icon>
              正式归档
            </el-button>
            <el-button
              v-if="canManageArchive"
              type="primary"
              @click="handleSupplementUpload"
            >
              <el-icon><Upload /></el-icon>
              补充上传
            </el-button>
            <el-button
              v-if="files.length > 0"
              type="success"
              :loading="isDownloading"
              @click="handleDownloadAll"
            >
              <el-icon><Download /></el-icon>
              打包下载
            </el-button>
            <el-button
              v-if="canBorrow"
              type="warning"
              @click="handleApplyBorrow"
            >
              <el-icon><Reading /></el-icon>
              申请借阅
            </el-button>
          </el-button-group>
        </template>
      </el-page-header>
      <div
        v-if="showStoreAlert"
        class="status-tip"
      >
        <el-alert
          :title="storeAlertTitle"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #default>
            <div class="status-alert-content">
              <span>{{ storeAlertDescription }}</span>
              <el-button
                v-if="showApproveAction"
                type="success"
                link
                :loading="approving"
                @click="handleApproveArchive"
              >
                现在审核
              </el-button>
              <el-button
                v-if="showStoreAction"
                type="success"
                link
                :loading="storing"
                @click="handleStoreArchive"
              >
                现在归档
              </el-button>
            </div>
          </template>
        </el-alert>
      </div>
      <div
        v-if="borrowUnavailableReason"
        class="borrow-tip"
      >
        <el-alert
          :title="borrowUnavailableReason"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      <div
        v-else-if="borrowRuleSummary"
        class="borrow-tip"
      >
        <el-alert
          :title="borrowRuleSummary"
          type="success"
          :closable="false"
          show-icon
        />
      </div>
    </div>

    <el-skeleton
      :loading="loading"
      animated
      :rows="10"
    >
      <template #default>
        <div
          v-if="archive"
          class="content"
        >
          <!-- 基本信息 -->
          <el-card
            shadow="never"
            class="info-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Document /></el-icon>
                <span>基本信息</span>
              </div>
            </template>
            <el-descriptions
              :column="3"
              border
            >
              <el-descriptions-item label="档案号">
                {{ archive.archiveNo }}
              </el-descriptions-item>
              <el-descriptions-item label="所属全宗">
                {{ archive.fondsNo || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="档案分类">
                {{ archive.categoryCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="档案类型">
                <el-tag size="small">
                  {{ getArchiveTypeName(archive.archiveType) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="保管期限">
                {{ getRetentionName(archive.retentionPeriod) }}
              </el-descriptions-item>
              <el-descriptions-item
                label="题名"
                :span="1"
              >
                {{ archive.title }}
              </el-descriptions-item>
              <el-descriptions-item label="责任者">
                {{ archive.responsibility || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="文件日期">
                {{ archive.documentDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="密级">
                {{ getSecurityName(archive.securityLevel) }}
              </el-descriptions-item>
              <el-descriptions-item label="页数">
                {{ archive.pageCount || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="件数">
                {{ archive.piecesCount || 1 }}
              </el-descriptions-item>
              <el-descriptions-item label="文件数量">
                {{ archive.fileCount || 0 }} 个
              </el-descriptions-item>
              <el-descriptions-item label="档案形式">
                <el-tag :type="getArchiveFormType(archive.archiveForm)">
                  {{ getArchiveFormName(archive.archiveForm) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item
                v-if="archive.archiveForm !== 'ELECTRONIC'"
                label="存放位置"
              >
                {{ archive.storageLocation || '-' }}
              </el-descriptions-item>
              <el-descriptions-item
                v-if="archive.archiveForm !== 'ELECTRONIC' && archive.boxNo"
                label="盒号"
              >
                {{ archive.boxNo }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 业务关联 -->
          <el-card
            v-if="archive.caseNo || archive.clientName"
            shadow="never"
            class="info-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Briefcase /></el-icon>
                <span>业务关联</span>
              </div>
            </template>
            <el-descriptions
              :column="3"
              border
            >
              <el-descriptions-item label="案件编号">
                {{ archive.caseNo || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="案件名称">
                {{ archive.caseName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="结案日期">
                {{ archive.caseCloseDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="委托人">
                {{ archive.clientName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="主办律师">
                {{ archive.lawyerName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="来源">
                {{ getSourceName(archive.sourceType) }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 电子文件 -->
          <el-card
            shadow="never"
            class="info-card files-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Folder /></el-icon>
                <span>电子文件</span>
                <el-tag
                  class="ml-2"
                  size="small"
                >
                  {{ files.length }} 个文件
                </el-tag>
              </div>
            </template>
            
            <!-- 按卷号和分段展示文件 -->
            <el-collapse
              v-if="groupedFiles.length > 0"
              v-model="activeCategories"
              class="file-categories"
            >
              <el-collapse-item
                v-for="volume in groupedFiles"
                :key="volume.key"
                :name="volume.key"
              >
                <template #title>
                  <div class="category-header volume-header">
                    <span
                      class="category-icon"
                      :style="{ backgroundColor: volume.color + '20', color: volume.color }"
                    >
                      <el-icon><Folder /></el-icon>
                    </span>
                    <span class="category-name">{{ volume.name }}</span>
                    <el-tag
                      size="small"
                      :color="volume.color + '20'"
                      :style="{ color: volume.color, borderColor: volume.color }"
                    >
                      {{ volume.fileCount }} 个
                    </el-tag>
                  </div>
                </template>

                <div class="volume-sections">
                  <section
                    v-for="section in volume.sections"
                    :key="section.key"
                    class="section-block"
                  >
                    <div class="section-header">
                      <div class="section-title-wrap">
                        <span
                          class="section-dot"
                          :style="{ backgroundColor: section.color }"
                        />
                        <span class="section-title">{{ section.name }}</span>
                      </div>
                      <el-tag
                        size="small"
                        effect="plain"
                        :style="{ color: section.color, borderColor: section.color }"
                      >
                        {{ section.files.length }} 个
                      </el-tag>
                    </div>

                    <div class="file-list">
                      <div
                        v-for="(file, index) in section.files"
                        :key="file.id"
                        class="file-item"
                      >
                        <div class="file-index">
                          {{ index + 1 }}
                        </div>
                        <div class="file-icon-wrapper">
                          <el-icon :class="getFileIconClass(file.fileExtension)">
                            <component :is="getFileIcon(file.fileExtension)" />
                          </el-icon>
                        </div>
                        <div class="file-info">
                          <div class="file-name-text">
                            {{ file.originalName || file.fileName }}
                          </div>
                          <div class="file-meta">
                            <span
                              v-if="getFileSourceLabel(file.fileSourceType)"
                              class="meta-chip"
                            >{{ getFileSourceLabel(file.fileSourceType) }}</span>
                            <span
                              v-if="file.scanBatchNo"
                              class="meta-chip"
                            >批次 {{ file.scanBatchNo }}</span>
                            <span
                              v-if="getScanCheckLabel(file.scanCheckStatus)"
                              class="meta-chip"
                            >复核 {{ getScanCheckLabel(file.scanCheckStatus) }}</span>
                            <span
                              v-if="file.scanTime"
                              class="meta-chip"
                            >扫描 {{ formatDateTime(file.scanTime) }}</span>
                            <span
                              v-if="file.documentNo"
                              class="meta-chip"
                            >件号 {{ file.documentNo }}</span>
                            <span
                              v-if="formatPageRange(file)"
                              class="meta-chip"
                            >页码 {{ formatPageRange(file) }}</span>
                            <span
                              v-if="file.versionLabel"
                              class="meta-chip"
                            >版本 {{ file.versionLabel }}</span>
                            <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
                            <span
                              v-if="file.isLongTermFormat"
                              class="long-term-badge"
                            >长期保存格式</span>
                          </div>
                        </div>
                        <div class="file-actions">
                          <el-button
                            type="primary"
                            link
                            size="small"
                            @click="handlePreview(file)"
                          >
                            预览
                          </el-button>
                          <el-button
                            type="primary"
                            link
                            size="small"
                            @click="handleDownload(file)"
                          >
                            下载
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </section>
                </div>
              </el-collapse-item>
            </el-collapse>
            
            <el-empty
              v-else
              description="暂无电子文件"
            />
          </el-card>

          <!-- 操作记录 -->
          <el-card
            shadow="never"
            class="info-card"
          >
            <template #header>
              <div class="card-header">
                <el-icon><Clock /></el-icon>
                <span>操作记录</span>
              </div>
            </template>
            <el-descriptions
              :column="2"
              border
            >
              <el-descriptions-item label="接收时间">
                {{ formatDateTime(archive.receivedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="接收人">
                {{ archive.receivedByName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="编目时间">
                {{ formatDateTime(archive.catalogedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="编目人">
                {{ archive.catalogedByName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="归档时间">
                {{ formatDateTime(archive.archivedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="归档人">
                {{ archive.archivedByName || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </div>
      </template>
    </el-skeleton>

    <!-- 文件预览组件 -->
    <FilePreview
      v-model="previewVisible"
      :file-id="previewFileId"
      :file-name="previewFileName"
      :file-extension="previewFileExtension"
    />

    <!-- 申请借阅弹窗 -->
    <el-dialog
      v-model="borrowDialogVisible"
      title="申请借阅"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="borrowFormRef"
        :model="borrowForm"
        :rules="borrowRules"
        label-width="100px"
      >
        <el-form-item label="档案信息">
          <div class="borrow-archive-info">
            <span class="archive-no">{{ archive?.archiveNo }}</span>
            <span class="archive-title">{{ archive?.title }}</span>
          </div>
        </el-form-item>
        <el-form-item
          label="借阅目的"
          prop="purpose"
        >
          <el-input 
            v-model="borrowForm.purpose" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入借阅目的"
          />
        </el-form-item>
        <el-form-item
          label="借阅方式"
          prop="borrowType"
        >
          <el-radio-group v-model="borrowForm.borrowType">
            <el-radio-button
              label="ONLINE"
              value="ONLINE"
              :disabled="!isBorrowTypeAllowed('ONLINE')"
            >
              在线查阅
            </el-radio-button>
            <el-radio-button
              label="DOWNLOAD"
              value="DOWNLOAD"
              :disabled="!isBorrowTypeAllowed('DOWNLOAD')"
            >
              允许下载
            </el-radio-button>
            <el-radio-button
              label="COPY"
              value="COPY"
              :disabled="!isBorrowTypeAllowed('COPY')"
            >
              复制利用
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="预计归还"
          prop="expectedReturnDate"
        >
          <el-date-picker
            v-model="borrowForm.expectedReturnDate"
            type="date"
            placeholder="选择预计归还日期"
            :disabled-date="disablePastDate"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="borrowForm.remarks" 
            type="textarea" 
            :rows="2" 
            placeholder="备注信息（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="borrowSubmitting"
          @click="submitBorrowApply"
        >
          提交申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 补充上传弹窗 -->
    <el-dialog
      v-model="supplementDialogVisible"
      title="补充上传"
      width="960px"
      top="5vh"
      destroy-on-close
      class="supplement-dialog"
    >
      <el-form
        ref="supplementFormRef"
        :model="supplementForm"
        label-width="100px"
        class="supplement-form"
      >
        <div class="supplement-summary">
          <div class="summary-item">
            <span class="summary-label">档案号</span>
            <span class="summary-value">{{ archive?.archiveNo || '-' }}</span>
          </div>
          <div class="summary-item summary-item-wide">
            <span class="summary-label">题名</span>
            <span class="summary-value">{{ archive?.title || '-' }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">当前形式</span>
            <el-tag :type="getArchiveFormType(archive?.archiveForm)">
              {{ getArchiveFormName(archive?.archiveForm) }}
            </el-tag>
          </div>
        </div>

        <div class="supplement-section">
          <div class="section-heading">
            存放形式与位置
          </div>
          <div class="supplement-grid">
            <el-form-item
              label="变更形式"
              class="span-2"
            >
              <el-radio-group v-model="supplementForm.archiveForm">
                <el-radio-button value="ELECTRONIC">
                  电子档案
                </el-radio-button>
                <el-radio-button value="PHYSICAL">
                  纸质档案
                </el-radio-button>
                <el-radio-button value="HYBRID">
                  混合档案
                </el-radio-button>
              </el-radio-group>
              <div
                v-if="supplementForm.archiveForm !== archive?.archiveForm"
                class="form-tip"
              >
                将从「{{ getArchiveFormName(archive?.archiveForm) }}」变更为「{{ getArchiveFormName(supplementForm.archiveForm) }}」
              </div>
            </el-form-item>

            <el-form-item
              v-if="supplementForm.archiveForm !== 'ELECTRONIC'"
              label="存放位置"
              :rules="[{ required: true, message: '请选择存放位置' }]"
              class="span-2"
            >
              <el-select
                v-model="supplementForm.locationId"
                placeholder="请选择存放位置"
                style="width: 100%"
                filterable
              >
                <el-option
                  v-for="item in locationOptions"
                  :key="item.id"
                  :label="`${item.locationName}（${item.roomName || ''}${item.shelfNo ? ' ' + item.shelfNo + '架' : ''}）`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item
              v-if="supplementForm.archiveForm !== 'ELECTRONIC'"
              label="盒号"
            >
              <el-input
                v-model="supplementForm.boxNo"
                placeholder="档案盒编号"
              />
            </el-form-item>
          </div>
        </div>

        <div class="supplement-section">
          <div class="section-heading">
            电子文件整理信息
          </div>
          <div class="supplement-grid">
            <el-form-item label="卷号">
              <el-input-number
                v-model="supplementForm.volumeNo"
                :min="1"
                :max="99"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="卷内分段">
              <el-select
                v-model="supplementForm.sectionType"
                style="width: 100%"
              >
                <el-option
                  v-for="item in fileSectionOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="版本标识">
              <el-input
                v-model="supplementForm.versionLabel"
                placeholder="如：扫描件、补扫件、整理版"
              />
            </el-form-item>

            <el-form-item label="文件来源">
              <el-select
                v-model="supplementForm.fileSourceType"
                style="width: 100%"
                clearable
              >
                <el-option
                  label="扫描件"
                  value="SCANNED"
                />
                <el-option
                  label="原生电子件"
                  value="ORIGINAL_ELECTRONIC"
                />
                <el-option
                  label="外部导入件"
                  value="IMPORTED"
                />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <div class="supplement-section">
          <div class="section-heading">
            扫描与复核留痕
          </div>
          <div class="supplement-grid">
            <el-form-item label="扫描批次号">
              <el-input
                v-model="supplementForm.scanBatchNo"
                placeholder="如：SCAN-20260330-01"
              />
            </el-form-item>

            <el-form-item label="扫描操作人">
              <el-input
                v-model="supplementForm.scanOperator"
                placeholder="录入扫描操作人"
              />
            </el-form-item>

            <el-form-item label="扫描时间">
              <el-date-picker
                v-model="supplementForm.scanTime"
                type="datetime"
                placeholder="选择扫描时间"
                style="width: 100%"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>

            <el-form-item label="复核状态">
              <el-select
                v-model="supplementForm.scanCheckStatus"
                style="width: 100%"
                clearable
              >
                <el-option
                  label="待复核"
                  value="PENDING"
                />
                <el-option
                  label="已通过"
                  value="PASSED"
                />
                <el-option
                  label="未通过"
                  value="FAILED"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="复核人">
              <el-input
                v-model="supplementForm.scanCheckBy"
                placeholder="录入复核人"
              />
            </el-form-item>

            <el-form-item label="复核时间">
              <el-date-picker
                v-model="supplementForm.scanCheckTime"
                type="datetime"
                placeholder="选择复核时间"
                style="width: 100%"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
          </div>
        </div>

        <div class="supplement-section">
          <div class="section-heading">
            上传文件
          </div>
          <el-form-item
            label-width="0"
            class="upload-form-item"
          >
            <BatchUpload
              ref="supplementUploadRef"
              :allowed-types="allowedFileTypes"
              :max-size="maxFileSize"
              :auto-upload="false"
              :extra-data="supplementUploadExtraData"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="supplementDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="supplementSubmitting"
          @click="submitSupplement"
        >
          确认提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document, Folder, Clock, Briefcase, Download, Upload,
  Picture, VideoPlay, Headset, FolderOpened, Reading, FolderChecked, CircleCheck
} from '@element-plus/icons-vue'
import { getArchiveDetail, getFileDownloadUrl, getArchiveDownloadUrl, supplementArchive, updateArchiveStatus, approveArchive } from '@/api/archive'
import FilePreview from '@/components/FilePreview.vue'
import BatchUpload from '@/components/BatchUpload.vue'
import { checkBorrowAvailable, applyBorrow } from '@/api/borrow'
import { getAvailableLocations } from '@/api/location'
import { useUserStore } from '@/stores/user'
import {
  getArchiveTypeName,
  getStatusName,
  getStatusType,
  getRetentionName,
  getSecurityName,
  getSourceName,
  getArchiveFormName,
  getArchiveFormType,
  FILE_SECTION_ORDER,
  getFileSectionName,
  getFileSectionColor
} from '@/utils/archiveEnums'
import { MANAGER_ROLES, REVIEW_ROLES } from '@/utils/permission'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const archive = ref(null)
const files = ref([])
const previewVisible = ref(false)
const previewFileId = ref(null)
const previewFileName = ref('')
const previewFileExtension = ref('')
const isDownloading = ref(false)
const storing = ref(false)
const approving = ref(false)

// 借阅相关
const canBorrow = ref(false)
const borrowUnavailableReason = ref('')
const allowedBorrowTypes = ref([])
const borrowRuleSummary = ref('')
const maxBorrowDaysByType = ref({})
const borrowDialogVisible = ref(false)
const borrowSubmitting = ref(false)
const borrowFormRef = ref(null)
const borrowForm = ref({
  purpose: '',
  borrowType: 'ONLINE',
  expectedReturnDate: null,
  remarks: ''
})
const borrowRules = {
  purpose: [{ required: true, message: '请输入借阅目的', trigger: 'blur' }],
  borrowType: [{ required: true, message: '请选择借阅方式', trigger: 'change' }],
  expectedReturnDate: [{ required: true, message: '请选择预计归还日期', trigger: 'change' }]
}

// 补充上传相关
const supplementDialogVisible = ref(false)
const supplementSubmitting = ref(false)
const supplementFormRef = ref(null)
const supplementUploadRef = ref(null)
const supplementForm = ref({
  archiveForm: 'ELECTRONIC',
  locationId: null,
  boxNo: '',
  volumeNo: 1,
  sectionType: 'MAIN',
  versionLabel: '',
  fileSourceType: 'IMPORTED',
  scanBatchNo: '',
  scanOperator: '',
  scanTime: '',
  scanCheckStatus: '',
  scanCheckBy: '',
  scanCheckTime: ''
})
const locationOptions = ref([])
const allowedFileTypes = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'jpg', 'jpeg', 'png', 'gif', 'zip', 'rar', 'ofd', 'tif', 'tiff']
const maxFileSize = 100 * 1024 * 1024 // 100MB
const fileSectionOptions = FILE_SECTION_ORDER.map(value => ({
  value,
  label: getFileSectionName(value)
}))
const fileSourceLabels = {
  SCANNED: '扫描件',
  ORIGINAL_ELECTRONIC: '原生电子件',
  IMPORTED: '外部导入件'
}
const scanCheckLabels = {
  PENDING: '待复核',
  PASSED: '已通过',
  FAILED: '未通过'
}

const canManageArchive = computed(() => MANAGER_ROLES.includes(userStore.userType))
const canReviewArchive = computed(() => REVIEW_ROLES.includes(userStore.userType))

const showStoreAction = computed(() => {
  const status = archive.value?.status
  return canManageArchive.value && (status === 'RECEIVED' || status === 'CATALOGING')
})

const showApproveAction = computed(() => {
  return canReviewArchive.value && archive.value?.status === 'PENDING_REVIEW'
})

const showStoreAlert = computed(() => {
  const status = archive.value?.status
  return status === 'PENDING_REVIEW' || status === 'RECEIVED' || status === 'CATALOGING' || route.query.created === '1'
})

const storeAlertTitle = computed(() => {
  if (archive.value?.status === 'PENDING_REVIEW') {
    return '当前档案已提交，等待审核员批准'
  }
  if (archive.value?.status === 'CATALOGING') {
    return '当前档案仍处于整理中，尚未正式归档'
  }
  if (archive.value?.status === 'STORED') {
    return '档案已创建成功'
  }
  return '当前档案已接收，但尚未正式归档'
})

const storeAlertDescription = computed(() => {
  if (archive.value?.status === 'PENDING_REVIEW') {
    return showApproveAction.value
      ? '请核对题名、电子文件和业务信息后执行“审核通过”，通过后档案会直接正式入库。'
      : '普通用户提交后不会直接入库。请等待审核员审批，审核通过后档案才会正式入库并开放后续利用流程。'
  }
  if (archive.value?.status === 'STORED') {
    return '该档案已处于可借阅状态，可继续补充材料、检索或发起借阅申请。'
  }
  return '未正式归档前，系统不会开放借阅申请。请在核对电子文件、纸质位置和著录信息后执行“正式归档”。'
})

const supplementUploadExtraData = () => ({
  volumeNo: supplementForm.value.volumeNo,
  sectionType: supplementForm.value.sectionType,
  versionLabel: supplementForm.value.versionLabel,
  fileSourceType: supplementForm.value.fileSourceType,
  scanBatchNo: supplementForm.value.scanBatchNo,
  scanOperator: supplementForm.value.scanOperator,
  scanTime: supplementForm.value.scanTime,
  scanCheckStatus: supplementForm.value.scanCheckStatus,
  scanCheckBy: supplementForm.value.scanCheckBy,
  scanCheckTime: supplementForm.value.scanCheckTime
})

const getVolumeName = (volumeNo) => {
  return volumeNo ? `第 ${volumeNo} 卷` : '未分卷'
}

const getSectionType = (file) => {
  return file.sectionType || file.fileCategory || 'MAIN'
}

const formatPageRange = (file) => {
  if (file.pageStart && file.pageEnd) {
    return `${file.pageStart}-${file.pageEnd}`
  }
  if (file.pageStart) {
    return `${file.pageStart}`
  }
  if (file.pageEnd) {
    return `${file.pageEnd}`
  }
  return ''
}

const getFileSourceLabel = (sourceType) => {
  return fileSourceLabels[sourceType] || sourceType || ''
}

const getScanCheckLabel = (status) => {
  return scanCheckLabels[status] || status || ''
}

// 按卷号、分段分组的文件列表
const groupedFiles = computed(() => {
  const volumeMap = new Map()

  files.value.forEach(file => {
    const volumeNo = file.volumeNo || 1
    if (!volumeMap.has(volumeNo)) {
      volumeMap.set(volumeNo, [])
    }
    volumeMap.get(volumeNo).push(file)
  })

  return Array.from(volumeMap.entries())
    .sort((a, b) => a[0] - b[0])
    .map(([volumeNo, volumeFiles]) => {
      const sectionMap = new Map()
      volumeFiles.forEach(file => {
        const sectionType = getSectionType(file)
        if (!sectionMap.has(sectionType)) {
          sectionMap.set(sectionType, [])
        }
        sectionMap.get(sectionType).push(file)
      })

      const orderedSections = []
      FILE_SECTION_ORDER.forEach(sectionType => {
        if (sectionMap.has(sectionType)) {
          orderedSections.push({
            key: `${volumeNo}-${sectionType}`,
            sectionType,
            name: getFileSectionName(sectionType),
            color: getFileSectionColor(sectionType),
            files: sectionMap.get(sectionType)
          })
          sectionMap.delete(sectionType)
        }
      })

      sectionMap.forEach((sectionFiles, sectionType) => {
        orderedSections.push({
          key: `${volumeNo}-${sectionType}`,
          sectionType,
          name: getFileSectionName(sectionType),
          color: getFileSectionColor(sectionType),
          files: sectionFiles
        })
      })

      return {
        key: `volume-${volumeNo}`,
        volumeNo,
        name: getVolumeName(volumeNo),
        color: '#409eff',
        fileCount: volumeFiles.length,
        sections: orderedSections
      }
    })
})

// 默认展开的卷
const activeCategories = computed(() => {
  return groupedFiles.value.map(g => g.key)
})

// 获取档案详情
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getArchiveDetail(route.params.id)
    archive.value = res.data
    files.value = res.data.files || []
    // 检查是否可借阅
    checkCanBorrow()
  } catch (e) {
    console.error('获取档案详情失败', e)
    ElMessage.error('获取档案详情失败')
  } finally {
    loading.value = false
  }
}

const handleStoreArchive = async () => {
  if (!archive.value?.id || !showStoreAction.value) return

  storing.value = true
  try {
    await updateArchiveStatus(archive.value.id, 'STORED')
    ElMessage.success('档案已正式归档')
    if (route.query.created === '1') {
      router.replace({ path: route.path, query: { ...route.query, created: undefined } })
    }
    await fetchData()
  } catch (e) {
    console.error('正式归档失败', e)
    ElMessage.error(e.response?.data?.message || '正式归档失败')
  } finally {
    storing.value = false
  }
}

const handleApproveArchive = async () => {
  if (!archive.value?.id || !showApproveAction.value) return

  approving.value = true
  try {
    await approveArchive(archive.value.id)
    ElMessage.success('审核通过，档案已正式入库')
    if (route.query.created === '1') {
      router.replace({ path: route.path, query: { ...route.query, created: undefined, submitted: undefined } })
    }
    await fetchData()
  } catch (e) {
    console.error('审核通过失败', e)
    ElMessage.error(e.response?.data?.message || '审核通过失败')
  } finally {
    approving.value = false
  }
}

// 检查档案是否可借阅
const checkCanBorrow = async () => {
  try {
    const res = await checkBorrowAvailable(route.params.id)
    canBorrow.value = res.data?.available === true
    allowedBorrowTypes.value = res.data?.allowedBorrowTypes || []
    borrowUnavailableReason.value = res.data?.unavailableReason || ''
    borrowRuleSummary.value = res.data?.borrowRules?.ruleSummary || ''
    maxBorrowDaysByType.value = res.data?.borrowRules?.maxBorrowDays || {}
  } catch (e) {
    canBorrow.value = false
    allowedBorrowTypes.value = []
    borrowUnavailableReason.value = '暂时无法校验借阅条件'
    borrowRuleSummary.value = ''
    maxBorrowDaysByType.value = {}
  }
}

// 申请借阅
const handleApplyBorrow = () => {
  if (!allowedBorrowTypes.value.includes(borrowForm.value.borrowType)) {
    borrowForm.value.borrowType = allowedBorrowTypes.value[0] || 'ONLINE'
  }
  borrowForm.value = {
    purpose: '',
    borrowType: allowedBorrowTypes.value[0] || 'ONLINE',
    expectedReturnDate: null,
    remarks: ''
  }
  borrowDialogVisible.value = true
}

const isBorrowTypeAllowed = (type) => allowedBorrowTypes.value.includes(type)

// 禁用过去日期
const disablePastDate = (time) => {
  if (time.getTime() < Date.now() - 8.64e7) {
    return true
  }
  const type = borrowForm.value.borrowType
  const maxDays = Number(maxBorrowDaysByType.value?.[type] || 0)
  if (!maxDays) {
    return false
  }
  const latest = new Date()
  latest.setHours(0, 0, 0, 0)
  latest.setDate(latest.getDate() + maxDays)
  return time.getTime() > latest.getTime()
}

// 提交借阅申请
const submitBorrowApply = async () => {
  if (!borrowFormRef.value) return
  
  try {
    await borrowFormRef.value.validate()
    borrowSubmitting.value = true
    
    const data = {
      archiveId: route.params.id,
      borrowPurpose: borrowForm.value.purpose,
      borrowType: borrowForm.value.borrowType,
      expectedReturnDate: borrowForm.value.expectedReturnDate.toISOString().split('T')[0],
      remarks: borrowForm.value.remarks
    }
    
    await applyBorrow(data)
    ElMessage.success('借阅申请已提交，请等待审批')
    borrowDialogVisible.value = false
    // 刷新数据，更新可借阅状态
    checkCanBorrow()
  } catch (e) {
    if (e !== 'cancel' && e !== false) {
      console.error('提交借阅申请失败', e)
      ElMessage.error(e.response?.data?.message || '提交申请失败')
    }
  } finally {
    borrowSubmitting.value = false
  }
}

// 加载存放位置
const loadLocations = async () => {
  try {
    const res = await getAvailableLocations()
    if (res.code === 0 && res.data) {
      locationOptions.value = res.data
    }
  } catch (e) {
    console.error('加载存放位置失败', e)
  }
}

// 打开补充上传弹窗
const handleSupplementUpload = () => {
  supplementForm.value = {
    archiveForm: archive.value?.archiveForm || 'ELECTRONIC',
    locationId: archive.value?.locationId || null,
    boxNo: archive.value?.boxNo || '',
    volumeNo: 1,
    sectionType: 'MAIN',
    versionLabel: '',
    fileSourceType: 'IMPORTED',
    scanBatchNo: '',
    scanOperator: '',
    scanTime: '',
    scanCheckStatus: '',
    scanCheckBy: '',
    scanCheckTime: ''
  }
  loadLocations()
  supplementDialogVisible.value = true
}

// 提交补充上传
const submitSupplement = async () => {
  supplementSubmitting.value = true
  
  try {
    // 先上传文件
    let uploadedFileIds = []
    if (supplementUploadRef.value) {
      const uploadResult = await supplementUploadRef.value.startUpload()
      if (uploadResult && uploadResult.length > 0) {
        uploadedFileIds = uploadResult.map(f => f.id)
      }
    }
    
    // 检查是否需要更新
    const formChanged = supplementForm.value.archiveForm !== archive.value?.archiveForm ||
                        supplementForm.value.locationId !== archive.value?.locationId ||
                        supplementForm.value.boxNo !== archive.value?.boxNo
    
    if (uploadedFileIds.length === 0 && !formChanged) {
      ElMessage.warning('请上传文件或修改档案信息')
      return
    }
    
    // 提交补充信息
    const data = {
      archiveForm: supplementForm.value.archiveForm,
      locationId: supplementForm.value.locationId,
      boxNo: supplementForm.value.boxNo,
      fileIds: uploadedFileIds
    }
    
    await supplementArchive(route.params.id, data)
    ElMessage.success('补充上传成功')
    supplementDialogVisible.value = false
    // 刷新页面数据
    fetchData()
  } catch (e) {
    console.error('补充上传失败', e)
    ElMessage.error(e.response?.data?.message || '补充上传失败')
  } finally {
    supplementSubmitting.value = false
  }
}

// 返回
const goBack = () => {
  router.push('/archives')
}

// 打包下载所有文件
const handleDownloadAll = async () => {
  if (files.value.length === 0) {
    ElMessage.warning('该档案暂无文件可下载')
    return
  }
  
  isDownloading.value = true
  try {
    const res = await getArchiveDownloadUrl(route.params.id)
    if (res.data?.url) {
      // 打开下载链接
      window.open(res.data.url, '_blank')
      ElMessage.success('开始下载档案文件包')
    } else {
      ElMessage.error('获取下载链接失败')
    }
  } catch (e) {
    console.error('打包下载失败', e)
    ElMessage.error(e.response?.data?.message || '打包下载失败')
  } finally {
    isDownloading.value = false
  }
}

// 预览文件
const handlePreview = (file) => {
  previewFileId.value = file.id
  previewFileName.value = file.originalName || file.fileName
  previewFileExtension.value = file.fileExtension || ''
  previewVisible.value = true
}

// 下载文件
const handleDownload = async (file) => {
  try {
    const res = await getFileDownloadUrl(file.id)
    if (res.data?.url) {
      window.open(res.data.url, '_blank')
    } else {
      ElMessage.error('获取下载链接失败')
    }
  } catch (e) {
    console.error('获取下载链接失败', e)
    ElMessage.error(e.response?.data?.message || '获取下载链接失败')
  }
}

// 格式化函数
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
// 注：getArchiveTypeName, getStatusName, getStatusType, getRetentionName, 
// getSecurityName, getSourceName 已从 archiveEnums.js 导入

const getFileIcon = (ext) => {
  if (!ext) return Document
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
  const videoExts = ['mp4', 'avi', 'mov', 'wmv']
  const audioExts = ['mp3', 'wav', 'flac']
  
  ext = ext.toLowerCase()
  if (imageExts.includes(ext)) return Picture
  if (videoExts.includes(ext)) return VideoPlay
  if (audioExts.includes(ext)) return Headset
  if (['zip', 'rar', '7z'].includes(ext)) return FolderOpened
  return Document
}

const getFileIconClass = (ext) => {
  if (!ext) return 'file-icon'
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp']
  ext = ext.toLowerCase()
  if (imageExts.includes(ext)) return 'file-icon file-icon-image'
  if (['pdf'].includes(ext)) return 'file-icon file-icon-pdf'
  if (['doc', 'docx'].includes(ext)) return 'file-icon file-icon-word'
  if (['xls', 'xlsx'].includes(ext)) return 'file-icon file-icon-excel'
  return 'file-icon'
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.archive-detail {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
  
  .title {
    font-size: 18px;
    font-weight: 600;
  }
  
  .ml-2 {
    margin-left: 8px;
  }
}

.borrow-tip {
  margin-top: 12px;
}

.status-tip {
  margin-top: 12px;
}

.status-alert-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card {
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;
  }
}

.files-card {
  :deep(.el-card__body) {
    padding: 16px;
  }
}

// 文件分类折叠面板
.file-categories {
  border: none;
  
  :deep(.el-collapse-item__header) {
    height: 52px;
    background: #fafafa;
    border-radius: 6px;
    margin-bottom: 8px;
    padding: 0 16px;
    border: 1px solid #ebeef5;
    
    &:hover {
      background: #f5f7fa;
    }
  }
  
  :deep(.el-collapse-item__wrap) {
    border: none;
  }
  
  :deep(.el-collapse-item__content) {
    padding: 0 0 16px 0;
  }
}

.category-header {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  
  .category-icon {
    width: 32px;
    height: 32px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
  }
  
  .category-name {
    font-weight: 500;
    font-size: 15px;
    color: #303133;
  }
}

.volume-sections {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-block {
  padding: 4px 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 8px 10px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

// 文件列表
.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 8px;
}

.file-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.2s;
  
  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
    
    .file-actions {
      opacity: 1;
    }
  }
  
  .file-index {
    width: 24px;
    height: 24px;
    background: #f0f2f5;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #909399;
    margin-right: 12px;
    flex-shrink: 0;
  }
  
  .file-icon-wrapper {
    width: 40px;
    height: 40px;
    background: #f5f7fa;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 12px;
    flex-shrink: 0;
    
    .file-icon {
      font-size: 20px;
      color: #909399;
      
      &-image { color: #67c23a; }
      &-pdf { color: #f56c6c; }
      &-word { color: #409eff; }
      &-excel { color: #67c23a; }
    }
  }
  
  .file-info {
    flex: 1;
    min-width: 0;
    
    .file-name-text {
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-bottom: 4px;
    }
    
    .file-meta {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;

      .meta-chip {
        font-size: 12px;
        color: #606266;
        background: #f5f7fa;
        border-radius: 999px;
        padding: 2px 8px;
      }
      
      .file-size {
        font-size: 12px;
        color: #909399;
      }
      
      .long-term-badge {
        font-size: 11px;
        color: #67c23a;
        background: #f0f9eb;
        padding: 2px 6px;
        border-radius: 4px;
      }
    }
  }
  
  .file-actions {
    display: flex;
    gap: 4px;
    opacity: 0.6;
    transition: opacity 0.2s;
  }
}

.preview-iframe {
  width: 100%;
  height: 70vh;
  border: none;
}

.borrow-archive-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  
  .archive-no {
    font-size: 12px;
    color: #909399;
  }
  
  .archive-title {
    font-weight: 500;
    color: #303133;
  }
}

.supplement-dialog {
  :deep(.el-dialog__body) {
    max-height: calc(100vh - 180px);
    overflow-y: auto;
    padding-top: 8px;
  }
}

.supplement-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.supplement-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 72px;
  padding: 14px 16px;
  border: 1px solid #e5eaf3;
  border-radius: 12px;
  background: linear-gradient(180deg, #fbfcfe 0%, #f5f7fa 100%);
}

.summary-item-wide {
  grid-column: span 2;
}

.summary-label {
  font-size: 12px;
  color: #909399;
}

.summary-value {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  word-break: break-word;
}

.supplement-section {
  padding: 18px 18px 4px;
  border: 1px solid #ebeef5;
  border-radius: 14px;
  background: #fff;
}

.section-heading {
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.supplement-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.span-2 {
  grid-column: 1 / -1;
}

.upload-form-item {
  margin-bottom: 0;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #8c8c8c;
}

@media (max-width: 900px) {
  .supplement-summary,
  .supplement-grid {
    grid-template-columns: 1fr;
  }

  .summary-item-wide,
  .span-2 {
    grid-column: auto;
  }
}
</style>
