import type { RouteRecordStringComponent } from '@vben/types';

import { requestClient } from '#/api/request';

export namespace MenuApi {
  /** 后端菜单DTO */
  export interface MenuDTO {
    id: number;
    parentId: number;
    name: string;
    path: string;
    component: string;
    redirect?: string;
    icon?: string;
    menuType: string;
    permission?: string;
    sortOrder: number;
    visible: boolean;
    status: string;
    isExternal?: boolean;
    isCache?: boolean;
    children?: MenuDTO[];
  }
}

/**
 * 图标映射：将 Ant Design 图标名转换为 Lucide 图标
 */
const iconMap: Record<string, string> = {
  DashboardOutlined: 'lucide:layout-dashboard',
  SettingOutlined: 'lucide:settings',
  TeamOutlined: 'lucide:users',
  FolderOutlined: 'lucide:folder',
  MoneyCollectOutlined: 'lucide:wallet',
  FileTextOutlined: 'lucide:file-text',
  AuditOutlined: 'lucide:shield-check',
  DatabaseOutlined: 'lucide:database',
  BankOutlined: 'lucide:building-2',
  IdcardOutlined: 'lucide:id-card',
  BookOutlined: 'lucide:book-open',
  UserOutlined: 'lucide:user',
  SafetyCertificateOutlined: 'lucide:shield',
  ApartmentOutlined: 'lucide:network',
  MenuOutlined: 'lucide:menu',
  ToolOutlined: 'lucide:wrench',
  FileSearchOutlined: 'lucide:file-search',
  ContactsOutlined: 'lucide:contact',
  SecurityScanOutlined: 'lucide:scan',
  FunnelPlotOutlined: 'lucide:filter',
  ProjectOutlined: 'lucide:briefcase',
  SolutionOutlined: 'lucide:clipboard-list',
  ClockCircleOutlined: 'lucide:clock',
  CheckSquareOutlined: 'lucide:check-square',
  FileProtectOutlined: 'lucide:file-check',
  PayCircleOutlined: 'lucide:credit-card',
  PercentageOutlined: 'lucide:percent',
  ProfileOutlined: 'lucide:receipt',
  BarChartOutlined: 'lucide:bar-chart-2',
  // 新增图标映射
  FileOutlined: 'lucide:file',
  FileAddOutlined: 'lucide:file-plus',
  SafetyOutlined: 'lucide:shield',
  FormOutlined: 'lucide:form-input',
  PlusCircleOutlined: 'lucide:plus-circle',
  FolderOpenOutlined: 'lucide:folder-open',
  ExportOutlined: 'lucide:external-link',
  DeleteOutlined: 'lucide:trash-2',
  CalendarOutlined: 'lucide:calendar',
  ScheduleOutlined: 'lucide:calendar-clock',
  ShopOutlined: 'lucide:store',
  ShoppingCartOutlined: 'lucide:shopping-cart',
  ReadOutlined: 'lucide:book',
  TrophyOutlined: 'lucide:trophy',
  RiseOutlined: 'lucide:trending-up',
  AimOutlined: 'lucide:target',
  ContainerOutlined: 'lucide:archive',
  // 财务模块新增图标
  WalletOutlined: 'lucide:wallet',
  DollarOutlined: 'lucide:dollar-sign',
  AccountBookOutlined: 'lucide:book-open-check',
  MailOutlined: 'lucide:mail',
  // 系统管理新增图标
  NotificationOutlined: 'lucide:bell',
};

/**
 * 转换图标名称
 */
function transformIcon(icon?: string): string | undefined {
  if (!icon) return undefined;
  // 如果已经是 lucide 或 ant-design 格式，直接返回
  if (icon.startsWith('lucide:') || icon.startsWith('ant-design:')) return icon;
  // 尝试从映射表获取
  return (
    iconMap[icon] || `lucide:${icon.replace('Outlined', '').toLowerCase()}`
  );
}

/**
 * 转换组件路径
 * 后端格式: "dashboard/index" 或 "system/user/index"
 * 前端格式: 保持原样，由 access.ts 中的 pageMap 处理
 */
function transformComponent(component?: string, menuType?: string): string {
  if (!component) return 'BasicLayout';

  // 目录类型使用布局组件
  if (menuType === 'DIRECTORY' || component === 'LAYOUT') {
    return 'BasicLayout';
  }

  // 菜单类型，返回组件路径（不带前缀和后缀）
  // 前端 pageMap 会自动匹配 ../views/**/*.vue
  return component;
}

/**
 * 生成路由名称（确保唯一）
 */
function generateRouteName(menu: MenuApi.MenuDTO): string {
  // 使用路径生成名称，如 /system/user -> SystemUser
  const pathParts = menu.path.split('/').filter(Boolean);
  return pathParts
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
}

/**
 * 将后端菜单转换为前端路由格式
 */
function transformMenuToRoute(
  menu: MenuApi.MenuDTO,
): RouteRecordStringComponent {
  const route: RouteRecordStringComponent = {
    name: generateRouteName(menu),
    path: menu.path,
    component: transformComponent(menu.component, menu.menuType),
    meta: {
      title: menu.name,
      icon: transformIcon(menu.icon),
      hideInMenu: !menu.visible,
      keepAlive: menu.isCache ?? true,
      order: menu.sortOrder,
      // 权限码，用于按钮级权限控制
      authority: menu.permission ? [menu.permission] : undefined,
    },
  };

  if (menu.redirect) {
    route.redirect = menu.redirect;
  }

  // 外链处理
  if (menu.isExternal && menu.path.startsWith('http')) {
    route.meta = {
      ...route.meta,
      title: route.meta?.title || menu.name,
      link: menu.path,
    };
  }

  if (menu.children && menu.children.length > 0) {
    route.children = menu.children.map(transformMenuToRoute);
  }

  return route;
}

/**
 * 获取用户所有菜单
 */
export async function getAllMenusApi() {
  const menus = await requestClient.get<MenuApi.MenuDTO[]>('/system/menu/user');
  // 转换为前端路由格式
  return menus.map(transformMenuToRoute);
}

/**
 * 获取菜单树（管理用）
 */
export async function getMenuTreeApi() {
  return requestClient.get<MenuApi.MenuDTO[]>('/system/menu/tree');
}
