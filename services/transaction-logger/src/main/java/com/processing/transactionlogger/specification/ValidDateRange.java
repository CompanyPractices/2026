package com.processing.transactionlogger.specification;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "dateFrom must not be after dateTo";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
