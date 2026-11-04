import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
// En GitHub Pages el sitio se publica bajo /maintena/. En Docker y en
// desarrollo local se mantiene en la raíz.
const base = process.env.VITE_BASE || '/'

export default defineConfig({
  base,
  plugins: [react()],
  build: {
    // Evita colisiones entre la carpeta de build y rutas SPA como /assets.
    assetsDir: 'static',
  },
})
