import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

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
          path: 'categories',
          name: 'CategoryManage',
          component: () => import('@/views/category/CategoryManage.vue'),
          meta: { title: '分类管理', requiresAuth: true }
        },
        {
          path: 'locations',
          name: 'LocationList',
          component: () => import('@/views/location/LocationList.vue'),
          meta: { title: '存放位置', requiresAuth: true }
        },
        {
          path: 'borrows',
          name: 'BorrowList',
          component: () => import('@/views/borrow/BorrowList.vue'),
          meta: { title: '借阅管理', requiresAuth: true }
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/statistics/Statistics.vue'),
          meta: { title: '统计分析', requiresAuth: true }
        },
        {
          path: 'sources',
          name: 'SourceList',
          component: () => import('@/views/source/SourceList.vue'),
          meta: { title: '来源管理', requiresAuth: true }
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

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 档案管理系统` : '档案管理系统'
  
  const token = localStorage.getItem('accessToken')
  
  // 需要认证的页面
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      ElMessage.warning('请先登录')
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    } else {
      next()
    }
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
