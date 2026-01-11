import type { RouteRecordRaw } from 'vue-router';

/**
 * 项目管理路由配置
 */
const matterRoutes: RouteRecordRaw[] = [
  {
    path: '/matter/detail/:id',
    name: 'MatterDetail',
    component: () => import('#/views/matter/detail/index.vue'),
    meta: {
      title: '项目详情',
      hideInMenu: true,
      hideInBreadcrumb: false,
      hideInTab: false,
    },
  },
];

export default matterRoutes;
