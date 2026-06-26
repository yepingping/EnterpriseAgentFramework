package com.enterprise.ai.agent.context.memory;

/**
 * Runtime user subject resolved from embed session/token identity.
 */
public record RuntimeUserIdentity(
        String tenantId,
        String userId,
        String globalUserId,
        String externalUserId,
        String projectCode
) {
}
