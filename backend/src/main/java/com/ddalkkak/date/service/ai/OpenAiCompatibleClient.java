package com.ddalkkak.date.service.ai;

import com.ddalkkak.date.config.LlmProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// OpenAI 호환 {baseUrl}/chat/completions 를 호출한다 (Groq, AIMLAPI 등). 기본 provider다
@Component
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "provider",
    havingValue = "openai-compatible",
    matchIfMissing = true)
public class OpenAiCompatibleClient implements LlmClient {

  private static final int MAX_RATE_LIMIT_ATTEMPTS = 3;
  // 이보다 오래 기다려야 하는 429(일일 한도 등)는 재시도하지 않는다
  private static final long MAX_RETRY_WAIT_SECONDS = 65;

  private final RestClient restClient;
  private final LlmProperties properties;
  private long lastCallAt;

  @Autowired
  public OpenAiCompatibleClient(LlmProperties properties) {
    this(RestClient.builder().baseUrl(properties.baseUrl()).build(), properties);
  }

  OpenAiCompatibleClient(RestClient restClient, LlmProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public String complete(String systemPrompt, String userPrompt) {
    if (!StringUtils.hasText(properties.apiKey())) {
      throw new IllegalStateException("LLM_API_KEY가 설정되지 않았습니다.");
    }
    Map<String, Object> body =
        Map.of(
            "model", properties.model(),
            "max_tokens", properties.maxTokens(),
            "messages",
                List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)));
    ChatResponse response = null;
    for (int attempt = 1; response == null; attempt++) {
      throttle();
      try {
        response =
            restClient
                .post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + properties.apiKey())
                .body(body)
                .retrieve()
                .body(ChatResponse.class);
        if (response == null) {
          break;
        }
      } catch (HttpClientErrorException.TooManyRequests e) {
        // 분당 한도처럼 곧 풀리는 429는 Retry-After만큼 쉬고 다시 시도하고, 아니면 한도 도달로 본다
        long waitSeconds = retryAfterSeconds(e);
        if (attempt >= MAX_RATE_LIMIT_ATTEMPTS
            || waitSeconds < 0
            || waitSeconds > MAX_RETRY_WAIT_SECONDS) {
          throw new LlmRateLimitException("LLM API 호출 한도에 도달했습니다.", e);
        }
        sleep(Math.max(waitSeconds, 1) * 1000);
      } catch (RestClientException e) {
        throw new IllegalStateException("LLM API 호출에 실패했습니다: " + e.getMessage(), e);
      }
    }
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw new IllegalStateException("LLM API 응답이 비어 있습니다.");
    }
    String content = response.choices().get(0).message().content();
    return content == null ? "" : content;
  }

  // 무료 한도의 분당 호출 수·토큰 수를 넘지 않도록 직전 호출과 최소 간격을 둔다
  private synchronized void throttle() {
    long wait = lastCallAt + properties.minIntervalMs() - System.currentTimeMillis();
    if (wait > 0) {
      sleep(wait);
    }
    lastCallAt = System.currentTimeMillis();
  }

  // Retry-After 헤더(초). 없거나 숫자가 아니면 -1
  private static long retryAfterSeconds(HttpClientErrorException e) {
    String value =
        e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
    try {
      return value == null ? -1 : (long) Math.ceil(Double.parseDouble(value));
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatResponse(List<Choice> choices) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Choice(Message message) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Message(@JsonProperty("content") String content) {}
}
