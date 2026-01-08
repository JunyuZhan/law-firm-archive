<template>
  <Page title="项目文档管理演示" description="以项目为核心的文档管理系统">
    <!-- 筛选条件 -->
    <Card class="mb-4">
      <Row :gutter="[16, 16]">
        <Col :xs="24" :sm="12" :md="6" :lg="4">
          <Select
            v-model:value="filterParams.year"
            placeholder="选择年度"
            allowClear
            style="width: 100%"
          >
            <Select.Option value="2024">2024年</Select.Option>
            <Select.Option value="2023">2023年</Select.Option>
            <Select.Option value="2022">2022年</Select.Option>
          </Select>
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="4">
          <Select
            v-model:value="filterParams.matterType"
            placeholder="项目类型"
            allowClear
            style="width: 100%"
          >
            <Select.Option value="CIVIL">民事案件</Select.Option>
            <Select.Option value="CRIMINAL">刑事案件</Select.Option>
            <Select.Option value="COMMERCIAL">商事案件</Select.Option>
            <Select.Option value="LEGAL_COUNSEL">法律顾问</Select.Option>
          </Select>
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="4">
          <Select
            v-model:value="filterParams.status"
            placeholder="项目状态"
            allowClear
            style="width: 100%"
          >
            <Select.Option value="ACTIVE">进行中</Select.Option>
            <Select.Option value="CLOSED">已结案</Select.Option>
            <Select.Option value="SUSPENDED">已暂停</Select.Option>
          </Select>
        </Col>
        <Col :xs="24" :sm="12" :md="6" :lg="8">
          <Space>
            <Input
              v-model:value="filterParams.keyword"
              placeholder="搜索项目名称"
              allowClear
              style="width: 200px"
            />
            <Button type="primary">筛选</Button>
            <Button>重置</Button>
          </Space>
        </Col>
      </Row>
    </Card>

    <!-- 项目卡片网格 -->
    <div class="project-grid">
      <Row :gutter="[16, 16]">
        <Col 
          v-for="project in mockProjects" 
          :key="project.id"
          :xs="24" 
          :sm="12" 
          :md="8" 
          :lg="6"
        >
          <Card 
            hoverable 
            class="project-card"
            @click="selectedProject = project"
            :class="{ 'selected': selectedProject?.id === project.id }"
          >
            <template #cover>
              <div class="project-cover">
                <Inbox class="project-icon" />
                <div class="project-stats">
                  <Tag color="blue">{{ project.typeName }}</Tag>
                  <div class="doc-count">{{ project.docCount }} 个文档</div>
                </div>
              </div>
            </template>
            
            <Card.Meta>
              <template #title>
                <Tooltip :title="project.name">
                  <div class="project-title">{{ project.name }}</div>
                </Tooltip>
              </template>
              <template #description>
                <div class="project-info">
                  <div>客户：{{ project.clientName }}</div>
                  <div>创建：{{ project.createdAt }}</div>
                  <div>状态：<Tag :color="getStatusColor(project.status)">{{ project.statusName }}</Tag></div>
                </div>
              </template>
            </Card.Meta>
          </Card>
        </Col>
      </Row>
    </div>

    <!-- 选中项目的文档管理 -->
    <Card v-if="selectedProject" class="mt-4">
      <template #title>
        <Space>
          <Folder />
          {{ selectedProject.name }} - 文档管理
          <Tag color="blue">{{ selectedProject.docCount }} 个文档</Tag>
          <Tag color="green">{{ selectedProject.totalSize }}</Tag>
        </Space>
      </template>
      
      <template #extra>
        <Space>
          <Button type="primary">
            <ArrowUp />
            上传文档
          </Button>
          <Button>
            <Plus />
            新建文件夹
          </Button>
        </Space>
      </template>

      <Row :gutter="16">
        <!-- 左侧文件夹树 -->
        <Col :span="6">
          <Card size="small" title="文件夹">
            <Tree
              :tree-data="folderTreeData"
              :selected-keys="[selectedFolder]"
              @select="handleFolderSelect"
              :show-icon="true"
            />
          </Card>
        </Col>

        <!-- 右侧文档列表 -->
        <Col :span="18">
          <Card size="small">
            <template #title>
              <Space>
                <span>{{ currentPath.join(' / ') }}</span>
                <Tag color="blue">{{ currentDocuments.length }} 个文档</Tag>
              </Space>
            </template>

            <Table
              :columns="documentColumns"
              :data-source="currentDocuments"
              :pagination="false"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <Space>
                    <File style="color: #1890ff" />
                    <a>{{ record.name }}</a>
                  </Space>
                </template>
                <template v-if="column.key === 'action'">
                  <Dropdown>
                    <template #overlay>
                      <Menu>
                        <Menu.Item>
                          <Eye />
                          预览
                        </Menu.Item>
                        <Menu.Item>
                          <SvgDownloadIcon />
                          下载
                        </Menu.Item>
                        <Menu.Item>
                          <ExternalLink />
                          编辑
                        </Menu.Item>
                        <Menu.Item>
                          <ExternalLink />
                          分享
                        </Menu.Item>
                        <Menu.Divider />
                        <Menu.Item style="color: red">
                          <X />
                          删除
                        </Menu.Item>
                      </Menu>
                    </template>
                    <Button size="small">操作</Button>
                  </Dropdown>
                </template>
              </template>
            </Table>
          </Card>
        </Col>
      </Row>
    </Card>
  </Page>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { Page } from '@vben/common-ui';
