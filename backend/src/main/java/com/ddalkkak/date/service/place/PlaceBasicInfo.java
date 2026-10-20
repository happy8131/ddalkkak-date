package com.ddalkkak.date.service.place;

import com.ddalkkak.date.domain.Region;
import java.math.BigDecimal;

// 카카오 Local API에서 수집한 장소의 기본 정보. AI 큐레이션의 입력이기도 하다
public record PlaceBasicInfo(
    String kakaoPlaceId,
    String name,
    String category,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String placeUrl,
    Region region,
    boolean alcohol) {}
