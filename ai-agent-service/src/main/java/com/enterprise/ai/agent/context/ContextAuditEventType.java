package com.enterprise.ai.agent.context;

public enum ContextAuditEventType {
    CREATE,
    UPDATE,
    READ,
    SEARCH,
    INJECT,
    DELETE,
    REVOKE,
    EXPIRE,
    VERIFY,
    MARK_STALE,
    CANDIDATE_CREATE,
    CANDIDATE_UPDATE,
    CANDIDATE_APPROVE,
    CANDIDATE_REJECT,
    CANDIDATE_DELETE,
    CANDIDATE_EXPIRE,
    LIFECYCLE_RUN
}
