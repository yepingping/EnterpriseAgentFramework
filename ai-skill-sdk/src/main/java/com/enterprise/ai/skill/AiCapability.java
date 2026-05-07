package com.enterprise.ai.skill;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明一个 Java 方法可作为 Agent 能力被扫描、治理和编排。
 * <p>
 * 第一阶段由源码扫描器消费；运行时直连 Java 方法不在本注解职责内。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiCapability {

    /** 能力唯一名；为空时由扫描器从方法名或路径生成。 */
    String name() default "";

    /** 面向运营和 Studio 的业务标题。 */
    String title() default "";

    /** 面向 Agent/LLM 的业务描述；优先级高于 JavaDoc / Swagger。 */
    String description() default "";

    /** 业务领域，如 finance / hr / crm。 */
    String domain() default "";

    /** 业务模块，如 customer / contract。 */
    String module() default "";

    /** 业务标签，用于检索、筛选和 Studio 展示。 */
    String[] tags() default {};

    /** 副作用等级；UNKNOWN 场景可保持 WRITE 由运营后续校准。 */
    SideEffectLevel sideEffect() default SideEffectLevel.WRITE;

    /** 是否默认对 Agent 可见。 */
    boolean agentVisible() default true;

    /** 权限建议；后续可生成 Tool ACL 草稿。 */
    String[] requiredRoles() default {};

    /** 调用超时建议，0 表示不覆盖平台默认值。 */
    int timeoutMs() default 0;

    /** 重试次数建议，-1 表示不覆盖平台默认值。 */
    int retryLimit() default -1;
}
