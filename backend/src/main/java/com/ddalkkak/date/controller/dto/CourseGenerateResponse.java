package com.ddalkkak.date.controller.dto;

import com.ddalkkak.date.domain.Course;
import com.ddalkkak.date.domain.CoursePlace;
import com.ddalkkak.date.domain.CourseRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// PRD FR-2.2 응답 스펙. 아직 값이 없는 필드(후기 요약, 동선 지도 URL 등)는 응답에서 빠진다
@Schema(description = "코스 생성 결과")
public record CourseGenerateResponse(
    @Schema(description = "생성 요청 식별자", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID requestId,
    @Schema(description = "요청 기록 시각(UTC, 초 단위)", example = "2026-11-05T03:21:45Z")
        Instant generatedAt,
    @Schema(description = "생성된 코스 후보 목록") List<CourseResponse> courses) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "추천된 데이트 코스 1개")
  public record CourseResponse(
      @Schema(description = "코스 식별자(UUID 문자열)", example = "0b6f5b1e-2c3d-4f8a-9a41-6d7e8f9a0b1c")
          String courseId,
      @Schema(description = "코스 제목", example = "[샘플] 홍대 전시 산책 코스") String title,
      @Schema(description = "방문 순서대로 나열된 장소 목록") List<PlaceResponse> places,
      @Schema(description = "코스 전체 예상 비용(원)", example = "100000") int totalCost,
      @Schema(description = "코스 전체 예상 소요 시간", example = "4시간") String totalTime,
      @Schema(description = "동선 지도 URL. 아직 제공하지 않아 응답에서 빠진다", nullable = true)
          String routeMapUrl) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "코스에 포함된 장소")
  public record PlaceResponse(
      @Schema(description = "장소 식별자. 임시 추천 엔진은 sample-로 시작", example = "sample-culture-0-0")
          String placeId,
      @Schema(description = "장소 이름", example = "홍대 소규모 갤러리") String name,
      @Schema(description = "장소 분류", example = "전시") String category,
      @Schema(description = "장소 주소", example = "서울 홍대 일대") String address,
      @Schema(description = "해당 장소 예상 비용(원). 무료면 0", example = "25000") int estimatedCost,
      @Schema(description = "해당 장소 예상 소요 시간", example = "1.5시간") String estimatedTime,
      @Schema(
              description = "카카오맵 장소 URL. 값이 없으면 응답에서 빠진다",
              example = "https://place.map.kakao.com/12345678")
          String kakaoPlaceUrl,
      @Schema(description = "장소 신뢰도 점수. 값이 없으면 응답에서 빠진다", example = "4.3")
          Double trustScore,
      @Schema(description = "블로그 후기 요약. 아직 채우지 않아(KAN-21) 응답에서 빠진다")
          ReviewSummary reviewSummary) {}

  // 블로그 후기 요약 (KAN-21에서 채운다)
  @Schema(description = "블로그 후기 요약")
  public record ReviewSummary(
      @Schema(description = "최근 30일 후기 수", example = "12") int reviewCount30d,
      @Schema(description = "가장 최근 후기 작성일", example = "2026-11-01") String lastReviewedAt,
      List<Review> reviews) {}

  @Schema(description = "블로그 후기 1건")
  public record Review(
      @Schema(description = "후기 제목", example = "홍대 갤러리 데이트 후기") String title,
      @Schema(description = "후기 본문 일부", example = "조용하고 작품 수가 적당해서 좋았어요.") String snippet,
      @Schema(description = "후기 원문 링크", example = "https://blog.naver.com/example/123456789")
          String link,
      @Schema(description = "후기 출처", example = "naver") String source,
      @Schema(description = "후기 작성일", example = "2026-11-01") String postedAt) {}

  public static CourseGenerateResponse of(CourseRequest request, List<Course> courses) {
    return new CourseGenerateResponse(
        request.getId(),
        request.getCreatedAt().toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS),
        courses.stream().map(CourseGenerateResponse::toCourse).toList());
  }

  private static CourseResponse toCourse(Course course) {
    return new CourseResponse(
        course.getId(),
        course.getTitle(),
        course.getPlaces().stream().map(CourseGenerateResponse::toPlace).toList(),
        course.getTotalCost(),
        course.getTotalTime(),
        null);
  }

  private static PlaceResponse toPlace(CoursePlace place) {
    return new PlaceResponse(
        place.placeId(),
        place.name(),
        place.category(),
        place.address(),
        place.estimatedCost(),
        place.estimatedTime(),
        place.kakaoPlaceUrl(),
        place.trustScore(),
        null);
  }
}
