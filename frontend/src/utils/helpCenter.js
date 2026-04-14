import { MANAGER_ROLES, REPORT_ROLES, REVIEW_ROLES, ROLES } from '@/utils/permission'

export const ROLE_LABELS = {
  [ROLES.SYSTEM_ADMIN]: '系统管理员',
  [ROLES.ARCHIVE_REVIEWER]: '档案审核员',
  [ROLES.ARCHIVE_MANAGER]: '档案管理员',
  [ROLES.USER]: '普通用户'
}

export const HELP_GROUPS = [
  {
    key: 'intake',
    title: '入库与接收',
    description: '适合整理纸质材料、扫描后入库、补充电子文件时参考。',
    entries: [
      {
        title: '档案接收',
        route: '/receive',
        description: '新建待归档记录，录入基础信息并接收扫描件。',
        roles: [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVE_MANAGER, ROLES.ARCHIVE_REVIEWER, ROLES.USER],
        steps: ['录入案卷标题、年份、分类和责任人等基础信息。', '上传扫描后的电子文件，补充文件类别和版本信息。', '确认目录完整后提交归档，进入后续保管和借阅流程。'],
        tips: ['普通用户提交后不等于正式入库，需按来源策略进入待审核或直接落库。', '纸质档案可同步记录盒号、柜号或库位位置。']
      },
      {
        title: '档案列表',
        route: '/archives',
        description: '查看已有档案，继续补录目录、文件和盒号位置。',
        steps: ['通过列表查看当前档案状态和电子文件数量。', '进入详情页继续补录文件、目录、保管期限和纸质位置。', '根据业务需要执行借阅、预览、下载等操作。'],
        tips: ['建议先核对电子文件是否齐全，再流转到借阅环节。']
      },
      {
        title: '档案设置',
        route: '/archive-settings',
        description: '维护分类、来源、全宗和库位等基础档案参数。',
        roles: MANAGER_ROLES,
        steps: ['先维护分类、来源、全宗等基础字典。', '再维护库位、位置和来源，保证入库时可直接选择。', '变更后再检查接收页和档案页下拉项是否正确。'],
        tips: ['分类和位置尽量统一命名，避免后续统计口径不一致。']
      },
      {
        title: '推送记录',
        route: '/push-records',
        description: '核对开放接口入库、回调推送和批量导入结果。',
        roles: MANAGER_ROLES,
        steps: ['查看最近一次接口入库或推送任务。', '核对成功、失败和重试状态。', '失败时结合错误信息回到业务页面修正数据。'],
        tips: ['开放接口入库后，应抽查样本档案是否已落库并可检索。']
      }
    ]
  },
  {
    key: 'storage',
    title: '保存与整理',
    description: '适合按年度、分类、保管期限整理电子档案并快速查找。',
    entries: [
      {
        title: '档案列表',
        route: '/archives',
        description: '核对案卷信息、电子文件数量和纸质存放位置。',
        steps: ['按列表筛选档案范围。', '检查电子文件数量、状态和归档完整性。', '必要时补录纸质位置和保管期限。'],
        tips: ['电子档案为主，纸质位置信息作为辅助手段记录。']
      },
      {
        title: '档案检索',
        route: '/search',
        description: '按档案号、标题、年份、分类和关键词快速检索。',
        steps: ['输入关键词或组合筛选条件。', '根据结果定位目标档案。', '进入详情查看文件、目录和借阅情况。'],
        tips: ['优先使用年份、分类、标题等条件组合，可减少误检。']
      },
      {
        title: '统计概览',
        route: '/statistics',
        description: '查看库藏数量、类型分布和业务趋势。',
        roles: REPORT_ROLES,
        steps: ['查看库藏总量、电子文件数量和分类分布。', '按时间段核对入库与借阅趋势。', '作为管理核查和汇报依据。'],
        tips: ['统计口径依赖基础分类配置，分类调整后应复核报表。']
      },
      {
        title: '报表导出',
        route: '/reports',
        description: '导出统计报表，便于管理核对和汇报。',
        roles: REPORT_ROLES,
        steps: ['选择报表类型和筛选条件。', '生成导出任务并下载结果。', '用于内部汇报或阶段性归档核查。'],
        tips: ['导出前先确认筛选条件，避免重复生成大批量报表。']
      }
    ]
  },
  {
    key: 'borrow',
    title: '借阅与利用',
    description: '适合申请借阅、审批借出、生成外链和跟踪归还状态。',
    entries: [
      {
        title: '借阅管理',
        route: '/borrows',
        description: '提交或处理借阅申请，查看审批、借出和归还状态。',
        steps: ['选择目标档案后发起借阅申请。', '填写用途、期限和借阅说明。', '跟踪审批、借出、归还和逾期状态。'],
        tips: ['电子借阅应优先使用受控预览或外链，减少文件外发风险。']
      },
      {
        title: '借阅链接',
        route: '/borrow-links',
        description: '发放电子借阅链接，并控制有效期和访问次数。',
        roles: MANAGER_ROLES,
        steps: ['选择档案并生成借阅链接。', '设置有效期、访问次数和访问人信息。', '发出链接后持续跟踪访问记录。'],
        tips: ['对外共享尽量设置较短有效期，并限制访问次数。']
      }
    ]
  }
]

