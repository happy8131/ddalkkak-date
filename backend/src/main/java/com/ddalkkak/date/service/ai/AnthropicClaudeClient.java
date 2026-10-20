package com.ddalkkak.date.service.ai;

import com.ddalkkak.date.config.AnthropicProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// Anthropic Messages API(https://api.anthropic.com/v1/messages)를 직접 호출한다. app.ai.provider=anthropic일 때만 쓴다
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "anthropic")
public class AnthropicClaudeClient implements LlmClient {

  private static final String BASE_URL = "https://api.anthropic.com";
  private static final String API_VERSION = "2023-06-01";

  private final RestClient restClient;
  private final AnthropicProperties properties;

  @Autowired
  public AnthropicClaudeClient(AnthropicProperties properties) {
    this(RestClient.builder().baseUrl(BASE_URL).build(), properties);
  }

  AnthropicClaudeClient(RestClient restClient, AnthropicProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public String complete(String systemPrompt, String userPrompt) {
    if (!StringUtils.hasText(properties.apiKey())) {
      throw new IllegalStateException("ANTHROPIC_API_KEY가 설정되지 않았습니다.");
    }
    Map<String, Object> body =
        Map.of(
            "model", properties.model(),
            "max_tokens", properties.maxTokens(),
            "system", systemPrompt,
            "messages", List.of(Map.of("role", "user", "content", userPrompt)));
    MessagesResponse response;
    try {
      response =
          restClient
              .post()
              .uri("/v1/messages")
              .header("x-api-key", properties.apiKey())
              .header("anthropic-version", API_VERSION)
              .body(body)
              .retrieve()
              .body(MessagesResponse.class);
    } catch (RestClientException e) {
      throw new IllegalStateException("Claude API 호출에 실패했습니다: " + e.getMessage(), e);
    }
    if (response == null || response.content() == null) {
      throw new IllegalStateException("Claude API 응답이 비어 있습니다.");
    }
    return response.content().stream()
        .filter(block -> "text".equals(block.type()))
        .map(ContentBlock::text)
        .reduce("", String::concat);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record MessagesResponse(List<ContentBlock> content) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ContentBlock(@JsonProperty("type") String type, @JsonProperty("text") String text) {}
}
