import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'
import fs from 'node:fs'
import { fileURLToPath } from 'node:url'

function readRepoVersion(repoRoot) {
  try {
    return fs.readFileSync(path.join(repoRoot, 'VERSION'), 'utf8').trim().replace(/\r/g, '')
  } catch {
    return ''
  }
}

export default defineConfig(({ mode }) => {
  const rootDir = fileURLToPath(new URL('.', import.meta.url))
  const repoRoot = path.resolve(rootDir, '..')
  const env = loadEnv(mode, rootDir, '')
  const fromEnv = (env.VITE_APP_PRODUCT_VERSION || '').trim()
  const fromFile = readRepoVersion(repoRoot)
  let fromPackage = ''
  try {
    fromPackage = JSON.parse(fs.readFileSync(path.join(rootDir, 'package.json'), 'utf8')).version || ''
  } catch {
    /* ignore */
  }
  const productVersion = fromEnv || fromFile || fromPackage || 'dev'

  return {
    define: {
      __APP_PRODUCT_VERSION__: JSON.stringify(productVersion)
    },
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/auto-imports.d.ts'
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts'
      })
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler'
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return
            }
            if (id.includes('echarts')) {
              return 'echarts'
            }
            if (id.includes('element-plus')) {
              return 'element-plus'
            }
            if (id.includes('vue') || id.includes('pinia') || id.includes('vue-router')) {
              return 'vue-vendor'
            }
            return 'vendor'
          }
        }
      }
    },
    server: {
      port: 3001,
      proxy: {
        '/api': {
          // 使用环境变量配置后端地址，默认为本地开发地址
          // 生产环境通过 .env.production 配置
          target: env.VITE_API_PROXY_TARGET || 'http://localhost:8090',
          changeOrigin: true
        }
      }
    }
  }
})
