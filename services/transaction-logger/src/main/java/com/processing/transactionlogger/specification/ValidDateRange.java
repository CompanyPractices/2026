package com.processing.transactionlogger.specification;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Проверяет, что начало диапазона не позже его конца.
 * Применяется к фильтрам с диапазонами дат.
 * Если одно из полей не задано — считается валидным.
 */
@Documented
@Constraint(validatedBy = {DateRangeValidator.class, ChartRangeValidator.class})
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "dateFrom must not be after dateTo";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