import {
  Card,
  Table,
  Button,
  Space,
  Input,
  Select,
  Row,
  Col,
  Tag,
  Tooltip,
  Dropdown,
  Menu,
  Tree,
} from 'ant-design-vue';
import {
  Eye,
  ExternalLink,
  X,
  Plus,
  Inbox,
  ArrowUp,
} from '@vben/icons';
import { SvgDownloadIcon } from '@vben/icons';

defineOptions({ name: 'ProjectDocumentDemo' });

// 筛选参数
const filterParams = reactive({
  year: undefined,
  matterType: undefined,
  status: undefined,
  keyword: undefined,
});

// 选中的项目和文件夹
const selectedProject = ref(null);
const selectedFolder = ref('root');
const currentPath = ref(['根目录']);

// 模拟项目数据
const mockProjects = ref([
  {
    id: 1,
    name: '张三诉李四合同纠纷案',
    clientName: '张三',
    typeName: '民事案件',
    type: 'CIVIL',
    status: 'ACTIVE',
    statusName: '进行中',
    createdAt: '2024-01-15',
    docCount: 12,
    totalSize: '25.6MB'
  },
  {
    id: 2,
    name: '王五涉嫌诈骗案',
    clientName: '王五',
    typeName: '刑事案件',
    type: 'CRIMINAL',
    status: 'ACTIVE',
    statusName: '进行中',
    createdAt: '2024-02-20',
    docCount: 8,
    totalSize: '18.3MB'
  },
  {
    id: 3,
    name: 'ABC公司法律顾问',
    clientName: 'ABC公司',
    typeName: '法律顾问',
    type: 'LEGAL_COUNSEL',
    status: 'ACTIVE',
    statusName: '进行中',
    createdAt: '2024-01-01',
    docCount: 35,
    totalSize: '156.8MB'
  },
  {
    id: 4,
    name: '赵六劳动争议案',
    clientName: '赵六',
    typeName: '劳动争议',
    type: 'LABOR',
    status: 'CLOSED',
    statusName: '已结案',
    createdAt: '2023-11-10',
    docCount: 6,
    totalSize: '12.4MB'
  },
  {
    id: 5,
    name: '钱七诉某区政府行政复议案',
    clientName: '钱七',
    typeName: '行政案件',
    type: 'ADMINISTRATIVE',
    status: 'ACTIVE',
    statusName: '进行中',
    createdAt: '2024-03-01',
    docCount: 9,
    totalSize: '21.2MB'
  }
]);

