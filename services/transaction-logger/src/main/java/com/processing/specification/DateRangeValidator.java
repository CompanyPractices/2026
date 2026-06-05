package com.processing.specification;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, TransactionFilter> {
    @Override
    public boolean isValid(TransactionFilter filter, ConstraintValidatorContext context) {
        if (filter.getDateFrom() == null || filter.getDateTo() == null) {
            return true;
        }
        return !filter.getDateFrom().isAfter(filter.getDateTo());
    }
}
