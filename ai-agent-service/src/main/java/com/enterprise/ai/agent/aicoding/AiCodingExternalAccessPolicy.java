package com.enterprise.ai.agent.aicoding;

import com.enterprise.ai.agent.workflow.aicoding.WorkflowAiCodingPaths;

/**
 * Central allow-list for external AI coding tool requests that should bypass platform login.
 * Business controllers still validate the project-level aiCodingKey before doing work.
 */
public final class AiCodingExternalAccessPolicy {

    private AiCodingExternalAccessPolicy() {
    }

    public static boolean matchesRequest(String method, String path) {
        if (method == null || path == null) {
            return false;
        }
        if (WorkflowAiCodingPaths.matchesRequestPath(path)) {
            return true;
        }
        if (AiCodingGatewayPaths.matchesRequest(method, path)) {
            return true;
        }
        return false;
    }
}
