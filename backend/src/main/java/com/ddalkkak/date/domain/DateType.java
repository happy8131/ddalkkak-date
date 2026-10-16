package com.ddalkkak.date.domain;

import java.util.Arrays;

// 데이트 유형 (PRD FR-2.1)
public enum DateType implements Labeled {
  CULTURE("문화데이트"),
  FOOD("맛집투어"),
  ACTIVITY("액티비티"),
  MOOD("감성데이트");

  private final String label;

  DateType(String label) {
    this.label = label;
  }

  @Override
  public String label() {
    return label;
  }

  public static DateType fromLabel(String label) {
    return Arrays.stream(values())
        .filter(type -> type.label.equals(label))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 데이트 유형: " + label));
  }
}
