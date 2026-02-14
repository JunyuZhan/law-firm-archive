<template>
  <div class="category-manage">
    <el-row :gutter="20">
      <!-- 左侧：分类树 -->
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>档案分类</span>
              <el-button
                type="primary"
                size="small"
                @click="handleAddRoot"
              >
                <el-icon><Plus /></el-icon>
                新增根分类
              </el-button>
            </div>
          </template>
          
          <el-tabs
            v-model="activeType"
            @tab-change="handleTypeChange"
          >
            <el-tab-pane
              label="全部"
              name="all"
            />
            <el-tab-pane
              label="文书"
              name="DOCUMENT"
            />
            <el-tab-pane
              label="科技"
              name="SCIENCE"
            />
            <el-tab-pane
              label="专业"
              name="SPECIAL"
            />
          </el-tabs>
          
          <el-tree
            ref="treeRef"
            :data="treeData"
            :props="treeProps"
            node-key="id"
            highlight-current
            default-expand-all
            draggable
            @node-click="handleNodeClick"
            @node-drop="handleNodeDrop"
          >
            <template #default="{ node, data }">
              <div class="tree-node">
                <span class="node-label">{{ data.categoryName }}</span>
                <span class="node-code">{{ data.categoryCode }}</span>
              </div>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：分类详情/编辑 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>{{ isEditing ? '编辑分类' : (currentCategory ? '分类详情' : '请选择分类') }}</span>
              <div v-if="currentCategory && !isEditing">
                <el-button
                  type="primary"
                  size="small"
                  @click="handleEdit"
                >
                  编辑
                </el-button>
                <el-button
                  size="small"
                  @click="handleAddChild"
                >
                  添加子分类
                </el-button>
                <el-popconfirm
                  title="确定删除该分类?"
                  @confirm="handleDelete"
                >
                  <template #reference>
                    <el-button
                      type="danger"
                      size="small"
                    >
                      删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </template>

          <div v-if="currentCategory || isEditing">
            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="100px"
              :disabled="!isEditing"
            >
              <el-form-item
                label="分类代码"
                prop="categoryCode"
              >
                <el-input
                  v-model="form.categoryCode"
                  placeholder="如：WS-01"
                />
              </el-form-item>
              <el-form-item
                label="分类名称"
                prop="categoryName"
              >
                <el-input
                  v-model="form.categoryName"
                  placeholder="分类名称"
                />
              </el-form-item>
              <el-form-item
                label="档案门类"
                prop="archiveType"
              >
                <el-select
                  v-model="form.archiveType"
                  placeholder="请选择"
                  style="width: 100%"
                >
                  <el-option
                    label="文书档案"
                    value="DOCUMENT"
                  />
                  <el-option
                    label="科技档案"
                    value="SCIENCE"
                  />
                  <el-option
                    label="会计档案"
                    value="ACCOUNTING"
                  />
                  <el-option
                    label="人事档案"
                    value="PERSONNEL"
                  />
                  <el-option
                    label="专业档案"
                    value="SPECIAL"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="保管期限">
                <el-select
                  v-model="form.retentionPeriod"
                  placeholder="默认保管期限"
                  style="width: 100%"
                >
                  <el-option
                    label="永久"
                    value="PERMANENT"
                  />
                  <el-option
                    label="30年"
                    value="Y30"
                  />
                  <el-option
                    label="15年"
                    value="Y15"
                  />
                  <el-option
                    label="10年"
                    value="Y10"
                  />
                  <el-option
                    label="5年"
                    value="Y5"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="排序号">
                <el-input-number
                  v-model="form.sortOrder"
                  :min="0"
                />
              </el-form-item>
              <el-form-item label="说明">
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="3"
                />
              </el-form-item>
            </el-form>

            <div
              v-if="isEditing"
              class="form-actions"
            >
              <el-button @click="cancelEdit">
                取消
              </el-button>
              <el-button
                type="primary"
                @click="handleSave"
              >
                保存
              </el-button>
            </div>

            <div v-if="!isEditing && currentCategory">
              <el-descriptions
                :column="2"
                border
                class="category-info"
              >
                <el-descriptions-item label="完整路径">
                  {{ currentCategory.fullPath }}
                </el-descriptions-item>
                <el-descriptions-item label="层级">
                  第 {{ currentCategory.level }} 级
                </el-descriptions-item>
                <el-descriptions-item label="档案数量">
                  {{ archiveCount }} 个
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                  {{ currentCategory.status }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>

          <el-empty
            v-else
            description="请在左侧选择分类"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { 
  getCategoryTree, getCategoryStatistics, 
  createCategory, updateCategory, deleteCategory, moveCategory 
} from '@/api/category'

const treeRef = ref(null)
const formRef = ref(null)
const activeType = ref('all')
const treeData = ref([])
const currentCategory = ref(null)
const isEditing = ref(false)
const isCreating = ref(false)
const archiveCount = ref(0)

const treeProps = {
  label: 'categoryName',
  children: 'children'
}

const form = reactive({
  parentId: null,
  categoryCode: '',
  categoryName: '',
  archiveType: 'DOCUMENT',
  retentionPeriod: '',
  sortOrder: 0,
  description: ''
})

const rules = {
  categoryCode: [{ required: true, message: '请输入分类代码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  archiveType: [{ required: true, message: '请选择档案门类', trigger: 'change' }]
}

// 加载分类树
const loadTree = async () => {
  try {
    const archiveType = activeType.value === 'all' ? null : activeType.value
    const res = await getCategoryTree(archiveType)
    treeData.value = res.data
  } catch (e) {
    console.error('加载分类树失败', e)
  }
}

// 切换档案类型
const handleTypeChange = () => {
  loadTree()
  currentCategory.value = null
}

// 点击节点
const handleNodeClick = async (data) => {
  if (isEditing.value) return
  
  currentCategory.value = data
  Object.assign(form, {
    parentId: data.parentId,
    categoryCode: data.categoryCode,
    categoryName: data.categoryName,
    archiveType: data.archiveType,
    retentionPeriod: data.retentionPeriod,
    sortOrder: data.sortOrder,
    description: data.description
  })
  
  // 获取统计信息
  try {
    const res = await getCategoryStatistics(data.id)
    archiveCount.value = res.data.archiveCount
  } catch (e) {
    archiveCount.value = 0
  }
}

// 拖拽放置
const handleNodeDrop = async (draggingNode, dropNode, dropType) => {
  const dragId = draggingNode.data.id
  let newParentId = null
  
  if (dropType === 'inner') {
    newParentId = dropNode.data.id
  } else {
    newParentId = dropNode.data.parentId
  }
  
  try {
    await moveCategory(dragId, newParentId)
    ElMessage.success('移动成功')
    loadTree()
  } catch (e) {
    console.error('移动失败', e)
    loadTree() // 刷新恢复
  }
}

// 新增根分类
const handleAddRoot = () => {
  isEditing.value = true
  isCreating.value = true
  currentCategory.value = null
  Object.assign(form, {
    parentId: null,
    categoryCode: '',
    categoryName: '',
    archiveType: activeType.value === 'all' ? 'DOCUMENT' : activeType.value,
    retentionPeriod: '',
    sortOrder: 0,
    description: ''
  })
}

// 添加子分类
const handleAddChild = () => {
  isEditing.value = true
  isCreating.value = true
  const parentData = { ...currentCategory.value }
  Object.assign(form, {
    parentId: parentData.id,
    categoryCode: '',
    categoryName: '',
    archiveType: parentData.archiveType,
    retentionPeriod: parentData.retentionPeriod,
    sortOrder: 0,
    description: ''
  })
  currentCategory.value = null
}

// 编辑
const handleEdit = () => {
  isEditing.value = true
  isCreating.value = false
}

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false
  isCreating.value = false
  if (currentCategory.value) {
    Object.assign(form, currentCategory.value)
  }
}

// 保存
const handleSave = async () => {
  try {
    await formRef.value.validate()
    
    if (isCreating.value) {
      await createCategory(form)
      ElMessage.success('创建成功')
    } else {
      await updateCategory(currentCategory.value.id, form)
      ElMessage.success('更新成功')
    }
    
    isEditing.value = false
    isCreating.value = false
    loadTree()
  } catch (e) {
    if (e !== false) {
      console.error('保存失败', e)
    }
  }
}

// 删除
const handleDelete = async () => {
  try {
    await deleteCategory(currentCategory.value.id)
    ElMessage.success('删除成功')
    currentCategory.value = null
    loadTree()
  } catch (e) {
    console.error('删除失败', e)
  }
}

onMounted(() => {
  loadTree()
})
</script>

<style lang="scss" scoped>
.category-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .node-code {
    font-size: 12px;
    color: #909399;
  }
}

.form-actions {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  text-align: right;
}

.category-info {
  margin-top: 20px;
}
</style>
