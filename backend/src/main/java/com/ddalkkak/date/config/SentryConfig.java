package com.ddalkkak.date.config;

import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Sentry SDK는 SENTRY_DSN이 있을 때만 켜진다(스타터의 자동 설정). 이 빈은 SDK가 꺼져 있으면 쓰이지 않는다
@Configuration
public class SentryConfig {

  @Bean
  public SentryOptions.BeforeSendCallback sentryBeforeSend() {
    return new SentryEventScrubber();
  }
}
