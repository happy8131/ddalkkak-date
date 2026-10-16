package com.ddalkkak.date.common.validation;

import com.ddalkkak.date.domain.Labeled;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class OneOfLabelsValidator implements ConstraintValidator<OneOfLabels, String> {

  private Set<String> labels;

  @Override
  public void initialize(OneOfLabels annotation) {
    labels =
        Arrays.stream(annotation.value().getEnumConstants())
            .map(Labeled::label)
            .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || labels.contains(value);
  }
}