// 文件夹树数据 - 根据项目类型动态生成
const folderTreeData = computed(() => {
  if (!selectedProject.value) return [];
  
  const projectType = selectedProject.value.type;
  
  switch (projectType) {
    case 'CRIMINAL':
      return [
        {
          title: '刑事卷宗',
          key: 'criminal-root',
          children: [
            { title: '01-收案审批表', key: 'case-approval' },
            { title: '02-授权委托书', key: 'authorization' },
            { title: '03-委托合同', key: 'contract' },
            { title: '04-收费发票', key: 'invoice' },
            { title: '05-办案机关卷宗材料/阅卷笔录', key: 'case-files' },
            { title: '06-会见笔录', key: 'meeting-records' },
            { title: '07-起诉书/上诉状/抗诉书/再审申请书', key: 'legal-documents' },
            { title: '08-重大案件集体讨论记录', key: 'discussion-records' },
            { title: '09-辩护词', key: 'defense-statement' },
            { title: '10-裁定书/判决书', key: 'judgment' },
            { title: '11-工作日志', key: 'work-log' },
            { title: '12-结案（归档）审批表', key: 'case-closure' },
            { title: '13-其他材料', key: 'others' }
          ]
        }
      ];
    case 'CIVIL':
    case 'ADMINISTRATIVE':  // 行政案件使用与民事案件相同的结构
      return [
        {
          title: '民事/行政卷宗',
          key: 'civil-root',
          children: [
            { title: '01-收案审批表', key: 'case-approval' },
            { title: '02-授权委托书', key: 'authorization' },
            { title: '03-委托合同', key: 'contract' },
            { title: '04-收费发票', key: 'invoice' },
            { title: '05-起诉状/上诉状/答辩状/再审申请书', key: 'legal-documents' },
            { title: '06-证据材料/阅卷笔录', key: 'evidence' },
            { title: '07-重大案件集体讨论记录', key: 'discussion-records' },
            { title: '08-代理词', key: 'representation-statement' },
            { title: '09-判决书/裁定书/调解书', key: 'judgment' },
            { title: '10-工作日志', key: 'work-log' },
            { title: '11-结案（归档）审批表', key: 'case-closure' },
            { title: '12-其他材料', key: 'others' }
          ]
        }
      ];
    case 'LEGAL_COUNSEL':
      return [
        {
          title: '法律顾问卷宗',
          key: 'counsel-root',
          children: [
            { title: '01-顾问合同', key: 'counsel-contract' },
            { title: '02-收费发票', key: 'invoice' },
            { 
              title: '03-法律事务记录及相关材料', 
              key: 'legal-affairs', 
              children: [
                { title: '合同起草审查', key: 'contract-drafting' },
                { title: '规章制度', key: 'regulations' },
                { title: '法律意见书', key: 'legal-opinions' },
                { title: '律师函', key: 'lawyer-letters' },
                { title: '咨询记录', key: 'consultation-records' }
              ]
            },
            { title: '04-工作日志', key: 'work-log' },
            { title: '05-工作小结', key: 'work-summary' },
            { title: '06-其他材料', key: 'others' }
          ]
        }
      ];
    default:
      return [
        {
          title: '非诉讼法律事务卷宗',
          key: 'non-litigation-root',
          children: [
            { title: '01-授权委托书', key: 'authorization' },
            { title: '02-收费发票', key: 'invoice' },
            { title: '03-证据材料', key: 'evidence' },
            { title: '04-法律意见书/律师函/其他法律文书', key: 'legal-documents' },
            { title: '05-工作小结', key: 'work-summary' },
            { title: '06-其他材料', key: 'others' }
          ]
        }
      ];
  }
});

