package com.ddalkkak.date.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddalkkak.date.common.exception.GlobalExceptionHandler;
import com.ddalkkak.date.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// DB 없이 웹 계층만 검증한다 (접두사, 응답 형식, 예외 처리, CORS)
@WebMvcTest(SampleController.class)
@Import({WebConfig.class, GlobalExceptionHandler.class})
class ApiWebLayerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void 컨트롤러에_api_v1_접두사가_붙고_공통_응답_형식을_따른다() throws Exception {
    mockMvc
        .perform(get("/api/v1/sample/ok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value("pong"))
        .andExpect(jsonPath("$.error").doesNotExist());
  }

  @Test
  void 접두사_없는_경로는_존재하지_않는다() throws Exception {
    mockMvc.perform(get("/sample/ok")).andExpect(status().isNotFound());
  }

  @Test
  void 비즈니스_예외는_에러_코드와_상태로_변환된다() throws Exception {
    mockMvc
        .perform(get("/api/v1/sample/business-error"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
  }

  @Test
  void 예상하지_못한_예외는_내부_정보_없이_500으로_변환된다() throws Exception {
    mockMvc
        .perform(get("/api/v1/sample/unexpected-error"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."));
  }

  @Test
  void 유효한_POST_본문은_처리된다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sample/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"성수\",\"count\":2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value("성수:2"));
  }

  @Test
  void 검증에_실패한_POST_본문은_400과_필드_메시지를_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sample/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"count\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.error.message").isNotEmpty());
  }

  @Test
  void 깨진_JSON은_400으로_변환된다() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sample/echo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
  }

  @Test
  void 지원하지_않는_메서드는_405로_변환된다() throws Exception {
    mockMvc
        .perform(delete("/api/v1/sample/ok"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.success").value(false));
  }

  @Test
  void 프론트엔드_로컬_주소는_CORS가_허용된다() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/sample/ok")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
  }

  @Test
  void 허용되지_않은_출처는_CORS가_거부된다() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/sample/ok")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }
}
