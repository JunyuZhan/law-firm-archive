import type { RouteRecordRaw } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';

import { $t } from '#/locales';

const BasicLayout = () => import('#/layouts/basic.vue');
const AuthPageLayout = () => import('#/layouts/auth.vue');
/** 全局404页面 */
const fallbackNotFoundRoute: RouteRecordRaw = {
  component: () => import('#/views/_core/fallback/not-found.vue'),
  meta: {
    hideInBreadcrumb: true,
    hideInMenu: true,
    hideInTab: true,
    title: '404',
  },
  name: 'FallbackNotFound',
  path: '/:path(.*)*',
};

/** 基本路由，这些路由是必须存在的 */
const coreRoutes: RouteRecordRaw[] = [
  /**
   * 根路由
   * 使用基础布局，作为所有页面的父级容器，子级就不必配置BasicLayout。
   * 此路由必须存在，且不应修改
   */
  {
    component: BasicLayout,
    meta: {
      hideInBreadcrumb: true,
      title: 'Root',
    },
    name: 'Root',
    path: '/',
    redirect: preferences.app.defaultHomePath,
    children: [
      /**
       * Dashboard 路由（重定向到工作台）
       */
      {
        path: '/dashboard',
        name: 'Dashboard',
        redirect: '/dashboard/workspace',
        meta: {
          title: '工作台',
          hideInMenu: true,
          hideInBreadcrumb: true,
          hideInTab: true,
        },
      },
      /**
       * 个人中心页面
       */
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('#/views/_core/profile/index.vue'),
        meta: {
          title: '个人中心',
          hideInMenu: true,
          hideInBreadcrumb: false,
          hideInTab: false,
        },
      },
      /**
       * 项目详情页路由（带参数）
       * 需要在coreRoutes中配置，因为它是动态路由，不通过菜单API生成
       */
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
    ],
  },
  /**
   * Office 文档预览页面（独立页面，不使用布局）
   */
  {
    path: '/office-preview',
    name: 'OfficePreview',
    component: () => import('#/views/office-preview/index.vue'),
    meta: {
      title: '文档预览',
      hideInMenu: true,
      hideInTab: true,
    },
  },
  {
    component: AuthPageLayout,
    meta: {
      hideInTab: true,
      title: 'Authentication',
    },
    name: 'Authentication',
    path: '/auth',
    redirect: LOGIN_PATH,
    children: [
      {
        name: 'Login',
        path: 'login',
        component: () => import('#/views/_core/authentication/login.vue'),
        meta: {
          title: $t('page.auth.login'),
        },
      },
      {
        name: 'CodeLogin',
        path: 'code-login',
        component: () => import('#/views/_core/authentication/code-login.vue'),
        meta: {
          title: $t('page.auth.codeLogin'),
        },
      },
      {
        name: 'QrCodeLogin',
        path: 'qrcode-login',
        component: () =>
          import('#/views/_core/authentication/qrcode-login.vue'),
        meta: {
          title: $t('page.auth.qrcodeLogin'),
        },
      },
      {
        name: 'ForgetPassword',
        path: 'forget-password',
        component: () =>
          import('#/views/_core/authentication/forget-password.vue'),
        meta: {
          title: $t('page.auth.forgetPassword'),
        },
      },
      {
        name: 'Register',
        path: 'register',
        component: () => import('#/views/_core/authentication/register.vue'),
        meta: {
          title: $t('page.auth.register'),
        },
      },
    ],
  },
];

export { coreRoutes, fallbackNotFoundRoute };
