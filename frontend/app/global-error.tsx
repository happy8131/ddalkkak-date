"use client";

import * as Sentry from "@sentry/nextjs";
import { useEffect } from "react";

// 루트 레이아웃까지 깨지는 렌더링 오류를 Sentry로 보내고 안내 화면을 보여줍니다.
// 이 컴포넌트는 루트 레이아웃을 대체하므로 html, body를 직접 렌더링해야 합니다
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    Sentry.captureException(error);
  }, [error]);

  return (
    <html lang="ko">
      <body className="bg-background text-foreground flex min-h-screen flex-col items-center justify-center gap-4 p-8">
        <h1 className="text-2xl font-bold">문제가 발생했어요</h1>
        <p className="text-foreground/70 text-center">
          잠시 후 다시 시도해 주세요. 문제가 계속되면 알려 주세요.
        </p>
        <button
          type="button"
          onClick={reset}
          className="bg-primary rounded-2xl px-6 py-3 font-bold text-white"
        >
          다시 시도
        </button>
      </body>
    </html>
  );
}
