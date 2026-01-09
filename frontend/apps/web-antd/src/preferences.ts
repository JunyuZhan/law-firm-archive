import { defineOverridesPreferences } from '@vben/preferences';

/**
 * @description 项目配置文件
 * 只需要覆盖项目中的一部分配置，不需要的配置不用覆盖，会自动使用默认配置
 * !!! 更改配置后请清空缓存，否则可能不生效
 */
export const overridesPreferences = defineOverridesPreferences({
  // overrides
  app: {
    name: import.meta.env.VITE_APP_TITLE || '智慧律所管理系统',
    // 使用后端权限模式，菜单从后端动态获取
    accessMode: 'backend',
    // 默认首页路径（工作台）
    defaultHomePath: '/dashboard/workspace',
    // 默认头像（用户没有头像时使用）
    defaultAvatar: '/default-avatar.svg',
  },
  logo: {
    enable: true,
    fit: 'contain',
    source: '/logo.png',
  },
  copyright: {
    companyName: '智慧律所管理系统',
    companySiteLink: '',
    date: new Date().getFullYear().toString(),
    enable: true,
    icp: '',
    icpLink: '',
    settingShow: true,
  },
  theme: {
    mode: 'light', // 默认浅色主题
  },
});
