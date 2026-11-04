import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    // Evita colisiones entre la carpeta de build y rutas SPA como /assets.
    assetsDir: 'static',
  },
})
