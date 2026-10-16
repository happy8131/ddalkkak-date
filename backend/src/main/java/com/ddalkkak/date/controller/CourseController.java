package com.ddalkkak.date.controller;

import com.ddalkkak.date.common.ApiResponse;
import com.ddalkkak.date.controller.dto.CourseGenerateRequest;
import com.ddalkkak.date.controller.dto.CourseGenerateResponse;
import com.ddalkkak.date.service.CourseGenerationService;
import com.ddalkkak.date.service.CourseGenerationService.GeneratedCourses;
import com.ddalkkak.date.service.GuestIdCookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
public class CourseController {

  private final CourseGenerationService courseGenerationService;
  private final GuestIdCookie guestIdCookie;

  public CourseController(
      CourseGenerationService courseGenerationService, GuestIdCookie guestIdCookie) {
    this.courseGenerationService = courseGenerationService;
    this.guestIdCookie = guestIdCookie;
  }

  // 비로그인 공개 API (PRD FR-1.3). 로그인(KAN-25) 전까지 user_id는 항상 null이다
  @PostMapping("/generate")
  public ApiResponse<CourseGenerateResponse> generate(
      @Valid @RequestBody CourseGenerateRequest request,
      @CookieValue(name = GuestIdCookie.NAME, required = false) String guestIdValue,
      HttpServletResponse response) {
    String guestId = guestIdCookie.resolve(guestIdValue, response);
    GeneratedCourses generated =
        courseGenerationService.generate(request.toCondition(), null, guestId);
    return ApiResponse.ok(CourseGenerateResponse.of(generated.request(), generated.courses()));
  }
}
