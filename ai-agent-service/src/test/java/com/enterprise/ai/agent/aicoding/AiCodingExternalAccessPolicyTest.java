package com.enterprise.ai.agent.aicoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCodingExternalAccessPolicyTest {

    @Test
    void aiAssistSdkOnboardingManifestNoLongerBypassesPlatformLogin() {
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/ai-assist/projects/1/onboarding-manifest"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/ai-assist/projects/1/onboarding-manifest"));
    }

    @Test
    void aiAssistAccessSessionReportAndChecksNoLongerBypassPlatformLogin() {
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/access-sessions/rai_demo/steps/gateway-whitelist/report"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/access-sessions/rai_demo/checks/run"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/access-sessions/rai_demo/checks/run"));
    }

    @Test
    void aiAssistPageAssistantExternalEndpointsNoLongerBypassPlatformLogin() {
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/ai-assist/projects/1/page-assistant/onboarding-manifest"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/page-assistant/pages/register"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/page-assistant/sessions/rai_page/catalog/sync"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/page-assistant/pages/register"));
    }

    @Test
    void workflowAiCodingRequestsBypassPlatformLoginEvenWithoutCapturedKey() {
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/workflows/wf-1/ai-coding/context"));
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/workflows/ai-coding/workflows"));
    }

    @Test
    void genericAiCodingGatewayRequestsBypassPlatformLoginEvenWithoutCapturedKey() {
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/manifest"));
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/onboarding-manifest"));
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/agents/provision"));
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/access-sessions"));
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/page-assistant/onboarding-manifest"));
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/page-assistant/pages/register"));
        assertTrue(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/context-candidates/batch"));
    }

    @Test
    void adjacentPathsDoNotMatchExternalAiCodingPolicy() {
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/workflows/studio/generate-draft"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/workflows/wf-1/ai-coding-extra/context"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "DELETE",
                "/api/ai-coding/projects/7/context-candidates"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/page-assistant/sessions/rai_page/unknown"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/access-sessions/rai_demo/steps/gateway-whitelist/extra/report"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "GET",
                "/api/ai-assist/projects/1/page-assistant/unknown/sessions"));
        assertFalse(AiCodingExternalAccessPolicy.matchesRequest(
                "POST",
                "/api/ai-assist/projects/1/page-assistant/sessions/rai_page/unknown/workflow-ai-coding-result"));
    }
}
