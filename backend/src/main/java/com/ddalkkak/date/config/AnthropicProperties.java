package com.ddalkkak.date.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Claude API 설정. 키가 비어 있어도 앱은 뜨고, AI 호출 시점에 검사한다
@ConfigurationProperties(prefix = "app.anthropic")
public record AnthropicProperties(
    String apiKey,
    @DefaultValue("claude-haiku-5-5") String model,
    @DefaultValue("1024") int maxTokens) {}