export const ADMIN_GROUPS = [
  {
    key: 'admin-control',
    title: '管理员手册',
        description: '面向系统管理员，聚焦后台配置、权限和恢复。',
    entries: [
      {
        title: '权限管理',
        route: '/system/permissions',
        description: '维护账号、角色和授权范围。',
        roles: [ROLES.SYSTEM_ADMIN],
        steps: ['先维护用户账号，再绑定角色。', '按岗位分配最小必要权限。', '变更后用对应账号抽查页面可见范围。'],
        tips: ['角色权限调整后，应重点复核借阅、日志和系统配置等敏感页面。']
      },
      {
        title: '基础设置',
        route: '/system/setup',
        description: '维护系统名称、Logo、备案号和版权信息。',
        roles: [ROLES.SYSTEM_ADMIN],
        steps: ['填写系统名称和英文名称。', '上传 Logo 或填写 Logo 地址。', '保存备案号和版权后检查登录页与侧栏展示。'],
        tips: ['Logo 建议使用透明背景 PNG 或 SVG，避免页面显示发虚。']
      },
      {
        title: '系统配置',
        route: '/system/config',
        description: '维护档案号规则、上传限制、借阅参数和站点信息。',
        roles: [ROLES.SYSTEM_ADMIN],
        steps: ['先核对档案号规则。', '再维护上传限制、借阅参数和站点信息。', '保存后刷新缓存并抽查关键业务流程。'],
        tips: ['涉及上传大小或借阅期限调整时，应先与业务口径保持一致。']
      },
      {
        title: '备份恢复',
        route: '/system/recovery',
        description: '执行备份、恢复和数据迁移前的核对。',
        roles: [ROLES.SYSTEM_ADMIN],
        steps: ['先执行备份并记录结果。', '恢复前确认目标环境和恢复范围。', '恢复完成后核查档案、文件和借阅数据是否可用。'],
        tips: ['恢复属于敏感操作，建议先在测试环境验证。']
      },
      {
        title: '操作日志',
        route: '/system/logs',
        description: '核查关键操作留痕，支持审计和责任追溯。',
        roles: [ROLES.SYSTEM_ADMIN],
        steps: ['按时间、模块、操作人筛选日志。', '查看创建、更新、删除、导出等敏感操作。', '结合业务事件进行追溯。'],
        tips: ['审计核查时可优先关注借阅、删除、导出和系统配置变更。']
      },
      {
        title: '系统信息',
        route: '/system/info',
        description: '查看当前版本和依赖运行状态。',
        roles: [ROLES.SYSTEM_ADMIN],
        steps: ['查看当前产品版本和构建时间。', '检查依赖状态是否正常。', '异常时再联系运维进一步处理。'],
        tips: ['此页仅用于状态查看与健康检查，不提供参数修改能力。']
      }
    ]
  }
]

