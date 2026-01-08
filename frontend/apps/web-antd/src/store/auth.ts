import type { Recordable, UserInfo } from '@vben/types';

import { ref } from 'vue';
import { useRouter } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { preferences } from '@vben/preferences';
import { resetAllStores, useAccessStore, useUserStore } from '@vben/stores';

import { notification } from 'ant-design-vue';
import { defineStore } from 'pinia';

import { getAccessCodesApi, getUserInfoApi, loginApi, logoutApi } from '#/api';
import { getPendingApprovals, getMyInitiatedApprovals } from '#/api/workbench';
import { $t } from '#/locales';

export const useAuthStore = defineStore('auth', () => {
  const accessStore = useAccessStore();
  const userStore = useUserStore();
  const router = useRouter();

  const loginLoading = ref(false);

  /**
   * 异步处理登录操作
   * Asynchronously handle the login process
   * @param params 登录表单数据
   */
  async function authLogin(
    params: Recordable<any>,
    onSuccess?: () => Promise<void> | void,
  ) {
    // 异步处理用户登录操作并获取 accessToken
    let userInfo: null | UserInfo = null;
    try {
      loginLoading.value = true;
      
      // 调用登录接口
      const loginResult = await loginApi({
        username: params.username,
        password: params.password,
      });

      const { accessToken, refreshToken, permissions, roles, realName, userId, username } = loginResult;

      // 如果成功获取到 accessToken
      if (accessToken) {
        // 存储 accessToken
        accessStore.setAccessToken(accessToken);
        
        // 存储 refreshToken 到 localStorage
        if (refreshToken) {
          localStorage.setItem('refreshToken', refreshToken);
        }

        // 构建用户信息
        userInfo = {
          userId: String(userId),
          username,
          realName,
          roles: roles ? Array.from(roles) : [],
          avatar: '',
          homePath: '/dashboard',
        };

        // 存储用户信息
        userStore.setUserInfo(userInfo);
        
        // 存储权限码
        const accessCodes = permissions ? Array.from(permissions) : [];
        accessStore.setAccessCodes(accessCodes);

        if (accessStore.loginExpired) {
          accessStore.setLoginExpired(false);
        } else {
          onSuccess
            ? await onSuccess?.()
            : await router.push(
                userInfo.homePath || preferences.app.defaultHomePath,
              );
        }

        if (userInfo?.realName) {
          notification.success({
            description: `${$t('authentication.loginSuccessDesc')}:${userInfo?.realName}`,
            duration: 3,
            message: $t('authentication.loginSuccess'),
          });
          
          // 登录成功后显示审批提醒
          showApprovalNotifications();
        }
      }
    } finally {
      loginLoading.value = false;
    }

    return {
      userInfo,
    };
  }

  async function logout(redirect: boolean = true) {
    try {
      await logoutApi();
    } catch {
      // 不做任何处理
    }
    
    // 清除 refreshToken
    localStorage.removeItem('refreshToken');
    
    resetAllStores();
    accessStore.setLoginExpired(false);

    // 回登录页带上当前路由地址
    await router.replace({
      path: LOGIN_PATH,
      query: redirect
        ? {
            redirect: encodeURIComponent(router.currentRoute.value.fullPath),
          }
        : {},
    });
  }

  async function fetchUserInfo() {
    let userInfo: null | UserInfo = null;
    userInfo = await getUserInfoApi();
    userStore.setUserInfo(userInfo);
    return userInfo;
  }

  function $reset() {
    loginLoading.value = false;
  }

  /**
   * 显示审批相关通知
   */
  async function showApprovalNotifications() {
    try {
      // 延迟1秒显示，避免和登录成功通知重叠
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      // 获取待审批列表
      const pendingList = await getPendingApprovals();
      const pendingCount = pendingList?.length || 0;
      
      if (pendingCount > 0) {
        notification.warning({
          message: '待审批提醒',
          description: `您有 ${pendingCount} 条待审批事项需要处理`,
          duration: 5,
          onClick: () => {
            router.push('/dashboard/approval');
          },
          style: { cursor: 'pointer' },
        });
      }
      
      // 获取我发起的审批中已处理的（最近24小时内）
      const initiatedList = await getMyInitiatedApprovals();
      const recentlyProcessed = (initiatedList || []).filter(item => {
        if (item.status === 'PENDING') return false;
        if (!item.approvedAt) return false;
        const approvedTime = new Date(item.approvedAt).getTime();
        const now = Date.now();
        const oneDayAgo = now - 24 * 60 * 60 * 1000;
        return approvedTime > oneDayAgo;
      });
      
      if (recentlyProcessed.length > 0) {
        // 延迟显示，避免通知堆叠
        await new Promise(resolve => setTimeout(resolve, 500));
        
        const approvedCount = recentlyProcessed.filter(item => item.status === 'APPROVED').length;
        const rejectedCount = recentlyProcessed.filter(item => item.status === 'REJECTED').length;
        
        let description = '您发起的审批有新进展：';
        if (approvedCount > 0) description += `${approvedCount} 项已通过`;
        if (approvedCount > 0 && rejectedCount > 0) description += '，';
        if (rejectedCount > 0) description += `${rejectedCount} 项已拒绝`;
        
        notification.info({
          message: '审批进度通知',
          description,
          duration: 5,
          onClick: () => {
            router.push('/dashboard/approval');
          },
          style: { cursor: 'pointer' },
        });
      }
    } catch (error) {
      // 静默处理错误，不影响登录流程
      console.warn('获取审批通知失败:', error);
    }
  }

  return {
    $reset,
    authLogin,
    fetchUserInfo,
    loginLoading,
    logout,
    showApprovalNotifications,
  };
});
