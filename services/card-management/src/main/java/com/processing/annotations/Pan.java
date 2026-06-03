package com.processing.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

@Pattern(regexp = "^\\d{16}$")
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface Pan {

    String message() default "PAN number must contain exactly 16 symbols";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
