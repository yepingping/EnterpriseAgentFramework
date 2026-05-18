package com.enterprise.ai.spring.registry;

public interface EmbeddedRuntimeExecutor {

    boolean supports(EmbeddedRuntimeRequest request);

    EmbeddedRuntimeResult execute(EmbeddedRuntimeRequest request);
}
