package com.ddalkkak.date.service;

import com.ddalkkak.date.domain.Course;
import com.ddalkkak.date.domain.CourseCondition;
import com.ddalkkak.date.domain.CourseRequest;
import com.ddalkkak.date.repository.CourseRepository;
import com.ddalkkak.date.repository.CourseRequestRepository;
import com.ddalkkak.date.service.recommend.CourseRecommender;
import com.ddalkkak.date.service.recommend.RecommendedCourse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CourseGenerationService {

  public record GeneratedCourses(CourseRequest request, List<Course> courses) {}

  private final CourseRecommender recommender;
  private final CourseRequestRepository courseRequestRepository;
  private final CourseRepository courseRepository;
  private final TransactionTemplate transactionTemplate;

  public CourseGenerationService(
      CourseRecommender recommender,
      CourseRequestRepository courseRequestRepository,
      CourseRepository courseRepository,
      TransactionTemplate transactionTemplate) {
    this.recommender = recommender;
    this.courseRequestRepository = courseRequestRepository;
    this.courseRepository = courseRepository;
    this.transactionTemplate = transactionTemplate;
  }

  // 추천(외부 API 호출로 수 초 걸릴 수 있음)은 트랜잭션 밖에서 하고, 저장만 한 트랜잭션으로 묶는다
  public GeneratedCourses generate(CourseCondition condition, Long userId, String guestId) {
    long startedAt = System.nanoTime();
    List<RecommendedCourse> recommended = recommender.recommend(condition);
    int responseTimeMs = (int) ((System.nanoTime() - startedAt) / 1_000_000);

    // DB의 TIMESTAMP 컬럼은 UTC 기준으로 저장한다
    LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
    return transactionTemplate.execute(
        status -> {
          CourseRequest request =
              courseRequestRepository.save(
                  new CourseRequest(userId, guestId, condition, now, responseTimeMs));
          List<Course> courses =
              courseRepository.saveAll(
                  recommended.stream()
                      .map(
                          course ->
                              new Course(
                                  request,
                                  course.title(),
                                  course.places(),
                                  course.totalCost(),
                                  course.totalTime()))
                      .toList());
          return new GeneratedCourses(request, courses);
        });
  }
}
