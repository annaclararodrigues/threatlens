import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        // A API emite o refreshToken com Path=/auth, mas o navegador o chama em /api/auth
        // (mesmo ajuste do proxy_cookie_path no nginx.conf); sem isso o refresh falha e a sessão cai em 5 min
        cookiePathRewrite: { '/auth': '/api/auth' },
      },
    },
  },
})
