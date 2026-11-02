package com.ddalkkak.date.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddalkkak.date.common.exception.BusinessException;
import com.ddalkkak.date.common.exception.ErrorCode;
import com.ddalkkak.date.common.exception.ExternalApiException;
import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class SentryEventScrubberTest {

  private final SentryEventScrubber scrubber = new SentryEventScrubber();

  private SentryEvent run(SentryEvent event) {
    return scrubber.execute(event, new Hint());
  }

  @Test
  void 클라이언트_잘못인_비즈니스_예외는_전송하지_않는다() {
    SentryEvent event = new SentryEvent(new BusinessException(ErrorCode.NOT_FOUND));

    assertThat(run(event)).isNull();
  }

  @Test
  void 서버_오류인_비즈니스_예외는_전송한다() {
    SentryEvent event = new SentryEvent(new BusinessException(ErrorCode.INTERNAL_ERROR));

    assertThat(run(event)).isNotNull();
  }

  @Test
  void 스프링_MVC_4xx_예외는_전송하지_않는다() {
    SentryEvent event = new SentryEvent(new HttpRequestMethodNotSupportedException("PUT"));

    assertThat(run(event)).isNull();
  }

  @Test
  void 처리되지_않은_예외는_전송한다() {
    SentryEvent event = new SentryEvent(new IllegalStateException("boom"));

    assertThat(run(event)).isNotNull();
  }

  @Test
  void 쿠키와_인증_헤더와_요청_본문과_사용자_정보를_지운다() {
    Request request = new Request();
    request.setCookies("guest_id=3f0c1d2e-0000-4000-8000-000000000000");
    request.setData("{\"region\":\"홍대\"}");
    request.setQueryString("token=abc");
    Map<String, String> headers = new HashMap<>();
    headers.put("Cookie", "guest_id=3f0c1d2e-0000-4000-8000-000000000000");
    headers.put("Authorization", "Bearer secret");
    headers.put("X-Forwarded-For", "203.0.113.7");
    headers.put("User-Agent", "JUnit");
    request.setHeaders(headers);
    SentryEvent event = new SentryEvent(new IllegalStateException("boom"));
    event.setRequest(request);
    User user = new User();
    user.setIpAddress("203.0.113.7");
    event.setUser(user);

    SentryEvent result = run(event);

    assertThat(result).isNotNull();
    assertThat(result.getUser()).isNull();
    Request scrubbed = result.getRequest();
    assertThat(scrubbed.getCookies()).isNull();
    assertThat(scrubbed.getData()).isNull();
    assertThat(scrubbed.getQueryString()).isNull();
    assertThat(scrubbed.getHeaders()).containsOnlyKeys("User-Agent");
  }

  @Test
  void 외부_API_실패는_external_태그로_구분한다() {
    RuntimeException wrapped =
        new RuntimeException(
            "수집 실패", new ExternalApiException(ExternalApiException.KAKAO, "카카오 실패", null));
    SentryEvent event = new SentryEvent(wrapped);

    SentryEvent result = run(event);

    assertThat(result.getTag("external")).isEqualTo("kakao");
  }

  @Test
  void 외부_API와_무관한_예외에는_external_태그가_없다() {
    SentryEvent result = run(new SentryEvent(new IllegalStateException("boom")));

    assertThat(result.getTag("external")).isNull();
  }
}
