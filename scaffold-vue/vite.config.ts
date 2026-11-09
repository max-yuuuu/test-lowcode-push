import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // @yuanda-lowcode-ai/scaffold-ui 发布的是 .vue/.ts 源码：交给 plugin-vue 编译，别让 esbuild 预打包（预打包只认 JS，遇到 .vue 直接失败）
  optimizeDeps: {
    exclude: ['@yuanda-lowcode-ai/scaffold-ui'],
  },
  server: {
    port: 5173,
    // 预检到 spawn 之间隔着分钟级构建，被占时要当场报错而不是漂到 5174（与平台侧同口径）
    strictPort: true,
    open: true,
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
})
