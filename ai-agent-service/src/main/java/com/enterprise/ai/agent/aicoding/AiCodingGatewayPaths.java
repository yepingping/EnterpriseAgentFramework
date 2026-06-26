package com.enterprise.ai.agent.aicoding;

import java.util.regex.Pattern;

/**
 * Path matcher for generic AI Coding Gateway endpoints shared by external tools.
 */
public final class AiCodingGatewayPaths {

    private static final Pattern PROJECT_MANIFEST_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/manifest$");

    private static final Pattern SDK_ONBOARDING_MANIFEST_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/onboarding-manifest$");

    private static final Pattern SDK_AGENT_PROVISION_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/agents/provision$");

    private static final Pattern SDK_ACCESS_SESSIONS_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/access-sessions$");

    private static final Pattern SDK_ACCESS_SESSION_LATEST_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/access-sessions/latest$");

    private static final Pattern SDK_ACCESS_SESSION_STEP_REPORT_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/access-sessions/[^/]+/steps/[^/]+/report$");

    private static final Pattern SDK_ACCESS_SESSION_CHECKS_RUN_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/access-sessions/[^/]+/checks/run$");

    private static final Pattern PAGE_ASSISTANT_MANIFEST_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/onboarding-manifest$");

    private static final Pattern PAGE_ASSISTANT_SESSIONS_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/sessions$");

    private static final Pattern PAGE_ASSISTANT_LATEST_SESSION_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/sessions/latest$");

    private static final Pattern PAGE_ASSISTANT_PAGES_REGISTER_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/pages/register$");

    private static final Pattern PAGE_ASSISTANT_STEP_REPORT_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/sessions/[^/]+/steps/[^/]+/report$");

    private static final Pattern PAGE_ASSISTANT_WORKFLOW_RESULT_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/sessions/[^/]+/workflow-ai-coding-result$");

    private static final Pattern PAGE_ASSISTANT_TARGET_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/sessions/[^/]+/target$");

    private static final Pattern PAGE_ASSISTANT_CATALOG_SYNC_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/sessions/[^/]+/catalog/sync$");

    private static final Pattern PAGE_ASSISTANT_CHECKS_RUN_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/page-assistant/sessions/[^/]+/checks/run$");

    private static final Pattern CONTEXT_CANDIDATES_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/context-candidates$");

    private static final Pattern CONTEXT_CANDIDATES_BATCH_PATH = Pattern.compile(
            "^/api/ai-coding/projects/[^/]+/context-candidates/batch$");

    private AiCodingGatewayPaths() {
    }

    public static boolean matchesRequest(String method, String path) {
        if (method == null || path == null) {
            return false;
        }
        if ("GET".equalsIgnoreCase(method) && PROJECT_MANIFEST_PATH.matcher(path).matches()) {
            return true;
        }
        if (matchesSdkAccessRequest(method, path) || matchesPageAssistantRequest(method, path)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && CONTEXT_CANDIDATES_BATCH_PATH.matcher(path).matches()) {
            return true;
        }
        return CONTEXT_CANDIDATES_PATH.matcher(path).matches()
                && ("GET".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method));
    }

    private static boolean matchesSdkAccessRequest(String method, String path) {
        return matches(method, "GET", SDK_ONBOARDING_MANIFEST_PATH, path)
                || matches(method, "POST", SDK_AGENT_PROVISION_PATH, path)
                || matches(method, "POST", SDK_ACCESS_SESSIONS_PATH, path)
                || matches(method, "GET", SDK_ACCESS_SESSION_LATEST_PATH, path)
                || matches(method, "POST", SDK_ACCESS_SESSION_STEP_REPORT_PATH, path)
                || matches(method, "POST", SDK_ACCESS_SESSION_CHECKS_RUN_PATH, path);
    }

    private static boolean matchesPageAssistantRequest(String method, String path) {
        return matches(method, "GET", PAGE_ASSISTANT_MANIFEST_PATH, path)
                || matches(method, "GET", PAGE_ASSISTANT_SESSIONS_PATH, path)
                || matches(method, "POST", PAGE_ASSISTANT_SESSIONS_PATH, path)
                || matches(method, "GET", PAGE_ASSISTANT_LATEST_SESSION_PATH, path)
                || matches(method, "POST", PAGE_ASSISTANT_PAGES_REGISTER_PATH, path)
                || matches(method, "POST", PAGE_ASSISTANT_STEP_REPORT_PATH, path)
                || matches(method, "POST", PAGE_ASSISTANT_WORKFLOW_RESULT_PATH, path)
                || matches(method, "DELETE", PAGE_ASSISTANT_WORKFLOW_RESULT_PATH, path)
                || matches(method, "PUT", PAGE_ASSISTANT_TARGET_PATH, path)
                || matches(method, "POST", PAGE_ASSISTANT_CATALOG_SYNC_PATH, path)
                || matches(method, "POST", PAGE_ASSISTANT_CHECKS_RUN_PATH, path);
    }

    private static boolean matches(String actualMethod, String expectedMethod, Pattern pathPattern, String path) {
        return expectedMethod.equalsIgnoreCase(actualMethod) && pathPattern.matcher(path).matches();
    }
}
