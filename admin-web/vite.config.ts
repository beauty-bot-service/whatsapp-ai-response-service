import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  base: "./",
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/whatsapp-ai-response-service/v1": {
        target: "http://localhost:8081",
        changeOrigin: true
      }
    }
  }
});
