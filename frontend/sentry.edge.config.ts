import * as Sentry from "@sentry/nextjs";
import { sentryOptions } from "@/lib/sentry";

// 엣지 런타임(미들웨어 등)에서 실행됩니다
Sentry.init(sentryOptions);
