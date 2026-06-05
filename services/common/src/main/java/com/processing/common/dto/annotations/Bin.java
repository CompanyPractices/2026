package com.processing.common.dto.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.*;

@Pattern(regexp = "^\\d{6}$")
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface Bin {

    String message() default "BIN number must contain exactly 6 digits";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
