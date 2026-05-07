package com.enterprise.ai.skill;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 Agent 能力出参字段的业务语义和可复用来源。
 */
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface AiOutput {

    /** 输出字段业务说明；扫描时优先写入 RESPONSE 字段 description。 */
    String description() default "";

    /** 企业业务对象键，如 customer.id / contract.contractId。 */
    String businessKey() default "";

    /** 建议可作为哪些下游入参的来源。 */
    String[] canBeSourceFor() default {};

    /** 是否敏感字段。 */
    boolean sensitive() default false;
}
