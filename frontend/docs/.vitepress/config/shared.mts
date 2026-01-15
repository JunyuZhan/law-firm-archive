import type { PwaOptions } from '@vite-pwa/vitepress';
import type { HeadConfig } from 'vitepress';

import { resolve } from 'node:path';

import {
  viteArchiverPlugin,
  viteVxeTableImportsPlugin,
} from '@vben/vite-config';

import {
  GitChangelog,
  GitChangelogMarkdownSection,
} from '@nolebase/vitepress-plugin-git-changelog/vite';
import tailwind from 'tailwindcss';
import { defineConfig, postcssIsolateStyles } from 'vitepress';
import {
  groupIconMdPlugin,
  groupIconVitePlugin,
} from 'vitepress-plugin-group-icons';
import type { Plugin } from 'vite';

import { demoPreviewPlugin } from './plugins/demo-preview';
import { search as zhSearch } from './zh.mts';

// 检测是否是开发模式
const isDev = process.env.NODE_ENV !== 'production';

export const shared = defineConfig({
  appearance: 'dark',
  // 开发模式下使用根路径，生产模式下使用 /docs/
  base: isDev ? '/' : '/docs/',
  head: head(),
  // 忽略文档中的配置示例链接
  ignoreDeadLinks: [/localhost/],
  markdown: {
    preConfig(md) {
      md.use(demoPreviewPlugin);
      md.use(groupIconMdPlugin);
    },
  },
  pwa: pwa(),
  srcDir: 'src',
  themeConfig: {
    i18nRouting: true,
    logo: '/logos/law-firm-logo.svg',
    search: {
      options: {
        locales: {
          ...zhSearch,
        },
      },
      provider: 'local',
    },
    siteTitle: '律所管理系统',
    socialLinks: [
      { icon: 'github', link: 'https://github.com/junyuzhan/law-firm' },
    ],
  },
  title: '律所管理系统文档',
  vite: {
    build: {
      chunkSizeWarningLimit: Infinity,
      minify: 'terser',
    },
    css: {
      postcss: {
        plugins: [
          tailwind(),
          postcssIsolateStyles({ includeFiles: [/vp-doc\.css/] }),
        ],
      },
      preprocessorOptions: {
        scss: {
          api: 'modern',
        },
      },
    },
    json: {
      stringify: true,
    },
    plugins: ([
      {
        name: 'docs-base-rewrite',
        enforce: 'pre',
        apply: 'serve',
        configureServer(server) {
          // 在所有中间件之前执行 URL 重写
          const rewriteMiddleware = (req: any, res: any, next: any) => {
            const url = req.url;
            if (!url) {
              next();
              return;
            }
            
            // 处理 /docs 重定向
            if (url === '/docs') {
              res.statusCode = 302;
              res.setHeader('Location', '/docs/');
              res.end();
              return;
            }
            
            // 从 /docs 开头的 URL 中去掉 /docs 前缀
            // 这样 Vite 才能正确处理这些请求
            if (url.startsWith('/docs/')) {
              const newUrl = url.slice('/docs'.length) || '/';
              // 只重写 Vite 内部路径，不重写页面路径
              if (
                newUrl.startsWith('/@') ||
                newUrl.startsWith('/__') ||
                newUrl.startsWith('/node_modules/') ||
                newUrl.startsWith('/src/') ||
                newUrl.includes('.vitepress')
              ) {
                console.log(`[docs-rewrite] ${url} -> ${newUrl}`);
                req.url = newUrl;
              }
            }
            next();
          };
          
          // 将中间件添加到栈的最前面
          server.middlewares.stack.unshift({
            route: '',
            handle: rewriteMiddleware,
          });
        },
      } satisfies Plugin,
      GitChangelog({
        mapAuthors: [
          {
            mapByNameAliases: ['Vben'],
            name: 'vben',
            username: 'anncwb',
          },
          {
            name: 'vince',
            username: 'vince292007',
          },
          {
            name: 'Li Kui',
            username: 'likui628',
          },
        ],
        repoURL: () => 'https://github.com/vbenjs/vue-vben-admin',
      }),
      GitChangelogMarkdownSection(),
      viteArchiverPlugin({ outputDir: '.vitepress' }),
      groupIconVitePlugin(),
      await viteVxeTableImportsPlugin(),
    ]) as any,
    server: {
      fs: {
        allow: ['../..'],
      },
      host: true,
      port: 6173,
    },

    ssr: {
      external: ['@vue/repl'],
    },
  },
});

function head(): HeadConfig[] {
  return [
    ['meta', { content: 'Vbenjs Team', name: 'author' }],
    [
      'meta',
      {
        content: 'vben, vitejs, vite, shacdn-ui, vue',
        name: 'keywords',
      },
    ],
    ['link', { href: '/favicon.ico', rel: 'icon', type: 'image/svg+xml' }],
    [
      'meta',
      {
        content:
          'width=device-width,initial-scale=1,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no',
        name: 'viewport',
      },
    ],
    ['meta', { content: 'vben admin docs', name: 'keywords' }],
    ['link', { href: '/favicon.ico', rel: 'icon' }],
    // [
    //   'script',
    //   {
    //     src: 'https://cdn.tailwindcss.com',
    //   },
    // ],
  ];
}

function pwa(): PwaOptions {
  return {
    includeManifestIcons: false,
    manifest: {
      description: '律师事务所管理系统使用文档',
      icons: [
        {
          sizes: '192x192',
          src: '/docs/logos/law-firm-logo.svg',
          type: 'image/svg+xml',
        },
      ],
      id: '/docs/',
      name: '律所管理系统文档',
      short_name: 'law_firm_docs',
      theme_color: '#1890ff',
    },
    outDir: resolve(process.cwd(), '.vitepress/dist'),
    registerType: 'autoUpdate',
    workbox: {
      globPatterns: ['**/*.{css,js,html,svg,png,ico,txt,woff2}'],
      maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
    },
  };
}
