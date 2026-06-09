package com.enterprise.ai.agent.identity;

public record PageCatalogRegisterResult(
        String projectCode,
        String appId,
        String pageKey,
        int actionCount
) {
}
