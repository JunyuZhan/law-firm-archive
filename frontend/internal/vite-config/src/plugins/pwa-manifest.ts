import type { Plugin } from 'vite';

import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

export interface PwaManifestOptions {
  /** 背景颜色 */
  backgroundColor?: string;
  /** 应用描述 */
  description?: string;
  /** 应用名称（从环境变量 VITE_APP_TITLE 获取） */
  name: string;
  /** 应用简称（可选，默认自动生成） */
  shortName?: string;
  /** 主题颜色 */
  themeColor?: string;
}

/**
 * 动态生成 PWA manifest.webmanifest 文件的 Vite 插件
 * 支持从环境变量加载应用名称
 */
export function vitePwaManifestPlugin(options: PwaManifestOptions): Plugin {
  const {
    name,
    shortName,
    description = '专业的律所业务管理平台，支持案件管理、客户管理、文档管理等功能',
    themeColor = '#1890ff',
    backgroundColor = '#ffffff',
  } = options;

  // 自动生成 short_name（PWA 标准建议不超过 12 个字符）
  let generatedShortName = shortName;
  if (!generatedShortName) {
    if (name.length > 12) {
      generatedShortName =
        name.includes('律所') || name.includes('律师')
          ? '律所系统'
          : name.slice(0, 12);
    } else {
      generatedShortName = name;
    }
  }

  const manifest = {
    name,
    short_name: generatedShortName,
    description,
    display: 'standalone',
    theme_color: themeColor,
    background_color: backgroundColor,
    start_url: '/',
    scope: '/',
    orientation: 'portrait-primary',
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
    ],
    categories: ['business', 'productivity'],
    lang: 'zh-CN',
  };

  return {
    name: 'vite-plugin-pwa-manifest',
    apply: 'serve', // 同时支持开发和生产环境

    configureServer(server) {
      // 开发环境：提供 manifest.webmanifest 文件
      server.middlewares.use('/manifest.webmanifest', (req, res, _next) => {
        res.setHeader('Content-Type', 'application/manifest+json');
        res.end(JSON.stringify(manifest, null, 2));
      });
    },

    writeBundle(outputOptions) {
      // 生产环境：写入 manifest 文件
      const outDir = outputOptions.dir || 'dist';
      const manifestPath = join(outDir, 'manifest.webmanifest');

      // 确保输出目录存在
      if (!existsSync(outDir)) {
        mkdirSync(outDir, { recursive: true });
      }

      // 写入 manifest 文件
      writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), 'utf8');
      console.log(`[PWA] Generated manifest.webmanifest with name: "${name}"`);
    },
  };
}