// 模拟文档数据 - 按照标准归档顺序
const mockDocuments = {
  // 刑事案件文档
  'case-approval': [
    { id: 1, name: '收案审批表-王五诈骗案.pdf', size: '125KB', updatedAt: '2024-02-20 09:00', version: 'v1.0', uploader: '张律师' }
  ],
  'authorization': [
    { id: 2, name: '授权委托书-王五.pdf', size: '89KB', updatedAt: '2024-02-20 10:30', version: 'v1.0', uploader: '张律师' }
  ],
  'contract': [
    { id: 3, name: '刑事辩护委托合同.pdf', size: '156KB', updatedAt: '2024-02-20 11:00', version: 'v1.0', uploader: '李律师' }
  ],
  'meeting-records': [
    { id: 4, name: '会见笔录-第1次.docx', size: '234KB', updatedAt: '2024-02-22 14:30', version: 'v1.0', uploader: '张律师' },
    { id: 5, name: '会见笔录-第2次.docx', size: '198KB', updatedAt: '2024-02-25 16:20', version: 'v1.0', uploader: '张律师' }
  ],
  'defense-statement': [
    { id: 6, name: '一审辩护词.docx', size: '345KB', updatedAt: '2024-03-01 10:15', version: 'v2.0', uploader: '张律师' }
  ],
  
  // 民事案件文档
  'evidence': [
    { id: 7, name: '证据1-合同原件.pdf', size: '2.3MB', updatedAt: '2024-01-22 09:15', version: 'v1.0', uploader: '张律师' },
    { id: 8, name: '证据2-银行流水.xlsx', size: '456KB', updatedAt: '2024-01-21 16:45', version: 'v1.0', uploader: '王助理' }
  ],
  'representation-statement': [
    { id: 9, name: '代理词-一审.docx', size: '298KB', updatedAt: '2024-01-25 11:20', version: 'v2.0', uploader: '张律师' }
  ],
  
  // 法律顾问文档
  'counsel-contract': [
    { id: 10, name: 'ABC公司法律顾问合同.pdf', size: '234KB', updatedAt: '2024-01-01 14:00', version: 'v1.0', uploader: '李律师' }
  ],
  'legal-opinions': [
    { id: 11, name: '关于股权转让的法律意见书.docx', size: '456KB', updatedAt: '2024-01-15 10:30', version: 'v1.0', uploader: '李律师' },
    { id: 12, name: '劳动合同条款审查意见.docx', size: '234KB', updatedAt: '2024-01-20 16:20', version: 'v1.0', uploader: '王律师' }
  ],
  'lawyer-letters': [
    { id: 13, name: '催款律师函.docx', size: '123KB', updatedAt: '2024-01-25 09:45', version: 'v1.0', uploader: '李律师' }
  ]
};

// 当前文档列表
const currentDocuments = computed(() => {
  return mockDocuments[selectedFolder.value] || [];
});

// 文档表格列
const documentColumns = [
  { title: '文档名称', dataIndex: 'name', key: 'name', width: 250 },
  { title: '文件大小', dataIndex: 'size', key: 'size', width: 100 },
  { title: '修改时间', dataIndex: 'updatedAt', key: 'updatedAt', width: 160 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '上传人', dataIndex: 'uploader', key: 'uploader', width: 100 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
];

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap = {
    'ACTIVE': 'green',
    'CLOSED': 'blue',
    'SUSPENDED': 'orange',
    'CANCELLED': 'red',
  };
  return colorMap[status] || 'default';
}

// 选择文件夹
function handleFolderSelect(selectedKeys: string[]) {
  if (selectedKeys.length > 0) {
    selectedFolder.value = selectedKeys[0];
    updateBreadcrumb(selectedKeys[0]);
  }
}

