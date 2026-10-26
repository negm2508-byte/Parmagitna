package com.papererp.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller endpoint as an idempotent operation.
 * Reads the Idempotency-Key HTTP header (or request body key) and interacts with IdempotencyService.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IdempotentOperation {
    String headerName() default "Idempotency-Key";
}
