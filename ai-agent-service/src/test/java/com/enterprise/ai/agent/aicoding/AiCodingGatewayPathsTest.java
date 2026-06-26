package com.enterprise.ai.agent.aicoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCodingGatewayPathsTest {

    @Test
    void matchesProjectManifestDiscoveryEndpoint() {
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/manifest"));
    }

    @Test
    void matchesSdkAccessGatewayAliases() {
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/onboarding-manifest"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/agents/provision"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/access-sessions"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/access-sessions/latest"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/access-sessions/rai_demo/steps/gateway-whitelist/report"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/access-sessions/rai_demo/checks/run"));
        assertFalse(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/agents/provision"));
        assertFalse(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/access-sessions"));
    }

    @Test
    void matchesPageAssistantGatewayAliases() {
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/page-assistant/onboarding-manifest"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/page-assistant/sessions"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/page-assistant/sessions"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/page-assistant/sessions/latest"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/page-assistant/pages/register"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/page-assistant/sessions/rai_page/steps/page-scan/report"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/page-assistant/sessions/rai_page/workflow-ai-coding-result"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "DELETE",
                "/api/ai-coding/projects/7/page-assistant/sessions/rai_page/workflow-ai-coding-result"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "PUT",
                "/api/ai-coding/projects/7/page-assistant/sessions/rai_page/target"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/page-assistant/sessions/rai_page/catalog/sync"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/page-assistant/sessions/rai_page/checks/run"));
        assertFalse(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/page-assistant/pages/register"));
    }

    @Test
    void matchesContextCandidateCollectionOnlyForReadAndCreate() {
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/context-candidates"));
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/context-candidates"));
        assertFalse(AiCodingGatewayPaths.matchesRequest(
                "DELETE",
                "/api/ai-coding/projects/7/context-candidates"));
        assertFalse(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/context-candidates/99"));
    }

    @Test
    void matchesContextCandidateBatchCreateEndpoint() {
        assertTrue(AiCodingGatewayPaths.matchesRequest(
                "POST",
                "/api/ai-coding/projects/7/context-candidates/batch"));
        assertFalse(AiCodingGatewayPaths.matchesRequest(
                "GET",
                "/api/ai-coding/projects/7/context-candidates/batch"));
    }
}