// 更新面包屑
function updateBreadcrumb(folderKey: string) {
  if (!selectedProject.value) return;
  
  const projectType = selectedProject.value.type;
  let pathMap: Record<string, string[]> = {};
  
  switch (projectType) {
    case 'CRIMINAL':
      pathMap = {
        'criminal-root': ['刑事卷宗'],
        'case-approval': ['刑事卷宗', '01-收案审批表'],
        'authorization': ['刑事卷宗', '02-授权委托书'],
        'contract': ['刑事卷宗', '03-委托合同'],
        'invoice': ['刑事卷宗', '04-收费发票'],
        'case-files': ['刑事卷宗', '05-办案机关卷宗材料/阅卷笔录'],
        'meeting-records': ['刑事卷宗', '06-会见笔录'],
        'legal-documents': ['刑事卷宗', '07-起诉书/上诉状/抗诉书/再审申请书'],
        'discussion-records': ['刑事卷宗', '08-重大案件集体讨论记录'],
        'defense-statement': ['刑事卷宗', '09-辩护词'],
        'judgment': ['刑事卷宗', '10-裁定书/判决书'],
        'work-log': ['刑事卷宗', '11-工作日志'],
        'case-closure': ['刑事卷宗', '12-结案（归档）审批表'],
        'others': ['刑事卷宗', '13-其他材料']
      };
      break;
    case 'CIVIL':
    case 'ADMINISTRATIVE':  // 行政案件使用与民事案件相同的路径
      pathMap = {
        'civil-root': ['民事/行政卷宗'],
        'case-approval': ['民事/行政卷宗', '01-收案审批表'],
        'authorization': ['民事/行政卷宗', '02-授权委托书'],
        'contract': ['民事/行政卷宗', '03-委托合同'],
        'invoice': ['民事/行政卷宗', '04-收费发票'],
        'legal-documents': ['民事/行政卷宗', '05-起诉状/上诉状/答辩状/再审申请书'],
        'evidence': ['民事/行政卷宗', '06-证据材料/阅卷笔录'],
        'discussion-records': ['民事/行政卷宗', '07-重大案件集体讨论记录'],
        'representation-statement': ['民事/行政卷宗', '08-代理词'],
        'judgment': ['民事/行政卷宗', '09-判决书/裁定书/调解书'],
        'work-log': ['民事/行政卷宗', '10-工作日志'],
        'case-closure': ['民事/行政卷宗', '11-结案（归档）审批表'],
        'others': ['民事/行政卷宗', '12-其他材料']
      };
      break;
    case 'LEGAL_COUNSEL':
      pathMap = {
        'counsel-root': ['法律顾问卷宗'],
        'counsel-contract': ['法律顾问卷宗', '01-顾问合同'],
        'invoice': ['法律顾问卷宗', '02-收费发票'],
        'legal-affairs': ['法律顾问卷宗', '03-法律事务记录及相关材料'],
        'contract-drafting': ['法律顾问卷宗', '03-法律事务记录及相关材料', '合同起草审查'],
        'regulations': ['法律顾问卷宗', '03-法律事务记录及相关材料', '规章制度'],
        'legal-opinions': ['法律顾问卷宗', '03-法律事务记录及相关材料', '法律意见书'],
        'lawyer-letters': ['法律顾问卷宗', '03-法律事务记录及相关材料', '律师函'],
        'consultation-records': ['法律顾问卷宗', '03-法律事务记录及相关材料', '咨询记录'],
        'work-log': ['法律顾问卷宗', '04-工作日志'],
        'work-summary': ['法律顾问卷宗', '05-工作小结'],
        'others': ['法律顾问卷宗', '06-其他材料']
      };
      break;
    default:
      pathMap = {
        'non-litigation-root': ['非诉讼法律事务卷宗'],
        'authorization': ['非诉讼法律事务卷宗', '01-授权委托书'],
        'invoice': ['非诉讼法律事务卷宗', '02-收费发票'],
        'evidence': ['非诉讼法律事务卷宗', '03-证据材料'],
        'legal-documents': ['非诉讼法律事务卷宗', '04-法律意见书/律师函/其他法律文书'],
        'work-summary': ['非诉讼法律事务卷宗', '05-工作小结'],
        'others': ['非诉讼法律事务卷宗', '06-其他材料']
      };
  }
  
  currentPath.value = pathMap[folderKey] || ['根目录'];
}
</script>

<style scoped>
.project-grid {
  min-height: 200px;
}

.project-card {
  transition: all 0.3s;
  cursor: pointer;
}

.project-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.project-card.selected {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.project-cover {
  height: 120px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}

.project-icon {
  font-size: 36px;
  color: white;
  margin-bottom: 8px;
}

.project-stats {
  position: absolute;
  top: 8px;
  right: 8px;
  text-align: right;
}

.doc-count {
  color: white;
  font-size: 12px;
  margin-top: 4px;
}

.project-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-info {
  font-size: 12px;
  color: #666;
}

.project-info > div {
  margin-bottom: 2px;
}
</style>