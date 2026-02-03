import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// vite.config.ts
export default defineConfig({
  plugins: [react()],
  server: {
    port: 6173, // Tady nastavujeme tvůj nový port
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})