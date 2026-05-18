package com.enterprise.ai.spring.registry;

import org.springframework.util.StringUtils;

import java.util.List;

public class EmbeddedRuntimeService {

    private final RuntimeGovernanceGuard governanceGuard;

    private final List<EmbeddedRuntimeExecutor> executors;

    public EmbeddedRuntimeService(RuntimeGovernanceGuard governanceGuard,
                                  List<EmbeddedRuntimeExecutor> executors) {
        this.governanceGuard = governanceGuard;
        this.executors = executors == null ? List.of() : List.copyOf(executors);
    }

    public EmbeddedRuntimeResult execute(EmbeddedRuntimeRequest request) {
        if (request == null || !StringUtils.hasText(request.agentKey())) {
            throw new IllegalArgumentException("agentKey 不能为空");
        }
        governanceGuard.assertEmbeddedExecutionAllowed();
        EmbeddedRuntimeExecutor executor = executors.stream()
                .filter(e -> e.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("没有可用的 EmbeddedRuntimeExecutor: " + request.agentKey()));
        return executor.execute(request);
    }

    public boolean available(EmbeddedRuntimeRequest request) {
        return governanceGuard.embeddedExecutionAllowed()
                && request != null
                && StringUtils.hasText(request.agentKey())
                && executors.stream().anyMatch(e -> e.supports(request));
    }
}
