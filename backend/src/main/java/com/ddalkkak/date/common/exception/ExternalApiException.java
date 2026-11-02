package com.ddalkkak.date.common.exception;

// 외부 API(카카오, LLM) 호출 실패. Sentry에서 어느 외부 서비스 때문인지 구분하기 위해 서비스 이름을 담는다
public class ExternalApiException extends IllegalStateException {

  public static final String KAKAO = "kakao";
  public static final String LLM = "llm";

  private final String service;

  public ExternalApiException(String service, String message, Throwable cause) {
    super(message, cause);
    this.service = service;
  }

  public String getService() {
    return service;
  }
}
