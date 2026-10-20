package com.ddalkkak.date.service.place;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// AI 큐레이터가 장소 이름·카테고리·위치로 추정한 결과.
// priceRange는 화면에 보여줄 문자열("30,000-50,000원")이고, places.price_range(정수)에는 estimatedCost()를 쓴다
public record CurationResult(
    int dateScore,
    List<String> moodTags,
    String priceRange,
    String bestTime,
    String recommendation) {

  private static final Pattern AMOUNT = Pattern.compile("\\d[\\d,]*");

  // 가격대 문자열에 든 금액들의 중간값(원). 금액이 하나면 그 값, 없으면 0
  public int estimatedCost() {
    Matcher matcher = AMOUNT.matcher(priceRange);
    List<Integer> amounts = new ArrayList<>();
    while (matcher.find()) {
      amounts.add(Integer.parseInt(matcher.group().replace(",", "")));
    }
    if (amounts.isEmpty()) {
      return 0;
    }
    int min = amounts.stream().mapToInt(Integer::intValue).min().getAsInt();
    int max = amounts.stream().mapToInt(Integer::intValue).max().getAsInt();
    return (min + max) / 2;
  }
}
