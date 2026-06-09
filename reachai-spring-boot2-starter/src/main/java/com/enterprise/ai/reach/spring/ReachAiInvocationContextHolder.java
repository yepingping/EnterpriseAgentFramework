package com.enterprise.ai.reach.spring;

public final class ReachAiInvocationContextHolder {

    private static final ThreadLocal<ReachAiInvocationContext> CURRENT = new ThreadLocal<ReachAiInvocationContext>();

    private ReachAiInvocationContextHolder() {
    }

    public static ReachAiInvocationContext get() {
        return CURRENT.get();
    }

    public static ReachAiInvocationContext getRequired() {
        ReachAiInvocationContext context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("ReachAI invocation context is not available");
        }
        return context;
    }

    static void set(ReachAiInvocationContext context) {
        CURRENT.set(context);
    }

    static void clear() {
        CURRENT.remove();
    }
}
