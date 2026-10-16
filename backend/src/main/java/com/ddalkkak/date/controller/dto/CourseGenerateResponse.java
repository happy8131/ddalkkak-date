package com.ddalkkak.date.controller.dto;

import com.ddalkkak.date.domain.Course;
import com.ddalkkak.date.domain.CoursePlace;
import com.ddalkkak.date.domain.CourseRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// PRD FR-2.2 응답 스펙. 아직 값이 없는 필드(후기 요약, 동선 지도 URL 등)는 응답에서 빠진다
public record CourseGenerateResponse(
    UUID requestId, Instant generatedAt, List<CourseResponse> courses) {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CourseResponse(
      String courseId,
      String title,
      List<PlaceResponse> places,
      int totalCost,
      String totalTime,
      String routeMapUrl) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record PlaceResponse(
      String placeId,
      String name,
      String category,
      String address,
      int estimatedCost,
      String estimatedTime,
      String kakaoPlaceUrl,
      Double trustScore,
      ReviewSummary reviewSummary) {}

  // 블로그 후기 요약 (KAN-21에서 채운다)
  public record ReviewSummary(int reviewCount30d, String lastReviewedAt, List<Review> reviews) {}

  public record Review(
      String title, String snippet, String link, String source, String postedAt) {}

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