export const ROLE_GUIDES = [
  {
    key: 'user',
    title: '普通用户手册',
    roles: [ROLES.USER],
    summary: '适合律师或普通业务人员，重点是检索、查看和申请借阅。',
    scenarios: [
      '进入档案检索，按标题、年份、分类或关键词找到目标档案。',
      '在档案详情中查看目录、电子文件和已有借阅情况。',
      '需要利用档案时，提交借阅申请并等待审批结果。'
    ],
    cautions: [
      '如无法看到某些档案或按钮，通常是权限或数据范围限制。',
      '优先通过系统内预览和受控借阅使用电子档案，避免私下传播文件。'
    ]
  },
  {
    key: 'archivist',
    title: '档案管理员手册',
    roles: MANAGER_ROLES,
    summary: '适合负责正式入库后整理、补录和借阅流转的业务人员。',
    scenarios: [
      '接管已审核通过或已直接落库的档案，继续完成整理和补录。',
      '按分类、年度、保管期限和位置要求完善档案，保证后续可查可借。',
      '处理借阅申请、生成借阅链接，并持续跟踪借出和归还状态。'
    ],
    cautions: [
      '只能补充正式档案内容，不能删除原始入库内容。',
      '借阅链接对外发放前，应先确认访问次数、有效期和访问对象。'
    ]
  },
  {
    key: 'reviewer',
    title: '档案审核员手册',
    roles: REVIEW_ROLES,
    summary: '适合处理入库前审核，决定档案是否允许正式进入档案库。',
    scenarios: [
      '查看普通用户提交或推送来源要求审核的待审核档案。',
      '核对档案材料完整性、分类和来源信息。',
      '作出通过或退回决定，并填写审核意见。'
    ],
    cautions: [
      '审核员只负责准入，不负责正式入库后的整理和借阅管理。',
      '对于缺少材料、分类错误或来源异常的档案，应退回补正。'
    ]
  },
  {
    key: 'system-admin',
    title: '系统管理员手册',
    roles: [ROLES.SYSTEM_ADMIN],
    summary: '适合负责基础设置、系统配置、备份恢复和全局策略的管理员。',
    scenarios: [
      '首次启用系统时先完善系统名称、Logo、备案号和版权等基础信息。',
      '按业务要求维护档案号规则、上传限制、借阅策略和站点信息。',
      '定期检查备份恢复、权限配置和操作日志，确保系统安全可持续使用。'
    ],
    cautions: [
      '系统配置调整后，应抽查入库、保存、借阅三条关键链路是否正常。',
      '恢复和权限调整属于敏感操作，建议先评估影响再执行。'
    ]
  },
]

export const FAQ_ITEMS = [
  {
    question: '上传电子文件失败怎么办？',
    answer: '先检查文件类型、大小和网络状态；如果是系统 Logo，则仅支持图片且不能超过 2MB。业务电子文件上传失败时，优先核对文件格式是否在允许范围内，再重试上传。'
  },
  {
    question: '为什么搜索不到目标档案？',
    answer: '先确认档案是否已正式入库、标题或年份是否录入完整，再检查当前账号是否有查看权限。可尝试减少筛选条件，先按年份或标题关键字检索。'
  },
  {
    question: '为什么我看不到某些菜单或按钮？',
    answer: '系统会按角色和权限自动显示页面。若当前账号无权访问，菜单会隐藏或跳转无权限页。此时应联系管理员核对账号角色。'
  },
  {
    question: '借阅申请被驳回后怎么办？',
    answer: '先查看驳回原因，再补充借阅用途、期限或材料后重新发起申请。若为权限限制，应联系档案员或管理员确认是否允许借阅。'
  },
  {
    question: '恢复模式开启后又不恢复，怎么退出？',
    answer: '应由有权限的管理员在备份恢复页面关闭维护状态。关闭后再刷新页面，确认系统已恢复正常操作。'
  },
  {
    question: '纸质档案位置是否必须填写？',
    answer: '本系统以电子档案为主，纸质位置不是必须项，但建议在有纸质原件时同步登记盒号、柜号或库位，便于线下查找。'
  }
]
