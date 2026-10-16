package com.ddalkkak.date.service.recommend;

import com.ddalkkak.date.domain.CoursePlace;
import java.util.List;

public record RecommendedCourse(
    String title, List<CoursePlace> places, int totalCost, String totalTime) {}
