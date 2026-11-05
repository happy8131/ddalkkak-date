package com.ddalkkak.date.controller;

import com.ddalkkak.date.common.ApiResponse;
import com.ddalkkak.date.controller.dto.CourseGenerateApiResponse;
import com.ddalkkak.date.controller.dto.CourseGenerateRequest;
import com.ddalkkak.date.controller.dto.CourseGenerateResponse;
import com.ddalkkak.date.service.CourseGenerationService;
import com.ddalkkak.date.service.CourseGenerationService.GeneratedCourses;
import com.ddalkkak.date.service.GuestIdCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @Operation(
      summary = "사용자의 조건에 맞는 데이트 코스를 AI를 통해 생성합니다.",
      description =
          """
          지역, 데이트 유형, 예산으로 데이트 코스 후보를 생성하고 결과를 저장합니다.
          요청에 유효한 `guest_id` 쿠키가 없으면 새로 발급해 `Set-Cookie`로 내려줍니다.
          (HttpOnly, Secure, SameSite=Lax, 1년)
          값이 없는 선택 필드는 응답에서 키 자체가 빠집니다.
          """)
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "코스 생성 성공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CourseGenerateApiResponse.class),
                examples =
                    @ExampleObject(
                        name = "홍대 문화데이트 / 10만원 (코스 3개 중 1개만 표기)",
                        value =
                            """
                            {
                              "success": true,
                              "data": {
                                "request_id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                                "generated_at": "2026-11-05T03:21:45Z",
                                "courses": [
                                  {
                                    "course_id": "0b6f5b1e-2c3d-4f8a-9a41-6d7e8f9a0b1c",
                                    "title": "[샘플] 홍대 전시 산책 코스",
                                    "places": [
                                      {
                                        "place_id": "sample-culture-0-0",
                                        "name": "홍대 소규모 갤러리",
                                        "category": "전시",
                                        "address": "서울 홍대 일대",
                                        "estimated_cost": 25000,
                                        "estimated_time": "1.5시간"
                                      },
                                      {
                                        "place_id": "sample-culture-0-1",
                                        "name": "홍대 디저트 카페",
                                        "category": "카페",
                                        "address": "서울 홍대 일대",
                                        "estimated_cost": 25000,
                                        "estimated_time": "1시간"
                                      }
                                    ],
                                    "total_cost": 100000,
                                    "total_time": "4시간"
                                  }
                                ]
                              }
                            }
                            """)),
        headers =
            @Header(
                name = "Set-Cookie",
                description = "유효한 guest_id 쿠키가 없을 때만 내려갑니다",
                schema = @Schema(type = "string"))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "검증 실패 (지원하지 않는 지역·유형, 예산 범위 초과, JSON 형식 오류)",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {"success": false, "error": {"code": "INVALID_REQUEST", "message": "budget: 30,000원 이상이어야 합니다."}}
                            """))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {"success": false, "error": {"code": "INTERNAL_ERROR", "message": "서버 오류가 발생했습니다."}}
                            """)))
  })
  @PostMapping("/generate")
  public ApiResponse<CourseGenerateResponse> generate(
      @Valid @RequestBody CourseGenerateRequest request,
      @Parameter(description = "비로그인 사용자 식별용 UUID 쿠키. 없거나 형식이 틀리면 서버가 새로 발급합니다")
          @CookieValue(name = GuestIdCookie.NAME, required = false)
          String guestIdValue,
      HttpServletResponse response) {
    String guestId = guestIdCookie.resolve(guestIdValue, response);
    GeneratedCourses generated =
        courseGenerationService.generate(request.toCondition(), null, guestId);
    return ApiResponse.ok(CourseGenerateResponse.of(generated.request(), generated.courses()));
  }
}
