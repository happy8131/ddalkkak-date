package com.ddalkkak.date.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.Sentry;
import io.sentry.spring.boot4.SentryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// 로컬·테스트·CI에서는 DSN이 없다. 그때 앱이 정상 기동하고 Sentry는 꺼져 있어야 한다
class SentryDisabledWithoutDsnTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(SentryAutoConfiguration.class))
          .withUserConfiguration(SentryConfig.class);

  @Test
  void DSN이_없으면_컨텍스트가_뜨고_SDK는_꺼져_있다() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).doesNotHaveBean("sentryHub");
          assertThat(Sentry.isEnabled()).isFalse();
        });
  }
}
