package com.ddalkkak.date.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Swagger 문서 전용 타입. 실제 응답은 ApiResponse<CourseGenerateResponse>이고,
// 제네릭이라 @Content에 스키마를 직접 지정할 수 없어서 같은 모양의 문서용 레코드를 둔다
@Schema(description = "코스 생성 성공 응답 (ApiResponse 형식)")
public record CourseGenerateApiResponse(
    @Schema(description = "성공 여부. 항상 true", example = "true") boolean success,
    @Schema(description = "코스 생성 결과") CourseGenerateResponse data) {}
