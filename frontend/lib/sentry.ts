import type { ErrorEvent, Event } from "@sentry/nextjs";

// Sentry 공통 설정. 클라이언트·서버·엣지 초기화가 모두 이 값을 씁니다.
// NEXT_PUBLIC_* 값은 빌드 시점에 번들에 들어가므로 Docker 빌드 인자로 전달해야 합니다.
export const SENTRY_DSN = process.env.NEXT_PUBLIC_SENTRY_DSN;

// 값이 숫자가 아니거나 0~1 밖이면 기본값을 씁니다
function parseSampleRate(value: string | undefined, fallback: number): number {
  const rate = Number(value);
  return value !== undefined && value !== "" && rate >= 0 && rate <= 1
    ? rate
    : fallback;
}

// 값에 식별 정보나 비밀이 들어갈 수 있는 요청 헤더 (소문자)
const SENSITIVE_HEADERS = new Set([
  "cookie",
  "set-cookie",
  "authorization",
  "proxy-authorization",
  "x-forwarded-for",
  "x-real-ip",
]);

// 전송 직전에 쿠키(guest_id), 인증 헤더, 요청 본문, 쿼리 문자열, 사용자 정보를 지웁니다
export function scrubEvent<T extends Event>(event: T): T {
  if (event.request) {
    delete event.request.cookies;
    delete event.request.data;
    delete event.request.query_string;
    if (event.request.headers) {
      event.request.headers = Object.fromEntries(
        Object.entries(event.request.headers).filter(
          ([name]) => !SENSITIVE_HEADERS.has(name.toLowerCase()),
        ),
      );
    }
  }
  delete event.user;
  return event;
}

export const sentryOptions = {
  dsn: SENTRY_DSN,
  // DSN이 없으면(로컬, CI 빌드) SDK를 켜지 않아 아무것도 전송하지 않습니다
  enabled: Boolean(SENTRY_DSN),
  environment:
    process.env.NEXT_PUBLIC_SENTRY_ENVIRONMENT ??
    (process.env.NODE_ENV === "production" ? "prod" : "local"),
  // 비용을 고려해 기본 10%. 0이면 트레이싱을 끕니다
  tracesSampleRate: parseSampleRate(
    process.env.NEXT_PUBLIC_SENTRY_TRACES_SAMPLE_RATE,
    0.1,
  ),
  // IP, 쿠키, 사용자 정보를 자동으로 보내지 않습니다 (guest_id 보호)
  sendDefaultPii: false,
  // 브라우저가 자체적으로 내는 무해한 경고
  ignoreErrors: [
    "ResizeObserver loop limit exceeded",
    "ResizeObserver loop completed with undelivered notifications.",
  ],
  beforeSend: (event: ErrorEvent) => scrubEvent(event),
};
