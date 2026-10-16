package com.ddalkkak.date.domain;

// 코스 생성 조건 (검증을 통과한 값만 담는다)
public record CourseCondition(Region region, DateType dateType, int budget) {}
