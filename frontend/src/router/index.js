import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { secureStorage } from '@/utils/security'
import { BORROW_ROLES, MANAGER_ROLES, REPORT_ROLES, ROLES, hasPermission, normalizeUserType } from '@/utils/permission'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/views/layout/MainLayout.vue'),
      redirect: '/archives',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'archives',
          name: 'ArchiveList',
          component: () => import('@/views/archive/ArchiveList.vue'),
          meta: { title: '档案列表', requiresAuth: true }
        },
        {
          path: 'archives/:id',
          name: 'ArchiveDetail',
          component: () => import('@/views/archive/ArchiveDetail.vue'),
          meta: { title: '档案详情', requiresAuth: true }
        },
        {
          path: 'receive',
          name: 'ArchiveReceive',
          component: () => import('@/views/archive/ArchiveReceive.vue'),
          meta: { title: '档案接收', requiresAuth: true }
        },
        {
          path: 'search',
          name: 'ArchiveSearch',
          component: () => import('@/views/archive/ArchiveSearch.vue'),
          meta: { title: '档案检索', requiresAuth: true }
        },
        {
          path: 'archive-settings',
          name: 'ArchiveSettingsCenter',
          component: () => import('@/views/archive/ArchiveSettingsCenter.vue'),
          meta: { title: '档案设置', requiresAuth: true, roles: MANAGER_ROLES }
        },
        {
          path: 'categories',
          redirect: '/archive-settings?tab=categories'
        },
        {
          path: 'locations',
          redirect: '/archive-settings?tab=locations'
        },
        {
          path: 'borrows',
          name: 'BorrowList',
          component: () => import('@/views/borrow/BorrowList.vue'),
          meta: { title: '借阅管理', requiresAuth: true, roles: BORROW_ROLES }
        },
        {
          path: 'borrow-links',
          name: 'BorrowLinkList',
          component: () => import('@/views/borrow/BorrowLinkList.vue'),
          meta: { title: '借阅链接', requiresAuth: true, roles: MANAGER_ROLES }
        },
        {
          path: 'appraisals',
          name: 'AppraisalList',
          component: () => import('@/views/appraisal/AppraisalList.vue'),
          meta: { title: '鉴定管理', requiresAuth: true, roles: MANAGER_ROLES }
        },
        {
          path: 'destructions',
          name: 'DestructionList',
          component: () => import('@/views/destruction/DestructionList.vue'),
          meta: { title: '销毁管理', requiresAuth: true, roles: MANAGER_ROLES }
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/statistics/Statistics.vue'),
          meta: { title: '统计分析', requiresAuth: true, roles: REPORT_ROLES }
        },
        {
          path: 'reports',
          name: 'Reports',
          component: () => import('@/views/statistics/ReportPage.vue'),
          meta: { title: '报表导出', requiresAuth: true, roles: REPORT_ROLES }
        },
        {
          path: 'sources',
          redirect: '/archive-settings?tab=sources'
        },
        {
          path: 'push-records',
          name: 'PushRecordList',
          component: () => import('@/views/push/PushRecordList.vue'),
          meta: { title: '推送记录', requiresAuth: true, roles: MANAGER_ROLES }
        },
        {
          path: 'fonds',
          redirect: '/archive-settings?tab=fonds'
        },
        // 系统管理 - 仅管理员可访问
        {
          path: 'system/setup',
          name: 'InitialSetup',
          component: () => import('@/views/system/InitialSetup.vue'),
          meta: { title: '基础设置', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'system/info',
          name: 'SystemInfo',
          component: () => import('@/views/system/SystemInfo.vue'),
          meta: { title: '系统信息', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'system/permissions',
          name: 'PermissionCenter',
          component: () => import('@/views/system/PermissionCenter.vue'),
          meta: { title: '权限管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'system/users',
          redirect: '/system/permissions?tab=users'
        },
        {
          path: 'system/roles',
          redirect: '/system/permissions?tab=roles'
        },
        {
          path: 'system/logs',
          name: 'OperationLog',
          component: () => import('@/views/system/OperationLog.vue'),
          meta: { title: '操作日志', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'system/config',
          name: 'SystemConfig',
          component: () => import('@/views/system/SystemConfig.vue'),
          meta: { title: '系统配置', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'system/recovery',
          name: 'BackupRecoveryCenter',
          component: () => import('@/views/system/BackupRecoveryCenter.vue'),
          meta: { title: '备份恢复', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'system/backup',
          redirect: '/system/recovery?tab=backup'
        },
        {
          path: 'system/restore',
          redirect: '/system/recovery?tab=restore'
        },
        // 个人设置 - 所有登录用户可访问
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/user/Profile.vue'),
          meta: { title: '个人设置', requiresAuth: true }
        },
        {
          path: 'help',
          name: 'HelpCenter',
          component: () => import('@/views/help/HelpCenter.vue'),
          meta: { title: '帮助中心', requiresAuth: true }
        },
        // 403 无权限页面
        {
          path: 'forbidden',
          name: 'Forbidden',
          component: () => import('@/views/error/Forbidden.vue'),
          meta: { title: '无权限', requiresAuth: true }
        }
      ]
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', guest: true }
    },
    {
      path: '/borrow/access/:token',
      name: 'BorrowAccess',
      component: () => import('@/views/borrow/BorrowAccess.vue'),
      meta: { title: '档案借阅', public: true }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      redirect: '/'
    }
  ]
})

// 获取用户角色
const getUserRole = () => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    if (userInfo) {
      const user = JSON.parse(userInfo)
      return normalizeUserType(user.userType)
    }
  } catch (e) {
    console.error('解析用户信息失败', e)
  }
  return null
}

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 档案管理系统` : '档案管理系统'
  
  // 使用安全存储获取Token
  const token = secureStorage.getAccessToken()
  
  // 公开页面（无需登录）
  if (to.matched.some(record => record.meta.public)) {
    next()
    return
  }

  // 需要认证的页面
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      ElMessage.warning('请先登录')
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }
    
    // 检查角色权限
    const userRole = getUserRole()
    const requiredRoles = to.meta.roles
    
    if (requiredRoles && !hasPermission(requiredRoles, userRole)) {
      ElMessage.error('您没有权限访问该页面')
      next({ name: 'Forbidden' })
      return
    }
    
    next()
  } 
  // 游客页面（已登录则跳转首页）
  else if (to.matched.some(record => record.meta.guest)) {
    if (token) {
      next('/')
    } else {
      next()
    }
  } 
  else {
    next()
  }
})

export default router
