package com.ddalkkak.date.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddalkkak.date.domain.CourseCondition;
import com.ddalkkak.date.domain.CoursePlace;
import com.ddalkkak.date.domain.DateType;
import com.ddalkkak.date.domain.Region;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SampleCourseRecommenderTest {

  private final SampleCourseRecommender recommender = new SampleCourseRecommender();

  static Stream<Arguments> conditions() {
    return Stream.of(DateType.values())
        .flatMap(
            type ->
                Stream.of(30_000, 100_000, 170_000, 300_000)
                    .map(budget -> Arguments.of(type, budget)));
  }

  @ParameterizedTest
  @MethodSource("conditions")
  void 코스_3개를_3에서_4곳으로_예산에_맞춰_만든다(DateType type, int budget) {
    List<RecommendedCourse> courses =
        recommender.recommend(new CourseCondition(Region.SEONGSU, type, budget));

    assertThat(courses).hasSize(3);
    for (RecommendedCourse course : courses) {
      assertThat(course.places()).hasSizeBetween(3, 4);
      int sum = course.places().stream().mapToInt(CoursePlace::estimatedCost).sum();
      assertThat(sum).isEqualTo(course.totalCost());
      assertThat(course.totalCost()).isBetween(budget * 95 / 100, budget);
      assertThat(course.places()).allSatisfy(place -> assertThat(place.estimatedCost()).isNotNegative());
      assertThat(course.title()).startsWith("[샘플] 성수");
    }
  }
}
