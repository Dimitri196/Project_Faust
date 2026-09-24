import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// vite.config.ts
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173, // Změněno z 6173 na 5173
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})