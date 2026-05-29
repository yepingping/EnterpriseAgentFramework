package com.enterprise.ai.reach.sdk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReachOutput {

    String description() default "";

    String example() default "";

    boolean sensitive() default false;
}
