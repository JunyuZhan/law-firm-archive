import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { secureStorage } from '@/utils/security'

// 角色常量
const ROLES = {
  SYSTEM_ADMIN: 'SYSTEM_ADMIN',
  SECURITY_ADMIN: 'SECURITY_ADMIN',
  AUDIT_ADMIN: 'AUDIT_ADMIN',
  ARCHIVIST: 'ARCHIVIST',
  USER: 'USER'
}

// 管理员角色
const ADMIN_ROLES = [ROLES.SYSTEM_ADMIN, ROLES.SECURITY_ADMIN]

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
          meta: { title: '档案接收', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVIST] }
        },
        {
          path: 'search',
          name: 'ArchiveSearch',
          component: () => import('@/views/archive/ArchiveSearch.vue'),
          meta: { title: '档案检索', requiresAuth: true }
        },
        {
          path: 'categories',
          name: 'CategoryManage',
          component: () => import('@/views/category/CategoryManage.vue'),
          meta: { title: '分类管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVIST] }
        },
        {
          path: 'locations',
          name: 'LocationList',
          component: () => import('@/views/location/LocationList.vue'),
          meta: { title: '存放位置', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVIST] }
        },
        {
          path: 'borrows',
          name: 'BorrowList',
          component: () => import('@/views/borrow/BorrowList.vue'),
          meta: { title: '借阅管理', requiresAuth: true }
        },
        {
          path: 'appraisals',
          name: 'AppraisalList',
          component: () => import('@/views/appraisal/AppraisalList.vue'),
          meta: { title: '鉴定管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVIST] }
        },
        {
          path: 'destructions',
          name: 'DestructionList',
          component: () => import('@/views/destruction/DestructionList.vue'),
          meta: { title: '销毁管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVIST] }
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/statistics/Statistics.vue'),
          meta: { title: '统计分析', requiresAuth: true, roles: ADMIN_ROLES }
        },
        {
          path: 'reports',
          name: 'Reports',
          component: () => import('@/views/statistics/ReportPage.vue'),
          meta: { title: '报表导出', requiresAuth: true, roles: ADMIN_ROLES }
        },
        {
          path: 'sources',
          name: 'SourceList',
          component: () => import('@/views/source/SourceList.vue'),
          meta: { title: '来源管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'fonds',
          name: 'FondsManage',
          component: () => import('@/views/fonds/FondsManage.vue'),
          meta: { title: '全宗管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.ARCHIVIST] }
        },
        // 系统管理 - 仅管理员可访问
        {
          path: 'system/users',
          name: 'UserManage',
          component: () => import('@/views/system/UserManage.vue'),
          meta: { title: '用户管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.SECURITY_ADMIN] }
        },
        {
          path: 'system/roles',
          name: 'RoleManage',
          component: () => import('@/views/system/RoleManage.vue'),
          meta: { title: '角色管理', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        {
          path: 'system/logs',
          name: 'OperationLog',
          component: () => import('@/views/system/OperationLog.vue'),
          meta: { title: '操作日志', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN, ROLES.AUDIT_ADMIN] }
        },
        {
          path: 'system/config',
          name: 'SystemConfig',
          component: () => import('@/views/system/SystemConfig.vue'),
          meta: { title: '系统配置', requiresAuth: true, roles: [ROLES.SYSTEM_ADMIN] }
        },
        // 个人设置 - 所有登录用户可访问
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/user/Profile.vue'),
          meta: { title: '个人设置', requiresAuth: true }
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
      return user.userType
    }
  } catch (e) {
    console.error('解析用户信息失败', e)
  }
  return null
}

// 检查用户是否有权限访问
const hasPermission = (requiredRoles, userRole) => {
  if (!requiredRoles || requiredRoles.length === 0) {
    return true // 没有角色要求，允许访问
  }
  if (!userRole) {
    return false
  }
  return requiredRoles.includes(userRole)
}

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 档案管理系统` : '档案管理系统'
  
  // 使用安全存储获取Token
  const token = secureStorage.getAccessToken()
  
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
