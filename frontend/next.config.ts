import type { NextConfig } from "next";
import { withSentryConfig } from "@sentry/nextjs/config";

const nextConfig: NextConfig = {
  output: "standalone",
  // Sentry의 진단 채널 훅(ESM)은 런타임에 동적으로 로드되어 파일 추적에 잡히지 않는다.
  // 빠지면 standalone 이미지에서 "Failed to register diagnostics-channel injection hooks" 경고가 나고 일부 스팬이 기록되지 않는다
  outputFileTracingIncludes: {
    "/*": [
      "./node_modules/@sentry/server-runtime-injection/build/esm/**/*",
      "./node_modules/@sentry/core/build/esm/**/*",
      "./node_modules/@sentry/server-utils/build/esm/orchestrion/**/*",
      "./node_modules/@sentry/conventions/dist/**/*.mjs",
    ],
  },
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

// Sentry: 소스맵 업로드와 릴리스 연결은 SENTRY_AUTH_TOKEN이 있는 빌드(배포 워크플로)에서만 동작합니다.
// 토큰이 없으면(로컬, PR CI) 업로드를 건너뛰고 빌드는 그대로 성공합니다
export default withSentryConfig(nextConfig, {
  org: process.env.SENTRY_ORG,
  project: process.env.SENTRY_PROJECT,
  authToken: process.env.SENTRY_AUTH_TOKEN,
  // 배포 워크플로가 git SHA를 넘깁니다. 이미지 태그, 백엔드 릴리스와 같은 값입니다
  release: { name: process.env.SENTRY_RELEASE },
  // 업로드 로그는 CI에서만 출력합니다
  silent: !process.env.CI,
  telemetry: false,
  // 더 많은 소스맵을 올려 스택 트레이스를 읽기 쉽게 합니다 (빌드 시간 증가)
  widenClientFileUpload: true,
  // 업로드가 끝난 소스맵은 이미지에서 지워서 브라우저에 공개되지 않게 합니다
  sourcemaps: { deleteSourcemapsAfterUpload: true },
});
