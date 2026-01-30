import type { Options as PwaPluginOptions } from 'vite-plugin-pwa';

import type { ImportmapPluginOptions } from './typing';

const isDevelopment = process.env.NODE_ENV === 'development';

const getDefaultPwaOptions = (name: string): Partial<PwaPluginOptions> => {
  let shortName = name;
  if (name.length > 12) {
    shortName = name.includes('律所') ? '律所系统' : name.slice(0, 12);
  }

  return {
    manifest: {
      // 使用从环境变量 VITE_APP_TITLE 加载的应用名称
      name: `${name}${isDevelopment ? ' dev' : ''}`,
      // short_name 建议使用简短版本（PWA 标准建议不超过 12 个字符）
      // 可以根据实际应用名称自定义，例如：
      // - "智慧律所管理系统" -> "律所系统"
      // - "Law Firm Management" -> "Law Firm"
      short_name: shortName,
      description:
        '专业的律所业务管理平台，支持案件管理、客户管理、文档管理等功能',
      display: 'standalone',
      theme_color: '#1890ff',
      background_color: '#ffffff',
      start_url: '/',
      icons: [
        {
          src: '/pwa-192x192.png',
          sizes: '192x192',
          type: 'image/png',
          purpose: 'any',
        },
        {
          src: '/pwa-512x512.png',
          sizes: '512x512',
          type: 'image/png',
          purpose: 'any',
        },
        // 可选：Android 自适应图标（maskable）
        // 如果提供了 maskable 版本，取消下面的注释并添加文件
        // {
        //   src: '/pwa-maskable-512x512.png',
        //   sizes: '512x512',
        //   type: 'image/png',
        //   purpose: 'maskable',
        // },
      ],
    },
    // 注册方式：'script' 使用 script 标签注入，更稳定
    injectRegister: 'script',
    // 更新策略
    registerType: 'autoUpdate',
    workbox: {
      // 强制 Workbox 忽略某些不兼容的元数据（解决 Vite 7 兼容性问题）
      skipWaiting: true,
      clientsClaim: true,
      // 缓存策略
      globPatterns: ['**/*.{js,css,html,woff2,png,svg,jpg,jpeg,gif,webp,ico}'],
      // 预缓存版本控制
      navigateFallback: '/index.html',
      // 最大缓存大小（建议根据实际调整）
      maximumFileSizeToCacheInBytes: 5 * 1024 * 1024, // 5MB
      // 运行时缓存策略
      runtimeCaching: [
        {
          // API 请求缓存 - 网络优先
          urlPattern: /^https?:\/\/.*\/api\/.*/i,
          handler: 'NetworkFirst',
          options: {
            cacheName: 'api-cache',
            expiration: {
              maxEntries: 100,
              maxAgeSeconds: 60 * 60 * 24, // 24 小时
            },
            networkTimeoutSeconds: 10,
          },
        },
        {
          // 静态资源缓存 - 缓存优先
          urlPattern: /\.(?:png|jpg|jpeg|svg|gif|webp|woff2?)$/i,
          handler: 'CacheFirst',
          options: {
            cacheName: 'static-assets',
            expiration: {
              maxEntries: 200,
              maxAgeSeconds: 60 * 60 * 24 * 30, // 30 天
            },
          },
        },
      ],
    },
    // 开发环境禁用
    devOptions: {
      enabled: false,
    },
  };
};

/**
 * importmap CDN 暂时不开启，因为有些包不支持，且网络不稳定
 */
const defaultImportmapOptions: ImportmapPluginOptions = {
  // 通过 Importmap CDN 方式引入,
  // 目前只有esm.sh源兼容性好一点，jspm.io对于 esm 入口要求高
  defaultProvider: 'esm.sh',
  importmap: [
    { name: 'vue' },
    { name: 'pinia' },
    { name: 'vue-router' },
    // { name: 'vue-i18n' },
    { name: 'dayjs' },
    { name: 'vue-demi' },
  ],
};

export { defaultImportmapOptions, getDefaultPwaOptions };
