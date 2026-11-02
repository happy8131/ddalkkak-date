package com.ddalkkak.date.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddalkkak.date.common.exception.GlobalExceptionHandler;
import com.ddalkkak.date.config.SentryConfig;
import com.ddalkkak.date.config.SentryEventScrubber;
import com.ddalkkak.date.config.WebConfig;
import io.sentry.Hint;
import io.sentry.ITransportFactory;
import io.sentry.RequestDetails;
import io.sentry.Sentry;
import io.sentry.SentryEnvelope;
import io.sentry.SentryItemType;
import io.sentry.SentryOptions;
import io.sentry.spring.boot4.SentryAutoConfiguration;
import io.sentry.transport.ITransport;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

// 실제 MVC 스택(예외 핸들러 포함)에서 Sentry가 500은 전송하고 4xx는 전송하지 않는지 확인한다 (DB, 네트워크 불필요)
@WebMvcTest(SampleController.class)
@Import({
  WebConfig.class,
  GlobalExceptionHandler.class,
  SentryConfig.class,
  SentryAutoConfiguration.class,
  SentryWebLayerTest.RecordingTransportConfig.class
})
// DSN만 지정한다. exception-resolver-order 등 나머지는 실제 application.yml 값을 그대로 쓴다
@TestPropertySource(properties = {"sentry.dsn=https://publickey@localhost/1", "sentry.environment=test"})
class SentryWebLayerTest {

  // 네트워크로 보내는 대신, 전송하려던 에러 이벤트 봉투를 모아 둔다
  @TestConfiguration
  static class RecordingTransportConfig {
    static final List<SentryEnvelope> sent = new CopyOnWriteArrayList<>();

    @Bean
    ITransportFactory recordingTransportFactory() {
      return (SentryOptions options, RequestDetails requestDetails) ->
          new ITransport() {
            @Override
            public void send(SentryEnvelope envelope, Hint hint) {
              sent.add(envelope);
            }

            @Override
            public boolean isHealthy() {
              return true;
            }

            @Override
            public void flush(long timeoutMillis) {}

            @Override
            public void close() {}

            @Override
            public void close(boolean isRestarting) {}

            @Override
            public io.sentry.transport.RateLimiter getRateLimiter() {
              return null;
            }
          };
    }
  }

  @Autowired MockMvc mockMvc;

  @BeforeEach
  void clear() {
    RecordingTransportConfig.sent.clear();
  }

  // 스프링 컨텍스트는 테스트 간에 재사용되므로 SDK는 클래스가 끝난 뒤에 닫는다 (다른 테스트에 전역 상태가 남지 않게)
  @AfterAll
  static void closeSentry() {
    Sentry.close();
  }

  private static long sentErrorEvents() {
    return RecordingTransportConfig.sent.stream()
        .flatMap(envelope -> java.util.stream.StreamSupport.stream(envelope.getItems().spliterator(), false))
        .filter(item -> item.getHeader().getType() == SentryItemType.Event)
        .count();
  }

  @Test
  void DSN이_있으면_SDK가_켜지고_스크러버가_등록된다() {
    assertThat(Sentry.isEnabled()).isTrue();
    assertThat(Sentry.getCurrentScopes().getOptions().getBeforeSend())
        .isInstanceOf(SentryEventScrubber.class);
    assertThat(Sentry.getCurrentScopes().getOptions().getEnvironment()).isEqualTo("test");
  }

  @Test
  void 예상하지_못한_예외는_GlobalExceptionHandler가_처리해도_Sentry로_전송된다() throws Exception {
    mockMvc.perform(get("/api/v1/sample/unexpected-error")).andExpect(status().isInternalServerError());

    assertThat(sentErrorEvents()).isEqualTo(1);
  }

  @Test
  void 클라이언트_오류_4xx는_전송되지_않는다() throws Exception {
    mockMvc.perform(get("/api/v1/sample/business-error")).andExpect(status().isNotFound());
    mockMvc.perform(get("/api/v1/sample/not-exists")).andExpect(status().isNotFound());

    assertThat(sentErrorEvents()).isZero();
  }
}
