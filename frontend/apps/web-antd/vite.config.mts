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
        },
      },
    },
  };
});
