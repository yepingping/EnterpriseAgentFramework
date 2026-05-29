package com.enterprise.ai.reach.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReachParam {

    String name() default "";

    String description() default "";

    boolean required() default false;

    String example() default "";

    String sourceHint() default "";

    String dictType() default "";

    boolean sensitive() default false;
}
