package com.ddalkkak.date.service.ai;

// LLM API의 호출 한도(429)에 걸렸을 때. 더 호출해도 소용없으므로 배치는 여기서 멈추고 다음 실행에서 이어간다
public class LlmRateLimitException extends RuntimeException {

  public LlmRateLimitException(String message, Throwable cause) {
    super(message, cause);
  }
}
