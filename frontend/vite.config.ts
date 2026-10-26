import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const port = Number(process.env.PORT || 5173)
const serverPort = Number(process.env.SERVER_PORT || port + 100)

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port,
    proxy: {
      '/api': `http://localhost:${serverPort}`
    }
  }
})
