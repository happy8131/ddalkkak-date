package com.ddalkkak.date.domain;

import java.util.Arrays;

// 초기 지원 지역: 서울 5대 상권 (PRD FR-2.1)
public enum Region implements Labeled {
  HONGDAE("홍대"),
  GANGNAM("강남"),
  SEONGSU("성수"),
  YEONNAM("연남"),
  ITAEWON("이태원");

  private final String label;

  Region(String label) {
    this.label = label;
  }

  @Override
  public String label() {
    return label;
  }

  public static Region fromLabel(String label) {
    return Arrays.stream(values())
        .filter(region -> region.label.equals(label))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 지역: " + label));
  }
}
