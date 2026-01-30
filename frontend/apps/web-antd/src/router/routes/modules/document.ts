import type { RouteRecordRaw } from 'vue-router';

/**
 * 文档与卷宗管理路由配置
 */
const documentRoutes: RouteRecordRaw[] = [
  {
    path: '/document/template',
    name: 'DocumentTemplate',
    component: () => import('#/views/document/template/index.vue'),
    meta: {
      title: '文书模板管理',
      icon: 'ant-design:file-text-outlined',
      authority: ['doc:template:list'],
    },
  },
  {
    path: '/document/dossier-template',
    name: 'DossierTemplate',
    component: () => import('#/views/document/dossier-template/index.vue'),
    meta: {
      title: '卷宗模板管理',
      icon: 'ant-design:file-protect-outlined',
      authority: ['doc:list'],
    },
  },
];

export default documentRoutes;
