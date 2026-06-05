package com.processing.common.dto.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.OverridesAttribute;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Size
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ExactSize {

    @OverridesAttribute(constraint = Size.class, name = "min")
    @OverridesAttribute(constraint = Size.class, name = "max")
    int value();

    String message() default "The length must be exactly {value} symbols";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
