import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            // 不需要rewrite，后端路径已经是/api开头
            // rewrite: (path) => path.replace(/^\/api/, ''),
            // 后端API地址
            target: 'http://localhost:8080',
            ws: true,
          },
          // OnlyOffice 代理（本地开发时需要）
          '/onlyoffice': {
            changeOrigin: true,
            rewrite: (path: string) => path.replace(/^\/onlyoffice/, ''),
            target: 'http://127.0.0.1:8088',
            ws: true,
            // 注意：vite 代理是服务器端代理，不存在 CORS 问题
            // 但如果 OnlyOffice 内部资源使用绝对URL，可能需要配置 OnlyOffice 的环境变量
          },
        },
      },
    },
  };
});
