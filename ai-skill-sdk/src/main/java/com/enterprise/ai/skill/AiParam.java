package com.enterprise.ai.skill;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 Agent 能力入参或 DTO 字段的业务语义。
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface AiParam {

    /** 参数业务说明；扫描时优先写入参数 description。 */
    String description() default "";

    /** 是否必填；false 表示不覆盖原有扫描推断。 */
    boolean required() default false;

    /** 示例值，供 Studio 调试和 AI 语义理解使用。 */
    String example() default "";

    /** 参数通常来自哪个上游 Tool 出参。 */
    String sourceHint() default "";

    /** 字典类型，如 dept / user / customer。 */
    String dictType() default "";

    /** 是否敏感字段。 */
    boolean sensitive() default false;
}
