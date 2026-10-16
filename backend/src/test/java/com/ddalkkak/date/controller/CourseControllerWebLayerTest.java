package com.ddalkkak.date.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ddalkkak.date.common.exception.GlobalExceptionHandler;
import com.ddalkkak.date.config.WebConfig;
import com.ddalkkak.date.domain.Course;
import com.ddalkkak.date.domain.CourseCondition;
import com.ddalkkak.date.domain.CoursePlace;
import com.ddalkkak.date.domain.CourseRequest;
import com.ddalkkak.date.domain.DateType;
import com.ddalkkak.date.domain.Region;
import com.ddalkkak.date.service.CourseGenerationService;
import com.ddalkkak.date.service.CourseGenerationService.GeneratedCourses;
import com.ddalkkak.date.service.GuestIdCookie;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// DB 없이 코스 생성 API의 입력 검증, guest_id 쿠키, 응답 형식을 검증한다
@WebMvcTest(CourseController.class)
@Import({WebConfig.class, GlobalExceptionHandler.class, GuestIdCookie.class})
class CourseControllerWebLayerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean CourseGenerationService courseGenerationService;

  @BeforeEach
  void setUp() {
    given(courseGenerationService.generate(any(), any(), any()))
        .willAnswer(invocation -> generated(invocation.getArgument(0)));
  }

  private static GeneratedCourses generated(CourseCondition condition) {
    CourseRequest request =
        new CourseRequest(null, "guest", condition, LocalDateTime.of(2026, 10, 16, 5, 30, 0), 12);
    ReflectionTestUtils.setField(
        request, "id", UUID.fromString("11111111-2222-3333-4444-555555555555"));
    CoursePlace place =
        new CoursePlace("p1", "갤러리", "전시", "서울 마포구", 30000, "1.5시간", null, null);
    Course course = new Course(request, "홍대 문화 코스", List.of(place), 95000, "4.5시간");
    ReflectionTestUtils.setField(course, "id", "c1");
    return new GeneratedCourses(request, List.of(course));
  }

  private ResultActions generate(String body, Cookie... cookies) throws Exception {
    var builder =
        post("/api/v1/courses/generate").contentType(MediaType.APPLICATION_JSON).content(body);
    if (cookies.length > 0) {
      builder.cookie(cookies);
    }
    return mockMvc.perform(builder);
  }

  private static String body(String region, String dateType, Object budget) {
    return """
        {"region": %s, "date_type": %s, "budget": %s}
        """
        .formatted(quote(region), quote(dateType), budget);
  }

  private static String quote(String value) {
    return value == null ? "null" : "\"" + value + "\"";
  }

  @Test
  void 유효한_요청은_snake_case_응답_스펙으로_반환된다() throws Exception {
    generate(body("홍대", "문화데이트", 100000))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.request_id").value("11111111-2222-3333-4444-555555555555"))
        .andExpect(jsonPath("$.data.generated_at").value("2026-10-16T05:30:00Z"))
        .andExpect(jsonPath("$.data.courses[0].course_id").value("c1"))
        .andExpect(jsonPath("$.data.courses[0].title").value("홍대 문화 코스"))
        .andExpect(jsonPath("$.data.courses[0].total_cost").value(95000))
        .andExpect(jsonPath("$.data.courses[0].total_time").value("4.5시간"))
        .andExpect(jsonPath("$.data.courses[0].places[0].place_id").value("p1"))
        .andExpect(jsonPath("$.data.courses[0].places[0].estimated_cost").value(30000))
        .andExpect(jsonPath("$.data.courses[0].places[0].estimated_time").value("1.5시간"))
        .andExpect(jsonPath("$.data.courses[0].places[0].kakao_place_url").doesNotExist());

    verify(courseGenerationService)
        .generate(
            eq(new CourseCondition(Region.HONGDAE, DateType.CULTURE, 100000)), isNull(), any());
  }

  @Test
  void 쿠키가_없으면_guest_id를_HttpOnly_Secure_SameSite_Lax로_발급한다() throws Exception {
    String setCookie =
        generate(body("강남", "맛집투어", 50000))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);

    assertThat(setCookie)
        .startsWith("guest_id=")
        .contains("Path=/")
        .contains("Max-Age=31536000")
        .contains("HttpOnly")
        .contains("Secure")
        .contains("SameSite=Lax");
    String guestId = setCookie.substring("guest_id=".length(), setCookie.indexOf(';'));
    verify(courseGenerationService).generate(any(), isNull(), eq(guestId));
  }

  @Test
  void 유효한_guest_id_쿠키가_있으면_재사용하고_새로_발급하지_않는다() throws Exception {
    String guestId = UUID.randomUUID().toString();

    generate(body("성수", "액티비티", 300000), new Cookie("guest_id", guestId))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verify(courseGenerationService).generate(any(), isNull(), eq(guestId));
  }

  @Test
  void 형식이_잘못된_guest_id_쿠키는_새로_발급한다() throws Exception {
    generate(body("연남", "감성데이트", 30000), new Cookie("guest_id", "not-a-uuid"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.startsWith("guest_id=")))
        .andExpect(
            header().string(
                HttpHeaders.SET_COOKIE,
                org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("not-a-uuid"))));
  }

  @Test
  void 예산_경계값은_허용된다() throws Exception {
    generate(body("이태원", "문화데이트", 30000)).andExpect(status().isOk());
    generate(body("이태원", "문화데이트", 300000)).andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(ints = {29999, 300001, 0, -10000})
  void 예산_범위를_벗어나면_400이다(int budget) throws Exception {
    generate(body("홍대", "문화데이트", budget))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.startsWith("budget: ")));
  }

  @Test
  void 지원하지_않는_지역은_400이다() throws Exception {
    generate(body("부산", "문화데이트", 100000))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.message").value("region: 지원하지 않는 지역입니다."));
  }

  @Test
  void 지원하지_않는_데이트_유형은_400이고_필드명은_snake_case다() throws Exception {
    generate(body("홍대", "CULTURE", 100000))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.message").value("date_type: 지원하지 않는 데이트 유형입니다."));
  }

  @Test
  void 필수값이_없으면_400이다() throws Exception {
    generate(body(null, "문화데이트", 100000)).andExpect(status().isBadRequest());
    generate(body("홍대", null, 100000)).andExpect(status().isBadRequest());
    generate(body("홍대", "문화데이트", null)).andExpect(status().isBadRequest());
    generate("{}").andExpect(status().isBadRequest());
  }

  @Test
  void 예산이_숫자가_아니면_400이다() throws Exception {
    generate(body("홍대", "문화데이트", "\"십만원\"")).andExpect(status().isBadRequest());
  }

  @Test
  void 검증에_실패하면_코스를_생성하지_않고_쿠키도_발급하지_않는다() throws Exception {
    generate(body("부산", "문화데이트", 100000))
        .andExpect(status().isBadRequest())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verifyNoInteractions(courseGenerationService);
  }
}
