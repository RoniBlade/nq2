import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// Use BASE_PATH for GitHub Pages (e.g. "/my-dashboard/")
export default defineConfig(() => {
  const base = process.env.BASE_PATH || "/";
  return {
    base,
    plugins: [react(), tailwindcss()],
  };
});
