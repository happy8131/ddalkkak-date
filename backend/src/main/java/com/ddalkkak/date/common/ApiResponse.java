package com.ddalkkak.date.common;

import com.fasterxml.jackson.annotation.JsonInclude;

// 모든 /api/v1 응답의 공통 형식
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

  public record ApiError(String code, String message) {}

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static ApiResponse<Void> fail(String code, String message) {
    return new ApiResponse<>(false, null, new ApiError(code, message));
  }
}
