import type { RouteRecordRaw } from 'vue-router';

/**
 * 财务管理路由配置
 * 注意：大部分页面通过后端菜单配置动态生成路由，这里只配置特殊需要的路由
 */
const financeRoutes: RouteRecordRaw[] = [
  {
    path: '/finance/payment-amendment',
    name: 'PaymentAmendment',
    component: () => import('#/views/finance/payment-amendment/index.vue'),
    meta: {
      title: '收款变更审批',
      icon: 'ant-design:audit-outlined',
      authority: ['fee:amendment:list'],
    },
  },
  {
    path: '/finance/commission/rules',
    name: 'CommissionRules',
    component: () => import('#/views/finance/commission/rules/index.vue'),
    meta: {
      title: '提成规则管理',
      icon: 'ant-design:setting-outlined',
      authority: ['finance:commission:rule:list'],
    },
  },
];

export default financeRoutes;
