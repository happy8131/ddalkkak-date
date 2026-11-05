package com.ddalkkak.date.controller.dto;

import com.ddalkkak.date.common.validation.OneOfLabels;
import com.ddalkkak.date.domain.CourseCondition;
import com.ddalkkak.date.domain.DateType;
import com.ddalkkak.date.domain.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// PRD FR-2.1 입력 검증
@Schema(description = "데이트 코스 생성 요청")
public record CourseGenerateRequest(
    @Schema(
            description = "데이트 지역 (서울 5대 상권)",
            allowableValues = {"홍대", "강남", "성수", "연남", "이태원"},
            example = "홍대")
        @NotBlank
        @OneOfLabels(value = Region.class, message = "지원하지 않는 지역입니다.")
        String region,
    @Schema(
            description = "데이트 유형",
            allowableValues = {"문화데이트", "맛집투어", "액티비티", "감성데이트"},
            example = "문화데이트")
        @NotBlank
        @OneOfLabels(value = DateType.class, message = "지원하지 않는 데이트 유형입니다.")
        String dateType,
    @Schema(description = "코스 전체 총예산(원). 30,000 이상 300,000 이하", example = "100000")
        @NotNull
        @Min(value = 30_000, message = "30,000원 이상이어야 합니다.")
        @Max(value = 300_000, message = "300,000원 이하여야 합니다.")
        Integer budget) {

  public CourseCondition toCondition() {
    return new CourseCondition(Region.fromLabel(region), DateType.fromLabel(dateType), budget);
  }
}
