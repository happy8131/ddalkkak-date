package com.ddalkkak.date.common.validation;

import com.ddalkkak.date.domain.Labeled;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 문자열이 지정한 enum의 한글 표기 중 하나인지 검사한다 (null은 @NotNull/@NotBlank에 맡긴다)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OneOfLabelsValidator.class)
public @interface OneOfLabels {

  Class<? extends Labeled> value();

  String message() default "지원하지 않는 값입니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
