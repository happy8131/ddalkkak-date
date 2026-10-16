package com.ddalkkak.date.controller.dto;

import com.ddalkkak.date.common.validation.OneOfLabels;
import com.ddalkkak.date.domain.CourseCondition;
import com.ddalkkak.date.domain.DateType;
import com.ddalkkak.date.domain.Region;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// PRD FR-2.1 입력 검증
public record CourseGenerateRequest(
    @NotBlank @OneOfLabels(value = Region.class, message = "지원하지 않는 지역입니다.") String region,
    @NotBlank @OneOfLabels(value = DateType.class, message = "지원하지 않는 데이트 유형입니다.")
        String dateType,
    @NotNull
        @Min(value = 30_000, message = "30,000원 이상이어야 합니다.")
        @Max(value = 300_000, message = "300,000원 이하여야 합니다.")
        Integer budget) {

  public CourseCondition toCondition() {
    return new CourseCondition(Region.fromLabel(region), DateType.fromLabel(dateType), budget);
  }
}
