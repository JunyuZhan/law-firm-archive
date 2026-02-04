import type { RouteRecordRaw } from 'vue-router';

/**
 * 项目管理路由配置
 * 注意：此文件目前未被使用，实际路由定义在 core.ts 中
 * 保留此文件以备将来可能的模块化重构
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
      activePath: '/matter/list',
    },
  },
];

export default matterRoutes;
