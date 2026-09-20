import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // Must match FRONTEND_ORIGIN in the backend's .env - the API Gateway's
    // CORS filter (Phase 7) only allows this exact origin through.
    port: 5174,
  },
})
