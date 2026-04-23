import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') }
  },
  server: {
    port: 3000,
    proxy: {
      '/api/hospital': { target: 'http://localhost:8080', rewrite: (p) => p.replace('/api/hospital', '') },
      '/api/telehealth': { target: 'http://localhost:8081', rewrite: (p) => p.replace('/api/telehealth', '') },
      '/api/billing': { target: 'http://localhost:8082', rewrite: (p) => p.replace('/api/billing', '') },
      '/api/insurance': { target: 'http://localhost:8083', rewrite: (p) => p.replace('/api/insurance', '') },
      '/api/notifications': { target: 'http://localhost:8084', rewrite: (p) => p.replace('/api/notifications', '') },
      '/api/eureka': { target: 'http://localhost:8761', rewrite: (p) => p.replace('/api/eureka', '') }
    }
  }
})
