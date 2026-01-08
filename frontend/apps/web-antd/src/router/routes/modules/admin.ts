import type { RouteRecordRaw } from 'vue-router';

/**
 * 行政管理路由配置
 */
const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/contract',
    name: 'AdminContract',
    component: () => import('#/views/admin/contract/index.vue'),
    meta: {
      title: '合同查询',
      icon: 'ant-design:file-search-outlined',
      authority: ['admin:contract:list'],
    },
  },
];

export default adminRoutes;
