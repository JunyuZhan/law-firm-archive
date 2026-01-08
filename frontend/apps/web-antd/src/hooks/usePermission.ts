import { computed } from 'vue';
import { useAccessStore } from '@vben/stores';

/**
 * 权限检查 Hook
 */
export function usePermission() {
  const accessStore = useAccessStore();

  /**
   * 检查是否有指定权限
   * @param permission 权限码
   * @returns 是否有权限
   */
  const hasPermission = (permission: string): boolean => {
    if (!permission) return true;
    return accessStore.accessCodes.includes(permission);
  };

  /**
   * 检查是否有任意一个权限
   * @param permissions 权限码数组
   * @returns 是否有权限
   */
  const hasAnyPermission = (permissions: string[]): boolean => {
    if (!permissions || permissions.length === 0) return true;
    return permissions.some(permission => accessStore.accessCodes.includes(permission));
  };

  /**
   * 检查是否有所有权限
   * @param permissions 权限码数组
   * @returns 是否有权限
   */
  const hasAllPermissions = (permissions: string[]): boolean => {
    if (!permissions || permissions.length === 0) return true;
    return permissions.every(permission => accessStore.accessCodes.includes(permission));
  };

  /**
   * 检查是否有指定角色
   * @param role 角色码
   * @returns 是否有角色
   */
  const hasRole = (role: string): boolean => {
    // 这里需要从用户信息中获取角色，暂时返回true
    // 可以根据实际需求从userStore中获取角色信息
    return true;
  };

  /**
   * 当前用户的权限码列表
   */
  const permissions = computed(() => accessStore.accessCodes);

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    permissions,
  };
}