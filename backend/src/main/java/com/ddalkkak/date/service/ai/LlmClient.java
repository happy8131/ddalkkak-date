package com.ddalkkak.date.service.ai;

// LLM에게 한 번 질문하고 텍스트 응답을 받는다. 테스트에서는 가짜 구현으로 바꾼다
public interface LlmClient {

  String complete(String systemPrompt, String userPrompt);
}
