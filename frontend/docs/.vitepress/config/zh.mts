import type { DefaultTheme } from 'vitepress';

import { defineConfig } from 'vitepress';

export const zh = defineConfig({
  description: '律所管理系统文档',
  lang: 'zh-Hans',
  themeConfig: {
    darkModeSwitchLabel: '主题',
    darkModeSwitchTitle: '切换到深色模式',
    docFooter: {
      next: '下一页',
      prev: '上一页',
    },
    footer: {
      copyright: `Copyright © ${new Date().getFullYear()} junyuzhan`,
      message: '内部文档',
    },
    langMenuLabel: '多语言',
    lastUpdated: {
      formatOptions: {
        dateStyle: 'short',
        timeStyle: 'medium',
      },
      text: '最后更新于',
    },
    lightModeSwitchTitle: '切换到浅色模式',
    nav: nav(),

    outline: {
      label: '页面导航',
    },
    returnToTopLabel: '回到顶部',

    sidebar: {
      '/guide/user/': { base: '/guide/user/', items: sidebarUser() },
      '/guide/ops/': sidebarProtected(),
      '/guide/api/': sidebarProtected(),
    },
    sidebarMenuLabel: '菜单',
  },
});

function sidebarUser(): DefaultTheme.SidebarItem[] {
  return [
    {
      collapsed: false,
      text: '入门',
      items: [
        { link: 'introduction', text: '系统简介' },
        { link: 'login', text: '登录系统' },
      ],
    },
    {
      collapsed: false,
      text: '客户与业务',
      items: [
        { link: 'crm', text: '案源与利冲' },
        { link: 'client-management', text: '客户管理' },
        { link: 'case-management', text: '项目管理' },
        { link: 'contract-management', text: '合同管理' },
      ],
    },
    {
      collapsed: false,
      text: '财务管理',
      items: [{ link: 'finance-management', text: '财务管理' }],
    },
    {
      collapsed: false,
      text: '文档与档案',
      items: [
        { link: 'document-management', text: '文档管理' },
        { link: 'evidence-management', text: '证据管理' },
        { link: 'archive-management', text: '档案管理' },
      ],
    },
    {
      collapsed: false,
      text: '日常办公',
      items: [
        { link: 'workbench', text: '工作台' },
        { link: 'admin-management', text: '行政管理' },
        { link: 'hr-management', text: '人力资源' },
        { link: 'knowledge-base', text: '知识库' },
      ],
    },
    {
      collapsed: false,
      text: '系统功能',
      items: [
        { link: 'data-handover', text: '数据交接' },
        { link: 'system-management', text: '系统管理' },
      ],
    },
    {
      collapsed: false,
      text: '帮助',
      items: [{ link: 'faq', text: '常见问题' }],
    },
  ];
}

// 受保护内容的合并侧边栏（运维手册 + API 文档）
function sidebarProtected(): DefaultTheme.SidebarItem[] {
  return [
    {
      collapsed: false,
      text: '运维手册',
      items: [
        { link: '/guide/ops/introduction', text: '概述' },
        { link: '/guide/ops/deployment', text: '部署指南' },
        { link: '/guide/ops/single-port-architecture', text: '单端口架构' },
        { link: '/guide/ops/single-port-migration', text: '单端口架构迁移' },
        { link: '/guide/ops/deployment-checklist', text: '部署检查清单' },
        { link: '/guide/ops/configuration', text: '配置说明' },
        { link: '/guide/ops/database-maintenance', text: '数据库维护' },
        { link: '/guide/ops/onlyoffice', text: 'OnlyOffice 配置' },
        { link: '/guide/ops/ocr', text: 'OCR 服务配置' },
        { link: '/guide/ops/shared-minio', text: '共享 MinIO 配置' },
        { link: '/guide/ops/grafana', text: 'Grafana 配置' },
        { link: '/guide/ops/frp', text: 'FRP 内网穿透' },
        { link: '/guide/ops/backup', text: '备份恢复' },
        { link: '/guide/ops/monitoring', text: '监控告警' },
        { link: '/guide/ops/security', text: '安全运维' },
        { link: '/guide/ops/troubleshooting', text: '故障排查' },
      ],
    },
    {
      collapsed: false,
      text: 'API 文档',
      items: [
        { link: '/guide/api/introduction', text: '接口概述' },
        { link: '/guide/api/development', text: '开发指南' },
        { link: '/guide/api/auth', text: '认证接口' },
        { link: '/guide/api/user', text: '用户接口' },
        { link: '/guide/api/case', text: '项目接口' },
        { link: '/guide/api/contract', text: '合同接口' },
        { link: '/guide/api/finance', text: '财务接口' },
        { link: '/guide/api/client', text: '客户接口' },
        { link: '/guide/api/conflict', text: '利冲接口' },
        { link: '/guide/api/lead', text: '案源接口' },
        { link: '/guide/api/document', text: '文档接口' },
        { link: '/guide/api/archive', text: '档案接口' },
        { link: '/guide/api/admin', text: '行政接口' },
        { link: '/guide/api/hr', text: '人力接口' },
        { link: '/guide/api/knowledge', text: '知识库接口' },
      ],
    },
  ];
}

function nav(): DefaultTheme.NavItem[] {
  return [
    {
      text: '用户手册',
      link: '/guide/user/introduction',
      activeMatch: '^/guide/user/',
    },
    {
      text: '运维手册',
      link: '/guide/ops/introduction',
      activeMatch: '^/guide/ops/',
    },
    {
      text: 'API 文档',
      link: '/guide/api/introduction',
      activeMatch: '^/guide/api/',
    },
    {
      text: '登录',
      link: '/login',
      activeMatch: '^/login',
    },
  ];
}

export const search: DefaultTheme.AlgoliaSearchOptions['locales'] = {
  root: {
    placeholder: '搜索文档',
    translations: {
      button: {
        buttonAriaLabel: '搜索文档',
        buttonText: '搜索文档',
      },
      modal: {
        errorScreen: {
          helpText: '你可能需要检查你的网络连接',
          titleText: '无法获取结果',
        },
        footer: {
          closeText: '关闭',
          navigateText: '切换',
          searchByText: '搜索提供者',
          selectText: '选择',
        },
        noResultsScreen: {
          noResultsText: '无法找到相关结果',
          reportMissingResultsLinkText: '点击反馈',
          reportMissingResultsText: '你认为该查询应该有结果？',
          suggestedQueryText: '你可以尝试查询',
        },
        searchBox: {
          cancelButtonAriaLabel: '取消',
          cancelButtonText: '取消',
          resetButtonAriaLabel: '清除查询条件',
          resetButtonTitle: '清除查询条件',
        },
        startScreen: {
          favoriteSearchesTitle: '收藏',
          noRecentSearchesText: '没有搜索历史',
          recentSearchesTitle: '搜索历史',
          removeFavoriteSearchButtonTitle: '从收藏中移除',
          removeRecentSearchButtonTitle: '从搜索历史中移除',
          saveRecentSearchButtonTitle: '保存至搜索历史',
        },
      },
    },
  },
};
