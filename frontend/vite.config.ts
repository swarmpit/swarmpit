import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: "/react/",
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
  build: { outDir: "../resources/public/react", emptyOutDir: true },
  server: {
    proxy: Object.fromEntries(
      [
        "/img",
        "/api",
        "/login",
        "/logout",
        "/version",
        "/initialize",
        "/password",
        "/api-token",
        "/slt",
        "/events",
      ].map((path) => [
        path,
        { target: "http://127.0.0.1:8080", changeOrigin: false },
      ]),
    ),
  },
  test: {
    environment: "jsdom",
    clearMocks: true,
    setupFiles: ["./src/test-setup.ts"],
  },
});
