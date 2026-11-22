package com.benzaazaa.codsettlement.settlement.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Class-level Bean Validation constraint: {@code periodEnd} must not be before {@code periodStart}. */
@Documented
@Constraint(validatedBy = ValidPeriodValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPeriod {

    String message() default "must be on or after periodStart";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
