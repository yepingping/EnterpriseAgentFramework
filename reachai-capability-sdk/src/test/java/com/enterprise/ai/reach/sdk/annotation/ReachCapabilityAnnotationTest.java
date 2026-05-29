package com.enterprise.ai.reach.sdk.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachCapabilityAnnotationTest {

    static class ContractApi {
        @ReachCapability(
                name = "contract.query",
                title = "查询合同",
                description = "根据合同编号查询合同详情",
                domain = "contract",
                module = "review",
                sideEffect = ReachSideEffectLevel.READ,
                requiredRoles = {"contract_reader"})
        String query(@ReachParam(description = "合同编号", required = true, example = "HT-2026-0001") String contractNo) {
            return contractNo;
        }
    }

    @Test
    void reachCapabilityIsRuntimeMethodAnnotation() throws Exception {
        Retention retention = ReachCapability.class.getAnnotation(Retention.class);
        Target target = ReachCapability.class.getAnnotation(Target.class);

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertTrue(Arrays.asList(target.value()).contains(ElementType.METHOD));

        Method method = ContractApi.class.getDeclaredMethod("query", String.class);
        ReachCapability capability = method.getAnnotation(ReachCapability.class);
        assertEquals("contract.query", capability.name());
        assertEquals("查询合同", capability.title());
        assertEquals(ReachSideEffectLevel.READ, capability.sideEffect());
        assertEquals("contract_reader", capability.requiredRoles()[0]);
    }

    @Test
    void reachParamIsRuntimeParameterAndFieldAnnotationWithoutRecordComponentTarget() {
        Retention retention = ReachParam.class.getAnnotation(Retention.class);
        Target target = ReachParam.class.getAnnotation(Target.class);

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertTrue(Arrays.asList(target.value()).contains(ElementType.PARAMETER));
        assertTrue(Arrays.asList(target.value()).contains(ElementType.FIELD));
    }
}
