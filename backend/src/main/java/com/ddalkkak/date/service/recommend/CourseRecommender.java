package com.ddalkkak.date.service.recommend;

import com.ddalkkak.date.domain.CourseCondition;
import java.util.List;

// 조건에 맞는 코스 3개를 만든다. 실제 AI 추천 엔진은 KAN-14에서 구현한다
public interface CourseRecommender {

  List<RecommendedCourse> recommend(CourseCondition condition);
}
