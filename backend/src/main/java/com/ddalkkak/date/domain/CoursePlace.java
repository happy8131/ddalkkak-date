package com.ddalkkak.date.domain;

// 코스에 포함된 장소. courses.places_json에 비정규화해서 저장한다
public record CoursePlace(
    String placeId,
    String name,
    String category,
    String address,
    int estimatedCost,
    String estimatedTime,
    String kakaoPlaceUrl,
    Double trustScore) {}
