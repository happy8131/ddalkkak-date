package com.ddalkkak.date.config;

import com.ddalkkak.date.common.exception.BusinessException;
import com.ddalkkak.date.common.exception.ExternalApiException;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.Request;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.ErrorResponse;

// Sentry로 보내기 직전에 이벤트를 걸러내고 개인정보를 지운다
// - 4xx(클라이언트 잘못)는 보내지 않는다. 장애가 아니라서 노이즈만 된다
// - 쿠키(guest_id), 인증 헤더, 요청 본문, 사용자 정보는 이벤트에서 제거한다
// - 외부 API 호출 실패는 external 태그(kakao, llm)로 구분한다
public class SentryEventScrubber implements SentryOptions.BeforeSendCallback {

  static final String EXTERNAL_TAG = "external";

  // 값에 식별 정보나 비밀이 들어갈 수 있는 헤더 (소문자)
  private static final Set<String> SENSITIVE_HEADERS =
      Set.of("cookie", "set-cookie", "authorization", "proxy-authorization", "x-forwarded-for", "x-real-ip");

  @Override
  public SentryEvent execute(SentryEvent event, Hint hint) {
    Throwable throwable = event.getThrowable();
    if (isClientError(throwable)) {
      return null;
    }
    scrubRequest(event.getRequest());
    event.setUser(null);
    tagExternalService(event, throwable);
    return event;
  }

  private static boolean isClientError(Throwable throwable) {
    if (throwable instanceof BusinessException e) {
      return e.getErrorCode().getStatus().is4xxClientError();
    }
    // 스프링 MVC 표준 예외(404, 405, 검증 오류 등)
    if (throwable instanceof ErrorResponse e) {
      return e.getStatusCode().is4xxClientError();
    }
    return false;
  }

  private static void scrubRequest(Request request) {
    if (request == null) {
      return;
    }
    request.setCookies(null);
    request.setData(null);
    request.setQueryString(null);
    Map<String, String> headers = request.getHeaders();
    if (headers != null) {
      Map<String, String> kept = new LinkedHashMap<>();
      headers.forEach(
          (name, value) -> {
            if (!SENSITIVE_HEADERS.contains(name.toLowerCase())) {
              kept.put(name, value);
            }
          });
      request.setHeaders(kept);
    }
  }

  private static void tagExternalService(SentryEvent event, Throwable throwable) {
    for (Throwable t = throwable; t != null; t = t.getCause() == t ? null : t.getCause()) {
      if (t instanceof ExternalApiException e) {
        event.setTag(EXTERNAL_TAG, e.getService());
        return;
      }
    }
  }
}
