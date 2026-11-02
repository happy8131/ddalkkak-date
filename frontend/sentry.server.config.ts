import * as Sentry from "@sentry/nextjs";
import { sentryOptions } from "@/lib/sentry";

// Node.js 런타임(서버 컴포넌트, 라우트 핸들러)에서 실행됩니다
Sentry.init(sentryOptions);
