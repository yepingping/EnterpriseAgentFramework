package com.enterprise.ai.reach.spring;

public interface ReachAiSecurityContextBridge {

    Object runWith(ReachAiInvocationContext context, Invocation invocation) throws Exception;

    interface Invocation {
        Object proceed() throws Exception;
    }
}
