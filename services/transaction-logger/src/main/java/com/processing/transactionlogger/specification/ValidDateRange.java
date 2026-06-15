package com.processing.transactionlogger.specification;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Проверяет, что {@code dateFrom} не позже {@code dateTo} в {@link TransactionFilter}.
 * Если одно из полей не задано — считается валидным.
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "dateFrom must not be after dateTo";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
