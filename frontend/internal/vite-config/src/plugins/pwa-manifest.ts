import type { Plugin } from 'vite';

import { writeFileSync, mkdirSync, existsSync, copyFileSync } from 'node:fs';
import { join, dirname } from 'node:path';

export interface PwaManifestOptions {
  /** 应用名称（从环境变量 VITE_APP_TITLE 获取） */
  name: string;
  /** 应用简称（可选，默认自动生成） */
  shortName?: string;
  /** 应用描述 */
  description?: string;
  /** 主题颜色 */
  themeColor?: string;
  /** 背景颜色 */
  backgroundColor?: string;
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
  const generatedShortName = shortName || (
    name.length > 12
      ? (name.includes('律所') || name.includes('律师') ? '律所系统' : name.substring(0, 12))
      : name
  );

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
    apply: 'build',
    
    writeBundle(outputOptions) {
      const outDir = outputOptions.dir || 'dist';
      const manifestPath = join(outDir, 'manifest.webmanifest');
      
      // 确保输出目录存在
      if (!existsSync(outDir)) {
        mkdirSync(outDir, { recursive: true });
      }
      
      // 写入 manifest 文件
      writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), 'utf-8');
      console.log(`[PWA] Generated manifest.webmanifest with name: "${name}"`);
    },
  };
}
