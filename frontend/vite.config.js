import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig({
  plugins: [vue()],
  server: { port: 15707, strictPort: true, proxy: { '/api': 'http://127.0.0.1:18707', '/swagger-ui': 'http://127.0.0.1:18707', '/v3/api-docs': 'http://127.0.0.1:18707' } },
})
