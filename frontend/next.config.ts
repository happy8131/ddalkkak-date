import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  // Docker Desktop(Windows/macOS) 바인드 마운트에서는 파일 변경 이벤트가 전달되지 않아
  // docker compose에서만 NEXT_WATCH_POLL_MS를 지정해 폴링으로 전환한다
  watchOptions: process.env.NEXT_WATCH_POLL_MS
    ? { pollIntervalMs: Number(process.env.NEXT_WATCH_POLL_MS) }
    : undefined,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
