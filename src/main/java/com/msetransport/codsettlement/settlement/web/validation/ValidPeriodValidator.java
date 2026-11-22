package com.msetransport.codsettlement.settlement.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPeriodValidator implements ConstraintValidator<ValidPeriod, HasPeriod> {

    @Override
    public boolean isValid(HasPeriod value, ConstraintValidatorContext context) {
        if (value == null || value.periodStart() == null || value.periodEnd() == null) {
            return true; // @NotNull on the fields reports missing values
        }
        if (!value.periodEnd().isBefore(value.periodStart())) {
            return true;
        }
        // Attach the error to the periodEnd field rather than to the whole object.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("periodEnd")
                .addConstraintViolation();
        return false;
    }
}
