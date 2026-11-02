import * as Sentry from "@sentry/nextjs";
import { sentryOptions } from "@/lib/sentry";

// 브라우저에서 실행됩니다. 세션 리플레이는 개인정보 때문에 켜지 않습니다
Sentry.init(sentryOptions);

// 클라이언트 내비게이션을 트레이싱에 포함합니다
export const onRouterTransitionStart = Sentry.captureRouterTransitionStart;
