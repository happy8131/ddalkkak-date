package com.ddalkkak.date.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddalkkak.date.common.exception.GlobalExceptionHandler;
import com.ddalkkak.date.config.OpenApiConfig;
import com.ddalkkak.date.config.WebConfig;
import com.ddalkkak.date.service.CourseGenerationService;
import com.ddalkkak.date.service.GuestIdCookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// DB 없이 springdoc이 만든 OpenAPI 명세가 실제 API(경로, snake_case 필드)와 맞는지 확인한다
// @WebMvcTest 슬라이스에는 springdoc 자동 설정이 없어서 필요한 것만 직접 가져온다
@WebMvcTest(CourseController.class)
@ImportAutoConfiguration({
  SpringDocConfiguration.class,
  SpringDocConfigProperties.class,
  SpringDocWebMvcConfiguration.class
})
@Import({WebConfig.class, GlobalExceptionHandler.class, GuestIdCookie.class, OpenApiConfig.class})
class OpenApiDocsWebLayerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean CourseGenerationService courseGenerationService;

  @Test
  void 명세에_접두사가_붙은_코스_생성_경로가_있다() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.info.title").value("데이트 딸깍 API"))
        .andExpect(jsonPath("$.paths['/api/v1/courses/generate'].post.summary").exists());
  }

  @Test
  void 요청_스키마_필드는_snake_case다() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(jsonPath("$.components.schemas.CourseGenerateRequest.properties.date_type").exists())
        .andExpect(jsonPath("$.components.schemas.CourseGenerateRequest.properties.dateType").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.CourseGenerateRequest.required").value(Matchers.hasItem("date_type")))
        .andExpect(jsonPath("$.components.schemas.PlaceResponse.properties.estimated_cost").exists())
        .andExpect(jsonPath("$.components.schemas.CourseResponse.properties.total_cost").exists());
  }
}
