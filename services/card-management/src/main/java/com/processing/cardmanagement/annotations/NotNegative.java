package com.processing.cardmanagement.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Min;

import java.lang.annotation.*;

@Min(0)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface NotNegative {

    String message() default "Value can not be negative";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
