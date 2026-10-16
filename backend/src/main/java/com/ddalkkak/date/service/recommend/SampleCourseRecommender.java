package com.ddalkkak.date.service.recommend;

import com.ddalkkak.date.domain.CourseCondition;
import com.ddalkkak.date.domain.CoursePlace;
import com.ddalkkak.date.domain.DateType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

// AI 추천 엔진(KAN-14)이 생기기 전까지 API 흐름을 끝까지 확인하기 위한 임시 구현.
// 실제 장소가 아니므로 제목에 [샘플]을 붙이고 place_id는 sample-로 시작한다.
// KAN-14에서 실제 CourseRecommender 빈을 추가하면 이 클래스는 삭제한다.
@Component
public class SampleCourseRecommender implements CourseRecommender {

  private record Slot(String category, String name, double hours, int weight) {}

  private record Template(String title, List<Slot> slots) {}

  private static final Map<DateType, List<Template>> TEMPLATES =
      Map.of(
          DateType.CULTURE,
          List.of(
              new Template(
                  "전시 산책 코스",
                  List.of(
                      new Slot("전시", "소규모 갤러리", 1.5, 2),
                      new Slot("카페", "디저트 카페", 1, 2),
                      new Slot("식당", "파스타 레스토랑", 1.5, 4))),
              new Template(
                  "책과 음악 코스",
                  List.of(
                      new Slot("서점", "독립 서점", 1, 1),
                      new Slot("카페", "LP 카페", 1, 2),
                      new Slot("공연", "소극장 공연", 2, 4),
                      new Slot("식당", "한식 다이닝", 1.5, 4))),
              new Template(
                  "뮤지엄 데이트 코스",
                  List.of(
                      new Slot("박물관", "테마 뮤지엄", 2, 2),
                      new Slot("카페", "브런치 카페", 1, 2),
                      new Slot("식당", "비스트로", 1.5, 4)))),
          DateType.FOOD,
          List.of(
              new Template(
                  "로컬 맛집 투어",
                  List.of(
                      new Slot("식당", "줄 서는 국밥집", 1, 3),
                      new Slot("디저트", "수제 디저트 가게", 1, 2),
                      new Slot("식당", "이자카야", 1.5, 4))),
              new Template(
                  "브런치부터 디너까지",
                  List.of(
                      new Slot("식당", "브런치 레스토랑", 1, 3),
                      new Slot("카페", "로스터리 카페", 1, 1),
                      new Slot("디저트", "베이커리", 0.5, 1),
                      new Slot("식당", "스테이크 하우스", 1.5, 5))),
              new Template(
                  "골목 미식 코스",
                  List.of(
                      new Slot("식당", "수제버거 가게", 1, 3),
                      new Slot("디저트", "젤라토 가게", 0.5, 1),
                      new Slot("식당", "타파스 바", 1.5, 4)))),
          DateType.ACTIVITY,
          List.of(
              new Template(
                  "방탈출 챌린지 코스",
                  List.of(
                      new Slot("방탈출", "테마 방탈출 카페", 1.5, 3),
                      new Slot("식당", "분식 맛집", 1, 2),
                      new Slot("카페", "보드게임 카페", 1.5, 2))),
              new Template(
                  "스포츠 데이트 코스",
                  List.of(
                      new Slot("볼링", "볼링장", 1.5, 2),
                      new Slot("식당", "치킨 맛집", 1, 3),
                      new Slot("스크린골프", "스크린골프장", 1.5, 4))),
              new Template(
                  "체험 공방 코스",
                  List.of(
                      new Slot("공방", "반지 공방", 2, 4),
                      new Slot("카페", "디저트 카페", 1, 1),
                      new Slot("오락실", "레트로 오락실", 1, 1),
                      new Slot("식당", "피자 레스토랑", 1, 3)))),
          DateType.MOOD,
          List.of(
              new Template(
                  "루프탑 야경 코스",
                  List.of(
                      new Slot("카페", "루프탑 카페", 1, 2),
                      new Slot("식당", "이탈리안 다이닝", 1.5, 5),
                      new Slot("산책", "야경 산책로", 1, 0))),
              new Template(
                  "와인 한 잔 코스",
                  List.of(
                      new Slot("식당", "프렌치 비스트로", 1.5, 5),
                      new Slot("와인바", "내추럴 와인바", 1.5, 4),
                      new Slot("디저트", "디저트 카페", 1, 1))),
              new Template(
                  "감성 사진 코스",
                  List.of(
                      new Slot("사진", "셀프 사진관", 0.5, 1),
                      new Slot("카페", "플라워 카페", 1, 2),
                      new Slot("산책", "골목 산책", 1, 0),
                      new Slot("식당", "캔들 다이닝", 1.5, 5)))));

  @Override
  public List<RecommendedCourse> recommend(CourseCondition condition) {
    List<Template> templates = TEMPLATES.get(condition.dateType());
    List<RecommendedCourse> courses = new ArrayList<>();
    for (int i = 0; i < templates.size(); i++) {
      courses.add(toCourse(condition, templates.get(i), i));
    }
    return courses;
  }

  private RecommendedCourse toCourse(CourseCondition condition, Template template, int index) {
    String region = condition.region().label();
    List<Slot> slots = template.slots();
    int[] costs = splitBudget(condition.budget(), slots);

    List<CoursePlace> places = new ArrayList<>();
    double totalHours = 0;
    for (int i = 0; i < slots.size(); i++) {
      Slot slot = slots.get(i);
      totalHours += slot.hours();
      places.add(
          new CoursePlace(
              "sample-%s-%d-%d".formatted(condition.dateType().name().toLowerCase(), index, i),
              region + " " + slot.name(),
              slot.category(),
              "서울 " + region + " 일대",
              costs[i],
              formatHours(slot.hours()),
              null,
              null));
    }
    return new RecommendedCourse(
        "[샘플] " + region + " " + template.title(),
        places,
        condition.budget(),
        formatHours(totalHours));
  }

  // 가중치대로 1,000원 단위로 나누고 남는 금액은 마지막 유료 장소에 더해 합계를 예산과 맞춘다
  private static int[] splitBudget(int budget, List<Slot> slots) {
    int totalWeight = slots.stream().mapToInt(Slot::weight).sum();
    int[] costs = new int[slots.size()];
    int assigned = 0;
    int lastPaid = 0;
    for (int i = 0; i < slots.size(); i++) {
      costs[i] = budget * slots.get(i).weight() / totalWeight / 1000 * 1000;
      assigned += costs[i];
      if (slots.get(i).weight() > 0) {
        lastPaid = i;
      }
    }
    costs[lastPaid] += budget - assigned;
    return costs;
  }

  private static String formatHours(double hours) {
    return BigDecimal.valueOf(hours).stripTrailingZeros().toPlainString() + "시간";
  }
}
