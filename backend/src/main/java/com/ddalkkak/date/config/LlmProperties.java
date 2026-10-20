package com.ddalkkak.date.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// OpenAI 호환 chat/completions API 설정 (Groq, AIMLAPI, OpenRouter 등).
// 키가 비어 있어도 앱은 뜨고, AI 호출 시점에 검사한다
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
    @DefaultValue("https://api.groq.com/openai/v1") String baseUrl,
    String apiKey,
    @DefaultValue("qwen/qwen3.8-27b") String model,
    @DefaultValue("350") int maxTokens,
    // 무료 한도(분당 호출 수)를 넘지 않도록 호출 사이에 두는 최소 간격
    @DefaultValue("5500") long minIntervalMs) {}
