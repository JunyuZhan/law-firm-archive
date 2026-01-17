import type { RouteRecordRaw } from 'vue-router';

const aiRoutes: RouteRecordRaw[] = [
  {
    path: '/personal/ai-usage',
    name: 'MyAiUsage',
    component: () => import('#/views/personal/ai-usage/index.vue'),
    meta: {
      title: '我的AI使用',
      icon: 'ant-design:robot-outlined',
      authority: ['ai:usage:view:my'],
    },
  },
  {
    path: '/finance/ai-billing',
    name: 'FinanceAiBilling',
    component: () => import('#/views/finance/ai-billing/index.vue'),
    meta: {
      title: 'AI费用账单',
      icon: 'ant-design:fund-outlined',
      authority: ['ai:billing:view'],
    },
  },
  // AI计费配置已整合到系统配置页面的Tab中，不再需要单独路由
];

export default aiRoutes;
