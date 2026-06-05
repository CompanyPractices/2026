package com.processing.cardmanagement.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

@Pattern(regexp = "^\\d*$")
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface DigitsOnly {

    String message() default "Value can contain digits only";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
