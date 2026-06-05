package com.processing.specification;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, TransactionFilter> {
    @Override
    public boolean isValid(TransactionFilter filter, ConstraintValidatorContext context) {
        if (filter.getFrom() == null || filter.getTo() == null) {
            return true;
        }
        return !filter.getFrom().isAfter(filter.getTo());
    }
}
