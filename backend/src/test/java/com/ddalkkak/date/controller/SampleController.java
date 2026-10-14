package com.ddalkkak.date.controller;

import com.ddalkkak.date.common.ApiResponse;
import com.ddalkkak.date.common.exception.BusinessException;
import com.ddalkkak.date.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1 접두사와 공통 응답/예외 형식을 검증하기 위한 테스트 전용 컨트롤러
@RestController
class SampleController {

  @GetMapping("/sample/ok")
  ApiResponse<String> ok() {
    return ApiResponse.ok("pong");
  }

  @GetMapping("/sample/business-error")
  ApiResponse<Void> businessError() {
    throw new BusinessException(ErrorCode.NOT_FOUND);
  }

  @GetMapping("/sample/unexpected-error")
  ApiResponse<Void> unexpectedError() {
    throw new IllegalStateException("boom");
  }
}
