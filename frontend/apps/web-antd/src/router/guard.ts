import type { Router } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';
import { useAccessStore, useUserStore } from '@vben/stores';
import { startProgress, stopProgress } from '@vben/utils';

import { accessRoutes, coreRouteNames } from '#/router/routes';
import { useAuthStore } from '#/store';

import { generateAccess } from './access';

/**
 * 通用守卫配置
 * @param router
 */
function setupCommonGuard(router: Router) {
  // 记录已经加载的页面
  const loadedPaths = new Set<string>();

  router.beforeEach((to) => {
    to.meta.loaded = loadedPaths.has(to.path);

    // 页面加载进度条
    if (!to.meta.loaded && preferences.transition.progress) {
      startProgress();
    }
    return true;
  });

  router.afterEach((to) => {
    // 记录页面是否加载,如果已经加载，后续的页面切换动画等效果不在重复执行

    loadedPaths.add(to.path);

    // 关闭页面加载进度条
    if (preferences.transition.progress) {
      stopProgress();
    }
  });
}

/**
 * 权限访问守卫配置
 * @param router
 */
function setupAccessGuard(router: Router) {
  router.beforeEach(async (to, from) => {
    const accessStore = useAccessStore();
    const userStore = useUserStore();
    const authStore = useAuthStore();

    // 基本路由，这些路由不需要进入权限拦截
    // 但如果是需要布局的页面（如项目详情），仍需要确保菜单已加载
    if (coreRouteNames.includes(to.name as string)) {
      // 登录页特殊处理：已登录则跳转首页，未登录则直接放行
      if (to.path === LOGIN_PATH) {
        if (accessStore.accessToken) {
          return decodeURIComponent(
            (to.query?.redirect as string) ||
              userStore.userInfo?.homePath ||
              preferences.app.defaultHomePath,
          );
        }
        // 未登录访问登录页，直接放行，不需要加载菜单
        return true;
      }
      // 其他核心路由：如果菜单已加载则放行，否则继续执行后续的菜单生成逻辑
      // 这样可以确保项目详情等页面刷新时侧边栏能正常显示
      if (accessStore.isAccessChecked) {
        return true;
      }
      // 继续执行，不返回，让后续逻辑生成菜单
    }

    // accessToken 检查
    if (!accessStore.accessToken) {
      // 明确声明忽略权限访问权限，则可以访问
      if (to.meta.ignoreAccess) {
        return true;
      }

      // 没有访问权限，跳转登录页面
      if (to.fullPath !== LOGIN_PATH) {
        return {
          path: LOGIN_PATH,
          // 如不需要，直接删除 query
          query:
            to.fullPath === preferences.app.defaultHomePath
              ? {}
              : { redirect: encodeURIComponent(to.fullPath) },
          // 携带当前跳转的页面，登录后重新跳转该页面
          replace: true,
        };
      }
      return to;
    }

    // 是否已经生成过动态路由
    if (accessStore.isAccessChecked) {
      return true;
    }

    // 生成路由表（添加 try-catch 防止异步错误导致路由守卫崩溃）
    try {
      // 当前登录用户拥有的角色标识列表
      const userInfo = userStore.userInfo || (await authStore.fetchUserInfo());
      const userRoles = userInfo?.roles ?? [];

      // 生成菜单和路由
      const { accessibleMenus, accessibleRoutes } = await generateAccess({
        roles: userRoles,
        router,
        // 则会在菜单中显示，但是访问会被重定向到403
        routes: accessRoutes,
      });

      // 保存菜单信息和路由信息
      accessStore.setAccessMenus(accessibleMenus);
      accessStore.setAccessRoutes(accessibleRoutes);
      accessStore.setIsAccessChecked(true);

      const redirectPath = (from.query.redirect ??
        (to.path === preferences.app.defaultHomePath
          ? userInfo?.homePath || preferences.app.defaultHomePath
          : to.fullPath)) as string;

      return {
        ...router.resolve(decodeURIComponent(redirectPath)),
        replace: true,
      };
    } catch (error) {
      // 获取用户信息或生成路由失败，可能是 token 失效
      console.error('路由守卫错误:', error);
      // 清除 token 并重定向到登录页
      accessStore.setAccessToken(null);
      accessStore.setIsAccessChecked(false);
      return {
        path: LOGIN_PATH,
        query: { redirect: encodeURIComponent(to.fullPath) },
        replace: true,
      };
    }
  });
}

/**
 * 项目守卫配置
 * @param router
 */
function createRouterGuard(router: Router) {
  /** 通用 */
  setupCommonGuard(router);
  /** 权限访问 */
  setupAccessGuard(router);
}

export { createRouterGuard };
