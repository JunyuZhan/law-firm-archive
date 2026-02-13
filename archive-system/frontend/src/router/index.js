import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/views/layout/MainLayout.vue'),
      redirect: '/archives',
      children: [
        {
          path: 'archives',
          name: 'ArchiveList',
          component: () => import('@/views/archive/ArchiveList.vue'),
          meta: { title: '档案列表' }
        },
        {
          path: 'archives/:id',
          name: 'ArchiveDetail',
          component: () => import('@/views/archive/ArchiveDetail.vue'),
          meta: { title: '档案详情' }
        },
        {
          path: 'receive',
          name: 'ArchiveReceive',
          component: () => import('@/views/archive/ArchiveReceive.vue'),
          meta: { title: '档案接收' }
        },
        {
          path: 'locations',
          name: 'LocationList',
          component: () => import('@/views/location/LocationList.vue'),
          meta: { title: '存放位置' }
        },
        {
          path: 'borrows',
          name: 'BorrowList',
          component: () => import('@/views/borrow/BorrowList.vue'),
          meta: { title: '借阅管理' }
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/statistics/Statistics.vue'),
          meta: { title: '统计分析' }
        },
        {
          path: 'sources',
          name: 'SourceList',
          component: () => import('@/views/source/SourceList.vue'),
          meta: { title: '来源管理' }
        }
      ]
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录' }
    }
  ]
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 档案管理系统` : '档案管理系统'
  next()
})

export default router
